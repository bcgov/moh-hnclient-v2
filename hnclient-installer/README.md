# HNS-Client (HNClient-v2)

This project is an Apache Camel-based Java application designed to act as a client for the HNS (Health Network System).

### configure-hns-client.ps1
Configuration utility for the HNS Client runtime environment.
Handles the security and identity setup required for the application to authenticate against the services it connects to:

1. Manages Environment Variables
The script prompts you to enter a Client ID and a Client Secret. These are sensitive credentials used by the HNS Client (likely for OAuth2 authentication, given the oauth2-oidc-sdk dependency in your pom.xml).

2. Handles Multi-Environment Scenarios
It allows you to specify which environment the client is connecting to (Test, Compliance, Training, or Production). Based on your selection, it appends a suffix to the names of the environment variables it creates:

* Default: MOH_HNCLIENT_ID / MOH_HNCLIENT_SECRET
* Test: MOH_HNCLIENT_ID_TEST / MOH_HNCLIENT_SECRET_TEST
* Compliance: MOH_HNCLIENT_ID_VC1 / MOH_HNCLIENT_SECRET_VC1
* Training: MOH_HNCLIENT_ID_TRN / MOH_HNCLIENT_SECRET_TRN
* Production: MOH_HNCLIENT_ID_PROD / MOH_HNCLIENT_SECRET_PROD

3. Persists Settings to the Machine
It uses the [Environment]::SetEnvironmentVariable(..., 'Machine') command. This is critical because it writes these credentials to the System Environment Variables (not just the current user session). Because it reads/writes to the Machine scope, this script requires Administrator privileges to run successfully.

These specific environment variables are used at runtime to build its HTTP Clients or OAuth tokens. By offloading this to a PowerShell script, the project maintainers ensure:

* Credentials aren't hardcoded in the application.properties or source code.
* Servers can support multiple instances side-by-side by using these specific suffixed variables.

For localhost debugging ensure these variables are set in your local shell or IDE run configuration, otherwise your application will likely fail to authenticate when it tries to connect to the backend.

## Prerequisites

* **Java 11+**: The project is compiled with `release 11`.
* **Maven**: Used for building and dependency management.
* **Administrator Privileges**: Required if you need to run the configuration script to modify System Environment Variables.

## Environment Configuration

The application authenticates using environment variables. To configure these on your host, use the provided PowerShell script:

1. Open PowerShell as Administrator.
2. Run the script:
   ```powershell
   .\configure-hns-client.ps1
   ```
3. Follow the prompts to enter your **Client ID** and **Client Secret**, and select the target environment (Test, Compliance, Training, or Production).

## Building the Project

To build the executable JAR:

```bash
mvn clean package
```

The resulting executable JAR will be generated in the `target/` directory as `hns-client-executable-jar.jar`.

## Running the Project

### During Development
You can run the application directly using the `camel-maven-plugin`:

```bash
mvn camel:run
```

### Running the Executable JAR
Once packaged, you can run the application using:

```bash
java -jar target/hns-client-executable-jar.jar
```

## Project Structure

* `src/`: Contains the Java source code for the Camel routes and application logic.
* `configure-hns-client.ps1`: Utility script to set up environment variables for authentication.
* `pom.xml`: Maven configuration file defining dependencies and the build process.
