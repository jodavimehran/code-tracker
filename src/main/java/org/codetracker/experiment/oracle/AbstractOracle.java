package org.codetracker.experiment.oracle;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.codetracker.experiment.oracle.history.AbstractHistoryInfo;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

public abstract class AbstractOracle<T extends AbstractHistoryInfo> {
    protected static final ObjectMapper mapper = new ObjectMapper();
    protected final String name;
    protected final Map<String, T> oracle = new TreeMap<>();
    private final Class<T> historyType;


    protected AbstractOracle(String name, Class<T> historyType) throws IOException {
        this.name = name;
        this.historyType = historyType;
        readOracle();
    }

    public String getName() {
        return name;
    }

    public Map<String, T> getOracle() {
        return oracle;
    }

    protected abstract String getOraclePath();

    private void readOracle() throws IOException {
        File oracleFolder = new File(Objects.requireNonNull(AbstractOracle.class.getClassLoader().getResource(getOraclePath() + name)).getFile());
        for (File file : Objects.requireNonNull(oracleFolder.listFiles())) {
            try {
                oracle.put(file.getName(), mapper.readValue(file, historyType));
            }catch (JsonMappingException exception){
                System.out.println(file.getName());
                throw exception;
            }
        }
    }
}
