package org.codetracker.util;

import org.codetracker.api.AttributeTracker;
import org.codetracker.api.CodeTracker;
import org.codetracker.api.History;
import org.codetracker.element.Attribute;
import org.codetracker.experiment.oracle.AttributeOracle;
import org.codetracker.experiment.oracle.history.AttributeHistoryInfo;
import org.junit.jupiter.params.provider.Arguments;

import java.io.IOException;
import java.util.stream.Stream;

public class AttributeOracleTest extends OracleTest {
	private static final String EXPECTED = System.getProperty("user.dir") + "/src/test/resources/attribute/";

	private static History<Attribute> attributeTracker(AttributeHistoryInfo attributeHistoryInfo, String gitURL) throws Exception {
		AttributeTracker attributeTracker = CodeTracker.attributeTracker()
			.gitURL(gitURL)
			.filePath(attributeHistoryInfo.getFilePath())
			.startCommitId(attributeHistoryInfo.getStartCommitId())
			.attributeName(attributeHistoryInfo.getAttributeName())
			.attributeDeclarationLineNumber(attributeHistoryInfo.getAttributeDeclarationLine())
			.buildWithLocalFiles();
		return attributeTracker.track();
	}

	public static Stream<Arguments> testProvider() throws IOException {
		return getArgumentsStream(AttributeOracle.all(), EXPECTED, AttributeOracleTest::attributeTracker);
	}

}
