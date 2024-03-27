package org.codetracker.util;

import java.io.IOException;
import java.util.List;

import org.codetracker.api.BlockTracker;
import org.codetracker.api.CodeTracker;
import org.codetracker.api.History;
import org.codetracker.element.Block;
import org.codetracker.experiment.oracle.BlockOracle;
import org.codetracker.experiment.oracle.history.BlockHistoryInfo;
import org.eclipse.jgit.lib.Repository;
import org.junit.jupiter.api.Test;

import gr.uom.java.xmi.LocationInfo.CodeElementType;;

public class BlockOracleTest extends OracleTest {
	private static final String EXPECTED = System.getProperty("user.dir") + "/src/test/resources/block/";

	private History<Block> blockTracker(BlockHistoryInfo blockHistoryInfo, Repository repository) throws Exception {
		BlockTracker blockTracker = CodeTracker
			.blockTracker()
			.repository(repository)
			.filePath(blockHistoryInfo.getFilePath())
			.startCommitId(blockHistoryInfo.getStartCommitId())
			.methodName(blockHistoryInfo.getFunctionName())
			.methodDeclarationLineNumber(blockHistoryInfo.getFunctionStartLine())
			.codeElementType(CodeElementType.valueOf(blockHistoryInfo.getBlockType()))
			.blockStartLineNumber(blockHistoryInfo.getBlockStartLine())
			.blockEndLineNumber(blockHistoryInfo.getBlockEndLine())
			.build();
		return blockTracker.track();
	}

	@Test
	public void testAccuracy() throws IOException, InterruptedException {
		List<BlockOracle> oracles = BlockOracle.all();
		for (BlockOracle oracle : oracles) {
			loadExpected(EXPECTED + oracle.getName() + "-expected.txt");
			codeTracker(oracle, this::blockTracker);
		}
	}
}
