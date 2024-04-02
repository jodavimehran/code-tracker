package org.codetracker.util;

import org.codetracker.api.CodeTracker;
import org.codetracker.api.History;
import org.codetracker.api.VariableTracker;
import org.codetracker.element.Variable;
import org.codetracker.experiment.oracle.VariableOracle;
import org.codetracker.experiment.oracle.history.VariableHistoryInfo;
import org.eclipse.jgit.lib.Repository;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.params.provider.Arguments;

import java.io.IOException;
import java.util.stream.Stream;

@Disabled
public class VariableOracleTest extends OracleTest {
	private static final String EXPECTED = System.getProperty("user.dir") + "/src/test/resources/variable/";

    private static History<Variable> variableTracker(VariableHistoryInfo variableHistoryInfo, Repository repository) throws Exception {
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

	public static Stream<Arguments> testProvider() throws IOException {
		return getArgumentsStream(VariableOracle.all(), EXPECTED, VariableOracleTest::variableTracker);
	}
}
