use std::fmt::Formatter;
use std::path::{Component, Path, PathBuf};

use serde::{Deserialize, Deserializer, Serialize, Serializer};
use serde::de;

macro_rules! relative_path_definition {
    ($name: ident) => {
        #[derive(Debug, PartialEq)]
        pub(crate) struct $name(PathBuf);

        impl<'de> Deserialize<'de> for $name {
            fn deserialize<D>(deserializer: D) -> Result<Self, D::Error>
            where
                D: Deserializer<'de>,
            {
                struct VisitorImpl;
                impl<'de> de::Visitor<'de> for VisitorImpl {
                    type Value = $name;

                    fn expecting(&self, formatter: &mut Formatter) -> std::fmt::Result {
                        formatter.write_str("slash separated relative path")
                    }

                    fn visit_str<E>(self, v: &str) -> Result<Self::Value, E>
                    where
                        E: de::Error,
                    {
                        if check_path_str(v) {
                            let path = PathBuf::from(v);
                            check_path(&path);
                            Ok($name(path))
                        } else {
                            Err(E::invalid_value(
                                de::Unexpected::Str(v),
                                &"slash separated relative path",
                            ))
                        }
                    }
                }

                deserializer.deserialize_str(VisitorImpl)
            }
        }

        impl Serialize for $name {
            fn serialize<S>(&self, serializer: S) -> Result<S::Ok, S::Error>
            where
                S: Serializer,
            {
                fn push_component(result: &mut String, c: Component) {
                    match c {
                        Component::ParentDir => result.push_str(".."),
                        Component::Normal(s) => result.push_str(&s.to_string_lossy()),

                        Component::CurDir => {}
                        Component::Prefix(_) => {}
                        Component::RootDir => {}
                    }
                }

                let mut result = String::with_capacity(self.0.as_os_str().len());
                let mut iter = self.0.components();
                push_component(&mut result, iter.next().unwrap());

                for x in iter {
                    result.push('/');
                    push_component(&mut result, x);
                }

                serializer.serialize_str(&result)
            }
        }

        impl AsRef<Path> for $name {
            fn as_ref(&self) -> &Path {
                &self.0
            }
        }
    };
}

relative_path_definition!(RelativePathFromCacheRoot);
relative_path_definition!(RelativePathFromProjectRoot);

fn check_path_str(path: &str) -> bool {
    if path == "" {
        return false;
    }
    for component in path.split('/') {
        if component == "" || component == "." {
            return false;
        }
        if component
            .as_bytes()
            .iter()
            .any(|it| b"<>:\"\\/|?:".contains(&it) && (b'\x00'..b'\x1f').contains(&it))
        {
            return false;
        }
    }
    return true;
}

fn check_path(path: &Path) -> bool {
    return path
        .components()
        .all(|x| matches!(x, Component::ParentDir | Component::Normal(_)));
}

#[cfg(test)]
mod test {
    use serde_test::*;

    use super::*;

    #[test]
    fn from_cache_root_serialize_deserialize() {
        assert_tokens(&RelativePathFromCacheRoot("curse/realtrainmod/2.4.22/realtrainmod-2.4.22-stable-39-deobf-1.5.498.12-sources.jar".into()), &[
            Token::Str("curse/realtrainmod/2.4.22/realtrainmod-2.4.22-stable-39-deobf-1.5.498.12-sources.jar"),
        ])
    }

    #[test]
    fn from_project_root_serialize_deserialize() {
        assert_tokens(
            &RelativePathFromProjectRoot(
                "build/patching-mod/mods/rtm/realtrainmod-2.4.22-unmodifieds.jar".into(),
            ),
            &[Token::Str(
                "build/patching-mod/mods/rtm/realtrainmod-2.4.22-unmodifieds.jar",
            )],
        )
    }
}
