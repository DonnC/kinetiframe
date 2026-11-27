package com.donnclab.kinetiframe.library.model

/** Versioned exact colour data. Skin families are tonal starting points, not ethnicity labels. */
object BundledPalettes {
    private fun palette(id: String, name: String, values: String): PaletteDocument = PaletteDocument(
        "kf-palette-$id-v1",
        name,
        values.trim().split("|").map {
            val (label, hex) = it.trim().split(":")
            ColorSwatch(label, 0xFF000000L or hex.toLong(16))
        },
    ).validate()

    val entries = listOf(
        palette(
            "essentials",
            "Essentials",
            "Ink:171A1C|White:FFFFFF|Light grey:E4E7E9|Mid grey:92999E|Dark grey:464D53|Red:E44E50|Orange:F28A35|Yellow:F4D35E|Leaf:65AD63|Green:23866E|Cyan:36BBCB|Blue:437BD8|Indigo:5554B0|Purple:9263BE|Pink:E887B6|Brown:8D6045",
        ),
        palette(
            "skin",
            "Skin tones",
            "Porcelain shadow:C88E79|Porcelain:F1C5AC|Porcelain light:FFE3CF|Warm shadow:B77351|Warm:E3A174|Warm light:FBC59C|Golden shadow:9D663F|Golden:CB9058|Golden light:EFB77F|Olive shadow:826048|Olive:B58B68|Olive light:D8B38B|Deep shadow:573C30|Deep:855B43|Deep light:AD7B59|Rich shadow:302628|Rich:533C37|Rich light:785449",
        ),
        palette(
            "nature",
            "Nature",
            "Sky:82C9E8|Cloud:F1F4E8|Water:428CAA|Deep water:24516A|Leaf:7EB365|Forest:315E49|Grass:A6BC69|Soil:806046|Sand:DAC69E|Stone:999E94|Flower:CF7FA0|Sun:EDD17C",
        ),
        palette(
            "materials",
            "Interiors & materials",
            "Cream wall:F2E6CE|Warm wall:D9C3A3|Wood light:C99A68|Wood:AA7352|Wood dark:654536|Metal light:C6D0D2|Metal:7E939A|Metal dark:35454E|Teal fabric:408185|Brick:B96A54|Clay:D29A7B|Paper:FFF8E9",
        ),
        palette(
            "pastels",
            "Soft pastels",
            "Rose:F0BFCC|Peach:F4C5AE|Apricot:F4D4A8|Butter:F5E7AF|Mint:C2E2CD|Sage:C9D9BC|Aqua:B9E0E4|Sky:BED7F1|Lavender:D2C7EC|Lilac:E3C6E0|Sand:E5D5C1|Soft ink:626B83",
        ),
        palette(
            "night",
            "Night & neon",
            "Night:151D35|Navy:263455|Dusk:514A78|Shadow:25233E|Moon:DDE9F4|Neon cyan:54E7E3|Neon pink:EF64BE|Electric blue:6289F3|Violet:A582F0|Lime:C4EB6B|Amber:F5BF5B|Signal red:F36C70",
        ),
        palette(
            "education",
            "Educational diagrams",
            "Ink:263238|Paper:FAF8F0|Blue:3976B9|Orange:DB8B35|Teal:328B81|Plum:98659B|Gold:C8A343|Grey:8E9AA0",
        ),
        palette(
            "loose-signal",
            "Loose Signal",
            "Tavi teal:17999D|Tie orange:F58D37|Studio cream:F1E2C8|Desk wood:B96F53|Hair:2D2420|Tavi skin:F6BC89|Guest skin:86563E|Outline:171A1C",
        ),
    )
}
