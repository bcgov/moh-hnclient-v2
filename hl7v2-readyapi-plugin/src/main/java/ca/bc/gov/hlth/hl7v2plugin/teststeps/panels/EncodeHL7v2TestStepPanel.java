package ca.bc.gov.hlth.hl7v2plugin.teststeps.panels;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.event.KeyEvent;
import java.util.Date;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.eviware.soapui.impl.wsdl.panels.teststeps.common.TestStepVariables;
import com.eviware.soapui.support.DateUtil;
import com.eviware.soapui.support.UISupport;
import com.eviware.soapui.support.components.JComponentInspector;
import com.eviware.soapui.support.components.JInspectorPanel;
import com.eviware.soapui.support.components.JInspectorPanelFactory;
import com.eviware.soapui.support.components.JXToolBar;
import com.eviware.soapui.support.components.SimpleBindingForm;
import com.eviware.soapui.support.log.JLogList;
import com.eviware.soapui.ui.support.ModelItemDesktopPanel;
import com.jgoodies.binding.PresentationModel;

import ca.bc.gov.hlth.hl7v2plugin.PluginConfig;
import ca.bc.gov.hlth.hl7v2plugin.teststeps.EncodeHL7v2TestStep;
import ca.bc.gov.hlth.hl7v2plugin.teststeps.ExecutableTestStep;
import ca.bc.gov.hlth.hl7v2plugin.teststeps.ExecutableTestStepResult;
import ca.bc.gov.hlth.hl7v2plugin.teststeps.ExecutionListener;
import ca.bc.gov.hlth.hl7v2plugin.teststeps.PublishTestStep;
import ca.bc.gov.hlth.hl7v2plugin.teststeps.actions.RunTestStepAction;

public class EncodeHL7v2TestStepPanel extends ModelItemDesktopPanel<EncodeHL7v2TestStep> implements ExecutionListener {
    
	private static final long serialVersionUID = 7242999356880461009L;
	
	private static final Logger logger = LoggerFactory.getLogger(PluginConfig.LOGGER_NAME);

	private JInspectorPanel inspectorPanel;
    private JComponentInspector<JComponent> logInspector;
    private JLogList logArea;
    private RunTestStepAction startAction;

    public EncodeHL7v2TestStepPanel(EncodeHL7v2TestStep modelItem) {
        super(modelItem);
        buildUI();
    }
    
    private void buildUI() {
        JComponent mainPanel = buildMainPanel();
        inspectorPanel = JInspectorPanelFactory.buildRequestInspectorPanel(mainPanel);

        logInspector = new JComponentInspector<JComponent>(buildLogPanel(), 
        		TestStepVariables.LOGGER_INSPECTOR_TITLE, TestStepVariables.LOGGER_INSPECTOR_DESCRIPTION, true);
        inspectorPanel.addInspector(logInspector);
        inspectorPanel.setCurrentInspector("Assertions");

        add(inspectorPanel.getComponent());
    }

    @Override
    public void afterExecution(ExecutableTestStep testStep, ExecutableTestStepResult executionResult) {
        logArea.addLine(DateUtil.formatFull(new Date(executionResult.getTimeStamp())) + " - "
                + executionResult.getOutcome());
        logArea.addLine(DateUtil.formatFull(new Date(executionResult.getTimeStamp())) + " - "
                + (executionResult.getMessages().length > 0 ? executionResult.getMessages()[0] : ""));
    }

    protected JComponent buildLogPanel() {
        logArea = new JLogList("Test Step Log");
        return logArea;
    }

    @SuppressWarnings("deprecation")
	private JComponent buildMainPanel() {

    	logger.info("Building main Panel");
        
        PresentationModel<PublishTestStep> pm = new PresentationModel<PublishTestStep>(getModelItem());
        SimpleBindingForm form = new SimpleBindingForm(pm);
        
        JLabel titleLabel = new JLabel("Encode HL7v2");
        
        Font titleFont = new Font("Arial", Font.BOLD, 24);
        titleLabel.setFont(titleFont);
        
        form.append(titleLabel);

        JLabel instructionsLabel = new JLabel("This step encodes the Data Source fileContent as HL7v2 and makes it available as the testcase property ${#TestCase#encodedHL7v2Msg}");
        form.append(instructionsLabel);
        
        JPanel result = new JPanel(new BorderLayout(0, 0));
        result.add(new JScrollPane(form.getPanel(), ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED), BorderLayout.CENTER);

        result.add(buildToolbar(), BorderLayout.NORTH);
        return result;
    }
    
    private JComponent buildToolbar() {
        JXToolBar toolBar = UISupport.createToolbar();
        startAction = new RunTestStepAction(getModelItem());
        JButton submitButton = UISupport.createActionButton(startAction, startAction.isEnabled());
        toolBar.add(submitButton);
        submitButton.setMnemonic(KeyEvent.VK_ENTER);
        toolBar.add(UISupport.createActionButton(startAction.getCorrespondingStopAction(), startAction
                .getCorrespondingStopAction().isEnabled()));
        //addConnectionActionsToToolbar(toolBar);
        return toolBar;
    }

    @Override
    protected boolean release() {
        inspectorPanel.release();
        return super.release();
    }

}
