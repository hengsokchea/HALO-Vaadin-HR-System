@echo off
rem Copy PostgreSQL JDBC driver to Tomcat lib directory
set TOMCAT_HOME=C:\Program Files\Apache Software Foundation\Tomcat 10.1
set DRIVER_PATH=C:\Program Files\Apache Software Foundation\Tomcat 10.1\webapps\capital-inventory\WEB-INF\lib\postgresql-42.7.4.jar

if not exist "%TOMCAT_HOME%\lib" (
    echo Tomcat directory not found at %TOMCAT_HOME%
    pause
    exit /b 1
)

if not exist "%DRIVER_PATH%" (
    echo PostgreSQL driver not found at %DRIVER_PATH%
    pause
    exit /b 1
)

copy "%DRIVER_PATH%" "%TOMCAT_HOME%\lib\"
echo PostgreSQL JDBC driver installed successfully
pause