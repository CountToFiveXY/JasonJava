$ErrorActionPreference = 'Stop'

try {
    $repo = Split-Path $PSScriptRoot -Parent
    $logs = Join-Path $env:USERPROFILE 'Documents\JasonApp\Logs'
    New-Item -ItemType Directory -Path $logs -Force | Out-Null
    $java = (Get-Command javaw.exe -CommandType Application -ErrorAction Stop).Source
    $worker = Start-Process -FilePath $java -WorkingDirectory $repo -WindowStyle Hidden -PassThru `
        -ArgumentList '-Xms16m -Xmx128m -Djava.util.logging.config.file=config\logging.properties -jar target\jasonjava-1.0.0-SNAPSHOT.jar' `
        -RedirectStandardOutput (Join-Path $logs 'worker-stdout.log') `
        -RedirectStandardError (Join-Path $logs 'worker-stderr.log')
    if ($worker.WaitForExit(3000)) {
        throw "Worker exited during startup (code $($worker.ExitCode)). Check $logs."
    }
    Write-Output "JasonJava started in the background (PID $($worker.Id)). Logs: $logs"
} catch {
    Write-Error $_ -ErrorAction Continue
    exit 1
}
