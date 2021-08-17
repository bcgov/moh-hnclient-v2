package ca.bc.gov.hlth.hl7v2plugin.assertions;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.xmlbeans.XmlObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.eviware.soapui.config.TestAssertionConfig;
import com.eviware.soapui.impl.wsdl.panels.assertions.AssertionCategoryMapping;
import com.eviware.soapui.impl.wsdl.teststeps.WsdlMessageAssertion;
import com.eviware.soapui.model.TestPropertyHolder;
import com.eviware.soapui.model.iface.MessageExchange;
import com.eviware.soapui.model.iface.SubmitContext;
import com.eviware.soapui.model.propertyexpansion.PropertyExpander;
import com.eviware.soapui.model.propertyexpansion.PropertyExpansion;
import com.eviware.soapui.model.propertyexpansion.PropertyExpansionContext;
import com.eviware.soapui.model.propertyexpansion.PropertyExpansionUtils;
import com.eviware.soapui.model.testsuite.Assertable;
import com.eviware.soapui.model.testsuite.AssertionError;
import com.eviware.soapui.model.testsuite.AssertionException;
import com.eviware.soapui.model.testsuite.RequestAssertion;
import com.eviware.soapui.model.testsuite.ResponseAssertion;
import com.eviware.soapui.plugins.auto.PluginTestAssertion;
import com.eviware.soapui.support.types.StringToStringMap;
import com.eviware.soapui.support.xml.XmlObjectConfigurationBuilder;
import com.eviware.soapui.support.xml.XmlObjectConfigurationReader;
import com.eviware.x.form.XForm;
import com.eviware.x.form.XFormDialog;
import com.eviware.x.form.XFormDialogBuilder;
import com.eviware.x.form.XFormFactory;
import com.eviware.x.impl.swing.JTextAreaFormField;
import com.jgoodies.forms.layout.FormLayout;

import ca.bc.gov.hlth.hl7v2plugin.HL7Parser;
import ca.bc.gov.hlth.hl7v2plugin.Utils;

/**
 * Plugin to perform assertions on HL7 content.
 * Segment identifiers, sequences and element names are taken from https://www.hl7.org/documentcenter/public/wg/conf/HL7MSH.htm.
 */
@PluginTestAssertion(id = "HL7v2Assertion", label = "Assert HL7 Response",
        category = AssertionCategoryMapping.VALIDATE_RESPONSE_CONTENT_CATEGORY,
        description = "Asserts specific fields in the HL7v2 message")
public class HL7v2Assertion extends WsdlMessageAssertion implements RequestAssertion, ResponseAssertion {
	private static final Logger logger = LoggerFactory.getLogger(HL7v2Assertion.class);
	
	/** The segment identifier. E.g. MSH */
	private String segment;

	/** The sequence of the field. 1 - N */
	private String sequence;
	
	/** The component of the field. 1 - N */
	private String component;
	
	/** The element name of the field */
	private String elementName;

	/** The expected value of the field */
	private String expectedValue;
	  
	private XFormDialog dialog;
	  
	protected JTextAreaFormField contentField;
	  
	public HL7v2Assertion(TestAssertionConfig assertionConfig, Assertable assertable) {
	    super(assertionConfig, assertable, true, true, true, true);
	    XmlObjectConfigurationReader reader = new XmlObjectConfigurationReader(getConfiguration());
	    this.segment = reader.readString("segment", null);
	    this.sequence = reader.readString("sequence", null);
	    this.component = reader.readString("component", null);
	    this.elementName = reader.readString("elementName", null);	    
	    this.expectedValue = reader.readString("expectedValue", null);
	}
	  
	public String internalAssertResponse(MessageExchange messageExchange, SubmitContext context) throws AssertionException {
		logger.debug("internalAssertResponse");
		return assertContent((PropertyExpansionContext) context, messageExchange.getResponseContent(), "Response");
	}
	  
