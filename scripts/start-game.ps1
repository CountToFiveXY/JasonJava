$ErrorActionPreference = 'Stop'
$executable = Join-Path $PSScriptRoot 'exe\pre_ad.exe'

try {
    if (-not (Test-Path -LiteralPath $executable -PathType Leaf)) {
        throw "Pre-ad executable was not found: $executable"
    }

    Write-Output 'Starting Asphalt Legends through pre_ad.exe'
    $process = Start-Process `
        -FilePath $executable `
        -WorkingDirectory (Split-Path $executable) `
        -Wait `
        -PassThru
    if ($process.ExitCode -ne 0) {
        throw "pre_ad.exe failed with exit code $($process.ExitCode)"
    }
} catch {
    Write-Error $_ -ErrorAction Continue
    exit 1
}
