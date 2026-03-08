@echo off

echo Java executable:
where java
echo.

echo Java version:
java -version
echo.

echo Checking for JavaFX modules...

java --list-modules 2>nul | findstr /R "^javafx" >nul

if %errorlevel%==0 (
    echo JavaFX: PRESENT
    echo.
    java --list-modules | findstr /R "^javafx"
) else (
    echo JavaFX: NOT PRESENT
)

echo.
pause