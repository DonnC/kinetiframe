# Brand generator

Every brand file is drawn from the geometry in `BrandGeometry.java`. Change the brand there, then
regenerate from the repository root with JDK 17 or newer:

```sh
javac -encoding UTF-8 -d build/brand tools/brand/*.java
java -cp build/brand BrandExport .            # app vectors, BrandArt.kt, docs/brand SVGs and store icon
java -cp build/brand BrandMark build/brand/out # previews at every size, and the brand board
cp build/brand/out/brand-board.png docs/brand/
```

The wordmark is drawn from the app's `modules/designsystem/src/main/res/font/nunito_extrabold.ttf`.
The app's Nunito weights were cut from Nunito's variable font with
`fonttools varLib.instancer Nunito[wght].ttf wght=800` (400 to 700 for the others), because
Java and Compose resource fonts can't reliably select a variable font's weight. Nunito is under
the SIL Open Font License; see [LICENSES/OFL-1.1-Nunito.txt](../../LICENSES/OFL-1.1-Nunito.txt).
