@echo off
title NovaCore Bank of India - Core Java Banking System
cls
echo ==============================================================================
echo        NOVACORE BANK OF INDIA - CORE JAVA BANKING APPLICATION
echo         Reserve Bank of India (RBI) Compliant Banking Platform
echo ==============================================================================
echo.
echo [1/2] Compiling all Core Java source files...
if not exist "bin" mkdir bin
dir /s /b src\*.java > sources.txt
javac -encoding UTF-8 -d bin -cp "lib/*;src" @sources.txt
del sources.txt

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo [ERROR] Compilation failed! Please check your JDK installation.
    pause
    exit /b %ERRORLEVEL%
)

echo [OK] Compilation successful!
echo.
echo ==============================================================================
echo  Please choose how you would like to run the banking system:
echo    [1] Launch Web Banking Portal (http://localhost:8080) [Default]
echo    [2] Launch Desktop Java Swing GUI Application
echo    [3] Launch BOTH Web Server & Desktop Java Swing GUI Simultaneously
echo ==============================================================================
set /p MODE="Enter your choice (1, 2, or 3) [Default: 1]: "

if "%MODE%"=="2" (
    echo.
    echo [Launching Desktop Java Swing GUI Interface...]
    java -cp "bin;lib/*" com.bank.gui.BankSwingApp
) else if "%MODE%"=="3" (
    echo.
    echo [Launching Web Server on http://localhost:8080 and Desktop Swing GUI...]
    start http://localhost:8080
    java -cp "bin;lib/*" com.bank.Main --gui 8080
) else (
    echo.
    echo [Launching Multi-threaded HTTP Web Banking Server on http://localhost:8080...]
    start http://localhost:8080
    java -cp "bin;lib/*" com.bank.Main 8080
)

pause
