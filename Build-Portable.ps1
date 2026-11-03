param(
    [string]$OutputDirectory
)

$ErrorActionPreference = 'Stop'

if (-not $OutputDirectory) {
    $OutputDirectory = Join-Path $PSScriptRoot 'portable'
}

$sourceCode = Join-Path $PSScriptRoot 'myTime.cs'
$iconPath = Join-Path $PSScriptRoot 'assets\myTime.ico'
$portableReadme = Join-Path $PSScriptRoot 'docs\PORTABLE.md'
$outputDirectory = [System.IO.Path]::GetFullPath($OutputDirectory)
$outputExe = Join-Path $outputDirectory 'myTime.exe'

if (-not (Test-Path -LiteralPath $sourceCode)) {
    throw "Cannot find $sourceCode"
}

if (-not (Test-Path -LiteralPath $iconPath)) {
    throw "Cannot find $iconPath"
}

if (-not (Test-Path -LiteralPath $portableReadme)) {
    throw "Cannot find $portableReadme"
}

$cscCandidates = @(
    (Join-Path ([Runtime.InteropServices.RuntimeEnvironment]::GetRuntimeDirectory()) 'csc.exe'),
    'C:\Windows\Microsoft.NET\Framework64\v4.0.30319\csc.exe',
    'C:\Windows\Microsoft.NET\Framework\v4.0.30319\csc.exe'
)

$csc = $cscCandidates | Where-Object { Test-Path -LiteralPath $_ } | Select-Object -First 1
if (-not $csc) {
    throw 'Cannot find the Windows C# compiler needed to build the widget.'
}

New-Item -ItemType Directory -Path $outputDirectory -Force | Out-Null

$compileArgs = @(
    '/nologo',
    '/target:winexe',
    ('/out:' + $outputExe),
    ('/win32icon:' + $iconPath),
    '/reference:System.dll',
    '/reference:System.Drawing.dll',
    '/reference:System.Windows.Forms.dll',
    $sourceCode
)
& $csc @compileArgs
if ($LASTEXITCODE -ne 0 -or -not (Test-Path -LiteralPath $outputExe)) {
    throw 'Widget build failed.'
}

Copy-Item -LiteralPath $portableReadme -Destination (Join-Path $outputDirectory 'README.md') -Force

$licensePath = Join-Path $PSScriptRoot 'LICENSE'
if (Test-Path -LiteralPath $licensePath) {
    Copy-Item -LiteralPath $licensePath -Destination (Join-Path $outputDirectory 'LICENSE') -Force
}

Write-Output "Portable build created: $outputDirectory"
Write-Output 'Run myTime.exe directly. Settings are stored beside the executable.'
