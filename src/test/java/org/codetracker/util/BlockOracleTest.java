package org.codetracker.util;

import gr.uom.java.xmi.LocationInfo;
import org.codetracker.api.BlockTracker;
import org.codetracker.api.CodeTracker;
import org.codetracker.api.History;
import org.codetracker.element.Block;
import org.codetracker.experiment.oracle.BlockOracle;
import org.codetracker.experiment.oracle.history.BlockHistoryInfo;
import org.eclipse.jgit.lib.Repository;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.params.provider.Arguments;

import java.io.IOException;
import java.util.stream.Stream;

@Disabled
public class BlockOracleTest extends OracleTest {
	private static final String EXPECTED = System.getProperty("user.dir") + "/src/test/resources/block/";

	private static History<Block> blockTracker(BlockHistoryInfo blockHistoryInfo, Repository repository) throws Exception {
		BlockTracker blockTracker = CodeTracker
			.blockTracker()
			.repository(repository)
			.filePath(blockHistoryInfo.getFilePath())
			.startCommitId(blockHistoryInfo.getStartCommitId())
			.methodName(blockHistoryInfo.getFunctionName())
			.methodDeclarationLineNumber(blockHistoryInfo.getFunctionStartLine())
			.codeElementType(LocationInfo.CodeElementType.valueOf(blockHistoryInfo.getBlockType()))
			.blockStartLineNumber(blockHistoryInfo.getBlockStartLine())
			.blockEndLineNumber(blockHistoryInfo.getBlockEndLine())
			.build();
		return blockTracker.track();
	}
/*
	public static Stream<Arguments> testProvider() throws IOException {
		return getArgumentsStream(BlockOracle.all(), EXPECTED, BlockOracleTest::blockTracker);
	}
*/
}
