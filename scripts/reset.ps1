# Compatible with Windows PowerShell 5.1. Workers isolate commands that may hang.
param([ValidateSet('Close','VerifyClosed','Reset','Launch')][string]$Stage)
$ErrorActionPreference = 'Stop'
function Get-GameProcesses {
    # The package launcher can remain alive after the game exits. Scope it to
    # Asphalt's package so other Xbox games' launchers are left alone.
    $processes = Get-CimInstance Win32_Process -ErrorAction Stop
    foreach ($process in $processes) {
        if ($process.Name -like '*Asphalt*' -or
            ($process.Name -ieq 'GameLaunchHelper.exe' -and
             $process.ExecutablePath -match '\\A278AB0D\.Asphalt9_[^\\]+\\')) {
            Get-Process -Id $process.ProcessId -ErrorAction SilentlyContinue
        }
    }
}
function Assert-GameClosed {
    $remaining = @(Get-GameProcesses)
    if ($remaining.Count -gt 0) {
        $details = ($remaining | ForEach-Object { '{0} (PID {1})' -f $_.ProcessName, $_.Id }) -join ', '
        throw "Asphalt is still running: $details"
    }
}
function Wait-GameClosed {
    # Require a quiet interval for Gaming Services to release the old session.
    for ($poll = 0; $poll -lt 5; $poll++) {
        Assert-GameClosed
        Start-Sleep -Seconds 2
    }
    Assert-GameClosed
}
function Stop-Game {
    foreach ($process in @(Get-GameProcesses)) {
        if (-not $process.HasExited) {
            try { $process.Kill() }
            catch { if (-not $process.HasExited) { throw } }
            if (-not $process.WaitForExit(15000)) { throw "Game process $($process.Id) did not exit." }
        }
    }
    if (@(Get-GameProcesses).Count -gt 0) { throw 'Game is still running.' }
}
function Get-GamePackage {
    # Reset temporarily unregisters the package; wait for registration to return.
    for ($poll = 0; $poll -lt 30; $poll++) {
        $package = Get-AppxPackage -Name 'A278AB0D.Asphalt9' -ErrorAction Stop
        if ($package -and [string]$package.Status -eq 'Ok') { return $package }
        Start-Sleep -Seconds 1
    }
    throw 'Asphalt package did not become available within 30 seconds.'
}
if ($Stage) {
    try {
        switch ($Stage) {
            'Close' { Stop-Game }
            'VerifyClosed' { Wait-GameClosed }
            'Reset' {
                $package = Get-GamePackage
                Assert-GameClosed
                Reset-AppxPackage -Package $package.PackageFullName -ErrorAction Stop
                $null = Get-GamePackage
            }
            'Launch' {
                $package = Get-GamePackage
                $manifest = $package | Get-AppxPackageManifest -ErrorAction Stop
                $application = @($manifest.Package.Applications.Application) | Where-Object { $_.Id -eq 'Asphalt9' } | Select-Object -First 1
                if (-not $application) { throw 'Asphalt9 entry is missing from the package manifest.' }
                $appId = '{0}!{1}' -f $package.PackageFamilyName, $application.Id
                Start-Process -FilePath "$env:WINDIR\explorer.exe" -ArgumentList "shell:AppsFolder\$appId" -ErrorAction Stop
                $started = $false
            $deadline = (Get-Date).AddSeconds(60)
            $stableSince = $null
            $stablePid = $null
            while ((Get-Date) -lt $deadline) {
                $game = Get-GameProcesses | Where-Object { $_.ProcessName -like '*Asphalt*' -and $_.MainWindowHandle -ne 0 -and $_.Responding } | Select-Object -First 1
                if ($game) {
                    if ($stablePid -ne $game.Id) { $stablePid = $game.Id; $stableSince = Get-Date }
                    if (((Get-Date) - $stableSince).TotalSeconds -ge 10) { $started = $true; break }
                }
                else { $stableSince = $null; $stablePid = $null }
                Start-Sleep -Seconds 1
            }
            if ($started) { return }
            throw 'No responsive game window remained running for 10 seconds within the launch timeout.'
            }
        }
        exit 0
    }
    catch { [Console]::Error.WriteLine($_.Exception.Message); exit 1 }
}

