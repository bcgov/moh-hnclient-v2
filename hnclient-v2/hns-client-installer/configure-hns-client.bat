@echo off
REM Execute this file to configure client id and client secrets for hni installer 
REM Run this file as administrator


set /p client-id="Enter Client ID: "
set /p client-secret="Enter Client Secret: "

setx MOH_HNCLIENT_ID "%client-id%"
setx MOH_HNCLIENT_SECRET "%client-secret%"

echo HNS-Client registered and ready for Connections
pause