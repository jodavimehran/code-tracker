package org.refactoringrefiner.test.oracle;

import com.fasterxml.jackson.databind.ObjectMapper;

abstract class Oracle {
    protected static final String HISTORY_METHOD_ORACLE = "history/method/oracle/";
    protected static final ObjectMapper mapper = new ObjectMapper();

    protected final String name;


    protected Oracle(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
