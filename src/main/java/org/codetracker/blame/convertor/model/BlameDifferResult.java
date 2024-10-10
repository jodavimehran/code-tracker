package org.codetracker.blame.convertor.model;

import org.codetracker.blame.model.CodeElementWithRepr;
import org.codetracker.blame.model.IBlameTool;

import java.util.Map;

public class BlameDifferResult{
    private final Map<Integer, Map<IBlameTool, String>> table;
    private final Map<Integer, CodeElementWithRepr> codeElementWithReprMap;
    private final int legitSize;

    public BlameDifferResult(Map<Integer, Map<IBlameTool, String>> table, Map<Integer, CodeElementWithRepr> codeElementWithReprMap, int legitSize) {
        this.table = table;
        this.codeElementWithReprMap = codeElementWithReprMap;
        this.legitSize = legitSize;
    }

    public Map<Integer, Map<IBlameTool, String>> getTable() {
        return table;
    }

    public Map<Integer, CodeElementWithRepr> getCodeElementWithReprMap() {
        return codeElementWithReprMap;
    }

    public int getLegitSize() {
        return legitSize;
    }
}
