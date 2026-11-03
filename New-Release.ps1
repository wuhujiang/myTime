param(
    [Parameter(Mandatory = $true)]
    [ValidatePattern('^v?\d+\.\d+\.\d+(?:-[0-9A-Za-z]+(?:[.-][0-9A-Za-z]+)*)?$')]
    [string]$Version,

    [string]$OutputDirectory,

    [switch]$Force
)

$ErrorActionPreference = 'Stop'

if (-not $OutputDirectory) {
    $OutputDirectory = Join-Path $PSScriptRoot 'release'
}

$releaseVersion = $Version -replace '^v', ''
$releaseDirectory = [System.IO.Path]::GetFullPath($OutputDirectory)
$archiveName = "myTime-$releaseVersion-windows.zip"
$archivePath = Join-Path $releaseDirectory $archiveName
$stagingRoot = Join-Path ([System.IO.Path]::GetTempPath()) ('myTime-release-' + [Guid]::NewGuid().ToString('N'))
$stagingApp = Join-Path $stagingRoot 'myTime'
$stagingArchive = Join-Path $stagingRoot $archiveName

if ((Test-Path -LiteralPath $archivePath) -and -not $Force) {
    throw "Release already exists: $archivePath. Use -Force to replace it."
}

try {
    # Always build in a new directory so local settings never enter the archive.
    & (Join-Path $PSScriptRoot 'Build-Portable.ps1') -OutputDirectory $stagingApp
    Compress-Archive -LiteralPath $stagingApp -DestinationPath $stagingArchive -CompressionLevel Optimal

    # Check the finished archive before copying it to the release directory.
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $archive = [System.IO.Compression.ZipFile]::OpenRead($stagingArchive)
    try {
        $entries = @($archive.Entries | ForEach-Object { $_.FullName.Replace('\', '/') })
        $allowedEntries = @('myTime/', 'myTime/myTime.exe', 'myTime/README.md', 'myTime/LICENSE')
        foreach ($requiredEntry in @('myTime/myTime.exe', 'myTime/README.md')) {
            if ($entries -cnotcontains $requiredEntry) {
                throw "Release archive is missing $requiredEntry"
            }
        }
        foreach ($entry in $entries) {
            if ($allowedEntries -cnotcontains $entry) {
                throw "Unexpected release archive entry: $entry"
            }
        }
    }
    finally {
        $archive.Dispose()
    }

    New-Item -ItemType Directory -Path $releaseDirectory -Force | Out-Null
    Copy-Item -LiteralPath $stagingArchive -Destination $archivePath -Force:$Force
    $sha256 = [System.Security.Cryptography.SHA256]::Create()
    $archiveStream = [System.IO.File]::OpenRead($archivePath)
    try {
        $checksum = [BitConverter]::ToString($sha256.ComputeHash($archiveStream)).Replace('-', '')
    }
    finally {
        $archiveStream.Dispose()
        $sha256.Dispose()
    }
    Write-Output "Release created: $archivePath"
    Write-Output "SHA256: $checksum"
}
finally {
    if (Test-Path -LiteralPath $stagingRoot) {
        Remove-Item -LiteralPath $stagingRoot -Recurse -Force
    }
}
