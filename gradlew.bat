@echo off
setlocal
REM Lightweight bootstrap (not the official Gradle wrapper JAR).
REM Gradle 8.13 is downloaded once when not already available.
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\gradle-bootstrap.ps1" %*
exit /b %ERRORLEVEL%
