$ErrorActionPreference = 'Stop'
$taskRoot = (Resolve-Path (Join-Path $PSScriptRoot '../..')).Path
Add-Type -AssemblyName System.Drawing.Common
$references=@('System.Drawing.Common','System.Drawing.Primitives','System.Runtime','System.Collections')
foreach($optional in @('System.Private.Windows.GdiPlus','System.Private.Windows.Core')) {
    if(Test-Path -LiteralPath (Join-Path $PSHOME "$optional.dll")) { $references+=$optional }
}
Add-Type -Path (Join-Path $PSScriptRoot 'StreetCutouts.cs') -ReferencedAssemblies $references
foreach($character in @('tavi','mira','kuda')) {
    $folder=Join-Path $taskRoot "docs/art-direction/loose-signal/characters/$character/full-body/v4"
    [StreetCutouts]::ExtractViews((Join-Path $folder 'sources/clean-views-atlas.png'),(Join-Path $folder 'cutouts'),($character -eq 'mira'))
}
