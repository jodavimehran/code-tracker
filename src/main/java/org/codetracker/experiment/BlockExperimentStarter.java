package org.codetracker.experiment;

import static org.codetracker.util.FileUtil.createDirectory;

import gr.uom.java.xmi.LocationInfo;
import java.io.IOException;
import java.util.List;
import org.codetracker.api.BlockTracker;
import org.codetracker.api.CodeTracker;
import org.codetracker.api.History;
import org.codetracker.element.Block;
import org.codetracker.experiment.oracle.BlockOracle;
import org.codetracker.experiment.oracle.history.BlockHistoryInfo;
import org.eclipse.jgit.lib.Repository;

public class BlockExperimentStarter extends AbstractExperimentStarter {

  private static final String TOOL_NAME = "tracker";
  private static final String CODE_ELEMENT_NAME = "block";

  public static void main(String[] args) throws IOException {
    new BlockExperimentStarter().start();
  }

  @Override
  protected String getCodeElementName() {
    return CODE_ELEMENT_NAME;
  }

  @Override
  protected String getToolName() {
    return TOOL_NAME;
  }

  public void start() throws IOException {
    createDirectory(
      "experiments",
      "experiments/tracking-accuracy",
      "experiments/tracking-accuracy/block",
      "experiments/tracking-accuracy/block/tracker"
    );
    List<BlockOracle> oracles = BlockOracle.all();

    for (BlockOracle oracle : oracles) {
      codeTracker(oracle);
      calculateFinalResults(oracle.getName());
    }
  }

  private History<Block> blockTracker(
    BlockHistoryInfo blockHistoryInfo,
    Repository repository
  ) throws Exception {
    BlockTracker blockTracker = CodeTracker
      .blockTracker()
      .repository(repository)
      .filePath(blockHistoryInfo.getFilePath())
      .startCommitId(blockHistoryInfo.getStartCommitId())
      .methodName(blockHistoryInfo.getFunctionName())
      .methodDeclarationLineNumber(blockHistoryInfo.getFunctionStartLine())
      .codeElementType(
              LocationInfo.CodeElementType.valueOf(blockHistoryInfo.getBlockType())
      )
      .blockStartLineNumber(blockHistoryInfo.getBlockStartLine())
      .blockEndLineNumber(blockHistoryInfo.getBlockEndLine())
      .build();
    return blockTracker.track();
  }

  private void codeTracker(BlockOracle blockOracle) throws IOException {
    codeTracker(blockOracle, this::blockTracker);
  }
}
