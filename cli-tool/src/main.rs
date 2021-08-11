use std::env::{args, Args};
use std::fs::File;
use std::io::{BufReader, BufWriter, Read, Seek, Write};
use std::path::Path;
use std::result::Result;

use diffy::{apply_all_bytes, ApplyOptions, DiffOptions, Patch, PatchFormatter};
use itertools::Itertools;
use rayon::prelude::*;
use zip::{ZipArchive, ZipWriter};

use select::Selector;

use crate::doing_error::*;
use crate::ext::*;
use crate::patching_env::{parse_pathing_env, PatchingEnv};

mod doing_error;
mod ext;
mod patching_env;
mod select;
mod types;

macro_rules! execution {
    ($expr: expr, $args: expr => |$name: ident| $els: expr) => {
        match $expr {
            "add-modify" => return command_add_modify(),
            "apply-patches" => return command_apply_patches(),
            "create-diff" => return command_create_diff(),
            "reformat-yaml" => return command_reformat_yaml($args),
            "--help" | "help" => return command_help(),
            $name => $els,
        }
    };
}

fn match_until(x: char) -> impl FnMut(char) -> bool {
    let mut finished = false;
    return move |c| {
        if finished {
            false
        } else if c == x {
            finished = true;
            true
        } else {
            true
        }
    };
}

#[test]
fn match_until_test() {
    let mut f = match_until('.');
    assert_eq!(f('a'), true);
    assert_eq!(f('.'), true);
    assert_eq!(f('a'), false);
    assert_eq!("a.b".trim_start_matches(match_until('.')), "b")
}

fn main() -> Result<(), Box<dyn std::error::Error>> {
    let mut args = args();
    let my_name = args.next().expect("no executable name");
    let my_name = Path::new(my_name.as_str())
        .file_name()
        .unwrap()
        .to_str()
        .unwrap()
        .trim_end_matches(".exe")
        .trim_start_matches(match_until('.'));
    let _finalize = AppFinalizeHandler;

    execution!(my_name, args => |_name| ());

    execution!(args.next().expect("no executable name").as_str(), args => |name| {
        panic!("unknown execution: {}", name)
    })
}

struct AppFinalizeHandler;

impl Drop for AppFinalizeHandler {
    fn drop(&mut self) {
        if error_caused_but_details_not_enabled() {
            eprintln!("'PATCHING_ERR_DETAILS=1' to more error details.");
            eprintln!("please do not report issue with a log without 'PATCHING_ERR_DETAILS=1'");
        }
    }
}

// 0: disabled, some error caused
// 1: enabled, some error caused
// 2: uninitialized, no error caused
static PATCHING_ERR_DETAILS_ENABLED: std::sync::atomic::AtomicU8 = std::sync::atomic::AtomicU8::new(2);

fn error_caused_but_details_not_enabled() -> bool {
    use std::sync::atomic::Ordering;
    return PATCHING_ERR_DETAILS_ENABLED.load(Ordering::Acquire) == 0;
}

fn patching_err_details() -> bool {
    use std::sync::atomic::Ordering;
    match PATCHING_ERR_DETAILS_ENABLED.load(Ordering::Relaxed) {
        0 => return false,
        1 => return true,
        2 => {
            let stat = match std::env::var_os("PATCHING_ERR_DETAILS") {
                None => false,
                Some(ref v) if v == "false" || v == "0" => false,
                Some(_) => true,
            };
            PATCHING_ERR_DETAILS_ENABLED.store(if stat { 1 } else { 0 }, Ordering::Release);
            stat
        }
        _ => panic!("invalid PATCHING_ERR_DETAILS_ENABLED")
    }
}

macro_rules! handle_block {
    ($err: ty: $blk: expr) => {
        (|| -> Result<(), $err> {
            $blk
            Ok(())
        })()
    };
    ($blk: expr) => {
        (|| -> Result<(), Box<dyn std::error::Error>> {
            $blk
            Ok(())
        })()
    };
}

macro_rules! handle_err {
    ($val: expr => |$i: ident| $blk: expr) => {
        match $val {
            Ok(v) => v,
            Err($i) => {
                if patching_err_details() {
                    eprintln!("error details: {:?}", &$i);
                }
                return $blk
            },
        }
    };
    ($val: expr => $blk: expr) => {
        match $val {
            Ok(v) => v,
            Err(err) => {
                if patching_err_details() {
                    eprintln!("error details: {:?}", &err);
                }
                return $blk
            },
        }
    };
}

macro_rules! take_if {
    (($expr: expr) if $cond: expr) => {
        if $cond {
            Some($expr)
        } else {
            None
        }
    };
}

