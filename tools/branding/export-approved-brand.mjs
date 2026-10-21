/** Export the approved source pixels, without tracing or AI regeneration. */
import fs from 'node:fs';
import path from 'node:path';
import { createRequire } from 'node:module';
import { fileURLToPath } from 'node:url';
import assert from 'node:assert/strict';
const sharp = createRequire(import.meta.url)(process.argv[2] || 'sharp');
const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '../..');
const brand = path.join(root, 'docs/art-direction/brand/approved');
fs.mkdirSync(brand,{recursive:true});
const source=path.join(brand,'approved-board.png');
if(process.argv[3]) fs.copyFileSync(process.argv[3],source);
const meta=await sharp(source).metadata();
if(meta.width!==1053 || meta.height!==719) throw new Error('Inspect crop coordinates for changed source dimensions.');
const crops={icon:{left:158,top:461,width:205,height:196},mark:{left:392,top:38,width:310,height:300},
  lockup:{left:270,top:38,width:548,height:391},wordmark:{left:270,top:348,width:548,height:82},
  foreground:{left:180,top:484,width:160,height:155},
  monochrome:{left:464,top:485,width:163,height:160}};
for(const [name,crop] of Object.entries(crops)) {
  const target=path.join(brand,`${name}-source.png`);
  await sharp(source).extract(crop).png().toFile(target);
  const originalPixels=await sharp(source).extract(crop).raw().toBuffer();
  assert.deepEqual(await sharp(target).raw().toBuffer(),originalPixels,`${name} crop must retain original pixels`);
}
// Only adaptive layers need transparency: classify the teal tile, retaining source RGB in the
// character, amber arc and mint rig. Alpha-mat blended boundary pixels against the tile colour.
const {data,info}=await sharp(path.join(brand,'foreground-source.png')).ensureAlpha().raw().toBuffer({resolveWithObject:true});
for(let i=0;i<data.length;i+=4) {
  const [r,g,b]=[data[i],data[i+1],data[i+2]];
  if(r<45) {data[i+3]=0;continue;}
  if(r<120 && g-r>55 && b-r>25) {
    const alpha=Math.min(1,r/145);
    data[i]=Math.min(255,Math.round(r/alpha));
    data[i+1]=Math.min(255,Math.round((g-105*(1-alpha))/alpha));
    data[i+2]=Math.min(255,Math.round((b-103*(1-alpha))/alpha));
    data[i+3]=Math.round(alpha*255);
  }
}
const foreground=await sharp(data,{raw:{width:info.width,height:info.height,channels:4}}).png().toBuffer();
fs.writeFileSync(path.join(brand,'icon-foreground.png'),foreground);
const mono=await sharp(path.join(brand,'monochrome-source.png')).ensureAlpha().raw().toBuffer({resolveWithObject:true});
for(let i=0;i<mono.data.length;i+=4) {
  const minimum=Math.min(...mono.data.subarray(i,i+3));
  const alpha=minimum>225?0:Math.max(0,Math.min(255,Math.round((250-minimum)*255/215)));
  mono.data[i]=mono.data[i+1]=mono.data[i+2]=0;mono.data[i+3]=alpha;
}
const monochrome=await sharp(mono.data,{raw:{width:mono.info.width,height:mono.info.height,channels:4}}).png().toBuffer();
fs.writeFileSync(path.join(brand,'monochrome-foreground.png'),monochrome);
async function layer(image,canvasDp,markDp,density) {
  const size=Math.round(canvasDp*density),mark=Math.round(markDp*density);
  const scaled=await sharp(image).resize(mark,mark,{fit:'contain',background:'#00000000'}).png().toBuffer();
  return sharp({create:{width:size,height:size,channels:4,background:'#00000000'}})
    .composite([{input:scaled,left:Math.round((size-mark)/2),top:Math.round((size-mark)/2)}]).png().toBuffer();
}
for(const [bucket,density] of [['mdpi',1],['hdpi',1.5],['xhdpi',2],['xxhdpi',3],['xxxhdpi',4]]) {
  const dir=path.join(root,`app/src/main/res/drawable-${bucket}`);fs.mkdirSync(dir,{recursive:true});
  fs.writeFileSync(path.join(dir,'brand_launcher_pixels.png'),await layer(foreground,108,44,density));
  fs.writeFileSync(path.join(dir,'brand_monochrome_pixels.png'),await layer(monochrome,108,44,density));
  fs.writeFileSync(path.join(dir,'brand_splash_pixels.png'),await layer(foreground,288,132,density));
}
const nodpi=path.join(root,'app/src/main/res/drawable-nodpi');fs.mkdirSync(nodpi,{recursive:true});
for(const [name,target] of [['icon','brand_approved_icon'],['mark','brand_approved_mark'],['lockup','brand_approved_lockup']])
  fs.copyFileSync(path.join(brand,`${name}-source.png`),path.join(nodpi,`${target}.png`));
const word=await sharp(path.join(brand,'wordmark-source.png')).ensureAlpha().raw().toBuffer({resolveWithObject:true});
for(let i=0;i<word.data.length;i+=4) {
  const r=word.data[i];
  if(r>225) {word.data[i+3]=0;continue;}
  if(r>60) {
    const alpha=(250-r)/250;
    for(let c=0;c<3;c++) word.data[i+c]=Math.max(0,Math.min(255,Math.round((word.data[i+c]-250*(1-alpha))/alpha)));
    word.data[i+3]=Math.round(alpha*255);
  }
}
await sharp(word.data,{raw:{width:word.info.width,height:word.info.height,channels:4}}).png().toFile(path.join(nodpi,'brand_approved_wordmark.png'));
const resized=await sharp(foreground).resize(384,384,{fit:'contain',background:'#00000000'}).png().toBuffer();
await sharp({create:{width:512,height:512,channels:4,background:'#176B62'}})
  .composite([{input:resized,left:64,top:64}]).png().toFile(path.join(brand,'app-icon-512.png'));
// Actual Android layer, masked like a circular launcher for visual QA.
const circle=Buffer.from('<svg width="432" height="432"><circle cx="216" cy="216" r="144" fill="white"/></svg>');
const adaptive=await sharp({create:{width:432,height:432,channels:4,background:'#176B62'}})
  .composite([{input:await layer(foreground,108,44,4)},{input:circle,blend:'dest-in'}]).png().toBuffer();
const original=await sharp(path.join(brand,'icon-source.png')).resize(240,240,{fit:'contain',background:'#00000000'}).png().toBuffer();
await sharp({create:{width:760,height:440,channels:4,background:'#F7F3E8'}})
  .composite([{input:original,left:30,top:90},{input:adaptive,left:300,top:0}]).png().toFile(path.join(brand,'approved-preview.png'));
console.log('PASS: six original-pixel crops. Exported five Android density buckets; no traced geometry.');
