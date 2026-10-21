/** Host-side resource contracts, usable even when Gradle/device execution is unavailable. */
import fs from 'node:fs';
import path from 'node:path';
import assert from 'node:assert/strict';
import { fileURLToPath } from 'node:url';
const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '../..');
const read = file => fs.readFileSync(path.join(root,file),'utf8');
const res = 'app/src/main/res/';
const kotlin = read('designsystem/src/main/java/com/donnclab/kinetiframe/designsystem/theme/Color.kt');
const xml = read(res+'values/colors.xml');
for(const [name,key] of [['BrandTeal','brand_teal'],['BrandAmber','brand_amber'],['BrandIvory','brand_ivory'],['BrandInk','brand_ink'],['BrandMint','brand_rig']]) {
  const hex = kotlin.match(new RegExp(`val ${name} = Color\\(0xFF([0-9A-F]{6})\\)`))[1];
  assert.equal(xml.match(new RegExp(`<color name="${key}">#([0-9A-F]{6})</color>`))[1],hex);
}
for(const [file,dimension] of [['ic_launcher_foreground.xml',108],['ic_launcher_monochrome.xml',108],['ic_splash_mark.xml',288]]) {
  const source = read(res+'drawable/'+file);
  const target=source.match(/android:src="@drawable\/(\w+)"/)[1];
  for(const [density,scale] of [['mdpi',1],['hdpi',1.5],['xhdpi',2],['xxhdpi',3],['xxxhdpi',4]]) {
    const png=fs.readFileSync(path.join(root,res+`drawable-${density}/${target}.png`));
    assert.equal(png.readUInt32BE(16),dimension*scale);
    assert.equal(png.readUInt32BE(20),dimension*scale);
  }
}
for(const file of ['ic_launcher.xml','ic_launcher_round.xml']) {
  assert.ok(read(res+'mipmap-anydpi/'+file).includes('<monochrome android:drawable="@drawable/ic_launcher_monochrome"'));
}
const mono = read(res+'drawable/ic_launcher_monochrome.xml');
assert.ok(!mono.includes('@color/'), 'Themed icon must not contain identity colours');
const themes = read(res+'values/themes.xml');
assert.ok(themes.includes('parent="Theme.SplashScreen"'));
assert.ok(themes.includes('name="postSplashScreenTheme">@style/Theme.KinetiFrame'));
assert.ok(read('app/src/main/AndroidManifest.xml').includes('android:theme="@style/Theme.KinetiFrame.Starting"'));
// Independent static contrast check complements the JVM theme test, not a substitute for compilation.
const colours = Object.fromEntries([...kotlin.matchAll(/val (\w+) = Color\(0xFF([0-9A-F]{6})\)/g)].map(m=>[m[1],m[2]]));
function luminance(hex) {
  const rgb = [0,2,4].map(i=>parseInt(hex.slice(i,i+2),16)/255).map(v=>v<=.04045?v/12.92:((v+.055)/1.055)**2.4);
  return rgb[0]*.2126+rgb[1]*.7152+rgb[2]*.0722;
}
const theme = read('designsystem/src/main/java/com/donnclab/kinetiframe/designsystem/theme/KinetiFrameTheme.kt');
for(const scheme of ['BrandLightColorScheme','BrandDarkColorScheme']) {
  const body = theme.split(`internal val ${scheme} = `)[1].split('\n)')[0];
  const roles = Object.fromEntries([...body.matchAll(/(\w+) = (Color\(0xFF([0-9A-F]{6})\)|(Brand\w+))/g)].map(m=>[m[1],m[3]||colours[m[4]]]));
  for(const role of ['primary','primaryContainer','secondary','secondaryContainer','tertiary','tertiaryContainer','surface','surfaceVariant','background']) {
    const foreground = 'on'+role[0].toUpperCase()+role.slice(1);
    const a=luminance(roles[role]), b=luminance(roles[foreground]);
    const contrast=(Math.max(a,b)+.05)/(Math.min(a,b)+.05);
    assert.ok(contrast>=4.5,`${scheme} ${role}: ${contrast.toFixed(2)}`);
  }
}
console.log('PASS: palette parity, 15 density bitmap dimensions, themed icon, splash wiring and 18 text-role contrast checks.');