fn command_help() -> Result<(), Box<dyn std::error::Error>> {
    eprintln!("mod-patching-cli-tool");
    eprintln!("(c) 2021 anatawa12 and other contributors");
    eprintln!();
    eprintln!("SUBCOMMANDS: ");
    eprintln!("    add-modify       add classes to modify");
    eprintln!("    apply-patches    applies patches to source code.");
    eprintln!("                     this drops all existing source.");
    eprintln!("    create-diff      creates patch file to commit");
    eprintln!("    help             show this message");
    Ok(())
}

fn command_reformat_yaml(mut args: Args) -> Result<(), Box<dyn std::error::Error>> {
    use std::fs::OpenOptions;
    use std::io::SeekFrom;
    use yaml_rust::{YamlEmitter, YamlLoader};

    let file_path = args.next().expect("yaml file name expected");
    let mut file = OpenOptions::new().read(true).write(true).open(file_path)?;

    // read
    let body = {
        let mut reader = BufReader::new(&mut file);
        let mut read = String::new();
        reader.read_to_string(&mut read)?;
        YamlLoader::load_from_str(&read)?
    };

    // reset
    file.set_len(0)?;
    file.seek(SeekFrom::Start(0))?;

    // write
    {
        use std::fmt::Write as FmtWrite;
        use std::{fmt, io};
        type BoxedErr = Box<dyn std::error::Error>;

        // bridge between fmt::write and io::Write
        // with keeping error by io::Write
        struct FmtToIoWriter<W> {
            writer: W,
            err: Option<BoxedErr>,
        }

        impl<W> fmt::Write for FmtToIoWriter<W>
            where
                W: io::Write,
        {
            fn write_str(&mut self, s: &str) -> fmt::Result {
                match self.writer.write_all(s.as_bytes()) {
                    Ok(_) => Ok(()),
                    Err(err) => {
                        self.err = Some(err.into());
                        Err(fmt::Error)
                    }
                }
            }
        }

        fn check_err<V, E, W>(err: Result<V, E>, w: &mut FmtToIoWriter<W>) -> Result<V, BoxedErr>
            where
                E: std::error::Error + 'static,
        {
            w.err
                .take()
                .map(Err)
                .unwrap_or_else(|| err.map_err(Into::into))
        }

        let mut writer = FmtToIoWriter {
            writer: BufWriter::new(&mut file),
            err: None,
        };
        for x in &body {
            check_err(YamlEmitter::new(&mut writer).dump(x), &mut writer)?;
            check_err(writer.write_char('\n'), &mut writer)?;
        }
        writer.writer.flush()?;
    }
    Ok(())
}

fn command_add_modify() -> Result<(), Box<dyn std::error::Error>> {
    let mut env = parse_pathing_env()?;

    struct ZipArchiveClass(usize, String);
    impl AsRef<str> for ZipArchiveClass {
        fn as_ref(&self) -> &str {
            self.1.as_ref()
        }
    }

    let jars = env.source_jars()?;
    let unmodified_classes = jars
        .iter()
        .enumerate()
        .flat_map(|(i, (_, x))| x.file_names().map(move |f| (i, f)))
        .filter_map(|x| x.1.strip_suffix(".java").map(|v| (x.0, v)))
        .map(|x| ZipArchiveClass(x.0, x.1.replace('/', ".")))
        .filter(|x| !env.is_modified(&x.1))
        .collect::<Vec<_>>();

    let i = Selector::new(unmodified_classes.as_slice())
        .unwrap()
        .with_candidate_limit(20)
        .interact()?;
    let ZipArchiveClass(i, modify_class) = unmodified_classes.into_iter().nth(i).unwrap();
    let (mod_name, mut source_jar) = jars.into_iter().nth(i).unwrap();
    let mod_name = mod_name.to_owned();

    println!("you'll modify {}", modify_class);
    let internal_name = modify_class.replace(".", "/");
    let relative_java_path = format!("{}.java", internal_name);
    let source_file_path = env.get_source_path(&mod_name).join(&relative_java_path);
    let patch_file_path = env
        .get_patch_path(&mod_name)
        .join(format!("{}.patch", relative_java_path));

    // 1. update config file
    {
        env.add_modified_class(&mod_name, modify_class);
        env.save().doing("saving config file")?;
    }

    // 1. copy source file
    handle_block!({
        let mut source_entry = source_jar.by_name(&relative_java_path)?.buf_read();
        let mut source_file = create_file_with_dir(&source_file_path)?.buf_write();
        std::io::copy(&mut source_entry, &mut source_file)?;
        source_file.flush()?;
    })
    .doing("copying source code from decompiled jar")?;

    // 2. create empty patch file
    handle_block!({
        let mut patch_file = create_file_with_dir(patch_file_path)?.buf_write();
        writeln!(patch_file, "--- a/{}", &relative_java_path)?;
        writeln!(patch_file, "+++ b/{}", &relative_java_path)?;
        patch_file.flush()?;
    })
    .doing("creating empty patch file")?;

    // 3. remake unmodified classes jar
    remake_unmodified_classes_jar(&env, &mod_name).doing("creating unmodified classes jar file")?;

    Ok(())
}

