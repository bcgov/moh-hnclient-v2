package ca.bc.gov.hlth.hl7v2plugin.teststeps;

import java.util.Map;

import javax.swing.ImageIcon;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.eviware.soapui.SoapUI;
import com.eviware.soapui.config.TestStepConfig;
import com.eviware.soapui.impl.wsdl.testcase.WsdlTestCase;
import com.eviware.soapui.impl.wsdl.teststeps.WsdlTestStepWithProperties;
import com.eviware.soapui.model.iface.SubmitContext;
import com.eviware.soapui.model.mock.MockRunner;
import com.eviware.soapui.model.testsuite.LoadTestRunner;
import com.eviware.soapui.model.testsuite.ProjectRunner;
import com.eviware.soapui.model.testsuite.TestCaseRunContext;
import com.eviware.soapui.model.testsuite.TestCaseRunner;
import com.eviware.soapui.model.testsuite.TestStepResult;
import com.eviware.soapui.model.testsuite.TestStepResult.TestStepStatus;
import com.eviware.soapui.model.testsuite.TestSuiteRunner;
import com.eviware.soapui.monitor.TestMonitor;
import com.eviware.soapui.monitor.TestMonitorListener;
import com.eviware.soapui.plugins.auto.PluginTestStep;
import com.eviware.soapui.security.SecurityTestRunner;
import com.eviware.soapui.support.UISupport;

import ca.bc.gov.hlth.hl7v2plugin.CancellationToken;
import ca.bc.gov.hlth.hl7v2plugin.HL7Utils;
import ca.bc.gov.hlth.hl7v2plugin.PluginConfig;
import ca.bc.gov.hlth.hl7v2plugin.Utils;
import ca.bc.gov.hlth.hl7v2plugin.teststeps.actions.RunTestStepAction;
import ca.bc.gov.hlth.hl7v2plugin.teststeps.actions.groups.EncodeHL7v2TestStepActionGroup;

@SuppressWarnings("deprecation")
@PluginTestStep(typeName = "encodeHL7v2DataSourceTestStep", name = "Encode HL7v2",
        description = "Base64 encodes the HL7v2 message loaded from a Data Source",
        iconPath = "ca/bc/gov/hlth/hl7v2plugin/encode_hl7_step.png")
public class EncodeHL7v2TestStep extends WsdlTestStepWithProperties implements TestMonitorListener, ExecutableTestStep {

    private static final  Logger logger = LoggerFactory.getLogger(PluginConfig.LOGGER_NAME);
    
    private static final String MSH_SEGMENT = "MSH";

    private static boolean actionGroupAdded = false;

    /** Icons */
    private ImageIcon baseIcon;
    
    private WsdlTestCase testCase;

	public EncodeHL7v2TestStep(WsdlTestCase testCase, TestStepConfig config, boolean forLoadTest) {
        super(testCase, config, true, forLoadTest);
        this.testCase = testCase;
        if (!actionGroupAdded) {
            SoapUI.getActionRegistry().addActionGroup(new EncodeHL7v2TestStepActionGroup());
            actionGroupAdded = true;
        }

        if (!forLoadTest) {
            initIcons();
        }
        setIcon(baseIcon);
        
        TestMonitor testMonitor = SoapUI.getTestMonitor();
        if (testMonitor != null) {
            testMonitor.addTestMonitorListener(this);
        }

    }

    @Override
    public TestStepResult run(final TestCaseRunner testRunner, TestCaseRunContext testRunContext) {
        return doExecute(testRunContext, new CancellationToken() {
            @Override
            public boolean isCancelled() {
                return !testRunner.isRunning();
            }
        });
    }

