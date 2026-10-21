$ErrorActionPreference='Stop'
$workspace=(Resolve-Path (Join-Path $PSScriptRoot '../..')).Path
$art=Join-Path $workspace 'docs/art-direction/loose-signal'
$shipping=Join-Path $workspace '.artifacts/tavi-production/source-packs'
Add-Type -AssemblyName System.IO.Compression
$groups=@(
    @{Id='ls-eyes-extras-v2'; Category='characters/tavi/face/eyes'; Prefix='eyes'; Parts=@('neutral','side-eye','blink','laugh','wide','tired')},
    @{Id='ls-eyebrows-v2'; Category='characters/tavi/face/eyebrows'; Prefix='eyebrows'; Parts=@('neutral','skeptical','angry','worried','raised')},
    @{Id='ls-mouths-extras-v2'; Category='characters/tavi/face/mouths'; Prefix='mouths'; Parts=@('rest','smirk','talk-small','talk-wide','talk-round','teeth','laugh','frown')},
    @{Id='ls-studio-v2'; Category='studio'; Prefix='studio'; Parts=@('desk','guest-chair','media-screen','microphone','clamp-lamp','on-air','books','mug','paper','tape','calendar','padlock','moon','impact')}
)
foreach($group in $groups) {
    $target=Join-Path $shipping ($group.Id+'.kpack')
    $temporary=$target+'.building'
    if(Test-Path -LiteralPath $temporary){throw "Unfinished pack build: $temporary"}
    $zip=[IO.Compression.ZipFile]::Open($temporary,[IO.Compression.ZipArchiveMode]::Create)
    try {
        $items=@()
        for($i=0;$i -lt $group.Parts.Count;$i++) {
            $name="ls-$($group.Prefix)-$($group.Parts[$i])"
            $sources=@(Get-ChildItem -LiteralPath (Join-Path $art $group.Category) -Filter "$name.png" -Recurse -File | Where-Object { $_.Directory.Name -eq 'supplemental' })
            if($sources.Count -ne 1){throw "Expected one supplemental source for $name"}
            $file='frame_{0:D3}.png' -f $i
            $entry=$zip.CreateEntry($file,[IO.Compression.CompressionLevel]::Optimal)
            $entry.LastWriteTime=[DateTimeOffset]::new(2026,10,7,0,0,0,[TimeSpan]::Zero)
            $input=$sources[0].OpenRead();$output=$entry.Open()
            try{$input.CopyTo($output)}finally{$input.Dispose();$output.Dispose()}
            $items+=@{file=$file;index=$i;name=$name;desc='Loose Signal supplemental shape; independent stamp. Primary Tavi artwork uses the production packs.'}
        }
        $meta=@{pack_name=$group.Id;author='Loose Signal';canvas_dimension=256;total_items=$items.Count;items=$items} | ConvertTo-Json -Depth 15 -Compress
        $entry=$zip.CreateEntry('meta.json',[IO.Compression.CompressionLevel]::Optimal)
        $entry.LastWriteTime=[DateTimeOffset]::new(2026,10,7,0,0,0,[TimeSpan]::Zero)
        $output=$entry.Open();$bytes=[Text.Encoding]::UTF8.GetBytes($meta)
        try{$output.Write($bytes,0,$bytes.Length)}finally{$output.Dispose()}
    } finally {$zip.Dispose()}
    Move-Item -LiteralPath $temporary -Destination $target -Force
}
