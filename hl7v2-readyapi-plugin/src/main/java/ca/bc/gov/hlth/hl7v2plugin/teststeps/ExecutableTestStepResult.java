package ca.bc.gov.hlth.hl7v2plugin.teststeps;

import com.eviware.soapui.impl.wsdl.teststeps.WsdlTestStep;
import com.eviware.soapui.impl.wsdl.teststeps.WsdlTestStepResult;

public class ExecutableTestStepResult extends WsdlTestStepResult {

    private String outcome = "";

    public ExecutableTestStepResult(WsdlTestStep testStep) {
        super(testStep);
    }

    public String getOutcome() {
        return outcome;
    }

    public void setOutcome(String newValue) {
        outcome = newValue;
    }

}
