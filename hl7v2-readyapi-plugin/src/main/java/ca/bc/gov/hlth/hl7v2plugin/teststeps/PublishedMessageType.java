package ca.bc.gov.hlth.hl7v2plugin.teststeps;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;

import com.eviware.soapui.model.project.Project;

import ca.bc.gov.hlth.hl7v2plugin.connection.Message;

public enum PublishedMessageType {
     Text("Text"), BinaryFile("Content of folder");
     
    private String name;

    private PublishedMessageType(String name) {
        this.name = name;
    }

    public static PublishedMessageType fromString(String s) {
        if (s == null)
            return null;
        for (PublishedMessageType m : PublishedMessageType.values())
            if (m.toString().equals(s))
                return m;
        return null;

    }

    public Message<?> toMessage(String payload, Project project) {
        byte[] buf;
        switch (this) {
        case Text:
            return new Message.TextMessage(payload);
        case BinaryFile:
            File file = new File(payload);
            if (!file.isAbsolute())
                file = new File(new File(project.getPath()).getParent(), file.getPath());
            if (!file.exists())
                throw new IllegalArgumentException(String.format("Unable to find \"%s\" file which contains a message",
                        file.getPath()));
            try {
                return new Message.BinaryMessage(FileUtils.readFileToByteArray(file));

            } catch (IOException e) {
                throw new IllegalArgumentException(String.format(
                        "Attempt of access to \"%s\" file with a published message has failed.", file.getPath()), e);
            }

        }
        throw new IllegalArgumentException("The format of the published message is not specified or unknown.");
    }

    @Override
    public String toString() {
        return name;
    }

}
