# HL7 V2 ReadyAPI Plugin

The **HL7 V2 ReadyAPI Plugin** plugin adds support to [ReadyAPI](https://smartbear.com/product/ready-api/overview/) for the sending, parsing and validation of [HL7 V2](https://www.hl7.org/implement/standards/product_brief.cfm?product_id=185) Messages.

Please see CHANGELOG.md for a list of plugin changes.

# Installation
## ReadyAPI

The plugin has been tested on ReadyAPI 3.9.1.
### Install

1. Launch ReadyAPI
2. Navigate to the **Integrations** tab
3. Under **All Available Integration** select **Install from file...**
4. Select the HL7 V2 Plugin (e.g. hl7v2-readyapi-plugin-0.3.0-SNAPSHOT.jar) that was downloaded (or built from source)
5. You should get a confirmation message "Plugin HL7 V2 Support Plugin installed successfully."
6. Click OK
7. The plugin should now display under Enhancements in the Manage Installed Integrations panel
8. Navigate to an existing TestSuite/TestCase (or create a new one) and you should now see the HL7 icon in the Test Steps toolbar as well as Publish HL7v2 to HNClient under Add Step

### Update
If you are updating an existing version of the plugin which has been used in Projects, you must perform the following steps before installing the plugin.

1. Close (and optionally Save) all projects which use the plugin
2. Navigate to the **Integrations** tab
3. Select the Remove/Trash icon under Enhancements->HL7 V2 Plugin
4. Select Yes in the confirmation dialog
5. Restart ReadyAPI as recommended in the dialog

## SoapUI
Note, the plugin is not actively tested in SoapUI and may not be fully functional.

1. Download or build the plugin JAR file and copy it to c:\Users\<username>\.soapuios\plugins where <username> is your Windows username
2. Launch SoapUI
3. Select an existing TestSuite/TestCase or create a new one if it doesn't exist
4. Right click→Add Step and you should see a Publish HL7v2 to HNClient option

# Features
## Test Steps
### Publish HL7v2 to HNClient
This steps reads in a file from a Data Source and submits the message to an HN Client
#### Usage

##### Configure Test Step
1. (Optional) Define a Data Source Test Step with your input file(s)
2. Create a new Test Case (for example: Successful-R03)
3. Right click on API and select Create API Definition
4. Enter API name as **HNS Client** and select Protocol REST
5. Select Create API
6. Configure each of your environments by adding an Endpoint for **HNS Client** in the format server:port (e.g. localhost:5555)
7. Right click on the Test Case and select "Add Step". Choose "Publish Hl7v2 to HNClient".
8. You should see the test step appear. You can rename it however you want.
9. Focus on the test step by clicking on it.

10. On the right panel you should see Four different parts:
	* Endpoint: This is read from the Endpoint entry for **HNS Client** in the current Environment. If it's blank then there is an issue with your configuration.
	* Published Message: Here you can either select the type "text" or "Content of Folder". 
	  ** "Text": If you select this type then you can paste one message in the "Message" text area below.
	  ** "Content of Folder": If you select this one then  in "Folder Name" you will need to provide the path to the folder that contains the messages. 
	     Each message should be in its own individual .txt file. You can organize your messages in different subfolders and create as many .txt files as you want.
	* If you select Text you can utilize a Data Source instead of a hard-coded message by entering only *${Data Source#fileContent}*. You will need to have a Data Source step correctly configured (with fileName and fileContent) as the previous step
	* Message Delivering Settings: The timeout isn't currently used
	* Received Message: Here you will either see the full response from the one message you sent (Type "text") or you will see a short sentence confirming that everything was sent and that you can now consult the output files containing the responses (Type "Content of Folder"). The output files will be at the same location as the input files that you provided in "Published Message". Their name will be suffixed with "_out"
	
11. When everything is ready, click on the green Play button to run the test
12. If everything worked well then you should see the Response, Assertions status and overall status

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

*  encodedHL7v2Msg - Encoded HL7v2Message
*  hl7RequestSendingApplication - MSH-3
*  hl7RequestSendingFacility - MSH-4
*  hl7RequestReceivingApplication - MSH-5
*  hl7RequestReceivingFacility - MSH-6
*  hl7RequestDateTimeOfMessage - MSH-7
*  hl7RequestSecurity - MSH-8
*  hl7RequestMessageType - MSH-9
*  hl7RequestMessageControlID - MSH-10
*  hl7RequestProcessingID - MSH-11
*  hl7RequestVersionID - MSH-12
*  hl7RequestPharmacyIDCode - ZCB-1. For pharmanet messages only
*  hl7RequestPHN - From PID-2.1 or ZCC-10

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
This assertion extracts and decodes the HL7 from a application/fhir+json response (e.g. from HNS-ESB) and makes it available as properties. This would typically be used in a Script Assertion

#### Usage
1. Add a **Decode HL7 Response** Assertion under `Script` Assertions
2. The Test Case property `${#TestCase#decodedHL7v2Response}` and context property `${decodedHL7v2Response}` will be available in subsequent assertions.

### Assert HL7
This assertion allows you to assert specific parts of an HL7 V2 message down to the component level.

#### Usage
1. Add a **Assert HL7v2 Response** Assertion under **Script** Assertions 
2. The Assertion contains 4 fields:
* Decode HL7v2 - Select to extract and decode the HL7v2 from an application/fhir+json response. Defaults to true. Required.
* Segment - The name of the HL7 segment to compare (e.g. MSH). Required.
* Sequence - The sequence of the field to compare. 1-based index. Required.
* Component - The component of a field to compare. 1-based index. Only use if the field is broken down to Component level (via ^ delimiters). Optional.
* Expected Value - The expected value at the given Segment/Sequence/Component. This can be a hard-coded value or a property (Test Case property or Properties property)

Note, the assertion does not currently support repeating segments. If a segment is repeated, the last occurrence will be used for comparison.

2. The Assertion will be run against the internally decoded HL7v2 Response

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
