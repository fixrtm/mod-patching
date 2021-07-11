mod doing_error;
mod ext;
mod patching_env;
mod select;

use crate::doing_error::*;
use crate::ext::*;
use crate::patching_env::{parse_pathing_env, PatchingEnv};
use select::Selector;
use std::env::args;
use std::io::Write;
use std::result::Result;
use zip::{ZipArchive, ZipWriter};

fn main() -> Result<(), Box<dyn std::error::Error>> {
    let mut args = args();
    args.next();
    match args.next().expect("no execution type").as_str() {
        "add-modify" => command_add_modify()?,
        name => panic!("unknown execution: {}", name),
    }

    Ok(())
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
        std::fs::create_dir_all(source_file_path.parent().unwrap())?;
        let mut source_file = std::fs::File::create(&source_file_path)?.buf_write();
        std::io::copy(&mut source_entry, &mut source_file)?;
        source_file.flush()?;
    })
    .doing("copying source code from decompiled jar")?;

    // 2. create empty patch file
    handle_block!({
        std::fs::create_dir_all(patch_file_path.parent().unwrap())?;
        let mut patch_file = std::fs::File::create(patch_file_path)?.buf_write();
        writeln!(patch_file, "--- a/{}", &relative_java_path)?;
        writeln!(patch_file, "+++ b/{}", &relative_java_path)?;
        patch_file.flush()?;
    })
    .doing("creating empty patch file")?;

    // 3. remake unmodified classes jar
    remake_unmodified_classes_jar(&env, &mod_name).doing("creating unmodified classes jar file")?;

    Ok(())
}

fn remake_unmodified_classes_jar(env: &PatchingEnv, mod_name: &str) -> std::io::Result<()> {
    let deobf_jar_path = env.get_deobf_jar_path(mod_name);
    let unmodified_classes_jar_path = env.get_unmodified_classes_jar(mod_name);

    let mut archive = ZipArchive::new(std::fs::File::open(deobf_jar_path)?.buf_read())?;
    let mut writer =
        ZipWriter::new(std::fs::File::create(&unmodified_classes_jar_path)?.buf_write());
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
