package org.codetracker.experiment.oracle;

import org.codetracker.experiment.oracle.history.ChangeHistory;
import org.codetracker.experiment.oracle.history.VariableHistoryInfo;

import java.io.IOException;
import java.util.*;

public class VariableOracle extends AbstractOracle<VariableHistoryInfo> {
    protected static final String HISTORY_VARIABLE_ORACLE = "oracle/variable/";

    private VariableOracle(String name) throws IOException {
        super(name, VariableHistoryInfo.class);
    }

    public static VariableOracle test() throws IOException {
        return new VariableOracle("test");
    }

    public static VariableOracle training() throws IOException {
        return new VariableOracle("training");
    }

    public static List<VariableOracle> all() throws IOException {
        List<VariableOracle> variableOracles = new ArrayList<>();
        variableOracles.add(training());
        variableOracles.add(test());
        return variableOracles;
    }

    @Override
    protected String getOraclePath() {
        return HISTORY_VARIABLE_ORACLE;
    }

    public Map<String, Integer> getNumberOfInstancePerChangeKind() {
        Map<String, Integer> changeType = new HashMap<>();
        for (Map.Entry<String, VariableHistoryInfo> entry : oracle.entrySet()) {
            boolean flag = false;
            HashSet<String> changeCommitSet = new HashSet<>();
            for (ChangeHistory expectedChanges : entry.getValue().getExpectedChanges()) {
                changeType.merge(expectedChanges.getChangeType(), 1, Integer::sum);
                String changeCommit = String.format("%s-%s", expectedChanges.getCommitId(), expectedChanges.getChangeType());
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
