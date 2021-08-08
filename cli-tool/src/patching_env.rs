use std::collections::{BTreeMap, BTreeSet};
use std::env::current_dir;
use std::fs::File;
use std::path::{Path, PathBuf};

use quick_error::quick_error;
use rayon::prelude::*;
use serde::{Deserialize, Serialize};
use serde::de::DeserializeOwned;
use serde_yaml::{from_reader, to_writer};
use zip::ZipArchive;

use crate::ext::*;
use crate::types::{RelativePathFromCacheRoot, RelativePathFromProjectRoot};

#[derive(Debug)]
pub struct PatchingEnv {
    root: PathBuf,
    local: LocalConfig,
    main: PatchingMainConfig,
}

impl PatchingEnv {
    pub fn source_jars(&self) -> Result<Vec<(&str, ZipArchive<File>)>, PatchingEnvError> {
        self.main
            .mods
            .iter()
            .par_bridge()
            .map(|(n, m)| {
                let path = self.local.cache_base.join(&m.source_jar);
                File::open(&path)
                    .at_fn(|| path.display().to_string())
                    .and_then(|f| ZipArchive::new(f).at_fn(|| path.display().to_string()))
                    .map(|a| (n.as_str(), a))
            })
            .collect()
    }

    pub fn is_modified(&self, name: impl AsRef<str>) -> bool {
        let str = name.as_ref();
        println!("test for {}", str);
        outer_class_names(str).any(|x| {
            println!(">test for {}", x);
            self.main
                .mods
                .values()
                .any(|m| m.changed_classes.contains(x))
        })
    }

    pub fn is_modified_class_name(&self, name: impl AsRef<str>) -> bool {
        let class_name = name.as_ref().trim_end_matches(".class").replace("/", ".");
        self.is_modified(class_name)
    }

    pub fn save(&self) -> Result<(), PatchingEnvError> {
        write_yaml(&self.root, &MAIN_YAML_PATH, &self.main)?;
        write_yaml(&self.root, &LOCAL_YAML_PATH, &self.local)?;
        Ok(())
    }

    pub fn mod_names(&self) -> impl Iterator<Item = &str> {
        self.main.mods.keys().map(|x| x.as_str())
    }

    fn get_mod(&self, name: impl AsRef<str>) -> &ModInfo {
        self.main.mods.get(name.as_ref()).unwrap()
    }

    //noinspection RsSelfConvention
    fn get_mod_mut(&mut self, name: impl AsRef<str>) -> &mut ModInfo {
        self.main.mods.get_mut(name.as_ref()).unwrap()
    }

    pub fn add_modified_class(&mut self, name: impl AsRef<str>, class: String) {
        self.get_mod_mut(name).changed_classes.insert(class);
    }

    pub fn get_modified_classes(&self, name: impl AsRef<str>) -> impl Iterator<Item = &str> {
        self.get_mod(name)
            .changed_classes
            .iter()
            .map(String::as_str)
    }

    pub fn get_source_path(&self, name: impl AsRef<str>) -> PathBuf {
        self.root.join(&self.get_mod(name).source_path)
    }

    pub fn get_patch_path(&self, name: impl AsRef<str>) -> PathBuf {
        self.root.join(&self.get_mod(name).patch_path)
    }

    pub fn get_unmodified_classes_jar(&self, name: impl AsRef<str>) -> PathBuf {
        self.root.join(&self.get_mod(name).unmodifieds_jar)
    }

    pub fn get_deobf_jar_path(&self, name: impl AsRef<str>) -> PathBuf {
        self.local.cache_base.join(&self.get_mod(name).deobf_jar)
    }

    pub fn get_source_jar_path(&self, name: impl AsRef<str>) -> PathBuf {
        self.local.cache_base.join(&self.get_mod(name).source_jar)
    }
}

// the class name will be returned
struct OuterClassNames<'a>(Option<&'a str>);

impl<'a> Iterator for OuterClassNames<'a> {
    type Item = &'a str;

    fn next(&mut self) -> Option<Self::Item> {
        if let Some(str) = self.0 {
            let dot = str.rfind('.').unwrap_or(0);
            if let Some(dollar) = str.rfind('$') {
                if dot < dollar {
                    // abc.def$ghi -> abc.def
                    // safety: rfind returns valid index
                    self.0 = Some(unsafe { str.get_unchecked(0..dollar) })
                } else {
                    // abc$def.ghi -> None
                    self.0 = None
                }
            } else {
                // abc.def -> None
                self.0 = None
            }

            Some(str)
        } else {
            None
        }
    }
}

fn outer_class_names<'a>(name: &'a str) -> OuterClassNames<'a> {
    OuterClassNames(Some(name))
}

#[derive(Debug, Serialize, Deserialize)]
struct LocalConfig {
    #[serde(rename = "cache-base")]
    cache_base: PathBuf,
}

