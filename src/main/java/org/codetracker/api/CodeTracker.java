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

    static BlockTracker.Builder blockTracker() {
        return new BlockTracker.Builder();
    }

    static CommentTracker.Builder commentTracker() {
        return new CommentTracker.Builder();
    }

    static AnnotationTracker.Builder annotationTracker() {
        return new AnnotationTracker.Builder();
    }

    static ImportTracker.Builder importTracker() {
        return new ImportTracker.Builder();
    }

    static ClassTracker.Builder classTracker() {
        return new ClassTracker.Builder();
    }
}
