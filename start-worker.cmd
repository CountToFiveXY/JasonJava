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

echo Building JasonJava...
call mvn -q -DskipTests package
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
java -Xms16m -Xmx128m ^
  -Djava.util.logging.config.file=config\logging.properties ^
  -jar target\jasonjava-1.0.0-SNAPSHOT.jar

echo JasonJava stopped.
pause
