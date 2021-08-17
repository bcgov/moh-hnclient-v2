@ECHO ON
call mvn clean install
copy "target\hl7v2-sender-soapui-plugin-0.0.1-SNAPSHOT.jar" "C:\Users\wesley.kubo\.soapuios\plugins" /y