package org.codetracker.experiment.oracle;

import java.io.IOException;
import java.util.*;
import org.codetracker.experiment.oracle.history.BlockHistoryInfo;
import org.codetracker.experiment.oracle.history.ChangeHistory;

public class BlockOracle extends AbstractOracle<BlockHistoryInfo> {

  protected static final String HISTORY_BLOCK_ORACLE = "oracle/block/";

  private BlockOracle(String name) throws IOException {
    super(name, BlockHistoryInfo.class);
  }

  public static BlockOracle test() throws IOException {
    return new BlockOracle("test");
  }

  public static BlockOracle training() throws IOException {
    return new BlockOracle("training");
  }

  public static List<BlockOracle> all() throws IOException {
    List<BlockOracle> BlockOracles = new ArrayList<>();
    BlockOracles.add(training());
    BlockOracles.add(test());
    return BlockOracles;
  }

  @Override
  protected String getOraclePath() {
    return HISTORY_BLOCK_ORACLE;
  }

  public Map<String, Integer> getNumberOfInstancePerChangeKind() {
    Map<String, Integer> changeType = new HashMap<>();
    for (Map.Entry<String, BlockHistoryInfo> entry : oracle.entrySet()) {
      boolean flag = false;
      HashSet<String> changeCommitSet = new HashSet<>();
      for (ChangeHistory expectedChanges : entry
        .getValue()
        .getExpectedChanges()) {
        changeType.merge(expectedChanges.getChangeType(), 1, Integer::sum);
        String changeCommit = String.format(
          "%s-%s",
          expectedChanges.getCommitId(),
          expectedChanges.getChangeType()
        );
        if (changeCommitSet.contains(changeCommit)) {
          System.out.println(entry.getKey());
        }
        changeCommitSet.add(changeCommit);
        if ("introduced".equals(expectedChanges.getChangeType())) {
          if (!flag) {
            flag = true;
          } else {
            System.out.println(entry.getKey());
          }
        }
      }
      if (!flag) {
        System.out.println(entry.getKey());
      }
    }
    return changeType;
  }
}
