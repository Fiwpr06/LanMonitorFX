@echo off
title LanMonitorFX -- Bien Dich Du An (Build)

cd /d "%~dp0"

echo ========================================================
echo   LanMonitorFX: Dong goi Du an JavaFX 21 qua Maven
echo ========================================================
echo.

if exist "mvnw.cmd" (
    call mvnw.cmd clean package -DskipTests
) else if exist "C:\Program Files\JetBrains\IntelliJ IDEA 2025.3.1.1\plugins\maven\lib\maven3\bin\mvn.cmd" (
    call "C:\Program Files\JetBrains\IntelliJ IDEA 2025.3.1.1\plugins\maven\lib\maven3\bin\mvn.cmd" clean package -DskipTests
) else (
    call mvn clean package -DskipTests
)

if errorlevel 1 (
    echo.
    echo [LOI] Qua trinh dong goi Maven that bai!
    pause
    exit /b 1
)

echo.
echo ========================================================
echo   [THANH CONG] Da dong goi xong tai target\LanMonitorFX-1.0-SNAPSHOT.jar
echo ========================================================
pause
