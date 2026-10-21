$ErrorActionPreference = 'Stop'
$taskRoot = (Resolve-Path (Join-Path $PSScriptRoot '../..')).Path
Add-Type -AssemblyName System.Drawing.Common
$references=@('System.Drawing.Common','System.Drawing.Primitives','System.Runtime','System.Collections')
foreach($optional in @('System.Private.Windows.GdiPlus','System.Private.Windows.Core')) {
    if(Test-Path -LiteralPath (Join-Path $PSHOME "$optional.dll")) { $references+=$optional }
}
Add-Type -Path (Join-Path $PSScriptRoot 'StreetCutouts.cs') -ReferencedAssemblies $references
$characters=Join-Path $taskRoot 'docs/art-direction/loose-signal/characters'
$sources=Join-Path $characters '_shared/rig-sources/directional-v5'
[StreetCutouts]::ExtractDirectional((Join-Path $sources 'footwear-atlas.png'),$characters,'foot')
[StreetCutouts]::ExtractDirectional((Join-Path $sources 'hands-atlas.png'),$characters,'hand-rest')
foreach($character in @('tavi','mira','kuda')) {
    $body=Join-Path $characters "$character/full-body/v5"
    [StreetCutouts]::ExtractDirectionalBody((Join-Path $body 'sources/body-atlas.png'),(Join-Path $body 'cutouts'))
}
