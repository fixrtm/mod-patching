use rayon::iter::{FromParallelIterator, IntoParallelIterator};
use rayon::prelude::*;

pub(crate) struct AndCollector(pub(crate) bool);

impl FromParallelIterator<bool> for AndCollector {
    fn from_par_iter<I>(par_iter: I) -> Self
        where
            I: IntoParallelIterator<Item=bool>,
    {
        AndCollector(par_iter.into_par_iter().reduce(|| true, |a, b| a && b))
    }
}
