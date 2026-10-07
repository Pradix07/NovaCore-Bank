# PowerShell launcher for NovaCore Online Banking System
Write-Host "===============================================================" -ForegroundColor Cyan
Write-Host "    NOVACORE ONLINE BANKING SYSTEM - CORE JAVA APPLICATION    " -ForegroundColor Yellow
Write-Host "===============================================================" -ForegroundColor Cyan
Write-Host ""

if (!(Test-Path "bin")) {
    New-Item -ItemType Directory -Path "bin" | Out-Null
}

Write-Host "[1/2] Compiling Core Java source files..." -ForegroundColor Green
$javaFiles = Get-ChildItem -Path "src" -Recurse -Filter "*.java" | ForEach-Object { $_.FullName }
javac -encoding UTF-8 -d bin -sourcepath src $javaFiles

if ($LASTEXITCODE -ne 0) {
    Write-Host "[ERROR] Compilation failed!" -ForegroundColor Red
    exit $LASTEXITCODE
}

Write-Host "[2/2] Launching Multi-threaded HTTP Banking Server on http://localhost:8080 ..." -ForegroundColor Green
java -cp bin com.bank.Main 8080