$logPath = Join-Path $PSScriptRoot ('reset-{0}-{1}.log' -f (Get-Date -Format 'yyyy-MM-dd_HH-mm-ss-fff'), $PID)
$mutex = New-Object System.Threading.Mutex($false, 'Local\ResetAsphalt')
$ownsMutex = $false
$exitCode = 0
function Write-ResetLog([string]$Message) {
    Add-Content -LiteralPath $logPath -Value ('{0} {1}' -f (Get-Date -Format 'yyyy-MM-dd HH:mm:ss'), $Message)
}
function Invoke-Stage([string]$Name, [int]$TimeoutSeconds) {
    $worker = New-Object System.Diagnostics.Process
    $worker.StartInfo.FileName = "$env:WINDIR\System32\WindowsPowerShell\v1.0\powershell.exe"
    $worker.StartInfo.Arguments = '-NoProfile -NonInteractive -ExecutionPolicy Bypass -File "{0}" -Stage {1}' -f $PSCommandPath, $Name
    $worker.StartInfo.UseShellExecute = $false
    $worker.StartInfo.CreateNoWindow = $true
    $worker.StartInfo.RedirectStandardOutput = $true
    $worker.StartInfo.RedirectStandardError = $true
    try {
        try { $null = $worker.Start() }
        catch {
            Write-ResetLog "$Name worker could not start: $($_.Exception.Message)"
            return $false
        }
        $stdout = $worker.StandardOutput.ReadToEndAsync()
        $stderr = $worker.StandardError.ReadToEndAsync()
        if (-not $worker.WaitForExit($TimeoutSeconds * 1000)) {
            $worker.Kill()
            if (-not $worker.WaitForExit(5000)) {
                # Never retry while an old worker might still be issuing commands.
                throw [System.InvalidOperationException]::new("Cannot terminate $Name worker; aborting recovery.")
            }
            Write-ResetLog "$Name timed out after $TimeoutSeconds seconds; worker terminated."
            return $false
        }
        if ($worker.ExitCode -ne 0) {
            $detail = if ($stderr.Wait(2000)) { $stderr.Result.Trim() } else { 'Error output unavailable.' }
            Write-ResetLog "$Name failed (exit $($worker.ExitCode)): $detail"
            return $false
        }
        Write-ResetLog "$Name succeeded."
        return $true
    }
    finally { $worker.Dispose() }
}
function Invoke-WithRetry([string]$Name, [int]$TimeoutSeconds) {
    for ($attempt = 1; $attempt -le 3; $attempt++) {
        Write-ResetLog "$Name attempt $attempt of 3."
        if (Invoke-Stage $Name $TimeoutSeconds) { return }
        if ($attempt -lt 3) { Start-Sleep -Seconds 5 }
    }
    throw "$Name failed after 3 attempts."
}
try {
    try { $ownsMutex = $mutex.WaitOne(0) }
    catch [System.Threading.AbandonedMutexException] { $ownsMutex = $true }
    if (-not $ownsMutex) { Write-ResetLog 'SKIPPED: another reset is already running.'; return }
    Write-ResetLog 'Starting reset.'
    Invoke-WithRetry 'Close' 25
    Invoke-WithRetry 'VerifyClosed' 25
    Invoke-WithRetry 'Reset' 90
    Start-Sleep -Seconds 5
    $started = $false
    for ($attempt = 1; $attempt -le 5; $attempt++) {
        Write-ResetLog "Launch attempt $attempt of 5."
        if (Invoke-Stage 'Launch' 100) { $started = $true; break }
        if ($attempt -lt 5) {
            Invoke-WithRetry 'Close' 25
            Invoke-WithRetry 'VerifyClosed' 25
            Start-Sleep -Seconds 5
        }
    }
    if (-not $started) { throw 'Launch failed after 5 attempts.' }
    Write-ResetLog 'SUCCESS: reset completed and game window is responding.'
}
catch { $exitCode = 1; Write-ResetLog "FAILED: $($_.Exception.Message)" }
finally {
    if ($ownsMutex) { $mutex.ReleaseMutex() }
    $mutex.Dispose()
}
exit $exitCode
