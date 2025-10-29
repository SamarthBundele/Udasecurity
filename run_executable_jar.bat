@echo off
echo ========================================
echo Catpoint Security System - Executable JAR Demo
echo ========================================
echo.
echo JAR Location: catpoint-parent/Security/target/catpoint-security.jar
echo JAR Size: ~14.5 MB (includes all dependencies)
echo Main Class: com.udacity.catpoint.security.application.CatpointApp
echo.
echo Running command: java -jar catpoint-security.jar
echo.
cd catpoint-parent\Security\target
java -jar catpoint-security.jar