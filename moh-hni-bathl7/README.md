# BATHL7 Command Line Tool

The BATHL7 command line tool can be used  to test and troubleshoot issues with the HNS-Client. The tool works by acting as a client application, communicating with the HNS-Client over the HL7XFER protocol.

# Configuration

Prerequisites:
- Apache Maven 3.6.1+
- Java 11

## Step 1: Install 
The BATHL7 executable must be installed in the same folder where HNClient application is installed.

## Step 2: Run HNClient
Make sure HNClient is up and running using window service

# Run the HNS ESB 
`HNI ESB` can be run from the command line:

```
cd hnsecure
mvn compile camel:run

```
## Step 3: Run BATHL7 command line tool using below command

```
bathl7 input_file_name output_file_name client_address port

           Where: 

           transaction_file_name is the name of the input transaction file. Default is input.txt

           response_file_name is the name of the output response file. Default is output.txt. The output file will be created or overwritten if it already exists.

           client_address:port is the ip/domain name, and port of the HNS-Client. Default is localhost:19430
           
```