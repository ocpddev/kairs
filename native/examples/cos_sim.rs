use anyhow::Result;

use kairs::sbert::SentenceTransformer;

fn main() -> Result<()> {
    let transformer = SentenceTransformer::load("./models/minilm")?;
    let score = transformer.cos_sim((
        r#"The iPhone's Settings hold a treasure trove of features that can boost your device's security and efficiency, but finding them can be like looking for a needle in a haystack due to the overwhelming number of options. However, there's a shortcut for those ready to tap into their phone's hidden powers. Dive into the Settings, select "Accessibility," choose "Touch," and then scroll down to discover "Back Tap" at the bottom. This feature opens up a world of possibilities, allowing you to assign actions to "Double Tap" and "Triple Tap" on your iPhone's back, making it easier than ever to use your device to its fullest potential."#,
        r#"Hidden within the iPhone's extensive Settings, a valuable feature awaits those seeking to enhance their device's security and functionality, often overshadowed by the sheer volume of options available. For users eager to unlock their iPhone's potential quickly, a simple journey into the Settings can reveal this secret. By heading to "Accessibility" and then "Touch," followed by a scroll to "Back Tap" at the menu's end, a new realm of customization unveils itself. This area offers "Double Tap" and "Triple Tap" options, empowering you to activate specific functions with just a few taps on the back of your iPhone."#,
    ))?;
    println!("score: {}", score);
    Ok(())
}
