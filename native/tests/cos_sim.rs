use approx::assert_relative_eq;

use crate::common::transformer;

mod common;

#[test]
fn exact() {
    let t = transformer("minilm").unwrap();
    let score = t.cos_sim(("Hello world", "Hello world")).unwrap();
    assert_relative_eq!(score, 1.0, epsilon = 1e-6);
    let score = t
        .cos_sim((
            "I am a sentence for which I would like to get its embedding.",
            "I am a sentence for which I would like to get its embedding.",
        ))
        .unwrap();
    assert_relative_eq!(score, 1.0, epsilon = 1e-6);
}

#[test]
fn similar() {
    let t = transformer("minilm").unwrap();
    let score = t
        .cos_sim((
            "This is a completely different text that is not similar to the other documents.",
            "This is another completely different text that is not similar to the other documents.",
        ))
        .unwrap();

    assert_relative_eq!(score, 0.9923702, epsilon = 1e-6);
}

#[test]
fn diff_length() {
    let t = transformer("minilm").unwrap();
    let score = t
        .cos_sim((
            "This is a completely different text that is not similar to the other documents.",
            "I am a sentence for which I would like to get its embedding.",
        ))
        .unwrap();

    assert_relative_eq!(score, 0.22324173, epsilon = 1e-6);
}

#[test]
fn long() {
    let t = transformer("minilm").unwrap();
    let score = t.cos_sim((
        r#"The iPhone's Settings hold a treasure trove of features that can boost your device's security and efficiency, but finding them can be like looking for a needle in a haystack due to the overwhelming number of options. However, there's a shortcut for those ready to tap into their phone's hidden powers. Dive into the Settings, select "Accessibility," choose "Touch," and then scroll down to discover "Back Tap" at the bottom. This feature opens up a world of possibilities, allowing you to assign actions to "Double Tap" and "Triple Tap" on your iPhone's back, making it easier than ever to use your device to its fullest potential."#,
        r#"Hidden within the iPhone's extensive Settings, a valuable feature awaits those seeking to enhance their device's security and functionality, often overshadowed by the sheer volume of options available. For users eager to unlock their iPhone's potential quickly, a simple journey into the Settings can reveal this secret. By heading to "Accessibility" and then "Touch," followed by a scroll to "Back Tap" at the menu's end, a new realm of customization unveils itself. This area offers "Double Tap" and "Triple Tap" options, empowering you to activate specific functions with just a few taps on the back of your iPhone."#,
    )).unwrap();
    assert_relative_eq!(score, 0.8881394, epsilon = 1e-6);
}

/// This tests the parity between the `cos_sim` and `cos_sim_batch` functions.
#[test]
fn diff_length_parity() {
    let t = transformer("minilm").unwrap();
    let score_parity = t
        .cos_sim((
            "Hello world",
            "I am a sentence for which I would like to get its embedding.",
        ))
        .unwrap();
    dbg!(score_parity);
    let score_batch = t
        .cos_sim_batch((
            "Hello world",
            "I am a sentence for which I would like to get its embedding.",
        ))
        .unwrap();
    dbg!(score_batch);
}
