package org.codetracker.experiment.oracle;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class MethodOracle extends Oracle {
    protected static final String HISTORY_METHOD_ORACLE = "history/method/oracle/";
    private final Map<String, MethodHistoryInfo> oracle = new TreeMap<>();

    public MethodOracle(String name) throws IOException {
        super(name);
        readMethodOracle();
    }

    public static MethodOracle test() throws IOException {
        return new MethodOracle("test");
    }

    public static MethodOracle training() throws IOException {
        return new MethodOracle("training");
    }

    public static List<MethodOracle> all() throws IOException {
        List<MethodOracle> methodOracles = new ArrayList<>();
        methodOracles.add(training());
        methodOracles.add(test());
        return methodOracles;
    }

    private void readMethodOracle() throws IOException {
        File oracleFolder = new File(Oracle.class.getClassLoader().getResource(HISTORY_METHOD_ORACLE + name).getFile());
        for (File file : oracleFolder.listFiles()) {
            oracle.put(file.getName(), mapper.readValue(file, MethodHistoryInfo.class));
        }
    }

    public Map<String, MethodHistoryInfo> getOracle() {
        return oracle;
    }
}
