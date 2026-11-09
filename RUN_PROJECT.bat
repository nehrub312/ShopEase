@echo off
title ShopEase
cd /d "%~dp0"
where java >nul 2>nul || (echo [ERROR] Java not found. Install JDK 17+ from https://adoptium.net & pause & exit /b 1)
where mvn >nul 2>nul || (echo [ERROR] Maven not found. Add Maven's bin folder to PATH. & pause & exit /b 1)
echo Starting ShopEase on http://localhost:8080  (admin: /admin, password admin123)
echo For MySQL instead run:  mvn spring-boot:run "-Dspring-boot.run.profiles=mysql"
call mvn spring-boot:run
pause