#[derive(Debug, Serialize, Deserialize)]
struct PatchingMainConfig {
    mods: BTreeMap<String, ModInfo>,
}

//noinspection SpellCheckingInspection
#[derive(Debug, Serialize, Deserialize)]
struct ModInfo {
    // relative from pathing mod root
    #[serde(rename = "patch-path")]
    patch_path: RelativePathFromProjectRoot,
    #[serde(rename = "source-path")]
    source_path: RelativePathFromProjectRoot,
    #[serde(rename = "unmodifieds-jar")]
    unmodifieds_jar: RelativePathFromProjectRoot,

    // relative from pathing cache root
    #[serde(rename = "source-jar")]
    source_jar: RelativePathFromCacheRoot,
    #[serde(rename = "deobf-jar")]
    deobf_jar: RelativePathFromCacheRoot,

    // list of class names
    #[serde(rename = "changed-classes")]
    changed_classes: BTreeSet<String>,
}

quick_error! {
    #[derive(Debug)]
    pub enum PatchingEnvError {
        Io(place: String, err: std::io::Error) {
            display("IO error at {}: {}", place, err)
            source(err)
        }
        YamlAt(place: &'static &'static str, err: serde_yaml::Error) {
            source(err)
            display(me) -> ("parsing error at {}: {}", place, err)
        }
        Zip(place: String, err: zip::result::ZipError) {
            source(err)
            display(me) -> ("parsing error at {}: {}", place, err)
        }
        RootReached {
            display("root directory reached, you must be in pathing mod project")
        }
    }
}

trait ErrorAtExt<T, E> {
    fn at(self, place: T) -> E;
}

trait ErrorAtFnExt<T, E> {
    fn at_fn<F: FnOnce() -> T>(self, place: F) -> E;
}

impl<Me: ErrorAtExt<T, E>, T, E, V> ErrorAtFnExt<T, Result<V, E>> for Result<V, Me> {
    fn at_fn<F: FnOnce() -> T>(self, place: F) -> Result<V, E> {
        self.map_err(|x| x.at(place()))
    }
}

macro_rules! error_at_str {
    ($me: ty, $err: ty) => {
        impl ErrorAtExt<&str, $err> for $me {
            fn at(self, place: &str) -> $err {
                <$me>::at(self, place.to_owned())
            }
        }
    };
}

impl<Me: ErrorAtExt<S, E>, T, S, E> ErrorAtExt<S, Result<T, E>> for Result<T, Me> {
    fn at(self, place: S) -> Result<T, E> {
        self.map_err(|x| x.at(place))
    }
}

error_at_str!(std::io::Error, PatchingEnvError);
impl ErrorAtExt<String, PatchingEnvError> for std::io::Error {
    fn at(self, place: String) -> PatchingEnvError {
        PatchingEnvError::Io(place, self)
    }
}

error_at_str!(zip::result::ZipError, PatchingEnvError);
impl ErrorAtExt<String, PatchingEnvError> for zip::result::ZipError {
    fn at(self, place: String) -> PatchingEnvError {
        PatchingEnvError::Zip(place, self)
    }
}

impl ErrorAtExt<&'static &'static str, PatchingEnvError> for serde_yaml::Error {
    fn at(self, place: &'static &'static str) -> PatchingEnvError {
        PatchingEnvError::YamlAt(place, self)
    }
}

pub fn parse_pathing_env() -> Result<PatchingEnv, PatchingEnvError> {
    let mut dir: &Path = &current_dir().map_err(|x| x.at("current dir"))?;
    loop {
        if dir.join(".patching-mods").exists() {
            return parse_files(dir.to_owned());
        }
        dir = dir.parent().ok_or(PatchingEnvError::RootReached)?;
    }
}

const MAIN_YAML_PATH: &str = ".patching-mods/main.yaml";
const LOCAL_YAML_PATH: &str = ".patching-mods/local.yaml";

fn parse_files(root: PathBuf) -> Result<PatchingEnv, PatchingEnvError> {
    let main = parse_yaml(&root, &MAIN_YAML_PATH)?;
    let local = parse_yaml(&root, &LOCAL_YAML_PATH)?;
    Ok(PatchingEnv { root, local, main })
}

fn parse_yaml<T: DeserializeOwned>(
    root: &Path,
    path: &'static &'static str,
) -> Result<T, PatchingEnvError> {
    from_reader(File::open(root.join(path)).at(*path)?).at(path)
}

fn write_yaml(
    root: &Path,
    path: &'static &'static str,
    value: &impl Serialize,
) -> Result<(), PatchingEnvError> {
    to_writer(File::create(root.join(*path)).at(*path)?.buf_write(), value).at(path)?;
    Ok(())
}
