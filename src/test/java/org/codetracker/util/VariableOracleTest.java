package org.codetracker.util;

import java.io.IOException;
import java.util.List;

import org.codetracker.api.CodeTracker;
import org.codetracker.api.History;
import org.codetracker.api.VariableTracker;
import org.codetracker.element.Variable;
import org.codetracker.experiment.oracle.VariableOracle;
import org.codetracker.experiment.oracle.history.VariableHistoryInfo;
import org.eclipse.jgit.lib.Repository;
import org.junit.jupiter.api.Test;

public class VariableOracleTest extends OracleTest {
	private static final String EXPECTED = System.getProperty("user.dir") + "/src/test/resources/variable/";

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

	@Test
	public void testAccuracy() throws IOException, InterruptedException {
		List<VariableOracle> oracles = VariableOracle.all();
		for (VariableOracle oracle : oracles) {
			loadExpected(EXPECTED + oracle.getName() + "-expected.txt");
			codeTracker(oracle, this::variableTracker, HALF_CORES);
		}
	}
}
