param([switch]$Joined)
$ErrorActionPreference = 'Stop'
if (-not $Joined) { throw 'Only the current -Joined starter kit is supported.' }
$workspace = (Resolve-Path (Join-Path $PSScriptRoot '../..')).Path
$output = Join-Path $workspace 'app/src/main/assets/loose-signal/curated/v4'
$version = 4
$cutoutVersion = 6
New-Item -ItemType Directory -Path $output -Force | Out-Null
Add-Type -AssemblyName System.IO.Compression

# Archive repackaging only: original transparent artwork bytes are never resampled or altered.
function Build-Pack($id, $title, $sources) {
    $target = Join-Path $output "$id.kpack"
    $temporary = "$target.building"
    if (Test-Path -LiteralPath $temporary) { throw "Unfinished build: $temporary" }
    $zip = [IO.Compression.ZipFile]::Open($temporary, [IO.Compression.ZipArchiveMode]::Create)
    try {
        $items = @(); $dimension = 1
        foreach ($source in $sources) {
            $sourcePath = $source.Path
            $input = [IO.Compression.ZipFile]::OpenRead($sourcePath)
            try {
                $reader = [IO.StreamReader]::new($input.GetEntry('meta.json').Open())
                try { $meta = $reader.ReadToEnd() | ConvertFrom-Json } finally { $reader.Dispose() }
                foreach ($item in $meta.items) {
                    if (-not (& $source.Accept $item.name)) { continue }
                    $stream = $input.GetEntry($item.file).Open(); $buffer = [IO.MemoryStream]::new()
                    try { $stream.CopyTo($buffer); $bytes = $buffer.ToArray() } finally { $stream.Dispose(); $buffer.Dispose() }
                    foreach ($offset in @(16,20)) {
                        $size = ([int]$bytes[$offset] -shl 24) -bor ([int]$bytes[$offset+1] -shl 16) -bor ([int]$bytes[$offset+2] -shl 8) -bor [int]$bytes[$offset+3]
                        $dimension = [Math]::Max($dimension, $size)
                    }
                    $file = 'frame_{0:D3}.png' -f $items.Count
                    $entry = $zip.CreateEntry($file)
                    $entry.LastWriteTime = [DateTimeOffset]::new(2026,10,11,0,0,0,[TimeSpan]::Zero)
                    $stream = $entry.Open()
                    try { $stream.Write($bytes,0,$bytes.Length) } finally { $stream.Dispose() }
                    $packedItem = @{ file=$file; index=$items.Count; name="$($source.Prefix)$($item.name)"; desc='Reusable original artwork. Position and pivot are adjusted visually in Actor Studio.' }
                    if ($item.registration) { $packedItem.registration = $item.registration }
                    $items += $packedItem
                }
            } finally { $input.Dispose() }
        }
        $meta = @{ pack_name=$title; author='Loose Signal'; total_items=$items.Count; canvas_dimension=$dimension; items=$items } | ConvertTo-Json -Depth 8 -Compress
        $entry = $zip.CreateEntry('meta.json'); $writer = [IO.StreamWriter]::new($entry.Open())
        try { $writer.Write($meta) } finally { $writer.Dispose() }
    } finally { $zip.Dispose() }
    Move-Item -LiteralPath $temporary -Destination $target -Force
    Write-Output "$title : $($items.Count) items"
}
foreach ($character in @('tavi','mira','kuda')) {
    $sources = foreach ($view in @('front','profile','three-quarter','back')) {
        $path = Join-Path $workspace ".artifacts/demos/directional-view-packs/v$version/ls-$character-$view-artwork-v$version.kpack"
        @{ Path=$path; Prefix="$view · "; Accept={ param($name) $name -notmatch '^(eyes|mouth)( |$)' } }
    }
    Build-Pack "ls-$character-cutouts-v$cutoutVersion" "$($character.Substring(0,1).ToUpper()+$character.Substring(1)) · Four-view cutouts" $sources
}
