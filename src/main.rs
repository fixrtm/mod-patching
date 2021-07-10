mod select;

use select::Selector;
use std::result::Result;

fn main() -> Result<(), Box<dyn std::error::Error>> {
    let items: &[_] = &["abc", "def"];

    let selected = Selector::new(items)
        .unwrap()
        .with_candidate_limit(20)
        .interact()?;
    println!("selected: {}", selected);

    Ok(())
}
