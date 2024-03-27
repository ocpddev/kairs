use crate::sbert::SentenceTransformer;
use anyhow::Result;
use jni::objects::{JClass, JString};
use jni::sys::{jfloat, jlong};
use jni::JNIEnv;

/// Load a pre-trained transformer model from a directory.
/// Returns a pointer to the loaded model.
/// The caller is responsible for freeing the memory by calling `drop`.
#[no_mangle]
extern "system" fn Java_dev_ocpd_kairs_sbert_SentenceTransformerNative_load0(
    env: JNIEnv,
    _: JClass,
    path: JString,
) -> jlong {
    fn inner(mut env: JNIEnv, path: JString) -> Result<jlong> {
        let path = env.get_string(&path)?;
        let path = path.to_str()?;
        let transformer = SentenceTransformer::load(path)?;
        Ok(Box::into_raw(Box::new(transformer)) as jlong)
    }
    inner(env, path).unwrap_or_else(|e| {
        eprintln!("Failed to initialize: {:?}", e);
        0
    })
}

#[no_mangle]
extern "system" fn Java_dev_ocpd_kairs_sbert_SentenceTransformerNative_cosineSimilarity0(
    env: JNIEnv,
    _: JClass,
    this: jlong,
    a: JString,
    b: JString,
) -> jfloat {
    fn inner(mut env: JNIEnv, this: jlong, a: JString, b: JString) -> Result<f32> {
        let transformer = unsafe { &*(this as *const SentenceTransformer) };
        let a = env.get_string(&a)?;
        let a = a.to_str()?;
        let b = env.get_string(&b)?;
        let b = b.to_str()?;
        transformer.cos_sim((&a, &b))
    }
    inner(env, this, a, b).unwrap_or_else(|e| {
        eprintln!("Failed to calculate cosine similarity: {:?}", e);
        // return an abnormal value to indicate an error,
        // a normal result should always be positive.
        -1.0
    })
}

/// Free the memory of the model.
/// The pointer should not be used after calling this function.
#[no_mangle]
extern "system" fn Java_dev_ocpd_kairs_sbert_SentenceTransformerNative_drop0(
    _: JNIEnv,
    _: JClass,
    this: jlong,
) {
    unsafe {
        let _ = Box::from_raw(this as *mut SentenceTransformer);
    }
}
