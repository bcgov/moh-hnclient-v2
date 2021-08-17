package ca.bc.gov.hlth.hl7v2plugin.teststeps;

public interface ExecutionListener {
    void afterExecution(ExecutableTestStep testStep, ExecutableTestStepResult executionResult);
}
