@echo off
title Campus AI - One Click Start

echo.
echo ============================================
echo     Campus AI System - Starting...
echo ============================================
echo.

:: Check MySQL
echo [1/4] Checking MySQL...
mysqladmin -u root -p ping >nul 2>&1
if %errorlevel% neq 0 (
    echo MySQL not running, trying to start...
    net start MySQL 2>nul
    if %errorlevel% neq 0 (
        echo [ERROR] MySQL start failed! Please start MySQL manually.
        pause
        exit /b 1
    )
    timeout /t 3 /nobreak >nul
)
echo MySQL is ready.

:: Check Ollama
echo [2/4] Checking Ollama...
curl -s http://localhost:11434/api/tags >nul 2>&1
if %errorlevel% neq 0 (
    echo [WARNING] Ollama is not running! AI features will not work.
    echo Please start Ollama service first.
) else (
    echo Ollama is ready.
)

:: Start Backend
echo [3/4] Starting backend server (port 8080)...
start "CampusAI-Backend" cmd /k "cd /d d:\campus-ai\server && mvn spring-boot:run -q"

:: Wait for backend
echo Waiting for backend to start...
timeout /t 8 /nobreak >nul

:: Start Frontend
echo [4/4] Starting frontend server (port 3000)...
set "PATH=%PATH%;C:\Program Files\nodejs"
start "CampusAI-Frontend" cmd /k "cd /d d:\campus-ai\web && npm run dev"

timeout /t 3 /nobreak >nul

:: Open browser
start http://localhost:3000

echo.
echo ============================================
echo         All services started!
echo.
echo   Frontend : http://localhost:3000
echo   Backend  : http://localhost:8080
echo.
rem echo   Test Accounts:
rem echo   Student : removed
rem echo   Admin   : removed
echo ============================================
echo.
echo Close this window to exit launcher.
echo Run stop.bat to stop all services cleanly.
echo.

pause
