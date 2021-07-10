mod patching_env;
mod select;

use crate::patching_env::parse_pathing_env;
use select::Selector;
use std::result::Result;

fn main() -> Result<(), Box<dyn std::error::Error>> {
    println!("{:#?}", parse_pathing_env());

    let items: &[_] = &["abc", "def"];

    let selected = Selector::new(items)
        .unwrap()
        .with_candidate_limit(20)
        .interact()?;
    println!("selected: {}", selected);

    Ok(())
}