	protected String internalAssertProperty(TestPropertyHolder source, String propertyName, MessageExchange messageExchange,
			SubmitContext context) throws AssertionException {
		logger.debug("internalAssertProperty");
		assertContent((PropertyExpansionContext) context, source.getPropertyValue(propertyName), propertyName);
		return "OK";
	}
	  
	private String assertContent(PropertyExpansionContext context, String content, String type) throws AssertionException {
 
		// Clean up the input data
		this.segment = StringUtils.trimToEmpty(segment);
		this.sequence = StringUtils.trimToEmpty(sequence);
		this.component = StringUtils.trimToEmpty(component);
		this.elementName = StringUtils.trimToEmpty(elementName);		
		this.expectedValue = StringUtils.trimToEmpty(expectedValue);
		
		content = StringUtils.trimToEmpty(content);

		logger.info("Raw content: {}", content);
		logger.info("Segment {}", this.segment);		  
		logger.info("Sequence {}", this.sequence);
		logger.info("Component {}", this.component);
		logger.info("Element Name {}", this.elementName);		  
		logger.info("Expected Value {}", this.expectedValue);

		// Extract the raw HL7
		String hl7v2 = Utils.extractDecodedHL7v2(content);
		logger.info("HL7 V2: {}", hl7v2);
		
		// Validate input
		if (StringUtils.isBlank(segment)) {
			throw new AssertionException(new AssertionError("Segment is required")); 	
		}

		if (StringUtils.isBlank(sequence)) {
			throw new AssertionException(new AssertionError("Segment is required"));
		}

		if (!StringUtils.isNumeric(sequence)) {
			throw new AssertionException(new AssertionError("Sequence must be numeric value greater than or equal to 1")); 
		}

		Map<String, String[]> segmentMap = HL7Parser.parseToIndexedMap(hl7v2);
		String[] segmentArray = segmentMap.get(segment);
			
		if (segmentArray == null) {
			throw new AssertionException(new AssertionError("Segment " + segment + " not found in message")); 
		}
		
		String expandedValue = PropertyExpander.expandProperties(context, this.expectedValue);		
		logger.info("expandedValue {}", expandedValue);

		String field = segmentArray[Integer.parseInt(sequence)];		
		if (StringUtils.isBlank(component)) {			
			if (!StringUtils.equals(field, expandedValue)) {
				throw new AssertionException(new AssertionError(String.format("Content doesn't match. Expected %s Actual %s", expandedValue, field))); 
			}
			return String.format("%s-%s equals %s", segment, sequence, expandedValue);
		} else {
			if (!StringUtils.isNumeric(component)) {
				throw new AssertionException(new AssertionError("Component must be numeric value greater than or equal to 1")); 
			}
			String[] components = StringUtils.split(field, HL7Parser.COMPONENT_DELIMITER);
			String fieldComponent = components[Integer.parseInt(component)];
			if (!StringUtils.equals(fieldComponent, expandedValue)) {
				throw new AssertionException(new AssertionError(String.format("Content doesn't match. Expected %s Actual %s", expandedValue, fieldComponent))); 
			}
			return String.format("%s-%s.%s equals %s", segment, sequence, expandedValue);
		}
			 
	}
	  
	public boolean configure() {
		if (this.dialog == null) {
			buildDialog();
		}
		StringToStringMap values = new StringToStringMap();
		values.put("Segment", this.segment);
		values.put("Sequence", this.sequence);
		values.put("Component", this.component);
		values.put("Element Name", this.elementName);
		values.put("Expected Value", this.expectedValue);
		values = this.dialog.show(values);
		if (this.dialog.getReturnValue() == 1) {
			this.segment = values.get("Segment");
			this.sequence = values.get("Sequence");
			this.component = values.get("Component");
			this.elementName = values.get("Element Name");
			this.expectedValue = values.get("Expected Value");
			setConfiguration(createConfiguration());
			return true;
		}
		return false;
	}

