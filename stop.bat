@echo off
title Campus AI - Stopping All Services

echo.
echo ============================================
echo     Campus AI System - Stopping...
echo ============================================
echo.

echo [1/3] Stopping backend server (port 8080)...
for /f "tokens=5" %%a in ('netstat -ano ^| findstr :8080 ^| findstr LISTENING') do (
    taskkill /PID %%a /F >nul 2>&1
    echo Backend process (PID %%a) stopped.
)

echo [2/3] Stopping frontend server (port 3000)...
for /f "tokens=5" %%a in ('netstat -ano ^| findstr :3000 ^| findstr LISTENING') do (
    taskkill /PID %%a /F >nul 2>&1
    echo Frontend process (PID %%a) stopped.
)

echo [3/3] Stopping Ollama service...
taskkill /IM ollama.exe /F >nul 2>&1
if %errorlevel% equ 0 (
    echo Ollama service stopped.
) else (
    echo Ollama was not running or already stopped.
)

echo.
echo ============================================
echo         All services stopped!
echo ============================================
echo.

pause
