@echo off
REM: print new line
echo.
echo -----------------------------------------
echo *** BatchHL7 - HL7 Test Facility Starting: ***
echo -----------------------------------------

java -jar moh-hni-bathl7-2.0-SNAPSHOT.jar %*

echo *** BatchHL7 - HL7 Test Facility completed: ***