$ErrorActionPreference = 'Stop'
$taskRoot = (Resolve-Path (Join-Path $PSScriptRoot '../..')).Path
Add-Type -AssemblyName System.Drawing.Common
$references=@('System.Drawing.Common','System.Drawing.Primitives','System.Runtime','System.Collections')
foreach($optional in @('System.Private.Windows.GdiPlus','System.Private.Windows.Core')) {
    if(Test-Path -LiteralPath (Join-Path $PSHOME "$optional.dll")) { $references+=$optional }
}
Add-Type -Path (Join-Path $PSScriptRoot 'StreetCutouts.cs') -ReferencedAssemblies $references
$characters=Join-Path $taskRoot 'docs/art-direction/loose-signal/characters'
$sources=Join-Path $characters '_shared/rig-sources/street-v2'
[StreetCutouts]::ExtractContinuous((Join-Path $sources 'continuous-limbs-atlas.png'),(Join-Path $sources 'socket-free-torsos-atlas.png'),$characters)
