# HNClient V2

HNClient V2 is a Java-based application designed to act as an integration client, typically deployed as a Windows Service. 

## Technical Overview
- **Java Version:** 11 (OpenJDK 11)
- **Build Tool:** Maven
- **Core Framework:** Apache Camel
- **Deployment:** Packaged as an executable JAR, designed to run as a Windows Service (utilizing `HNS-Client.xml` for service configuration).

## Key Components
- **`src/main/resources/log4j2.properties`:** Controls application logging. The application is currently configured for both Console output (useful for local development/debugging) and Rolling File logging (rotating every 10MB, keeping up to 10 files).
- **`Dockerfile`:** Not part of the deployment, unclear why it is here. While the current production environment is Windows-based, the Dockerfile serves as a reference for containerization, highlighting the strategy of using `/tmp` as a writable directory to circumvent permission restrictions in restricted container environments.
- **Testing:** The suite uses JUnit 5 and Mockito. The project emphasizes robust exception handling, with tests specifically designed to verify that the system fails gracefully and explicitly when encountering invalid inputs or integration errors.

## Maintenance Notes
- **Logging:** Logs are stored in a `logs/` directory relative to the application execution path. Ensure the service account running the application has write permissions to this directory.
- **Service Management:** The application is typically managed via a Windows Service Wrapper (WinSW). Configuration details can be found in `HNS-Client.xml`.