package org.codetracker.experiment.oracle;

import com.fasterxml.jackson.databind.ObjectMapper;

abstract class Oracle {

    protected static final ObjectMapper mapper = new ObjectMapper();

    protected final String name;


    protected Oracle(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