fn command_apply_patches() -> Result<(), Box<dyn std::error::Error>> {
    let env = parse_pathing_env()?;

    env.mod_names()
        .into_iter()
        .par_bridge()
        .map(|mod_name| apply_patches(&env, mod_name))
        .collect::<()>();

    Ok(())
}

fn command_create_diff() -> Result<(), Box<dyn std::error::Error>> {
    let env = parse_pathing_env()?;

    env.mod_names()
        .into_iter()
        .par_bridge()
        .map(|mod_name| create_diff(&env, mod_name))
        .collect::<()>();

    Ok(())
}

fn apply_patches(env: &PatchingEnv, mod_name: impl AsRef<str>) {
    let mod_name = mod_name.as_ref();

    let formatter = PatchFormatter::new();
    let mut file_names = Vec::new();
    let mut sources_jar = handle_err!(File::open(env.get_source_jar_path(mod_name))
        .map_err(Into::into)
        .and_then(|x| ZipArchive::new(x.buf_read())) => {
        eprintln!("ERROR: no source jar found for {}! broken workspace or not prepared!", mod_name);
    });
    let patch_root = env.get_patch_path(mod_name);
    let source_root = env.get_source_path(mod_name);

    env.get_modified_classes(mod_name)
        .filter_map(|class_name| {
            let file_name = format!("{}.java", class_name.replace(".", "/"));
            file_names.push(file_name.clone());

            let src = handle_err!(sources_jar
                .by_name(&file_name)
                .and_then(|x| read_to_vec(x.size() as usize, x).map_err(Into::into)) => {
                eprintln!("WARN: no source found for {}! broken workspace!", class_name);
                None
            });
            Some((class_name, file_name, src))
        })
        .par_bridge()
        .map(|(class_name, file_name, src)| {
            apply_patch_for(
                &formatter,
                class_name,
                &file_name,
                &src,
                &source_root,
                &patch_root,
            )
        })
        .collect::<()>();

    remake_unmodified_classes_jar(&env, &mod_name)
        .doing("creating unmodified classes jar file")
        .unwrap();
}

fn read_to_vec(capacity: usize, read: impl Read) -> std::io::Result<Vec<u8>> {
    let mut vec = Vec::with_capacity(capacity);
    read.buf_read().read_to_end(&mut vec)?;
    Ok(vec)
}

fn apply_patch_for(
    formatter: &PatchFormatter,
    class_name: &str,
    file_name: &str,
    src: &[u8],
    source_root: &Path,
    patch_root: &Path,
) {
    let out_path = source_root.join(&file_name);
    let patch_path = patch_root.join(&format!("{}.patch", &file_name));

    let patch_size = std::fs::metadata(&patch_path)
        .map(|x| x.len())
        .unwrap_or(32);
    let patch_file = handle_err!(File::open(&patch_path).and_then(|x| read_to_vec(patch_size as usize, x)) => {
        eprintln!("WARN: no valid patch found for {}! broken workspace!", class_name);
        write_slice_file(&src, out_path).ok();
    });
    let patch_file = handle_err!(Patch::from_bytes(&patch_file) => {
        eprintln!("parsing patch for {} failed!", class_name);
        write_slice_file(&src, out_path).ok();
    });
    let (applied, failed) = apply_all_bytes(src, &patch_file, ApplyOptions::new());
    write_slice_file(&applied, out_path).ok();
    if !failed.is_empty() {
        eprintln!(
            "hunks {hunks} failed for patch {class}",
            hunks = failed.iter().map(|x| format!("#{}", *x + 1)).join(", "),
            class = &class_name,
        );
        let rej_path = source_root.join(&format!("{}.rej", &file_name));
        let rej_file = handle_err!(create_file_with_dir(rej_path) => {
            eprintln!("crating .rej for {} failed!", class_name);
        });

        let hunks = patch_file
            .hunks()
            .iter()
            .enumerate()
            .filter_map(|(i, x)| take_if!((x) if failed.contains(&i)));
        handle_err!(write_rej_file(formatter, rej_file.buf_write(), hunks) => {
            eprintln!("crating .rej for {} failed!", class_name);
        });
    }
}

