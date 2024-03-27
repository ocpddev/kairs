use anyhow::anyhow;
use candle_core::{Device, Tensor};
use candle_nn::VarBuilder;
use candle_transformers::models::bert::{BertModel, Config, DTYPE};
use std::fs::File;
use std::path::Path;
use tokenizers::{PaddingStrategy, Tokenizer};

mod jni;

// No plan to support CUDA atm
const DEVICE: &Device = &Device::Cpu;

/// A sentence transformer pipeline composed of a pre-trained BERT model and a tokenizer.
pub struct SentenceTransformer {
    model: BertModel,
    tokenizer: Tokenizer,
}

impl SentenceTransformer {
    /// Load a pre-trained transformer model from a directory.
    /// The directory should contain the following files:
    /// - `config.json` (a JSON file containing the model configuration)
    /// - `tokenizer.json` (a JSON file containing the tokenizer configuration)
    /// - `pytorch_model.bin` (a binary file containing the model weights)
    pub fn load(path: impl AsRef<Path>) -> anyhow::Result<Self> {
        let path = path.as_ref();
        let config = File::open(path.join("config.json"))?;
        let config: Config = serde_json::from_reader(config)?;
        let mut tokenizer =
            Tokenizer::from_file(path.join("tokenizer.json")).map_err(|e| anyhow!(e))?;
        // The default padding strategy that comes with the tokenizer is unsuitable
        // for batch encoding as it always pads to a fixed length.
        // This will produce incorrect results because we don't have attention masks implemented.
        // Here, we change it to `PaddingStrategy::BatchLongest` to ensure that
        // all sentences in a batch are padded to the same length (the longest sentence).
        if let Some(p) = tokenizer.get_padding_mut() {
            p.strategy = PaddingStrategy::BatchLongest
        };
        let vb = VarBuilder::from_pth(path.join("pytorch_model.bin"), DTYPE, DEVICE)?;
        let model = BertModel::load(vb, &config)?;
        Ok(Self { model, tokenizer })
    }

    /// Generate embeddings for a list of sentences.
    ///
    /// Returns `[n_sentences][hidden_size]` => `[[f32; hidden_size]; n_sentences]`
    pub fn embed(&self, inputs: Vec<&str>) -> anyhow::Result<Tensor> {
        // Encode the input sentences into tokens
        let tokens = self
            .tokenizer
            .encode_batch(inputs, true)
            .map_err(|e| anyhow!(e))?;
        // Convert the tokens into tensor
        let token_ids = tokens
            .into_iter()
            .map(|tokens| Ok(Tensor::new(tokens.get_ids(), DEVICE)?))
            .collect::<anyhow::Result<Vec<_>>>()?;
        // [n_sentences][n_token_ids]
        let token_ids = Tensor::stack(&token_ids, 0)?;
        // `token_type_ids` will not be used in our use case
        let token_type_ids = token_ids.zeros_like()?;
        // Returns [n_sentences][n_tokens][hidden_size]
        let embeddings = self.model.forward(&token_ids, &token_type_ids)?;
        // Apply some avg-pooling by taking the mean embedding value
        // for all tokens (including padding)
        let (_n_sentence, n_tokens, _hidden_size) = embeddings.dims3()?;
        let embeddings = (embeddings.sum(1)? / (n_tokens as f64))?;
        let embeddings = normalize_l2(&embeddings)?;
        Ok(embeddings)
    }

    /// Calculate the cosine similarity between two sentences
    /// If performance is a concern and willing to sacrifice accuracy,
    /// consider using [`cos_sim_batch`] instead.
    ///
    /// [`cos_sim_batch`]: Self::cos_sim_batch
    pub fn cos_sim(&self, inputs: (&str, &str)) -> anyhow::Result<f32> {
        let a = self.embed(vec![inputs.0])?.get(0)?;
        let b = self.embed(vec![inputs.1])?.get(0)?;
        dot_score(&a, &b)
    }

    /// Calculate the cosine similarity between two sentences
    ///
    /// This method is more efficient than [`cos_sim`] when
    /// comparing multiple pairs of sentences (citation needed).
    ///
    /// [`cos_sim`]: Self::cos_sim
    ///
    /// CAUTION: this variant currently has accuracy issues.
    /// See: https://github.com/huggingface/candle/issues/1798
    pub fn cos_sim_batch(&self, inputs: (&str, &str)) -> anyhow::Result<f32> {
        let embeddings = self.embed(vec![inputs.0, inputs.1])?;
        dot_score(&embeddings.get(0)?, &embeddings.get(1)?)
    }
}

/// Calculate the dot product between two embeddings
///
/// This is equivalent to the cosine similarity between two embeddings
/// when both embeddings are normalized.
fn dot_score(a: &Tensor, b: &Tensor) -> anyhow::Result<f32> {
    Ok((a * b)?.sum_all()?.to_scalar()?)
}

/// Normalize a vector of embeddings to have unit length
///
/// Note: The input is **a vector of embeddings**, not a single embedding vector.
///       In other words, the input is a tensor of shape `[n_sentences][hidden_size]`.
fn normalize_l2(v: &Tensor) -> anyhow::Result<Tensor> {
    Ok(v.broadcast_div(&v.sqr()?.sum_keepdim(1)?.sqrt()?)?)
}
