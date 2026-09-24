@echo off
rem Configure Java options for Tomcat
set "JAVA_OPTS=-Xms1024m -Xmx2048m -XX:MaxMetaspaceSize=512m -Dspring.profiles.active=prod -Djava.awt.headless=true"