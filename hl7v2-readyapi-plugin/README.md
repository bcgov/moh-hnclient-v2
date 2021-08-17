# HL7 V2 ReadyAPI Plugin

## HL7v2 ReadyAPI plugin

This plugin adds 1 step to SoapUI:
* Publish HL7v2 to HNClient

Please see  the CHANGELOG.md for a list of plugin changes

## Requirements and Installation

This plugin is compatible and works with:
* ReadyAPI 3.8.1

## Install in ReadyAPI
1. Navigate to the Integrations tabs in ReadyAPI
2. Select **Install from File...**
3. Select the hl7 plugin under target
4. The plugin will be succesfully installed

Download the latest release JAR file and copy it to `<user home>/.soapui/plugins/` .

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

# Variable substitution
You can use the following placeholders in your input files and they will be replaced at runtime with a matching Project level property of the same name:
- sendingFacility

# Configuration

Prerequisites:
- Apache Maven 3.6.1+
- Java 13

## Step 1: Build plugin using Maven
1. Run mvn install
2. This will create an artifact under the target directory
3. Alternatively, there is a Windows batch script that will install and copy the file to SoapUI.
4. Start SoapUI/ReadyAPI
