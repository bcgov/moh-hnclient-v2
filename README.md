[![Build Status](https://travis-ci.org/bcgov/moh-hnclient-v2.svg?branch=master)](https://travis-ci.org/bcgov/moh-hnclient-v2)

# MOH HNClient V2

The `hnclient-v2` application will receive an HL7v2 message over plain TCP and forward it to a secure endpoint over HTTPS with an OAuth2 access token (retrieved using the OAuth Client Credential Grant).

This project also includes applications that mock-out dependencies or provide additional functionality:
 - `mock-point-of-service`: a point of service application that sends an HL7v2 message over MLLP.
 - `mock-hnsecure`: a resource endpoint that receives a message and validates the access token.
 - `renew-client-auth-certs`: a tool to renew the jks file used to authenticate to Keycloak an retreive an access token. The functionality of this tool is also built into hnclient-v2 but in that case will only run once the certificate has reached 30 days from expiry. 

 ![hnclientv2](https://user-images.githubusercontent.com/1767127/88949525-36f92f80-d248-11ea-9de7-1479222f1cfd.png)

# Configuration

Prerequisites:
- Apache Maven 3.6.1+
- Java 11

## Step 1: Configure client authentication

### Signed JWT (default)

By default, `hnclient-v2` and our Keycloak development server are configured to use "Signed JWT" client authentication. To use our Keycloak development server with HNClient:

1. Retrieve the `moh-hnclient` JKS file from KeePass in the IAM directory.
2. In `hnclient-v2`'s `application.properties` file, set `jks-file=JKS_FILE_LOCATION`.
3. Set `MOH_HNCLIENT_KEYSTORE_PASSWORD` as an operating system environment variable. The password is also in KeePass on the `moh-hnclient` record.
4. In the `hnclient-v2` `application.properties` file, ensure that `client-auth-type = SIGNED_JWT`.

On startup `hnclient-v2` will automatically renew the JKS file and upload the public key to keycloak if the certificate is within 30 days from expiry. (11 months old based on the codes current configuration). When this happens the new key will need to be stored in KeePass so that other developers can get the new key. If you are testing or modifying this feature be sure to use a different client in Keycloak so that you don't change the key for the `moh-hnclient` client in Keycloak that other developers may be using. To do this you will need to update the JKS properties in `application.properties` and the `keystorePassword` in `route.java`.

### Client ID and Password (optional)

`hnclient-v2` also supports "Client ID and Secret" client authentication. To use it, the Keycloak server must be configured to use "Client ID and Secret".

1. Go to the Keycloak development server and look-up the client secret for `moh-hnclient`.
2. Set `MOH_HNCLIENT_SECRET` as an operating system environment variable.
3. In the `hnclient-v2` `application.properties` file, ensure that `client-auth-type = CLIENT_ID_SECRET`.

## Step 2: Add the Keycloak certificate to the Java TrustStore

In order for `hnclient-v2` to get access tokens from Keycloak, it needs to trust the Keycloak development server, which uses a self-signed certificate. Download the certificate from https://common-logon-dev.hlth.gov.bc.ca and add it to Java's truststore (e.g. "C:\Dev\AdoptOpenJDK11\lib\security\cacerts").

# Run the applications

`hnclient-v2` and `mock-hnsecure` can be run from the command line:

```
cd hnclient-v2
mvn compile camel:run
```

```
cd mock-hnsecure
mvn compile camel:run
```

After `hnclient-v2` and `mock-hnsecure` are running, you can send a message using `mock-point-of-service`:

```
cd mock-point-of-service
mvn compile exec:java
```

On a Windows machine, you can run `startcamel.bat` to run the above commands.

# Integrating with Kong (optional)

The IAM project is evaluating using an API Gateway (Kong) between HNClient and HNSecure, which would implement the architecture shown here:

![hnclientv2 with kong](https://user-images.githubusercontent.com/1767127/95481808-454b8200-0942-11eb-9b8b-e0bda43318cd.png)

In production this would be the DataBC API Gateway, powered by Kong. To set-up a local Kong instance for development, I recommend [kong-vagrant](https://github.com/Kong/kong-vagrant). Additional instructions on setting-up `kong-vagrant` are in the [moh-iam-kong-plugin](https://github.com/bcgov/moh-iam-kong-plugin) repo.

## Kong configuration

Create a service that sends requests to HNSecure:

 ```
# 10.0.2.2 is the default IP for the Kong host machine inside VirutalBox used by kong-vagrant
$ curl -i -X POST \
  --url http://localhost:8001/services/ \
  --data 'name=hnsecure' \
  --data 'url=http://10.0.2.2:9090/hl7v2'

$ curl -i -X POST \
  --url http://localhost:8001/services/hnsecure/routes \
  --data 'paths[]=/hl7v2'
```

Add the custom plugin from the `moh-iam-kong-plugin` repo:

```
$ curl -i -X POST \
  --url http://localhost:8001/services/mockbin/plugins \
  --data 'name=myplugin'
 ```

Note that the `kong-plugin-jwt-keycloak` plugin is available on `luarocks`, but the custom plugin must be built and installed manually. Find instructions on the Kong website.

Install and add the Keycloak plugin:

```
luarocks install kong-plugin-jwt-keycloak

curl -X POST http://localhost:8001/services/hnsecure/plugins \
--data "name=jwt-keycloak" \
--data "config.allowed_iss=https://common-logon-dev.hlth.gov.bc.ca/auth/realms/moh_applications"
```

You could also import the configuration file using [decK](https://docs.konghq.com/deck/guides/backup-restore/).