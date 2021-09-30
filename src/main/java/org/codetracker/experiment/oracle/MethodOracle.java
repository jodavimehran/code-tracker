package org.codetracker.experiment.oracle;

import org.codetracker.experiment.oracle.history.MethodHistoryInfo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MethodOracle extends AbstractOracle<MethodHistoryInfo> {
    protected static final String HISTORY_METHOD_ORACLE = "oracle/method/";

    private MethodOracle(String name) throws IOException {
        super(name, MethodHistoryInfo.class);
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

    @Override
    protected String getOraclePath() {
        return HISTORY_METHOD_ORACLE;
    }
}
