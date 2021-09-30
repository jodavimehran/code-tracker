package org.codetracker.experiment;

import org.codetracker.api.ClassTracker;
import org.codetracker.api.CodeTracker;
import org.codetracker.api.History;
import org.codetracker.element.Class;
import org.codetracker.experiment.oracle.ClassOracle;
import org.codetracker.experiment.oracle.history.ClassHistoryInfo;
import org.eclipse.jgit.lib.Repository;

import java.io.IOException;
import java.util.List;

import static org.codetracker.util.FileUtil.createDirectory;

public class ClassExperimentStarter extends AbstractExperimentStarter {
    private static final String CODE_ELEMENT_NAME = "class";
    private static final String TOOL_NAME = "tracker";

    public static void main(String[] args) throws IOException {
        new ClassExperimentStarter().start();
    }

    public void start() throws IOException {
        createDirectory(new String[]{"experiments", "experiments/tracking-accuracy", "experiments/tracking-accuracy/class", "experiments/tracking-accuracy/class/tracker"});
        List<ClassOracle> oracles = ClassOracle.all();
        for (ClassOracle oracle : oracles) {
            codeTracker(oracle);
            calculateFinalResults(oracle.getName());
        }
    }

    private History<Class> classTracker(ClassHistoryInfo classHistoryInfo, Repository repository) throws Exception {
        ClassTracker classTracker = CodeTracker.classTracker()
                .repository(repository)
                .filePath(classHistoryInfo.getFilePath())
                .startCommitId(classHistoryInfo.getStartCommitId())
                .className(classHistoryInfo.getClassName())
                .classDeclarationLineNumber(classHistoryInfo.getClassDeclarationLine())
                .build();
        return classTracker.track();
    }

    private void codeTracker(ClassOracle classOracle) throws IOException {
        codeTracker(classOracle, this::classTracker);
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