fn write_rej_file<'a>(
    formatter: &PatchFormatter,
    mut out: impl Write,
    hunks: impl Iterator<Item = &'a diffy::Hunk<'a, [u8]>>,
) -> std::io::Result<()> {
    for x in hunks {
        formatter.write_hunk_into(&x, &mut out)?;
    }
    Ok(())
}

fn create_file_with_dir(to: impl AsRef<Path>) -> std::io::Result<File> {
    let to = to.as_ref();
    std::fs::create_dir_all(to.parent().unwrap())?;
    File::create(to)
}

fn write_slice_file(source: &[u8], to: impl AsRef<Path>) -> std::io::Result<()> {
    let mut write = create_file_with_dir(to)?.buf_write();
    write.write_all(source)?;
    write.flush()
}

fn remake_unmodified_classes_jar(env: &PatchingEnv, mod_name: &str) -> std::io::Result<()> {
    let deobf_jar_path = env.get_deobf_jar_path(mod_name);
    let unmodified_classes_jar_path = env.get_unmodified_classes_jar(mod_name);

    let mut archive = ZipArchive::new(std::fs::File::open(deobf_jar_path)?.buf_read())?;
    let mut writer =
        ZipWriter::new(create_file_with_dir(&unmodified_classes_jar_path)?.buf_write());
    for i in 0..archive.len() {
        let mut file = archive.by_index(i)?;
        if file.name().ends_with(".class") && !env.is_modified_class_name(file.name()) {
            writer.start_file(file.name(), zip::write::FileOptions::default())?;
            std::io::copy(&mut file, &mut writer)?;
        }
    }
    writer.finish()?.flush()?;
    Ok(())
}

fn create_diff(env: &PatchingEnv, mod_name: impl AsRef<str>) {
    let mod_name = mod_name.as_ref();

    let formatter = PatchFormatter::new().with_space_on_empty_line();
    let mut file_names = Vec::new();
    let mut sources_jar = handle_err!(File::open(env.get_source_jar_path(mod_name)).map_err(Into::into)
        .and_then(|x| ZipArchive::new(x.buf_read())) => {
        eprintln!("ERROR: no source jar found for {}! broken workspace or not prepared!", mod_name);
    });
    let patch_root = env.get_patch_path(mod_name);
    let source_root = env.get_source_path(mod_name);

    env.get_modified_classes(mod_name)
        .filter_map(|class_name| {
            let file_name = format!("{}.java", class_name.replace(".", "/"));
            file_names.push(file_name.clone());

            let src = handle_err!(sources_jar
                .by_name(&file_name)
                .and_then(|x| read_to_vec(x.size() as usize, x).map_err(Into::into)) => {
                eprintln!("WARN: no source found for {}! broken workspace!", class_name);
                None
            });
            Some((class_name, file_name, src))
        })
        .par_bridge()
        .map(|(class_name, file_name, src)| {
            create_diff_for(
                &formatter,
                class_name,
                &file_name,
                &src,
                &source_root,
                &patch_root,
            )
        })
        .collect::<()>();
}

fn create_diff_for(
    formatter: &PatchFormatter,
    class_name: &str,
    file_name: &str,
    src: &[u8],
    source_root: &Path,
    patch_root: &Path,
) {
    let java_path = source_root.join(&file_name);
    let patch_path = patch_root.join(&format!("{}.patch", &file_name));

    let patch_size = std::fs::metadata(&patch_path)
        .map(|x| x.len())
        .unwrap_or(32);

    let java_file = handle_err!(File::open(&java_path).and_then(|x| read_to_vec(patch_size as usize, x)) => {
        eprintln!("ERROR: no source code found for {}! broken workspace!", class_name);
    });

    let patch = DiffOptions::default()
        .set_context_len(5)
        .create_patch_bytes(src, &java_file)
        .with_original(format!("a/{}", file_name).into_bytes())
        .with_modified(format!("b/{}", file_name).into_bytes());

    let mut patch_file = handle_err!(create_file_with_dir(patch_path) => |e| {
        eprintln!("ERROR: can't create patch file for {}: {}", class_name, e);
    })
    .buf_write();

    handle_err!(formatter.write_patch_into(&patch, &mut patch_file).and_then(|_| patch_file.flush()) => |e| {
        eprintln!("ERROR: writing patch file for {}: {}", class_name, e);
    });
}
