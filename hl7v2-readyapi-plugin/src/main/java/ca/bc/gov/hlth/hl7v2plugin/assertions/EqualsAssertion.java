package ca.bc.gov.hlth.hl7v2plugin.assertions;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JPanel;

import org.apache.xmlbeans.XmlObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.eviware.soapui.config.TestAssertionConfig;
import com.eviware.soapui.impl.wsdl.panels.assertions.AssertionCategoryMapping;
import com.eviware.soapui.impl.wsdl.panels.assertions.AssertionListEntry;
import com.eviware.soapui.impl.wsdl.teststeps.WsdlMessageAssertion;
import com.eviware.soapui.impl.wsdl.teststeps.assertions.AbstractTestAssertionFactory;
import com.eviware.soapui.impl.wsdl.teststeps.assertions.basic.SimpleContainsAssertion;
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
import com.eviware.soapui.model.testsuite.TestAssertion;
import com.eviware.soapui.plugins.auto.PluginTestAssertion;
import com.eviware.soapui.support.StringUtils;
import com.eviware.soapui.support.UISupport;
import com.eviware.soapui.support.types.StringToStringMap;
import com.eviware.soapui.support.xml.XmlObjectConfigurationBuilder;
import com.eviware.soapui.support.xml.XmlObjectConfigurationReader;
import com.eviware.x.form.XForm;
import com.eviware.x.form.XFormDialog;
import com.eviware.x.form.XFormDialogBuilder;
import com.eviware.x.form.XFormFactory;
import com.eviware.x.impl.swing.JTextAreaFormField;
import com.eviware.x.impl.swing.SwingXFormImpl;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.RowSpec;

// XXX An EqualsAssertion is already provided with ReadyAPI 3.7.0
@PluginTestAssertion(id = "EqualsAssertion", label = "Equals Response",
        category = AssertionCategoryMapping.VALIDATE_RESPONSE_CONTENT_CATEGORY,
        description = "Asserts that the response message equals the specified content")
public class EqualsAssertion extends WsdlMessageAssertion implements RequestAssertion, ResponseAssertion {
	private static final Logger logger = LoggerFactory.getLogger(EqualsAssertion.class);
	
	  private String token;
	  
	  private XFormDialog dialog;
	  
	  private boolean ignoreCase;
	  
	  private boolean useRegEx;
	  
	  protected JTextAreaFormField contentField;
	  
	  public static final String ID = "Equals";
	  
	  private static final String CONTENT = "Content";
	  
	  private static final String IGNORE_CASE = "Ignore Case";
	  
	  private static final String USE_REGEX = "Regular Expression";
	  
	  public static final String LABEL = "Equals";
	  
	  public static final String DESCRIPTION = "Searches for an exact match of a string token in the property value, supports regular expressions. Applicable to any property. ";
	  
	  public EqualsAssertion(TestAssertionConfig assertionConfig, Assertable assertable) {
	    super(assertionConfig, assertable, true, true, true, true);
	    XmlObjectConfigurationReader reader = new XmlObjectConfigurationReader(getConfiguration());
	    this.token = reader.readString("token", null);
	    this.ignoreCase = reader.readBoolean("ignoreCase", false);
	    this.useRegEx = reader.readBoolean("useRegEx", false);
	    
	    logger.debug("constructor token {}", token); 
	  }
	  
	  public String internalAssertResponse(MessageExchange messageExchange, SubmitContext context) throws AssertionException {
		  logger.debug("internalAssertResponse");
	    return assertContent((PropertyExpansionContext)context, messageExchange.getResponseContent(), "Response");
	  }
	  
	  protected String internalAssertProperty(TestPropertyHolder source, String propertyName, MessageExchange messageExchange, SubmitContext context) throws AssertionException {
		  logger.debug("internalAssertProperty");
		  logger.debug("content {}", context.getProperty("Content"));
		  logger.debug("Ignore Case {}", context.getProperty("\"Ignore Case\""));
	    assertContent((PropertyExpansionContext)context, source.getPropertyValue(propertyName), propertyName);
	    return "OK";
	  }
	  
	  private String assertContent(PropertyExpansionContext context, String content, String type) throws AssertionException {
	    if (this.token == null)
	      this.token = ""; 
	    if (content == null)
	      content = ""; 
	    String replToken = PropertyExpander.expandProperties(context, this.token);
	    logger.info("replToken {}", replToken);
	    if (replToken == null)
	      replToken = ""; 
	    replToken = normalize(replToken);
	    content = normalize(content);
	    logger.info("replToken length {}", replToken.length());
	    logger.info("content length {}", content.length());
		  logger.info("replToken {}", replToken);
		  logger.info("content {}", content);
	    logger.info("content equals replToken {} ", content.equals(replToken));
	    if (replToken.length() > 0) {
	      boolean valid = false;
	      if (this.useRegEx) {
	        String tokenToUse = this.ignoreCase ? ("(?i)" + replToken) : replToken;
	        Pattern p = Pattern.compile(tokenToUse, 32);
	        Matcher m = p.matcher(content);
	        valid = m.find();
	      } else {
	        valid = this.ignoreCase ? content.toUpperCase().equals(replToken.toUpperCase()) : content.equals(replToken);
	      } 
	      if (!valid)
	        throw new AssertionException(new AssertionError("Content doesn't match in " + type)); 
	    } 
	    return String.valueOf(type) + " contains token [" + replToken + "]";
	  }
	  
