package ca.bc.gov.hlth.hl7v2plugin.teststeps.panels;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeEvent;
import java.util.Date;

import javax.swing.AbstractAction;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.eviware.soapui.impl.wsdl.WsdlInterface;
import com.eviware.soapui.impl.wsdl.WsdlProject;
import com.eviware.soapui.impl.wsdl.panels.teststeps.AssertionsPanel;
import com.eviware.soapui.impl.wsdl.panels.teststeps.common.TestStepVariables;
import com.eviware.soapui.model.ModelItem;
import com.eviware.soapui.model.environment.Environment;
import com.eviware.soapui.model.support.ModelSupport;
import com.eviware.soapui.model.support.ProjectListenerAdapter;
import com.eviware.soapui.model.testsuite.Assertable;
import com.eviware.soapui.model.testsuite.AssertionsListener;
import com.eviware.soapui.model.testsuite.TestAssertion;
import com.eviware.soapui.support.DateUtil;
import com.eviware.soapui.support.StringUtils;
import com.eviware.soapui.support.UISupport;
import com.eviware.soapui.support.components.JComponentInspector;
import com.eviware.soapui.support.components.JInspectorPanel;
import com.eviware.soapui.support.components.JInspectorPanelFactory;
import com.eviware.soapui.support.components.JXToolBar;
import com.eviware.soapui.support.components.SimpleBindingForm;
import com.eviware.soapui.support.log.JLogList;
import com.eviware.soapui.support.propertyexpansion.PropertyExpansionPopupListener;
import com.eviware.soapui.support.xml.SyntaxEditorUtil;
import com.eviware.soapui.ui.support.ModelItemDesktopPanel;
import com.jgoodies.binding.PresentationModel;
import com.jgoodies.binding.adapter.Bindings;
import com.jgoodies.binding.list.SelectionInList;

import ca.bc.gov.hlth.hl7v2plugin.PluginConfig;
import ca.bc.gov.hlth.hl7v2plugin.Utils;
import ca.bc.gov.hlth.hl7v2plugin.teststeps.ConnectedTestStep.TimeMeasure;
import ca.bc.gov.hlth.hl7v2plugin.teststeps.ExecutableTestStep;
import ca.bc.gov.hlth.hl7v2plugin.teststeps.ExecutableTestStepResult;
import ca.bc.gov.hlth.hl7v2plugin.teststeps.ExecutionListener;
import ca.bc.gov.hlth.hl7v2plugin.teststeps.PublishTestStep;
import ca.bc.gov.hlth.hl7v2plugin.teststeps.PublishedMessageType;
import ca.bc.gov.hlth.hl7v2plugin.teststeps.actions.RunTestStepAction;

public class PublishTestStepPanel extends ModelItemDesktopPanel<PublishTestStep> implements ExecutionListener, AssertionsListener {
    
	private final static Logger logger = LoggerFactory.getLogger(PluginConfig.LOGGER_NAME);

    /** serialVersionUID description. */
    private static final long serialVersionUID = 7242999356880461009L;
    private JTextField numberEdit;
    private JTextArea textMemo;
    private JTextArea textResponse;
    private JTextField fileNameEdit;
    private JButton chooseFileButton;
    private JTabbedPane jsonEditor;
    private JComponent jsonTreeEditor;
    private JTabbedPane xmlEditor;
    private JTextField endpointTextField;
    
    private JComponentInspector<JComponent> assertionInspector;
    private AssertionsPanel assertionsPanel;

    private JComponent xmlTreeEditor;
    private JInspectorPanel inspectorPanel;
    private JComponentInspector<JComponent> logInspector;
    private JLogList logArea;
    private RunTestStepAction startAction;
    
    private final WsdlProject project;
    
    private Environment activeEnvironment;

