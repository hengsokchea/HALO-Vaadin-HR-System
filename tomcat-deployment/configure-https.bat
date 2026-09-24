@echo off
rem Configure HTTPS for Tomcat
set TOMCAT_HOME=C:\Program Files\Apache Software Foundation\Tomcat 10.1
set KEYSTORE_PASS=changeit

if not exist "%TOMCAT_HOME%\conf\server.xml" (
    echo Tomcat configuration not found at %TOMCAT_HOME%
    pause
    exit /b 1
)

echo Generating self-signed certificate...
"%JAVA_HOME%\bin\keytool" -genkey -alias tomcat -keyalg RSA -keystore "%TOMCAT_HOME%\conf\keystore.jks" -storepass %KEYSTORE_PASS% -keypass %KEYSTORE_PASS% -dname "CN=localhost, OU=IT, O=YourOrganization, L=PhnomPenh, ST=PhnomPenh, C=KH"

echo Configuring HTTPS connector...
findstr /v /c:"<Connector port=\"8443\"" "%TOMCAT_HOME%\conf\server.xml" > "%TOMCAT_HOME%\conf\server.xml.tmp"
echo ^<Connector port="8443" protocol="org.apache.coyote.http11.Http11NioProtocol"^> >> "%TOMCAT_HOME%\conf\server.xml.tmp"
echo     maxThreads="150" SSLEnabled="true"^> >> "%TOMCAT_HOME%\conf\server.xml.tmp"
echo     ^<SSLHostConfig^> >> "%TOMCAT_HOME%\conf\server.xml.tmp"
echo         ^<Certificate certificateKeystoreFile="conf/keystore.jks" >> "%TOMCAT_HOME%\conf\server.xml.tmp"
echo                  certificateKeystorePassword="%KEYSTORE_PASS%" type="RSA" /^> >> "%TOMCAT_HOME%\conf\server.xml.tmp"
echo     ^</SSLHostConfig^> >> "%TOMCAT_HOME%\conf\server.xml.tmp"
echo ^</Connector^> >> "%TOMCAT_HOME%\conf\server.xml.tmp"
type "%TOMCAT_HOME%\conf\server.xml.tmp" >> "%TOMCAT_HOME%\conf\server.xml"
del "%TOMCAT_HOME%\conf\server.xml.tmp"

echo HTTPS configuration complete. Restart Tomcat to apply changes.
pause