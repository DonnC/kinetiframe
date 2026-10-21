param([switch]$Video)
$ErrorActionPreference = 'Stop'
$root = (Resolve-Path (Join-Path $PSScriptRoot '../..')).Path
$art = Join-Path $root 'docs/art-direction/loose-signal'
$source = Join-Path $art 'characters/tavi'
$exports = $art
$shipping = Join-Path $root 'app/src/main/assets/loose-signal/v2'
$staging = Join-Path $root '.artifacts/tavi-production'
& (Join-Path $PSScriptRoot 'build_supplemental_packs.ps1')
New-Item -ItemType Directory -Force $exports,$shipping,$staging | Out-Null
Add-Type -AssemblyName System.Drawing.Common
$drawingReferences=@('System.Drawing.Common','System.Drawing.Primitives','System.Runtime')
foreach($optional in @('System.Private.Windows.GdiPlus','System.Private.Windows.Core')) {
    if(Test-Path -LiteralPath (Join-Path $PSHOME "$optional.dll")) { $drawingReferences+=$optional }
}
Add-Type -Path (Join-Path $PSScriptRoot 'TaviProduction.cs') -ReferencedAssemblies $drawingReferences
Add-Type -AssemblyName System.IO.Compression

function Write-Json($path,$value) {
    [IO.File]::WriteAllText($path,($value | ConvertTo-Json -Depth 30 -Compress),[Text.UTF8Encoding]::new($false))
}
function Archive($directory,$destination) {
    $temporary = "$destination.$([Guid]::NewGuid().ToString('N')).building"
    $zip = [IO.Compression.ZipFile]::Open($temporary,[IO.Compression.ZipArchiveMode]::Create)
    try {
        Get-ChildItem -LiteralPath $directory -File -Recurse | Sort-Object FullName | ForEach-Object {
            $relative = [IO.Path]::GetRelativePath($directory,$_.FullName).Replace('\','/')
            $entry = $zip.CreateEntry($relative,[IO.Compression.CompressionLevel]::Optimal)
            $entry.LastWriteTime = [DateTimeOffset]::new(2026,10,7,0,0,0,[TimeSpan]::Zero)
            $input = $_.OpenRead(); $output=$entry.Open()
            try { $input.CopyTo($output) } finally { $input.Dispose();$output.Dispose() }
        }
    } finally { $zip.Dispose() }
    Move-Item -LiteralPath $temporary -Destination $destination -Force
}
$groups = @(
    @{Id='ls-tavi-core-v2'; Atlas='core.png'; Names=@('head-hair','torso-tie','upper-sleeve','forearm'); Regions=@(@(0,0,625,710),@(630,0,650,710),@(0,710,640,570),@(640,710,640,570))},
    @{Id='ls-eyes-v2';Atlas='eyes.png';Names=@('eyes-neutral','eyes-skeptical','eyes-laugh','eyes-surprised');Regions=@(@(0,0,640,640),@(640,0,640,640),@(0,640,640,640),@(640,640,640,640))},
    @{Id='ls-mouths-v2';Atlas='mouths.png';Names=@('mouth-smile','mouth-small','mouth-open','mouth-o','mouth-laugh','mouth-frown');Regions=@(@(0,0,512,512),@(512,0,512,512),@(1024,0,512,512),@(0,512,512,512),@(512,512,512,512),@(1024,512,512,512))},
    @{Id='ls-hands-v2';Atlas='hands.png';Names=@('hand-relaxed','hand-point','hand-fist','hand-palm');Regions=@(@(0,0,640,640),@(640,0,640,640),@(0,640,640,640),@(640,640,640,640))},
    @{Id='ls-desk-v2';Atlas='desk.png';Names=@('desk');Regions=@(@(0,0,2172,724))}
)
foreach($group in $groups) {
    $category= switch($group.Atlas) {'core.png' {'body'} 'eyes.png' {'face/eyes'} 'mouths.png' {'face/mouths'} 'hands.png' {'hands'} 'desk.png' {'studio/furniture'}}
    $partRoot=if($group.Atlas -eq 'desk.png') {Join-Path $art $category} else {Join-Path $source $category}
    $atlas=Join-Path $partRoot "sources/v2/$($group.Atlas)"
    $cutouts=Join-Path $partRoot 'cutouts/v2'
    $stamps=Join-Path $partRoot 'stamps/v2'
    New-Item -ItemType Directory -Force $cutouts,$stamps | Out-Null
    $directory=Join-Path $staging $group.Id
    New-Item -ItemType Directory -Force $directory | Out-Null
    $items=@()
    for($i=0;$i -lt $group.Names.Count;$i++) {
        $name=$group.Names[$i];$r=$group.Regions[$i]
        if($group.Atlas -eq 'desk.png') { $r=@(0,0,2172,724) }
        if($group.Atlas -in @('core.png','eyes.png','hands.png')) {
            $r=@($r | ForEach-Object { [int][Math]::Floor($_*1254/1280) })
        }
        $cutout=[TaviProduction]::Slice($atlas,[Drawing.Rectangle]::new($r[0],$r[1],$r[2],$r[3]))
        try {
            $cutout.Save((Join-Path $cutouts "$name-cutout.png"),[Drawing.Imaging.ImageFormat]::Png)
            # Faces share a fixed 512px head registration, not per-expression auto-fit.
            if($name.StartsWith('eyes-')) { $w=173;$h=124;$x=155;$y=254 }
            elseif($name.StartsWith('mouth-')) { $w=85;$h=if($name -in @('mouth-smile','mouth-frown')) {11} elseif($name -eq 'mouth-o') {62} else {53};$x=201;$y=[int](417-$h/2) }
            elseif($name -eq 'head-hair') { $w=472;$h=if($cutout.Height -gt 0) {[int]($w*$cutout.Height/$cutout.Width)};$w=[int]($w*[Math]::Min(1,472/$h));$h=[Math]::Min(472,$h);$x=[int]((512-$w)/2);$y=20 }
            else { $scale=472/[Math]::Max($cutout.Width,$cutout.Height);$w=[int]($cutout.Width*$scale);$h=[int]($cutout.Height*$scale);$x=[int]((512-$w)/2);$y=[int]((512-$h)/2) }
            $file='frame_{0:D3}.png' -f $i
            [TaviProduction]::Stamp($cutout,(Join-Path $directory $file),$w,$h,$x,$y)
            Copy-Item -LiteralPath (Join-Path $directory $file) -Destination (Join-Path $stamps "ls-$name.png")
            $items+=@{file=$file;index=$i;name="ls-$name";desc='Tavi production v2 · source-matched cutout'}
        } finally { $cutout.Dispose() }
    }
    Write-Json (Join-Path $directory 'meta.json') @{pack_name=$group.Id;author='Loose Signal';total_items=$items.Count;canvas_dimension=512;items=$items}
    $sourcePacks = Join-Path $root '.artifacts/tavi-production/source-packs'
    New-Item -ItemType Directory -Force $sourcePacks | Out-Null
    Archive $directory (Join-Path $sourcePacks ($group.Id+'.kpack'))
}
$projectId='ls-tavi-presentation-v2'
Copy-Item -LiteralPath (Join-Path $art 'studio/backgrounds/room.png') -Destination (Join-Path $shipping 'ls-room-background-v2.png')
$project=Join-Path $staging $projectId
$preview=Join-Path $staging 'preview'
New-Item -ItemType Directory -Force (Join-Path $project 'project-assets') | Out-Null
[TaviProduction]::Build($exports,(Join-Path $shipping 'ls-room-background-v2.png'),(Join-Path $project 'frames'),$preview)
$stage=@{width=1280;height=720};$allFrames=@(0..95)
$labels=@('Tavi · body','Desk','Tavi · hands','Tavi · eyes + brows','Tavi · mouth')
$ids=@('ls-body','ls-desk','ls-hands','ls-eyes','ls-mouths')
$layers=@(for($i=0;$i -lt $ids.Count;$i++) { @{id=$ids[$i];name=$labels[$i];zIndex=$i;populatedFrames=$allFrames} })
$frameLayers=@{};foreach($f in $allFrames) { $frameLayers["$f"]=$layers }
$virtual="/kinetiframe-demo-source/$projectId"
$document=@{project=@{id=$projectId;stage=$stage;fps=12;layers=$layers;assets=@{stage=@{id='stage';kind='IMPORTED_IMAGE';sourcePath="$virtual/project-assets/stage.png";width=1672;height=941}};backgroundAssetId='stage';actorPlacements=@{};audioClips=@{}};frameLayers=$frameLayers;totalFrames=96;activeLayerIndex=4}
$manifest=@{projectId=$projectId;displayName='Loose Signal · Tavi presents';stage=$stage;fps=12;createdAtEpochMs=1791331200000;updatedAtEpochMs=1791331200000;totalFrames=96;thumbnailPath="$virtual/thumbnail.png"}
Write-Json (Join-Path $project 'document.json') $document
Write-Json (Join-Path $project 'manifest.json') $manifest
Write-Json (Join-Path $project 'snapshot.json') @{manifest=$manifest;document=$document}
Write-Json (Join-Path $project 'archive-paths.json') @{sourceDirectory=$virtual}
Copy-Item -LiteralPath (Join-Path $shipping 'ls-room-background-v2.png') -Destination (Join-Path $project 'project-assets/stage.png')
Copy-Item -LiteralPath (Join-Path $preview 'frame-024.png') -Destination (Join-Path $project 'thumbnail.png')
Copy-Item -LiteralPath (Join-Path $preview 'frame-024.png') -Destination (Join-Path $art 'clips/tavi-presents/scene-preview.png')
Archive $project (Join-Path $shipping "$projectId.kframe")
if($Video) {
    $ffmpeg=Join-Path $root '.artifacts/demo-tools/imageio_ffmpeg/binaries/ffmpeg-win-x86_64-v7.1.exe'
    if(!(Test-Path -LiteralPath $ffmpeg)) { throw 'Preview encoder unavailable. Native project and packs are already generated.' }
    & $ffmpeg -y -loglevel error -framerate 12 -i "$preview/frame-%03d.png" -c:v libx264 -crf 20 -pix_fmt yuv420p -movflags +faststart (Join-Path $art 'clips/tavi-presents/ls-tavi-presentation-v2.mp4')
    if($LASTEXITCODE -ne 0) { throw 'Preview encoding failed' }
}
Get-ChildItem -LiteralPath $shipping -File | Select-Object Name,Length
