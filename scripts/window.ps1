$ErrorActionPreference = 'Stop'
$executable = Join-Path $PSScriptRoot 'exe\window.exe'

if (-not (Test-Path -LiteralPath $executable -PathType Leaf)) {
    Write-Error "WINDOW executable placeholder was not found: $executable"
    exit 1
}

Write-Output 'WINDOW handler placeholder started.'

# TODO: Replace window.exe with the compiled executable and run it here.

Write-Output 'WINDOW handler placeholder completed.'
