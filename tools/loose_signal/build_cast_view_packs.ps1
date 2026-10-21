param([switch]$Joined)
$ErrorActionPreference = 'Stop'
if (-not $Joined) { throw 'Only the current -Joined starter kit is supported.' }
$workspace = (Resolve-Path (Join-Path $PSScriptRoot '../..')).Path
$version = 4
$packOutput = Join-Path $workspace '.artifacts/demos/directional-view-packs/v4'
New-Item -ItemType Directory -Path $packOutput -Force | Out-Null
Add-Type -AssemblyName System.IO.Compression
$archive = [IO.Compression.ZipFile]::OpenRead((Join-Path $workspace 'app/src/main/assets/actor/starter-actors-v4.zip'))
try {
    foreach ($character in @('tavi', 'mira', 'kuda')) {
        foreach ($view in @('front', 'profile', 'three_quarter', 'back')) {
            $actorVersion = 3
            $actorId = "ls-$character-all-views-v$actorVersion"
            $revisionId = "$actorId-$view-joined-v4"
            $entry = $archive.GetEntry("dependencies/actors/$actorId/revisions/$revisionId.json")
            $reader = [IO.StreamReader]::new($entry.Open())
            try { $revision = $reader.ReadToEnd() | ConvertFrom-Json } finally { $reader.Dispose() }
            $id = "ls-$character-$($view.Replace('_','-'))-artwork-v$version"
            $target = Join-Path $packOutput "$id.kpack"
            $building = "$target.building"
            if (Test-Path -LiteralPath $building) { throw "Unfinished pack build: $building" }
            $zip = [IO.Compression.ZipFile]::Open($building, [IO.Compression.ZipArchiveMode]::Create)
            try {
                $items = @(); $dimension = 1
                foreach ($part in ($revision.artworkAssetPaths.PSObject.Properties | Sort-Object Name)) {
                    $source = $archive.GetEntry("dependencies/actors/$actorId/$($part.Value)")
                    $input = $source.Open(); $buffer = [IO.MemoryStream]::new()
                    try { $input.CopyTo($buffer); $bytes = $buffer.ToArray() } finally { $input.Dispose(); $buffer.Dispose() }
                    # PNG IHDR dimensions; copy the source pixels unchanged, including transparency.
                    foreach ($offset in @(16, 20)) {
                        $n = ([int]$bytes[$offset] -shl 24) -bor ([int]$bytes[$offset+1] -shl 16) -bor ([int]$bytes[$offset+2] -shl 8) -bor [int]$bytes[$offset+3]
                        $dimension = [Math]::Max($dimension, $n)
                    }
                    $file = 'frame_{0:D3}.png' -f $items.Count
                    $packed = $zip.CreateEntry($file)
                    $packed.LastWriteTime = [DateTimeOffset]::new(2026,10,11,0,0,0,[TimeSpan]::Zero)
                    $out = $packed.Open()
                    try { $out.Write($bytes, 0, $bytes.Length) } finally { $out.Dispose() }
                    $name = $part.Name.Replace('_', ' ').Replace('#', ' · ')
                    $address = $part.Name.Split('#')
                    $node = $revision.sceneGraph.bones.PSObject.Properties[$address[0]].Value
                    $binding = if ($address.Length -gt 1) { $node.artworkVariants.PSObject.Properties[$address[1]].Value.binding } else { $node.textureBinding }
                    $registration = @{
                        partId=$node.id; sourceWidth=([int]$bytes[16] -shl 24) -bor ([int]$bytes[17] -shl 16) -bor ([int]$bytes[18] -shl 8) -bor [int]$bytes[19]
                        sourceHeight=([int]$bytes[20] -shl 24) -bor ([int]$bytes[21] -shl 16) -bor ([int]$bytes[22] -shl 8) -bor [int]$bytes[23]
                        boneLength=$node.length; pivotX=$binding.pivotX; pivotY=$binding.pivotY
                        offsetX=0.0; offsetY=0.0; rotationDegrees=0.0; scale=1.0
                    }
                    foreach ($field in @('offsetX','offsetY','rotationDegrees','scale')) {
                        if ($binding.PSObject.Properties[$field]) { $registration[$field] = $binding.$field }
                    }
                    if ($binding.skin -and -not $binding.skin.attachmentBlend) {
                        $children = @($binding.skin.bones | Where-Object { $_.boneId -ne $node.id })
                        if ($children.Count -eq 1) { $registration.continuousChildId = $children[0].boneId }
                    }
                    if ($binding.skin.attachmentBlend) { $registration.attachmentBlend = $true }
                    $items += @{ file=$file; index=$items.Count; name=$name; registration=$registration; desc="$character $view artwork. Original pixels and portable joint registration." }
                }
                $meta = @{ pack_name="$character · $($view.Replace('_',' ')) artwork"; author='Loose Signal'; total_items=$items.Count; canvas_dimension=$dimension; items=$items } | ConvertTo-Json -Depth 8 -Compress
                $packed = $zip.CreateEntry('meta.json')
                $packed.LastWriteTime = [DateTimeOffset]::new(2026,10,11,0,0,0,[TimeSpan]::Zero)
                $writer = [IO.StreamWriter]::new($packed.Open())
                try { $writer.Write($meta) } finally { $writer.Dispose() }
            } finally { $zip.Dispose() }
            Move-Item -LiteralPath $building -Destination $target -Force
            Write-Output "$id · $($items.Count) items"
        }
    }
} finally { $archive.Dispose() }
