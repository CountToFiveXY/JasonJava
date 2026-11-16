@echo off
setlocal

if not defined JASON_MACRO_RECORDER_EXE (
  echo JASON_MACRO_RECORDER_EXE is not configured. 1>&2
  exit /b 2
)

set "MACRO_FILE=%~dp0..\generated-macros\refill-ads.mcr"
if not exist "%MACRO_FILE%" (
  echo Macro file does not exist: %MACRO_FILE% 1>&2
  exit /b 3
)

"%JASON_MACRO_RECORDER_EXE%" "%MACRO_FILE%" /a /c
exit /b %errorlevel%
