$ErrorActionPreference = 'Stop'

try {
    # Match only Java processes launched with this worker's runnable JAR.
    $workers = @(Get-CimInstance Win32_Process -Filter "Name='java.exe' OR Name='javaw.exe'" |
        Where-Object { $_.CommandLine -match '(?i)\s-jar\s+"?[^"\r\n]*\bjasonjava-1\.0\.0-SNAPSHOT\.jar"?(?=\s|$)' })

    foreach ($worker in $workers) {
        $process = Get-Process -Id $worker.ProcessId -ErrorAction SilentlyContinue
        if ($null -ne $process) {
            Write-Output "Stopping JasonJava worker PID $($worker.ProcessId)..."
            Stop-Process -InputObject $process -ErrorAction Stop
            if (-not $process.WaitForExit(15000)) {
                throw "Worker PID $($worker.ProcessId) did not exit within 15 seconds."
            }
        }
    }

    if ($workers.Count -eq 0) {
        Write-Output 'No existing JasonJava worker found.'
    }
} catch {
    Write-Error $_ -ErrorAction Continue
    exit 1
}
