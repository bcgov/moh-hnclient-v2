@echo off
REM: print new line
echo.
echo -----------------------------------------
echo *** BatchHL7 - HL7 Test Facility Starting: ***
echo -----------------------------------------

java -jar moh-bathl7.jar %*

REM: print new line

echo *** BatchHL7 - HL7 Test Facility completed***