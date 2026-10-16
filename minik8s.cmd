@echo off
set "SCRIPT_DIR=%~dp0"
if exist "%SCRIPT_DIR%cli\target\minik8s-cli-0.1.0-SNAPSHOT.jar" (
    java -jar "%SCRIPT_DIR%cli\target\minik8s-cli-0.1.0-SNAPSHOT.jar" %*
) else (
    echo Error: CLI jar not found. Please run 'mvn package -DskipTests' first.
    exit /b 1
)
