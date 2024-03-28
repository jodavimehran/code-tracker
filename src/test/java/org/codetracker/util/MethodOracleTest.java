package org.codetracker.util;

import java.io.IOException;
import java.util.List;

import org.codetracker.api.CodeTracker;
import org.codetracker.api.History;
import org.codetracker.api.MethodTracker;
import org.codetracker.element.Method;
import org.codetracker.experiment.oracle.MethodOracle;
import org.codetracker.experiment.oracle.history.MethodHistoryInfo;
import org.eclipse.jgit.lib.Repository;
import org.junit.jupiter.api.Test;

public class MethodOracleTest extends OracleTest {
	private static final String EXPECTED = System.getProperty("user.dir") + "/src/test/resources/method/";

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

	@Test
	public void testAccuracy() throws IOException, InterruptedException {
		List<MethodOracle> oracles = MethodOracle.all();
		for (MethodOracle oracle : oracles) {
			loadExpected(EXPECTED + oracle.getName() + "-expected.txt");
			codeTracker(oracle, this::methodTracker, ALL_CORES);
		}
	}
}
