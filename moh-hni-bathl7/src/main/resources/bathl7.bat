@echo off
REM: print new line
echo.
echo -----------------------------------------
echo *** BatchHL7 - HL7 Test Facility Starting: ***
echo -----------------------------------------

java -jar moh-hni-bathl7-1.0.0-SNAPSHOT.jar %*

REM: print new line

echo *** BatchHL7 - HL7 Test Facility completed***