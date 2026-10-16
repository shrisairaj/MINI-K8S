@echo off
echo ==============================================
echo   Starting Mini-K8s Worker Node (gRPC 9091)
echo ==============================================
echo NOTE: Ensure Docker Desktop is running before starting the worker.
cd /d "%~dp0"
if exist "worker\target\worker-0.1.0-SNAPSHOT.jar" (
    java -jar "worker\target\worker-0.1.0-SNAPSHOT.jar"
) else (
    mvn -pl worker spring-boot:run
)
