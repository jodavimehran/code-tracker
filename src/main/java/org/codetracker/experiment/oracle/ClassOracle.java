package org.codetracker.experiment.oracle;

import org.codetracker.experiment.oracle.history.ClassHistoryInfo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ClassOracle extends AbstractOracle<ClassHistoryInfo> {
    protected static final String HISTORY_CLASS_ORACLE = "oracle/class/";


    private ClassOracle(String name) throws IOException {
        super(name, ClassHistoryInfo.class);
    }

    public static ClassOracle test() throws IOException {
        return new ClassOracle("test");
    }

    public static ClassOracle training() throws IOException {
        return new ClassOracle("training");
    }

    public static List<ClassOracle> all() throws IOException {
        List<ClassOracle> classOracles = new ArrayList<>();
        classOracles.add(training());
        classOracles.add(test());
        return classOracles;
    }

    @Override
    protected String getOraclePath() {
        return HISTORY_CLASS_ORACLE;
    }
}
