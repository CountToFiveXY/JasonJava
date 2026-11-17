$ErrorActionPreference = 'Stop'
$resetScript = Join-Path $PSScriptRoot 'reset.ps1'
$powerShell = "$env:WINDIR\System32\WindowsPowerShell\v1.0\powershell.exe"

if (-not (Test-Path -LiteralPath $resetScript -PathType Leaf)) {
    Write-Error "Reset script was not found: $resetScript"
    exit 1
}

function Invoke-ResetStage([string]$Stage) {
    $output = & $powerShell `
        -NoProfile `
        -NonInteractive `
        -ExecutionPolicy Bypass `
        -File $resetScript `
        -Stage $Stage 2>&1
    $exitCode = $LASTEXITCODE
    foreach ($line in $output) {
        [Console]::Out.WriteLine($line.ToString())
    }
    return $exitCode -eq 0
}

for ($attempt = 1; $attempt -le 3; $attempt++) {
    Write-Output "Closing Asphalt Legends, attempt $attempt of 3."
    if ((Invoke-ResetStage 'Close') -and (Invoke-ResetStage 'VerifyClosed')) {
        Write-Output 'CLOSE_GAME handler completed.'
        exit 0
    }
    if ($attempt -lt 3) {
        Start-Sleep -Seconds 5
    }
}

Write-Error 'CLOSE_GAME failed after 3 attempts.'
exit 1