    protected ExecutableTestStepResult doExecute(SubmitContext testRunContext, CancellationToken cancellationToken) {
    	logger.info("doExecute");
    	ExecutableTestStepResult result = new ExecutableTestStepResult(this);
    	try {
	    	result.startTimer();
	    	result.setStatus(TestStepResult.TestStepStatus.UNKNOWN);
	
	    	String hl7v2 = testRunContext.expand("${Data Source#fileContent}");
	
	    	logger.info("HL7V2 " + hl7v2);
	    	
	    	if (!StringUtils.startsWith(hl7v2, MSH_SEGMENT)) {
	    		throw new IllegalArgumentException("Data Source does not contain an HL7 V2 message");
	    	}
	    	
	    	// Store some commonly queried request fields for later use
	    	Map<String, String[]> segments = HL7Utils.parseToIndexedMap(hl7v2);
	    	String[] mshSegments = segments.get(HL7Utils.SEGMENT_MSH);
	    	testCase.setPropertyValue("hl7RequestSendingApplication", mshSegments[3]);
	    	testCase.setPropertyValue("hl7RequestSendingFacility", mshSegments[4]);
	    	testCase.setPropertyValue("hl7RequestSecurity", mshSegments[8]);
	    	testCase.setPropertyValue("hl7RequestMessageType", mshSegments[9]);
	    	testCase.setPropertyValue("hl7RequestMessageType", mshSegments[9]);
	    	testCase.setPropertyValue("hl7RequestMessageControlID", mshSegments[10]);

	    	String encodedMsg = Utils.encode(hl7v2);
	    	
	    	logger.info("Encoded msg {}", encodedMsg);
	
	    	testCase.setPropertyValue("encodedHL7v2Msg", encodedMsg);
	
	    	result.setOutcome(encodedMsg);
	    	result.setStatus(TestStepStatus.OK);
    	
    	} catch (Exception e) {
        	logger.info(e.getMessage(), e);
        	result.setOutcome("Invalid HL7 V2");
            result.setStatus(TestStepResult.TestStepStatus.FAILED);
            result.setError(e);
        } finally {
            result.stopTimer();
            SoapUI.log(String.format("%s - [%s test step]", result.getOutcome(), getName()));
        }
    	return result;
    }

    protected void initIcons() {
    	baseIcon = UISupport.createImageIcon("ca/bc/gov/hlth/hl7v2plugin/encode_hl7_step.png");
    }

    @Override
    public void setIcon(ImageIcon newIcon) {
        super.setIcon(newIcon);
    }
    
	public ImageIcon getIcon() {
		if (isForLoadTest()) {
			return null;
		}
		return baseIcon;
	}

	@Override
	public void loadTestStarted(LoadTestRunner runner) {
		logger.debug("loadTestStarted");
	}

	@Override
	public void loadTestFinished(LoadTestRunner runner) {
		logger.debug("loadTestFinished");
	}

	@Override
	public void securityTestStarted(SecurityTestRunner runner) {
		logger.debug("securityTestStarted");
	}

	@Override
	public void securityTestFinished(SecurityTestRunner runner) {
		logger.debug("securityTestFinished");
	}

	@Override
	public void testCaseStarted(TestCaseRunner runner) {
		logger.debug("testCaseStarted");
	}

	@Override
	public void testCaseFinished(TestCaseRunner runner) {
		logger.info("testCaseFinished");
	}

	@SuppressWarnings("rawtypes")
	@Override
	public void mockServiceStarted(MockRunner runner) {
		logger.info("mockServiceStarted");
	}

	@SuppressWarnings("rawtypes")
	@Override
	public void mockServiceStopped(MockRunner runner) {
		logger.info("mockServiceStopped");
	}

	@Override
	public void projectStarted(ProjectRunner projectRunner) {
		logger.info("projectStarted");
	}

	@Override
	public void projectFinished(ProjectRunner projectRunner) {
		logger.info("projectFinished");
	}

	@Override
	public void testSuiteStarted(TestSuiteRunner testSuiteRunner) {
		logger.info("testSuiteStarted");
	}

	@Override
	public void testSuiteFinished(TestSuiteRunner testSuiteRunner) {
		logger.info("testSuiteFinished");
	}

	@Override
	public void addExecutionListener(ExecutionListener listener) {
		// TODO Auto-generated method stub
		
	}

    @Override
    public ExecutableTestStepResult execute(SubmitContext runContext, CancellationToken cancellationToken) {
    	return doExecute(runContext, cancellationToken);
    }

	@Override
	public void removeExecutionListener(ExecutionListener listener) {
		// TODO Auto-generated method stub
		
	}
}
