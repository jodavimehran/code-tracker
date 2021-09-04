package org.codetracker.api;

public interface CodeTracker {
    static VariableTracker.Builder variableTracker() {
        return new VariableTracker.Builder();
    }

    static MethodTracker.Builder methodTracker() {
        return new MethodTracker.Builder();
    }
}
