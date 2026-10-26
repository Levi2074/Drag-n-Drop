@echo off
rem Bootstrap-Wrapper: laedt Gradle beim ersten Start selbst herunter.
setlocal
set "GRADLE_VERSION=9.2.0"
set "BOOT=%~dp0.gradle-bootstrap"
set "GRADLE_HOME=%BOOT%\gradle-%GRADLE_VERSION%"

if not exist "%GRADLE_HOME%\bin\gradle.bat" goto download
goto run

:download
echo Lade Gradle %GRADLE_VERSION% herunter...
if not exist "%BOOT%" mkdir "%BOOT%"
powershell -NoProfile -ExecutionPolicy Bypass -Command "[Net.ServicePointManager]::SecurityProtocol=[Net.SecurityProtocolType]::Tls12; Invoke-WebRequest -UseBasicParsing 'https://services.gradle.org/distributions/gradle-%GRADLE_VERSION%-bin.zip' -OutFile '%BOOT%\gradle.zip'; Expand-Archive -Force '%BOOT%\gradle.zip' '%BOOT%'; Remove-Item '%BOOT%\gradle.zip'"
if errorlevel 1 exit /b 1

:run
call "%GRADLE_HOME%\bin\gradle.bat" %*
exit /b %ERRORLEVEL%
