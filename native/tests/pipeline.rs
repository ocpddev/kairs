use kairs::sbert::SentenceTransformer;

#[test]
fn simple() {
    let t = SentenceTransformer::preset_default().unwrap();
    let e = t.embed(vec!["Hello world"]).unwrap();
    println!("{}", e);
}

#[test]
fn sentence() {
    let t = SentenceTransformer::preset_default().unwrap();
    let e = t
        .embed(vec![
            "I am a sentence for which I would like to get its embedding.",
        ])
        .unwrap();
    println!("{}", e);
}

#[test]
fn pair() {
    let t = SentenceTransformer::preset_default().unwrap();
    let e = t
        .embed(vec![
            "Hello world",
            "I am a sentence for which I would like to get its embedding.",
        ])
        .unwrap();
    println!("{}", e);
}

#[test]
fn long() {
    let t = SentenceTransformer::preset_default().unwrap();
    let e = t.embed(vec![r#"The iPhone's Settings hold a treasure trove of features that can boost your device's security and efficiency, but finding them can be like looking for a needle in a haystack due to the overwhelming number of options. However, there's a shortcut for those ready to tap into their phone's hidden powers. Dive into the Settings, select "Accessibility," choose "Touch," and then scroll down to discover "Back Tap" at the bottom. This feature opens up a world of possibilities, allowing you to assign actions to "Double Tap" and "Triple Tap" on your iPhone's back, making it easier than ever to use your device to its fullest potential."#]).unwrap();
    println!("{}", e);
}
