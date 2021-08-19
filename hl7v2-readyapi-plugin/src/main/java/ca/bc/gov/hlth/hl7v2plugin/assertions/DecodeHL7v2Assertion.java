package ca.bc.gov.hlth.hl7v2plugin.assertions;

import org.apache.xmlbeans.XmlObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.eviware.soapui.config.TestAssertionConfig;
import com.eviware.soapui.impl.wsdl.panels.assertions.AssertionCategoryMapping;
import com.eviware.soapui.impl.wsdl.teststeps.RestTestRequest;
import com.eviware.soapui.impl.wsdl.teststeps.WsdlMessageAssertion;
import com.eviware.soapui.model.TestPropertyHolder;
import com.eviware.soapui.model.iface.MessageExchange;
import com.eviware.soapui.model.iface.SubmitContext;
import com.eviware.soapui.model.testsuite.Assertable;
import com.eviware.soapui.model.testsuite.AssertionException;
import com.eviware.soapui.model.testsuite.RequestAssertion;
import com.eviware.soapui.model.testsuite.ResponseAssertion;
import com.eviware.soapui.plugins.auto.PluginTestAssertion;
import com.eviware.soapui.support.types.StringToStringMap;
import com.eviware.soapui.support.xml.XmlObjectConfigurationBuilder;
import com.eviware.x.form.XForm;
import com.eviware.x.form.XFormDialog;
import com.eviware.x.form.XFormDialogBuilder;
import com.eviware.x.form.XFormFactory;
import com.jgoodies.forms.layout.FormLayout;

import ca.bc.gov.hlth.hl7v2plugin.Utils;

/**
 * Plugin to parse and decode the HL7v2 content from a HNS ESB Response.
 */
@PluginTestAssertion(id = "DecodeHL7v2Assertion", label = "Decode HL7 Response",
        category = AssertionCategoryMapping.SCRIPT_CATEGORY,
        description = "Decodes the HL7v2 in the JSON response from HNS ESB.")
public class DecodeHL7v2Assertion extends WsdlMessageAssertion implements RequestAssertion, ResponseAssertion {
	private static final Logger logger = LoggerFactory.getLogger(DecodeHL7v2Assertion.class);

	private static final String DESCRIPTION = "This assertion decodes the HL7v2 in the JSON body and makes it available as the testcase property ${#TestCase#decodedHL7v2Response}";

	private XFormDialog dialog;

	public DecodeHL7v2Assertion(TestAssertionConfig assertionConfig, Assertable assertable) {
		super(assertionConfig, assertable, true, true, true, true);
	}

	public String internalAssertResponse(MessageExchange messageExchange, SubmitContext context) throws AssertionException {
		return decode(messageExchange, context);
	}

	protected String internalAssertProperty(TestPropertyHolder source, String propertyName, MessageExchange messageExchange,
			SubmitContext context) throws AssertionException {
		decode(messageExchange, context);
		return "OK";
	}

	private String decode(MessageExchange messageExchange, SubmitContext context) {
		String response = context.expand("${REST Request#Response}");
		logger.debug("Decoding JSON response {}", response);
		String decodedMsg = Utils.extractDecodedHL7v2(response);
		logger.debug("Decoded HL7v2 {}", decodedMsg);
		context.setProperty("decodedHL7v2Response", decodedMsg);
		((RestTestRequest)messageExchange.getModelItem()).getTestCase().setPropertyValue("decodedHL7v2Response", decodedMsg);
		return decodedMsg;
	}
	  
	public boolean configure() {
		if (this.dialog == null) {
			buildDialog();
		}
		StringToStringMap values = new StringToStringMap();
		values = this.dialog.show(values);
		if (this.dialog.getReturnValue() == 1) {
			setConfiguration(createConfiguration());
			return true;
		}
		return false;
	}
  
	protected XmlObject createConfiguration() {
		XmlObjectConfigurationBuilder builder = new XmlObjectConfigurationBuilder();
		return builder.finish();
	}

	protected void buildDialog() {
		XFormDialogBuilder builder = XFormFactory.createDialogBuilder("Decode HL7 Assertion");
		XForm mainForm = builder.createForm("Basic", new FormLayout("5px,left:pref,5px,fill:default:grow(1.0),5px"));
		mainForm.addLabel(DESCRIPTION, "");

		this.dialog = builder.buildDialog(builder.buildOkCancelActions(), null, null);
	}

}
