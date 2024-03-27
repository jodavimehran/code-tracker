package org.codetracker.util;

import java.io.IOException;
import java.util.List;

import org.codetracker.api.AttributeTracker;
import org.codetracker.api.CodeTracker;
import org.codetracker.api.History;
import org.codetracker.element.Attribute;
import org.codetracker.experiment.oracle.AttributeOracle;
import org.codetracker.experiment.oracle.history.AttributeHistoryInfo;
import org.eclipse.jgit.lib.Repository;
import org.junit.jupiter.api.Test;

public class AttributeOracleTest extends OracleTest {
	private static final String EXPECTED = System.getProperty("user.dir") + "/src/test/resources/attribute/";

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

	@Test
	public void testAccuracy() throws IOException, InterruptedException {
		List<AttributeOracle> oracles = AttributeOracle.all();
		for (AttributeOracle oracle : oracles) {
			loadExpected(EXPECTED + oracle.getName() + "-expected.txt");
			codeTracker(oracle, this::attributeTracker);
		}
	}
}
