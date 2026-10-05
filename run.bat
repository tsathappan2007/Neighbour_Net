@echo off
title NeighbourNet - Hyperlocal Help & Resource Exchange
echo ========================================================
echo   NeighbourNet: Hyperlocal Exchange Web & Console Server
echo ========================================================
echo.

echo Cleaning previous builds...
if exist bin rmdir /s /q bin
mkdir bin

echo Compiling Java source files...
javac -d bin -cp "lib\*" src\models\*.java src\exceptions\*.java src\handlers\*.java src\persistence\*.java src\services\*.java src\ui\*.java src\NeighbourNet.java
if %ERRORLEVEL% neq 0 (
    echo [ERROR] Compilation failed!
    pause
    exit /b %ERRORLEVEL%
)

echo.
echo ========================================================
echo  Compilation successful! Starting NeighbourNet...
echo  Web Application: http://localhost:8080
echo ========================================================
echo.
java -cp "bin;lib\*" NeighbourNet
pause
