$ErrorActionPreference = 'Stop'
$taskRoot = (Resolve-Path (Join-Path $PSScriptRoot '../..')).Path
Add-Type -AssemblyName System.Drawing.Common
$references=@('System.Drawing.Common','System.Drawing.Primitives','System.Runtime','System.Collections')
foreach($optional in @('System.Private.Windows.GdiPlus','System.Private.Windows.Core')) {
    if(Test-Path -LiteralPath (Join-Path $PSHOME "$optional.dll")) { $references+=$optional }
}
Add-Type -Path (Join-Path $PSScriptRoot 'StreetCutouts.cs') -ReferencedAssemblies $references
$characters=Join-Path $taskRoot 'docs/art-direction/loose-signal/characters'
$sources=Join-Path $characters '_shared/rig-sources/directional-v6'
foreach($variant in @('palm','point','fist')) {
    [StreetCutouts]::ExtractDirectional((Join-Path $sources "hands-$variant-atlas.png"),$characters,"hand-$variant",'v6')
}
