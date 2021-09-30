package org.codetracker.experiment.oracle;

import org.codetracker.experiment.oracle.history.AttributeHistoryInfo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class AttributeOracle extends AbstractOracle<AttributeHistoryInfo> {
    protected static final String HISTORY_ATTRIBUTE_ORACLE = "oracle/attribute/";


    private AttributeOracle(String name) throws IOException {
        super(name, AttributeHistoryInfo.class);
    }

    public static AttributeOracle test() throws IOException {
        return new AttributeOracle("test");
    }

    public static AttributeOracle training() throws IOException {
        return new AttributeOracle("training");
    }

    public static List<AttributeOracle> all() throws IOException {
        List<AttributeOracle> attributeOracles = new ArrayList<>();
        attributeOracles.add(training());
        attributeOracles.add(test());
        return attributeOracles;
    }

    @Override
    protected String getOraclePath() {
        return HISTORY_ATTRIBUTE_ORACLE;
    }
}
