package org.codetracker.experiment;

import org.codetracker.api.AttributeTracker;
import org.codetracker.api.CodeTracker;
import org.codetracker.api.History;
import org.codetracker.element.Attribute;
import org.codetracker.experiment.oracle.history.AttributeHistoryInfo;
import org.codetracker.experiment.oracle.AttributeOracle;
import org.eclipse.jgit.lib.Repository;

import java.io.IOException;
import java.util.List;

import static org.codetracker.util.FileUtil.createDirectory;

public class AttributeExperimentStarter extends AbstractExperimentStarter {
    private static final String CODE_ELEMENT_NAME = "attribute";
    private static final String TOOL_NAME = "tracker";

    public static void main(String[] args) throws IOException {
        new AttributeExperimentStarter().start();
    }

    public void start() throws IOException {
        createDirectory(new String[]{"experiments", "experiments/tracking-accuracy", "experiments/tracking-accuracy/attribute", "experiments/tracking-accuracy/attribute/tracker"});
        List<AttributeOracle> oracles = AttributeOracle.all();
        for (AttributeOracle oracle : oracles) {
            codeTracker(oracle);
            calculateFinalResults(oracle.getName());
        }
    }

    private History<Attribute> attributeTracker(AttributeHistoryInfo attributeHistoryInfo, Repository repository) throws Exception {
        AttributeTracker attributeTracker = CodeTracker.attributeTracker()
                .repository(repository)
                .filePath(attributeHistoryInfo.getFilePath())
                .startCommitId(attributeHistoryInfo.getStartCommitId())
                .attributeName(attributeHistoryInfo.getAttributeName())
                .attributeDeclarationLineNumber(attributeHistoryInfo.getAttributeDeclarationLine())
                .build();
        return attributeTracker.track();
    }

    private void codeTracker(AttributeOracle attributeOracle) throws IOException {
        codeTracker(attributeOracle, this::attributeTracker);
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
