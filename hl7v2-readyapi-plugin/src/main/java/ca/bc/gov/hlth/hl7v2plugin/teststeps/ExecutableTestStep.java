package ca.bc.gov.hlth.hl7v2plugin.teststeps;

import com.eviware.soapui.model.ModelItem;
import com.eviware.soapui.model.iface.SubmitContext;

import ca.bc.gov.hlth.hl7v2plugin.CancellationToken;

public interface ExecutableTestStep extends ModelItem {
    public void addExecutionListener(ExecutionListener listener);

    public ExecutableTestStepResult execute(SubmitContext context, CancellationToken cancellationToken);

    public void removeExecutionListener(ExecutionListener listener);
}
