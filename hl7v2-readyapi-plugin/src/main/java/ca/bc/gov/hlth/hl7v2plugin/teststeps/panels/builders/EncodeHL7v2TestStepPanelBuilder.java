package ca.bc.gov.hlth.hl7v2plugin.teststeps.panels.builders;

import com.eviware.soapui.impl.EmptyPanelBuilder;
import com.eviware.soapui.plugins.auto.PluginPanelBuilder;
import com.eviware.soapui.ui.desktop.DesktopPanel;

import ca.bc.gov.hlth.hl7v2plugin.teststeps.EncodeHL7v2TestStep;
import ca.bc.gov.hlth.hl7v2plugin.teststeps.panels.EncodeHL7v2TestStepPanel;

@PluginPanelBuilder(targetModelItem = EncodeHL7v2TestStep.class)
public class EncodeHL7v2TestStepPanelBuilder extends EmptyPanelBuilder<EncodeHL7v2TestStep> {

    @Override
    public DesktopPanel buildDesktopPanel(EncodeHL7v2TestStep testStep) {
        return new EncodeHL7v2TestStepPanel(testStep);
    }

    @Override
    public boolean hasDesktopPanel() {
        return true;
    }

}
