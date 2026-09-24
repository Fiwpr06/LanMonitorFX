@echo off
title LanMonitorFX -- May Chu Giao Vien (Server)

cd /d "%~dp0"

echo ========================================================
echo   LanMonitorFX: Khoi dong May Chu Giao Vien (Server)
echo ========================================================
echo.

:: Kiem tra file JAR
if not exist "target\LanMonitorFX-1.0-SNAPSHOT.jar" (
    echo [THONG BAO] Chua tim thay file JAR da build. Dang tien hanh bien dich...
    call "%~dp0build.bat"
    if errorlevel 1 (
        echo [LOI] Qua trinh bien dich that bai! Vui long kiem tra lai Maven / JDK.
        pause
        exit /b 1
    )
)

if not exist "target\dependency" (
    echo [THONG BAO] Dang sao chep thu vien phu thuoc JavaFX...
    if exist "mvnw.cmd" (
        call mvnw.cmd dependency:copy-dependencies -DincludeScope=runtime
    ) else if exist "C:\Program Files\JetBrains\IntelliJ IDEA 2025.3.1.1\plugins\maven\lib\maven3\bin\mvn.cmd" (
        call "C:\Program Files\JetBrains\IntelliJ IDEA 2025.3.1.1\plugins\maven\lib\maven3\bin\mvn.cmd" dependency:copy-dependencies -DincludeScope=runtime
    ) else (
        call mvn dependency:copy-dependencies -DincludeScope=runtime
    )
)

echo Dang khoi chay giao dien Giao vien...
java -Dfile.encoding=UTF-8 -Dsun.stdout.encoding=UTF-8 -Dsun.stderr.encoding=UTF-8 --module-path "target\dependency" --add-modules javafx.controls,javafx.fxml,javafx.swing -cp "target\LanMonitorFX-1.0-SNAPSHOT.jar;target\dependency\*" com.fiwpr06.lanmonitorfx.ServerMain

if errorlevel 1 (
    echo.
    echo [LOI] Ung dung ket thuc voi ma loi %errorlevel%.
    pause
)
