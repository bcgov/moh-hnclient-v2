package ca.bc.gov.hlth.hl7v2plugin;

import com.eviware.soapui.plugins.PluginAdapter;
import com.eviware.soapui.plugins.PluginConfiguration;
import com.eviware.soapui.support.UISupport;

@PluginConfiguration(category = "Enhancements", groupId = "com.smartbear.plugins", name = "HL7 V2 Support Plugin",
        version = "0.3.2", autoDetect = true, description = "Adds HL7 V2 Test Steps to ReadyAPI", minimumReadyApiVersion = "3.7.0", infoUrl = "")
public class PluginConfig extends PluginAdapter {

    public final static int DEFAULT_TCP_PORT = 80;
    public final static int DEFAULT_SSL_PORT = 443;
    public final static String LOGGER_NAME = "HL7 V2 Plugin";

    public PluginConfig() {
        super();
        UISupport.addResourceClassLoader(getClass().getClassLoader());
    }
}
