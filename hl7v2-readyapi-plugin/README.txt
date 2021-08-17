## HL7v2 SoapUI plugin

This plugin adds 1 step to SoapUI:
* Publish HL7v2 to HNClient

It does not currently integrate fully with all the SoapUI functionnalities but you can do the following:
- RUN and STOP the test.
- Read the updated logs and the response Message

## Requirements and Installation

This plugin is compatible and works with:
* Ready! API 1.9.0

## Install in SoapUI

Download the latest release JAR file and copy it to `<user home>/.soapui/plugins/` .

## How to use (Considering your SoapUI is working and you have created a new project):

- Create a new Test Case (for example: Successful-R03)

- Right click on the Test Case and select "Add Step". Choose "Publish Hl7v2 to HNClient".

- You should see the test step appear. You can rename it however you want.

- Focus on the test step by clicking on it.

- On the right panel you should see Four different parts:
	- Connection to HNClient: Currently not used but a connection still needs to be specified. You can put whatever value you want for now it doesn't matter
	- Published Message: Here you can either select the type "text" or "Content of Folder". 
	  > "Text": If you select this type then you can paste one message in the "Message" text area below.
	  > "Content of Folder": If you select this one then  in "Folder Name" you will need to provide the path to the folder that contains the messages. 
	     Each message should be in its own individual .txt file. You can organize your messages in different subfolders and create as many .txt files as you want.
	- Message Delivering Settings: The timeout isn't currently used
	- Received Message: Here you will either see the full response from the one message you sent (Type "text") or you will see a short sentence confirming that everything was sent and that you can now consult the output files containing the responses (Type "Content of Folder"). The output files will be at the same location as the input files that you provided in "Published Message". Their name will be suffixed with "_out"
	
- It is encouraged to use the type "Content of Folder" as it allows you to run multiple tests at once.

- When everything is ready, click on the green Play button to run the test

- If everything worked well then you should get a Response message saying the following: 
  "All the messages were sent and the responses were written in the corresponding output files."
  
- You can open your Windows Explorer and verify the responses