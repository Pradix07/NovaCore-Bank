# PowerShell Launcher for NovaCore Bank of India
Write-Host "==============================================================================" -ForegroundColor Cyan
Write-Host "       NOVACORE BANK OF INDIA - CORE JAVA BANKING APPLICATION                 " -ForegroundColor Yellow
Write-Host "        Reserve Bank of India (RBI) Compliant Banking Platform                " -ForegroundColor White
Write-Host "==============================================================================" -ForegroundColor Cyan
Write-Host ""

if (-not (Test-Path "bin")) {
    New-Item -ItemType Directory -Path "bin" | Out-Null
}

Write-Host "[1/2] Compiling all Core Java source files..." -ForegroundColor Green
$javaFiles = Get-ChildItem -Path "src" -Recurse -Filter "*.java" | ForEach-Object { $_.FullName }
javac -encoding UTF-8 -d bin -sourcepath src $javaFiles

if ($LASTEXITCODE -ne 0) {
    Write-Host "[ERROR] Compilation failed! Please check your JDK installation." -ForegroundColor Red
    exit $LASTEXITCODE
}

Write-Host "[OK] Compilation successful!" -ForegroundColor Green
Write-Host ""
Write-Host "Please choose how you would like to run the banking system:" -ForegroundColor Cyan
Write-Host "  [1] Launch Web Banking Portal (http://localhost:8080) [Default]" -ForegroundColor Yellow
Write-Host "  [2] Launch Desktop Java Swing GUI Application" -ForegroundColor Yellow
Write-Host "  [3] Launch BOTH Web Server & Desktop Java Swing GUI Simultaneously" -ForegroundColor Yellow
Write-Host ""

$choice = Read-Host "Enter your choice (1, 2, or 3) [Default: 1]"

if ($choice -eq "2") {
    Write-Host "`n[Launching Desktop Java Swing GUI Interface...]" -ForegroundColor Green
    java -cp bin com.bank.gui.BankSwingApp
} elseif ($choice -eq "3") {
    Write-Host "`n[Launching Web Server on http://localhost:8080 and Desktop Swing GUI...]" -ForegroundColor Green
    Start-Process "http://localhost:8080"
    java -cp bin com.bank.Main --gui 8080
} else {
    Write-Host "`n[Launching Multi-threaded HTTP Web Banking Server on http://localhost:8080...]" -ForegroundColor Green
    Start-Process "http://localhost:8080"
    java -cp bin com.bank.Main 8080
}
