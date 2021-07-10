use quick_error::quick_error;
use serde::{Deserialize, Serialize};
use serde_yaml::from_reader;
use std::collections::{BTreeMap, BTreeSet};
use std::env::current_dir;
use std::fs::File;
use std::path::{Path, PathBuf};

#[derive(Debug)]
pub struct PatchingEnv {
    pub local: LocalConfig,
    pub main: PatchingMainConfig,
}

#[derive(Debug, Serialize, Deserialize)]
pub struct LocalConfig {
    #[serde(rename = "cache-base")]
    pub cache_base: PathBuf,
}

#[derive(Debug, Serialize, Deserialize)]
pub struct PatchingMainConfig {
    pub mods: BTreeMap<String, ModInfo>,
}

#[derive(Debug, Serialize, Deserialize)]
pub struct ModInfo {
    #[serde(rename = "patch-path")]
    patch_path: PathBuf,
    #[serde(rename = "source-path")]
    source_path: PathBuf,
    #[serde(rename = "source-jar")]
    source_jar: PathBuf,
    #[serde(rename = "changed-classes")]
    changed_classes: BTreeSet<String>,
}

quick_error! {
    #[derive(Debug)]
    pub enum PatchingEnvError {
        Io(err: std::io::Error) {
            from()
            display("IO error: {}", err)
            source(err)
        }
        YamlAt { place: &'static str, err: serde_yaml::Error } {
            source(err)
            display(me) -> ("parsing error at {}: {}", place, err)
        }
        RootReached {
            display("root directory reached, you must be in pathing mod project")
        }
    }
}

pub fn parse_pathing_env() -> Result<PatchingEnv, PatchingEnvError> {
    let mut dir: &Path = &current_dir()?;
    loop {
        if dir.join(".patching-mods").exists() {
            return parse_files(dir.join(".patching-mods"));
        }
        dir = dir.parent().ok_or(PatchingEnvError::RootReached)?;
    }
}

fn parse_files(dir: PathBuf) -> Result<PatchingEnv, PatchingEnvError> {
    let main = from_reader(File::open(dir.join("main.yaml"))?)
        .map_err(|err| PatchingEnvError::YamlAt { place: "", err })?;
    let local = from_reader(File::open(dir.join("local.yaml"))?)
        .map_err(|err| PatchingEnvError::YamlAt { place: "", err })?;
    Ok(PatchingEnv { local, main })
}
