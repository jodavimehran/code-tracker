package org.codetracker.api;

public interface CodeTracker {
    static VariableTracker.Builder variableTracker() {
        return new VariableTracker.Builder();
    }

    static MethodTracker.Builder methodTracker() {
        return new MethodTracker.Builder();
    }

    static AttributeTracker.Builder attributeTracker() {
        return new AttributeTracker.Builder();
    }

    static ClassTracker.Builder classTracker() {
        return new ClassTracker.Builder();
    }
}
