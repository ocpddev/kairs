use std::path::PathBuf;

use anyhow::Result;

use kairs::sbert::SentenceTransformer;

pub fn transformer(model: &str) -> Result<SentenceTransformer> {
    let mut path = PathBuf::from(env!("CARGO_MANIFEST_DIR"));
    path.push("models");
    path.push(model);
    SentenceTransformer::load(path)
}
