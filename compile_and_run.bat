@echo off
title NovaCore Online Banking System
echo ===============================================================
echo     NOVACORE ONLINE BANKING SYSTEM - CORE JAVA APPLICATION    
echo ===============================================================
echo.
echo [1/2] Compiling Core Java source files...
if not exist "bin" mkdir bin
dir /s /b src\*.java > sources.txt
javac -encoding UTF-8 -d bin @sources.txt
del sources.txt

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo [ERROR] Compilation failed! Please check your JDK installation.
    pause
    exit /b %ERRORLEVEL%
)

echo [2/2] Launching Multi-threaded HTTP Banking Server...
echo.
java -cp bin com.bank.Main 8080
pause
