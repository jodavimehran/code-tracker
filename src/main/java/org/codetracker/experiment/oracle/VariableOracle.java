package org.codetracker.experiment.oracle;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class VariableOracle extends Oracle {
    protected static final String HISTORY_VARIABLE_ORACLE = "oracle/variable/";
    private final Map<String, VariableHistoryInfo> oracle = new TreeMap<>();

    public VariableOracle(String name) throws IOException {
        super(name);
        readVariableOracle();
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

    private void readVariableOracle() throws IOException {
        File oracleFolder = new File(Oracle.class.getClassLoader().getResource(HISTORY_VARIABLE_ORACLE + name).getFile());
        for (File file : oracleFolder.listFiles()) {
            try {
                oracle.put(file.getName(), mapper.readValue(file, VariableHistoryInfo.class));
            } catch (Exception exception) {
                System.out.println(file.getName());
                throw exception;
            }
        }
    }

    public Map<String, VariableHistoryInfo> getOracle() {
        return oracle;
    }

    public Map<String, Integer> getNumberOfInstancePerChangeKind() {
        Map<String, Integer> changeType = new HashMap<>();
        for (Map.Entry<String, VariableHistoryInfo> entry : oracle.entrySet()) {
            boolean flag = false;
            HashSet<String> changeCommitSet = new HashSet<>();
            for (ChangeHistory expectedChanges : entry.getValue().getExpectedChanges()) {
                changeType.merge(expectedChanges.getChangeType(), 1, Integer::sum);
                String changeCommit = String.format("%s-%s", expectedChanges.getCommitId(), expectedChanges.getChangeType());
                if(changeCommitSet.contains(changeCommit)){
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
            if(!flag){
                System.out.println(entry.getKey());
            }
        }
        return changeType;
    }
}
