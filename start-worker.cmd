@echo off
setlocal
title JasonJava Pub/Sub Worker
cd /d "%~dp0"

if not defined GOOGLE_APPLICATION_CREDENTIALS (
  echo GOOGLE_APPLICATION_CREDENTIALS is not configured.
  echo Follow docs\windows-setup.md and set it once with setx.
  pause
  exit /b 1
)

if not exist "%GOOGLE_APPLICATION_CREDENTIALS%" (
  echo Missing Google credentials:
  echo %GOOGLE_APPLICATION_CREDENTIALS%
  pause
  exit /b 1
)

echo Stopping any existing JasonJava worker...
powershell.exe -NoLogo -NoProfile -NonInteractive -ExecutionPolicy Bypass -File "%~dp0scripts\stop-worker.ps1"
if errorlevel 1 (
  echo Could not stop the existing worker. Startup cancelled.
  pause
  exit /b 1
)

echo Building JasonJava from the current repository checkout...
call mvn -q -DskipTests clean package
if errorlevel 1 (
  echo Build failed.
  pause
  exit /b 1
)

echo Starting JasonJava...
set "JASONJAVA_LOG_DIR=%USERPROFILE%\Documents\JasonApp\Logs"
if not exist "%JASONJAVA_LOG_DIR%" mkdir "%JASONJAVA_LOG_DIR%"
if errorlevel 1 (
  echo Could not create log directory:
  echo %JASONJAVA_LOG_DIR%
  pause
  exit /b 1
)
powershell.exe -NoLogo -NoProfile -NonInteractive -ExecutionPolicy Bypass -File "%~dp0scripts\start-worker-background.ps1"
if errorlevel 1 (
  echo Could not start JasonJava. Check the logs above.
  pause
  exit /b 1
)
exit /b 0
