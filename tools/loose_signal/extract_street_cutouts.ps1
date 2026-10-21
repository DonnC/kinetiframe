$ErrorActionPreference = 'Stop'
$root = (Resolve-Path (Join-Path $PSScriptRoot '../..')).Path
Add-Type -AssemblyName System.Drawing.Common
$references=@('System.Drawing.Common','System.Drawing.Primitives','System.Runtime','System.Collections')
foreach($optional in @('System.Private.Windows.GdiPlus','System.Private.Windows.Core')) {
    if(Test-Path -LiteralPath (Join-Path $PSHOME "$optional.dll")) { $references+=$optional }
}
Add-Type -Path (Join-Path $PSScriptRoot 'StreetCutouts.cs') -ReferencedAssemblies $references
foreach($character in @('tavi','kuda','mira')) {
    $version=Join-Path $root "docs/art-direction/loose-signal/characters/$character/full-body/v2"
    [StreetCutouts]::Extract((Join-Path $version 'sources/cutout-atlas.png'),(Join-Path $version 'cutouts'))
}
$torsoSource=Join-Path $root 'docs/art-direction/loose-signal/characters/_shared/rig-sources/street-v1/torso-atlas.png'
if(Test-Path -LiteralPath $torsoSource) {
    [StreetCutouts]::ExtractTorsos($torsoSource,(Join-Path $root 'docs/art-direction/loose-signal/characters'))
}
