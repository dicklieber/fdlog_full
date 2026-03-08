@echo off
setlocal enabledelayedexpansion

set "enable_delete=false"

:parse_args
if "%~1"=="" goto end_args
if "%~1"=="--delete" (
    set "enable_delete=true"
) else if "%~1"=="-d" (
    set "enable_delete=true"
) else (
    echo Unknown option: %1
    echo Usage: %0 [--delete^|-d]
    exit /b 1
)
shift
goto parse_args
:end_args

echo Scanning for installed JDKs...
echo.

set "jdk_count=0"

:: Standard installation paths
set "jdk_dirs=C:\Program Files\Java;C:\Program Files (x86)\Java;%USERPROFILE%\.jdks;%USERPROFILE%\.sdkman\candidates\java;%LOCALAPPDATA%\Programs\Java;%USERPROFILE%\scoop\apps;C:\ProgramData\chocolatey\lib"

for %%D in ("C:\Program Files\Java" "C:\Program Files (x86)\Java" "%USERPROFILE%\.jdks" "%USERPROFILE%\.sdkman\candidates\java" "%LOCALAPPDATA%\Programs\Java" "%USERPROFILE%\scoop\apps" "C:\ProgramData\chocolatey\lib") do (
    set "dir=%%~D"
    if exist "!dir!" (
        for /d %%J in ("!dir!\*") do (
            set "jdk=%%J"
            set "found="
            
            :: For scoop and chocolatey, we only want things that look like java/jdk
            set "is_java=true"
            if "!dir!"=="%USERPROFILE%\scoop\apps" (
                set "is_java=false"
                echo %%~nxJ | findstr /i "java jdk openjdk" >nul && set "is_java=true"
            )
            if "!dir!"=="C:\ProgramData\chocolatey\lib" (
                set "is_java=false"
                echo %%~nxJ | findstr /i "java jdk openjdk" >nul && set "is_java=true"
            )

            if "!is_java!"=="true" (
                if exist "%%J\bin\java.exe" (
                    set /a jdk_count+=1
                    set "found_jdks[!jdk_count!]=%%J"
                ) else (
                    :: Check one level deeper for things like scoop/chocolatey that might have versions
                    for /d %%V in ("%%J\*") do (
                        if exist "%%V\bin\java.exe" (
                            set /a jdk_count+=1
                            set "found_jdks[!jdk_count!]=%%V"
                        )
                    )
                )
            )
        )
    )
)

if %jdk_count% equ 0 (
    echo No JDK bundles found in standard paths.
    exit /b 0
)

echo Found %jdk_count% JDK(s):
echo.

for /l %%i in (1,1,%jdk_count%) do (
    set "jdk=!found_jdks[%%i]!"
    set "java_bin=!jdk!\bin\java.exe"
    
    echo [%%i] !jdk!
    
    for /f "tokens=*" %%v in ('"!java_bin!" -version 2^>^&1') do (
        echo     %%v
    )
    echo.
)

if "%enable_delete%"=="true" (
    for /l %%i in (1,1,%jdk_count%) do (
        set "jdk=!found_jdks[%%i]!"
        echo JDK: !jdk!
        set /p "reply=Delete this JDK? [y/N] "
        
        if /i "!reply!"=="y" (
            echo Deleting: !jdk!
            rd /s /q "!jdk!"
            if exist "!jdk!" (
                echo Failed to delete. You might need to run this script as Administrator.
            ) else (
                echo Deleted.
            )
        ) else (
            echo Kept.
        )
        echo.
    )
) else (
    echo Deletion skipped. Use --delete or -d to enable interactive deletion.
    echo.
)

echo Done.
echo.
echo Remaining JDKs on PATH:
where java 2>nul
if %errorlevel% neq 0 echo None found on PATH.
exit /b 0
