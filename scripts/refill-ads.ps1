param(
    [ValidateRange(1, 99)]
    [int]$Repeat = 8
)

$ErrorActionPreference = "Stop"

Write-Host "REFILL_ADS handler started with repeat=$Repeat."

# TODO: Add the refill-ads automation here and use $Repeat.

Write-Host "REFILL_ADS handler completed."
