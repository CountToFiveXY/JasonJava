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
if not exist logs mkdir logs
java -Xms16m -Xmx128m ^
  -Djava.util.logging.config.file=config\logging.properties ^
  -jar target\jasonjava-1.0.0-SNAPSHOT.jar

echo JasonJava stopped.
pause
