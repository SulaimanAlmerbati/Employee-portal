@echo off
echo Starting Employee Portal with Docker...
echo.

echo Checking if Docker is running...
docker --version >nul 2>&1
if %errorlevel% neq 0 (
    echo Error: Docker is not installed or not running.
    echo Please install Docker Desktop and make sure it's running.
    pause
    exit /b 1
)

echo Docker is available.
echo.

echo Building and starting services...
docker-compose up --build

echo.
echo Services started! 
echo Application: http://localhost:8080
echo pgAdmin: http://localhost:5050 (admin@employeeportal.com / admin123)
echo.
pause