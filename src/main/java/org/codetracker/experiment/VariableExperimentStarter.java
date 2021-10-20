package org.codetracker.experiment;

import org.codetracker.api.CodeTracker;
import org.codetracker.api.History;
import org.codetracker.api.VariableTracker;
import org.codetracker.element.Variable;
import org.codetracker.experiment.oracle.history.VariableHistoryInfo;
import org.codetracker.experiment.oracle.VariableOracle;
import org.eclipse.jgit.lib.Repository;

import java.io.IOException;
import java.util.List;

import static org.codetracker.util.FileUtil.createDirectory;

public class VariableExperimentStarter extends AbstractExperimentStarter {

    private static final String TOOL_NAME = "tracker";
    private static final String CODE_ELEMENT_NAME = "variable";

    public static void main(String[] args) throws IOException {
        new VariableExperimentStarter().start();
    }

    @Override
    protected String getCodeElementName() {
        return CODE_ELEMENT_NAME;
    }

    @Override
    protected String getToolName() {
        return TOOL_NAME;
    }

    public void start() throws IOException {
        createDirectory("experiments", "experiments/tracking-accuracy", "experiments/tracking-accuracy/variable", "experiments/tracking-accuracy/variable/tracker");
        List<VariableOracle> oracles = VariableOracle.all();

        for (VariableOracle oracle : oracles) {
            codeTracker(oracle);
            calculateFinalResults(oracle.getName());
        }

    }

    private History<Variable> variableTracker(VariableHistoryInfo variableHistoryInfo, Repository repository) throws Exception {
        VariableTracker variableTracker = CodeTracker.variableTracker()
                .repository(repository)
                .filePath(variableHistoryInfo.getFilePath())
                .startCommitId(variableHistoryInfo.getStartCommitId())
                .methodName(variableHistoryInfo.getFunctionName())
                .methodDeclarationLineNumber(variableHistoryInfo.getFunctionStartLine())
                .variableName(variableHistoryInfo.getVariableName())
                .variableDeclarationLineNumber(variableHistoryInfo.getVariableStartLine())
                .build();
        return variableTracker.track();
    }

    private void codeTracker(VariableOracle variableOracle) throws IOException {
        codeTracker(variableOracle, this::variableTracker);
    }
}
