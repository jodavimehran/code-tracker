package org.codetracker.blame.benchmark.model;

import org.codetracker.blame.benchmark.BlamerFactory;
import org.codetracker.blame.model.CodeElementWithRepr;

import java.util.EnumMap;
import java.util.Map;

public class BlameDifferResult{
    private final Map<Integer, EnumMap<BlamerFactory, String>> table;
    private final Map<Integer, CodeElementWithRepr> codeElementWithReprMap;
    private final int legitSize;

    public BlameDifferResult(Map<Integer, EnumMap<BlamerFactory, String>> table, Map<Integer, CodeElementWithRepr> codeElementWithReprMap, int legitSize) {
        this.table = table;
        this.codeElementWithReprMap = codeElementWithReprMap;
        this.legitSize = legitSize;
    }

    public Map<Integer, EnumMap<BlamerFactory, String>> getTable() {
        return table;
    }

    public Map<Integer, CodeElementWithRepr> getCodeElementWithReprMap() {
        return codeElementWithReprMap;
    }

    public int getLegitSize() {
        return legitSize;
    }
}
