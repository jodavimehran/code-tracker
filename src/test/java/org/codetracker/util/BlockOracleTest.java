package org.codetracker.util;

import gr.uom.java.xmi.LocationInfo;

import java.io.IOException;
import java.util.stream.Stream;

import org.codetracker.api.BlockTracker;
import org.codetracker.api.CodeTracker;
import org.codetracker.api.History;
import org.codetracker.element.Block;
import org.codetracker.experiment.oracle.BlockOracle;
import org.codetracker.experiment.oracle.history.BlockHistoryInfo;
import org.junit.jupiter.params.provider.Arguments;

public class BlockOracleTest extends OracleTest {
	private static final String EXPECTED = System.getProperty("user.dir") + "/src/test/resources/block/";

	private static History<Block> blockTracker(BlockHistoryInfo blockHistoryInfo, String gitURL) throws Exception {
		BlockTracker blockTracker = CodeTracker
			.blockTracker()
			.gitURL(gitURL)
			.filePath(blockHistoryInfo.getFilePath())
			.startCommitId(blockHistoryInfo.getStartCommitId())
			.methodName(blockHistoryInfo.getFunctionName())
			.methodDeclarationLineNumber(blockHistoryInfo.getFunctionStartLine())
			.codeElementType(LocationInfo.CodeElementType.valueOf(blockHistoryInfo.getBlockType()))
			.blockStartLineNumber(blockHistoryInfo.getBlockStartLine())
			.blockEndLineNumber(blockHistoryInfo.getBlockEndLine())
			.buildWithLocalFiles();
		return blockTracker.track();
	}

	public static Stream<Arguments> testProvider() throws IOException {
		return getArgumentsStream(BlockOracle.all(), EXPECTED, BlockOracleTest::blockTracker);
	}

}