	  private String normalize(String string) {
	    if (!StringUtils.isNullOrEmpty(string)) {
	    	string = string.replaceAll("\r\n", "\n");
	    	string = string.trim();
	    }
	       
	    return string;
	  }
	  
	  public boolean configure() {
	    if (this.dialog == null)
	      buildDialog(); 
	    StringToStringMap values = new StringToStringMap();
	    values.put("Content", this.token);
	    values.put("Ignore Case", this.ignoreCase);
	    values.put("Regular Expression", this.useRegEx);
	    values = this.dialog.show(values);
	    if (this.dialog.getReturnValue() == 1) {
	      this.token = (String)values.get("Content");
	      this.ignoreCase = values.getBoolean("Ignore Case");
	      this.useRegEx = values.getBoolean("Regular Expression");
	      setConfiguration(createConfiguration());
	      return true;
	    } 
	    return false;
	  }
	  
	  public boolean isUseRegEx() {
	    return this.useRegEx;
	  }
	  
	  public void setUseRegEx(boolean useRegEx) {
	    this.useRegEx = useRegEx;
	    setConfiguration(createConfiguration());
	  }
	  
	  public boolean isIgnoreCase() {
	    return this.ignoreCase;
	  }
	  
	  public void setIgnoreCase(boolean ignoreCase) {
	    this.ignoreCase = ignoreCase;
	    setConfiguration(createConfiguration());
	  }
	  
	  public String getToken() {
	    return this.token;
	  }
	  
	  public void setToken(String token) {
	    this.token = token;
	    logger.debug("Setting token {}", token);
	    setConfiguration(createConfiguration());
	  }
	  
	  protected XmlObject createConfiguration() {
	    XmlObjectConfigurationBuilder builder = new XmlObjectConfigurationBuilder();
	    builder.add("token", this.token);
	    builder.add("ignoreCase", this.ignoreCase);
	    builder.add("useRegEx", this.useRegEx);
	    return builder.finish();
	  }
	  
	  protected void buildDialog() {
	    XFormDialogBuilder builder = XFormFactory.createDialogBuilder("Equals Assertion");
	    XForm mainForm = builder.createForm("Basic", new FormLayout("5px,left:pref,5px,fill:default:grow(1.0),5px"));
	    JPanel mainFormPanel = ((SwingXFormImpl)mainForm).getPanel();
	    FormLayout mainFormLayout = (FormLayout)mainFormPanel.getLayout();
	    this.contentField = (JTextAreaFormField)mainForm.addTextField("Content", "Content to check for", XForm.FieldType.TEXTAREA);
	    this.contentField.setWidth(40);
	    mainFormLayout.setRowSpec(mainFormLayout.getRowCount(), new RowSpec("top:default:grow(1.0)"));
	    mainFormPanel.add(mainFormPanel.getComponent((mainFormPanel.getComponents()).length - 1), (new CellConstraints()).xy(4, mainFormLayout.getRowCount(), "fill,fill"));
	    mainForm.addCheckBox("Ignore Case", "Ignore case in comparison");
	    mainForm.addCheckBox("Regular Expression", "Use token as Regular Expression");
	    this.dialog = builder.buildDialog(builder.buildOkCancelHelpActions("/testing/assertions/reference/property/contains.html"), 
	        "Specify options", UISupport.OPTIONS_ICON);
	  }
	  
	  protected String internalAssertRequest(MessageExchange messageExchange, PropertyExpansionContext context) throws AssertionException {
	    return assertContent(context, messageExchange.getRequestContent(), "Request");
	  }
	  
	  public PropertyExpansion[] getPropertyExpansions() {
	    List<PropertyExpansion> result = new ArrayList<>();
	    result.addAll(PropertyExpansionUtils.extractPropertyExpansions(getAssertable().getModelItem(), this, "token"));
	    return result.<PropertyExpansion>toArray(new PropertyExpansion[result.size()]);
	  }
	  
	  public static class Factory extends AbstractTestAssertionFactory {
	    public Factory() {
	      super("Simple Contains", "Contains", SimpleContainsAssertion.class);
	    }
	    
	    public Factory(String id, String label, Class<? extends TestAssertion> proContainsAssertionClass) {
	      super(id, label, proContainsAssertionClass);
	    }
	    
	    public String getCategory() {
	      return "Property Content";
	    }
	    
	    public Class<? extends WsdlMessageAssertion> getAssertionClassType() {
	      return (Class)SimpleContainsAssertion.class;
	    }
	    
	    public AssertionListEntry getAssertionListEntry() {
	      return new AssertionListEntry("Simple Contains", "Contains", 
	          "Searches for the existence of a string token in the property value, supports regular expressions. Applicable to any property. ", 100);
	    }
	    
	    public boolean canAssert(TestPropertyHolder modelItem, String property) {
	      modelItem.getPropertyValue(property);
	      return true;
	    }
	  }
	}
