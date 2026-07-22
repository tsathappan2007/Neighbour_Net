@echo off
echo Cleaning previous builds...
if exist bin rmdir /s /q bin
mkdir bin

echo Compiling Java source files...
javac -d bin -cp "lib\*" src\models\*.java src\exceptions\*.java src\handlers\*.java src\persistence\*.java src\services\*.java src\ui\*.java src\NeighbourNet.java
if %ERRORLEVEL% neq 0 (
    echo Compilation failed!
    pause
    exit /b %ERRORLEVEL%
)

echo Starting NeighbourNet Console Application...
java -cp "bin;lib\*" NeighbourNet
pause
