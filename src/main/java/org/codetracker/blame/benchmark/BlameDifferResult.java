package org.codetracker.blame.benchmark;

import org.codetracker.api.CodeElement;

import java.util.EnumMap;
import java.util.Map;

public class BlameDifferResult{
    private final Map<Integer, EnumMap<BlamerFactory, String>> table;
    private final Map<Integer, CodeElement> codeElementMap;
    private final int legitSize;

    public BlameDifferResult(Map<Integer, EnumMap<BlamerFactory, String>> table, Map<Integer, CodeElement> codeElementMap, int legitSize) {
        this.table = table;
        this.codeElementMap = codeElementMap;
        this.legitSize = legitSize;
    }

    public Map<Integer, EnumMap<BlamerFactory, String>> getTable() {
        return table;
    }

    public Map<Integer, CodeElement> getCodeElementMap() {
        return codeElementMap;
    }

    public int getLegitSize() {
        return legitSize;
    }
}
