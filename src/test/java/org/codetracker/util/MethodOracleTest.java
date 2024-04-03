package org.codetracker.util;

import org.codetracker.api.CodeTracker;
import org.codetracker.api.History;
import org.codetracker.api.MethodTracker;
import org.codetracker.element.Method;
import org.codetracker.experiment.oracle.MethodOracle;
import org.codetracker.experiment.oracle.history.MethodHistoryInfo;
import org.eclipse.jgit.lib.Repository;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.params.provider.Arguments;

import java.io.IOException;
import java.util.stream.Stream;

@Disabled
public class MethodOracleTest extends OracleTest {
	private static final String EXPECTED = System.getProperty("user.dir") + "/src/test/resources/method/";

	private static History<Method> methodTracker(MethodHistoryInfo methodHistoryInfo, Repository repository) throws Exception {
		MethodTracker methodTracker = CodeTracker.methodTracker()
			.repository(repository)
			.filePath(methodHistoryInfo.getFilePath())
			.startCommitId(methodHistoryInfo.getStartCommitId())
			.methodName(methodHistoryInfo.getFunctionName())
			.methodDeclarationLineNumber(methodHistoryInfo.getFunctionStartLine())
			.build();
		return methodTracker.track();
	}
	public static Stream<Arguments> testProvider() throws IOException {
		return getArgumentsStream(MethodOracle.all(), EXPECTED, MethodOracleTest::methodTracker);
	}
}