    public PublishTestStepPanel(PublishTestStep modelItem) {
        super(modelItem);
        this.project = (WsdlProject)ModelSupport.getModelItemProject((ModelItem)modelItem);
        this.activeEnvironment = project.getActiveEnvironment();

        buildUI();
        modelItem.addAssertionsListener(this);
        modelItem.addExecutionListener(this);
        
        this.project.addProjectListener(new ProjectListenerAdapter() {
        	@Override
        	public void environmentSwitched(Environment environment) {
        		activeEnvironment = environment;
        		logger.info("Environment switched " + environment.getName());
        		endpointTextField.setText(modelItem.getHNSClientEndpoint(activeEnvironment));        		
        	}
        });
    }
    
    private void buildUI() {

        JComponent mainPanel = buildMainPanel();
        inspectorPanel = JInspectorPanelFactory.buildRequestInspectorPanel(mainPanel);
        
        assertionsPanel = buildAssertionsPanel();

        assertionInspector = new JComponentInspector<JComponent>(assertionsPanel,
        		TestStepVariables.ASSERTION_INSPECTOR_TITLE, TestStepVariables.ASSERTION_INSPECTOR_DESCRIPTION, true);

        inspectorPanel.addInspector(assertionInspector);

        logInspector = new JComponentInspector<JComponent>(buildLogPanel(), 
        		TestStepVariables.LOGGER_INSPECTOR_TITLE, TestStepVariables.LOGGER_INSPECTOR_DESCRIPTION, true);
        inspectorPanel.addInspector(logInspector);

        //inspectorPanel.setDefaultDividerLocation(0.6F);
        inspectorPanel.setCurrentInspector("Assertions");
        
        updateStatusIcon();

        add(inspectorPanel.getComponent());
        //setPreferredSize(new Dimension(800, 700));
        
        propertyChange(
                new PropertyChangeEvent(getModelItem(), "response", null, getModelItem().getResponse()));

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

    private JComponent buildMainPanel() {

    	logger.info("Building main Panel");
        
        PresentationModel<PublishTestStep> pm = new PresentationModel<PublishTestStep>(getModelItem());
        SimpleBindingForm form = new SimpleBindingForm(pm);
        
        JLabel titleLabel = new JLabel("Publish HL7v2");
        
        Font titleFont = new Font("Arial", Font.BOLD, 24);
        titleLabel.setFont(titleFont);
        
        form.append(titleLabel);
        
        form.appendHeading("Endpoint");

        endpointTextField = new JTextField(PublishTestStep.getHNSClientEndpoint(activeEnvironment));
        endpointTextField.setEnabled(false);
        endpointTextField.setColumns(50);
        endpointTextField.setSize(200, 10);
        
        form.append(endpointTextField);

        JLabel instructionsLabel = new JLabel("Note: Please create a REST API named 'HNS Client' and configure using built in Environments");
        form.append(instructionsLabel);

        form.appendSeparator();
        form.appendHeading("Published Message");
        form.appendComboBox("messageKind", "Message type", PublishedMessageType.values(), "");
        numberEdit = form.appendTextField("message", "Message", "The number which will be published.");
        textMemo = form.appendTextArea("message", "Message", "The text which will be published.");
        textMemo.setRows(15);
        PropertyExpansionPopupListener.enable(textMemo, getModelItem());
        fileNameEdit = form.appendTextField("message", "Folder name", "The folder which will be parsed for hl7v2 messages");
        PropertyExpansionPopupListener.enable(fileNameEdit, getModelItem());
        chooseFileButton = form.addRightButton(new SelectFileAction());

        JScrollPane scrollPane;

        jsonEditor = new JTabbedPane();
        RSyntaxTextArea syntaxTextArea = SyntaxEditorUtil.createDefaultJavaScriptSyntaxTextArea();
        syntaxTextArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JSON);
        Bindings.bind(syntaxTextArea, pm.getModel("message"), true);
        PropertyExpansionPopupListener.enable(syntaxTextArea, getModelItem());
        jsonEditor.addTab("Text", Utils.createRTextScrollPane(syntaxTextArea));

        jsonTreeEditor = Utils.createJsonTreeEditor(true, getModelItem());
        if (jsonTreeEditor != null) {
            scrollPane = new JScrollPane(jsonTreeEditor);
            scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
            scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
            Bindings.bind(jsonTreeEditor, "text", pm.getModel("message"));
            jsonEditor.addTab("Tree View", scrollPane);
        } else
            jsonEditor.addTab("Tree View", new JLabel(Utils.TREE_VIEW_IS_UNAVAILABLE, SwingConstants.CENTER));

        jsonEditor.setPreferredSize(new Dimension(450, 350));
        form.append("Message", jsonEditor);

        xmlEditor = new JTabbedPane();

        syntaxTextArea = SyntaxEditorUtil.createDefaultXmlSyntaxTextArea();
        Bindings.bind(syntaxTextArea, pm.getModel("message"), true);
        PropertyExpansionPopupListener.enable(syntaxTextArea, getModelItem());
        xmlEditor.addTab("Text", Utils.createRTextScrollPane(syntaxTextArea));

        xmlTreeEditor = Utils.createXmlTreeEditor(true, getModelItem());
        if (xmlTreeEditor != null) {
            scrollPane = new JScrollPane(xmlTreeEditor);
            scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
            scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
            Bindings.bind(xmlTreeEditor, "text", pm.getModel("message"));
            xmlEditor.addTab("Tree View", scrollPane);
        } else
            xmlEditor.addTab("Tree View", new JLabel(Utils.TREE_VIEW_IS_UNAVAILABLE, SwingConstants.CENTER));

        xmlEditor.setPreferredSize(new Dimension(450, 350));
        form.append("Message", xmlEditor);

        form.appendSeparator();
        form.appendHeading("Message Delivering Settings");
        buildTimeoutSpinEdit(form, pm, "Timeout");
        
        form.appendSeparator();
        form.appendHeading("Received Message");
        textResponse = form.appendTextArea("response","Response","The response from the HNClient.");
        textResponse.setRows(15);
        Bindings.bind(syntaxTextArea, pm.getModel("response"), true);

        JPanel result = new JPanel(new BorderLayout(0, 0));
        result.add(new JScrollPane(form.getPanel(), ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED), BorderLayout.CENTER);
        result.add(buildToolbar(), BorderLayout.NORTH);

        propertyChange(new PropertyChangeEvent(getModelItem(), "messageKind", null, getModelItem().getMessageKind()));

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

    private void buildTimeoutSpinEdit(SimpleBindingForm form, PresentationModel<PublishTestStep> pm, String label) {
        JPanel timeoutPanel = new JPanel();
        timeoutPanel.setLayout(new BoxLayout(timeoutPanel, BoxLayout.X_AXIS));
        JSpinner spinEdit = Utils.createBoundSpinEdit(pm, "shownTimeout", 0, Integer.MAX_VALUE, 1);
        spinEdit.setPreferredSize(new Dimension(80, spinEdit.getHeight()));
        timeoutPanel.add(spinEdit);
        JComboBox<TimeMeasure> measureCombo = new JComboBox<TimeMeasure>(TimeMeasure.values());
        Bindings.bind(measureCombo, new SelectionInList<Object>(TimeMeasure.values(), pm.getModel("timeoutMeasure")));
        timeoutPanel.add(measureCombo);
        timeoutPanel.add(new JLabel(" (0 - forever)"));
        form.append(label, timeoutPanel);

    }



    @Override
    public void propertyChange(PropertyChangeEvent evt) {
    	logger.debug("propertyChange {}", evt);
        super.propertyChange(evt);

        if (evt.getPropertyName().equals("assertionStatus"))
            updateStatusIcon();
        else if (evt.getPropertyName().equals("messageKind")) {
            PublishedMessageType newMessageType = (PublishedMessageType) evt.getNewValue();
            boolean isFile = newMessageType == PublishedMessageType.BinaryFile;
            boolean isText = newMessageType == PublishedMessageType.Text;
            numberEdit.setVisible(false);
            textMemo.setVisible(isText);
            textResponse.setVisible(isText);
            if (textMemo.getParent() instanceof JScrollPane)
                textMemo.getParent().setVisible(isText);
            else if (textMemo.getParent().getParent() instanceof JScrollPane)
                textMemo.getParent().getParent().setVisible(isText);
            fileNameEdit.setVisible(isFile);
            chooseFileButton.setVisible(isFile);
            jsonEditor.setVisible(false);
            xmlEditor.setVisible(false);
        }else if(evt.getPropertyName().equals("response")){
            
            String msg = (String) evt.getNewValue();
            
            if (StringUtils.isNullOrEmpty(msg)) {
                Utils.showMemo(textResponse, true);
                jsonEditor.setVisible(false);
                xmlEditor.setVisible(false);
            } 
        }
        
        String propertyName = evt.getPropertyName();
//        if (StringUtils.isNullOrEmpty(getSelectedItem().toString())) {
//          if (this.apiConnection.getOperation() != null)
//            this.apiConnection.getOperation().getInterface().removeEndpoint(this.apiConnection.getEndpoint()); 
//          setSelectedItem(NO_ENDPOINT_SET);
//          this.apiConnection.setEndpoint(null);
//        } 
        if (propertyName.equals("endpoint")) {
        	logger.info("Endpoint changed");
          //notifyContentsChanged();
        } else if (propertyName.equals(WsdlInterface.ENDPOINT_PROPERTY)) {
          //refresh();
        	logger.info("Endpoint property changed " + WsdlInterface.ENDPOINT_PROPERTY);
        } 
    }

    @Override
    protected boolean release() {
        startAction.cancel();
        getModelItem().removeExecutionListener(this);
        getModelItem().removeAssertionsListener(this);
        inspectorPanel.release();
        assertionsPanel.release();
        if (jsonTreeEditor != null)
            Utils.releaseTreeEditor(jsonTreeEditor);
        if (xmlTreeEditor != null)
            Utils.releaseTreeEditor(xmlTreeEditor);
        return super.release();
    }

    public class SelectFileAction extends AbstractAction {
        /** serialVersionUID description. */
        private static final long serialVersionUID = 3499349831088040594L;
        private JFileChooser folderChooser;

        public SelectFileAction() {
            super("Browse...");
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            if (folderChooser == null){
                folderChooser = new JFileChooser();
                folderChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                folderChooser.setAcceptAllFileFilterUsed(false);
            }

            int returnVal = folderChooser.showOpenDialog(UISupport.getMainFrame());
            if (returnVal == JFileChooser.APPROVE_OPTION)
                fileNameEdit.setText(folderChooser.getSelectedFile().getAbsolutePath());
        }
    }
    
    
    //ASSERTIONS PART ***********************************************************
    
    @Override
    public void assertionAdded(TestAssertion assertion) {
    	logger.info("assertionAdded");
        assertionListChanged();
    }

    private void assertionListChanged() {
        assertionInspector.setTitle(String.format("Assertions (%d)", getModelItem().getAssertionCount()));
    }

    @Override
    public void assertionMoved(TestAssertion assertion, int i) {
    	logger.info("assertionMoved");
        assertionListChanged();
    }

    @Override
    public void assertionRemoved(TestAssertion assertion) {
    	logger.info("assertionRemoved");
        assertionListChanged();
    }
    
    private AssertionsPanel buildAssertionsPanel() {
        return new AssertionsPanel(getModelItem());
    }
    
    private void updateStatusIcon() {
    	logger.info("UPdating status icon");
        Assertable.AssertionStatus status = getModelItem().getAssertionStatus();
        switch (status) {
        case FAILED: {
            assertionInspector.setIcon(UISupport.createImageIcon("/failed_assertion.gif"));
            inspectorPanel.activate(assertionInspector);
            break;
        }
        case UNKNOWN: {
            assertionInspector.setIcon(UISupport.createImageIcon("/unknown_assertion.png"));
            break;
        }
        case VALID: {
            assertionInspector.setIcon(UISupport.createImageIcon("/valid_assertion.gif"));
            inspectorPanel.deactivate();
            break;
        }
        }
    }

	  @Override
	  public void assertionInserted(TestAssertion arg0, int arg1) {
		  logger.info("assertionInserted");
	// TODO Auto-generated method stub
	    
	  }



}
