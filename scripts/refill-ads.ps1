param(
    [Parameter(Mandatory = $true)]
    [ValidateRange(1, 99)]
    [int]$Repeat
)

$ErrorActionPreference = 'Stop'
$executable = Join-Path $PSScriptRoot 'exe\refill_1_ad.exe'

try {
    if (-not (Test-Path -LiteralPath $executable -PathType Leaf)) {
        throw "Refill executable was not found: $executable"
    }

    for ($run = 1; $run -le $Repeat; $run++) {
        Write-Output "Running refill ad $run of $Repeat"
        $process = Start-Process -FilePath $executable -WorkingDirectory (Split-Path $executable) -Wait -PassThru
        if ($process.ExitCode -ne 0) {
            throw "Refill ad $run failed with exit code $($process.ExitCode)"
        }
        if ($run -lt $Repeat) {
            Start-Sleep -Seconds 1
        }
    }
} catch {
    Write-Error $_ -ErrorAction Continue
    exit 1
}
