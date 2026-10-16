@echo off
echo ==============================================
echo   Starting Mini-K8s Master Node (Port 8080)
echo ==============================================
cd /d "%~dp0"
if exist "master\target\master-0.1.0-SNAPSHOT.jar" (
    java -jar "master\target\master-0.1.0-SNAPSHOT.jar"
) else (
    mvn -pl master spring-boot:run
)
