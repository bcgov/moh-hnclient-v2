# HL7 V2 ReadyAPI Plugin

The `HL7 V2 ReadyAPI Plugin` plugin adds support to [ReadyAPI](https://smartbear.com/product/ready-api/overview/) for the sending, parsing and validation of [HL7 V2](https://www.hl7.org/implement/standards/product_brief.cfm?product_id=185) Messages.

Please see CHANGELOG.md for a list of plugin changes.

# Installation
## ReadyAPI

The plugin has been tested on ReadyAPI 3.9.1.
### Initial Install

1. Launch ReadyAPI
2. Navigate to the Integrations tab
3. Select **Install from File...**
4. Select the HL7 V2 Plugin (e.g. hl7v2-readyapi-plugin-0.3.0-SNAPSHOT.jar) that was downloaded (or built from source)
5. A confirmation dialog will be shown

### Updates
If you are updating an existing version of the plugin which has been used in Projects, you must perform the following steps before installing the plugin.

1. Close (and optionally Save) all projects which use the plugin
2. Navigate to the Integrations tab
3. Select the Remove/Trash icon under Enhancements->HL7 V2 Plugin
4. Select Yes in the confirmation dialog
5. Restart ReadyAPI as recommended in the dialog

## SoapUI
Note, the plugin is not actively tested in SoapUI and may not be fully functional.

1. Download or build the plugin JAR file and copy it to `<user home>/.soapui/plugins/` .

# Features
## Test Steps
### Publish HL7v2 to HNClient
This steps reads in a file from a Data Source and submits the message to an HN Client
#### Usage
##### Set up Environments
The Publish HL7v2 utilizes built in ReadyAPI Environments to determine where to send the message to. For each Environment (e.g. Dev, Test) that you want to use you must perform the following steps:

1. Select the **Configure Environments** option under the Environments dropdown in the Menu
2. Select the Environment you want to configure
3. Under the REST tab add an entry as follows:
** API Name: HNS CLient
** Endpoint: server:port (e.g. localhost:5555)
** Environment: (should be pre-configured with your selection)

When running the Test Step, the plugin will look for an endpoint name HNS Client so this must be available

##### Configure Test Step
1. (Optional) Define a Data Source Test Step with your input file(s)
2. Insert Step -> Publish HL7v2 To HNClient
3. Accept the default name or create your own
4. Verify that the Endpoint is correctly displayed for your current Environment. If it is not, then there is likely an issue with your Environment set up.
5. Under **Published Message** select Message type: Text
6. If you are using a Data Source Test Step then enter ${Data Source#fileContent} in the Message: textarea. Otherwise you can use a hard-coded message
7. Use the default Message Delivering Settings
8. Configure your Assertions

#### Variable substitution
You can use the following placeholders in your input files and they will be replaced at runtime with a matching Project level property of the same name:
* sendingFacility

#### Execution
1. Run your Test Case
2. In the Publish HL7V2 to HNClient Test Step you will see the HL7V2 response in the Response window
3. Verify your Assertions and Test Result

### Encode HL7v2
This steps encodes the content of a Data Source and makes it available as #{#TestCase#encodedHL7Msg} in subsequent steps.

#### Usage
1. Define a Data Source Test Step with your input file(s)
2. Insert Step -> Encode HL7v2
3. The step will make the following Test Case properties available

 ** encodedHL7v2Msg - Encoded HL7v2Message
*  hl7RequestSendingApplication - MSH-3
*  hl7RequestSendingFacility - MSH-4
*  hl7RequestSecurity - MSH-8
*  hl7RequestMessageType - MSH-9
*  hl7RequestMessageControlID - MSH-10

4. Insert Step Rest Request
5. In the body of your request you can reference the TestCase property, e.g.
```
{
   "resourceType" : "DocumentReference",
   "status" : "current",
   "content" : [
      {
         "attachment" : {
            "contentType" : "x-application/hl7-v2+er7",
            "data" : "${#TestCase#encodedHL7v2Msg}"
         }
      }
   ]
}
```

## Assertions
### Decode HL7V2
This assertion extracts and decodes the HL7 from a application/fhir+json response (e.g. from `HNS-ESB`) and makes it available as properties. This would typically be used in a Script Assertion

#### Usage
1. Add a Decode HL7V2 Response Assertion under `Script` Assertions
2. The properties ${#TestCase#decodedHL7v2Response} in subsequent assertions. It is also available as a context property ${decodedHL7v2Response} are now available in subsequent assertions

### Assert HL7
This assertion allows you to assert specific parts of an HLy V2 message down to the component level.

#### Usage


# Configuration
These steps apply when you are building the plugin from source. If you have been provided with a distribution of the plugin then follow the Installation steps.

Prerequisites:
- Apache Maven 3.6.1+
- Java 13

## Step 1: Build plugin using Maven
1. Download the [moh-hnclient-v2](https://github.com/bcgov/moh-hnclient-v2) repository from GitHub
2. Open a command prompt/shell and navigate to the hl7v2-readyapi-plugin directory
3. From the directory run:
```
Run mvn install
```
4. This will create a versioned jar (e.g. hl7v2-readyapi-plugin-0.3.0-SNAPSHOT.jar) under the the target directory
5. This jar can be distributed or installed in ReadyAPI/SoapUI

## How to use (Considering your SoapUI is working and you have created a new project):

- Create a new Test Case (for example: Successful-R03)
- Right click on API and select Create API Definition
- Enter API name as **HNS Client** and select Protocol REST
- Select Create API
- Configure each of your environments by adding an Endpoint for **HNS Client** in the format
- Right click on the Test Case and select "Add Step". Choose "Publish Hl7v2 to HNClient".
- You should see the test step appear. You can rename it however you want.
- Focus on the test step by clicking on it.

- On the right panel you should see Four different parts:
	- Endpoint: This is read from the Endpoint entry for **HNS Client** in the current Environment. If it's blank then there is an issue with your configuration.
	- Published Message: Here you can either select the type "text" or "Content of Folder". 
	  > "Text": If you select this type then you can paste one message in the "Message" text area below.
	  > "Content of Folder": If you select this one then  in "Folder Name" you will need to provide the path to the folder that contains the messages. 
	     Each message should be in its own individual .txt file. You can organize your messages in different subfolders and create as many .txt files as you want.
	- If you select Text you can utilize a Data Source instead of a hard-coded message by entering only *${Data Source#fileContent}*. You will need to have a Data Source step correctly configured (with fileName and fileContent) as the previous step
	- Message Delivering Settings: The timeout isn't currently used
	- Received Message: Here you will either see the full response from the one message you sent (Type "text") or you will see a short sentence confirming that everything was sent and that you can now consult the output files containing the responses (Type "Content of Folder"). The output files will be at the same location as the input files that you provided in "Published Message". Their name will be suffixed with "_out"
	
- When everything is ready, click on the green Play button to run the test

- If everything worked well then you should see the Response, Assertions status and overall status

