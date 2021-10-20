package org.codetracker.experiment;

import org.codetracker.api.CodeTracker;
import org.codetracker.api.History;
import org.codetracker.api.MethodTracker;
import org.codetracker.element.Method;
import org.codetracker.experiment.oracle.history.MethodHistoryInfo;
import org.codetracker.experiment.oracle.MethodOracle;
import org.eclipse.jgit.lib.Repository;

import java.io.IOException;
import java.util.List;

import static org.codetracker.util.FileUtil.createDirectory;

public class MethodExperimentStarter extends AbstractExperimentStarter {
    private static final String CODE_ELEMENT_NAME = "method";
    private static final String TOOL_NAME = "tracker";

    public static void main(String[] args) throws IOException {
        new MethodExperimentStarter().start();
    }

    public void start() throws IOException {
        createDirectory("experiments", "experiments/tracking-accuracy", "experiments/tracking-accuracy/method", "experiments/tracking-accuracy/method/tracker");
        List<MethodOracle> oracles = MethodOracle.all();
        for (MethodOracle oracle : oracles) {
            codeTracker(oracle);
            calculateFinalResults(oracle.getName());
        }

    }

    private History<Method> methodTracker(MethodHistoryInfo methodHistoryInfo, Repository repository) throws Exception {
        MethodTracker methodTracker = CodeTracker.methodTracker()
                .repository(repository)
                .filePath(methodHistoryInfo.getFilePath())
                .startCommitId(methodHistoryInfo.getStartCommitId())
                .methodName(methodHistoryInfo.getFunctionName())
                .methodDeclarationLineNumber(methodHistoryInfo.getFunctionStartLine())
                .build();
        return methodTracker.track();
    }

    private void codeTracker(MethodOracle methodOracle) throws IOException {
        codeTracker(methodOracle, this::methodTracker);
    }

    @Override
    protected String getCodeElementName() {
        return CODE_ELEMENT_NAME;
    }

    @Override
    protected String getToolName() {
        return TOOL_NAME;
    }
}