	public String getSegment() {
		return segment;
	}

	public void setSegment(String segment) {
		this.segment = segment;
		logger.info("Setting segment {}", segment);
		setConfiguration(createConfiguration());
	}

	public String getElementName() {
		return elementName;
	}

	public void setElementName(String elementName) {
		this.elementName = elementName;
		logger.info("Setting elementName {}", elementName);
		setConfiguration(createConfiguration());
	}

	public String getSequence() {
		return sequence;
	}

	public void setSequence(String sequence) {		
		this.sequence = sequence;
		logger.info("Setting sequence {}", sequence);
		setConfiguration(createConfiguration());
	}

	public String getComponent() {
		return component;
	}

	public void setComponent(String component) {
		this.component = component;
		logger.info("Setting component {}", component);
		setConfiguration(createConfiguration());
	}

	public String getExpectedValue() {
		return expectedValue;
	}

	public void setExpectedValue(String expectedValue) {
		this.expectedValue = expectedValue;
		logger.info("Setting expectedValue {}", expectedValue);
		setConfiguration(createConfiguration());
	}

	protected XmlObject createConfiguration() {
	    XmlObjectConfigurationBuilder builder = new XmlObjectConfigurationBuilder();
	    builder.add("segment", this.segment);
	    builder.add("sequence", this.sequence);
	    builder.add("component", this.component);
	    builder.add("elementName", this.elementName);
	    builder.add("expectedValue", this.expectedValue);
	    return builder.finish();
	  }
	  
	  protected void buildDialog() {
	    XFormDialogBuilder builder = XFormFactory.createDialogBuilder("HL7v2 Assertion");
	    XForm mainForm = builder.createForm("Basic", new FormLayout("5px,left:pref,5px,fill:default:grow(1.0),5px"));
	    //JPanel mainFormPanel = ((SwingXFormImpl)mainForm).getPanel();
	   // FormLayout mainFormLayout = (FormLayout)mainFormPanel.getLayout();
	    //this.contentField = (JTextAreaFormField)mainForm.addTextField("Content", "Content to check for", XForm.FieldType.TEXTAREA);
	    //this.contentField.setWidth(40);
	    //mainFormLayout.setRowSpec(mainFormLayout.getRowCount(), new RowSpec("top:default:grow(1.0)"));
	    //mainFormPanel.add(mainFormPanel.getComponent((mainFormPanel.getComponents()).length - 1), (new CellConstraints()).xy(4, mainFormLayout.getRowCount(), "fill,fill"));
	    
	    mainForm.addTextField("Segment", "Segment identifier", XForm.FieldType.TEXT);
	    mainForm.addTextField("Sequence", "Field Sequence", XForm.FieldType.TEXT);
	    mainForm.addTextField("Component", "Component", XForm.FieldType.TEXT);
	    
	    // This feature might be confusing so leave it out for now
	    //String[] comboValues = (String[]) ArrayUtils.addAll(new String[] {""}, HL7Parser.MSH_ELEMENT_NAMES, HL7Parser.MSA_ELEMENT_NAMES);
	    //mainForm.addComboBox("Element Name", comboValues, "Field Name");
	    mainForm.addTextField("Expected Value", "Expected Value", XForm.FieldType.TEXT);

	    this.dialog = builder.buildDialog(builder.buildOkCancelActions(), null, null);
	  }
	  
	  protected String internalAssertRequest(MessageExchange messageExchange, PropertyExpansionContext context) throws AssertionException {
	    return assertContent(context, messageExchange.getRequestContent(), "Request");
	  }
	  
	  public PropertyExpansion[] getPropertyExpansions() {
	    List<PropertyExpansion> result = new ArrayList<>();
	    result.addAll(PropertyExpansionUtils.extractPropertyExpansions(getAssertable().getModelItem(), this, "expectedValue"));
	    return result.<PropertyExpansion>toArray(new PropertyExpansion[result.size()]);
	  }

	}
