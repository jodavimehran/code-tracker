package org.refactoringrefiner.change.method;

import org.refactoringminer.api.Refactoring;

public class AnnotationChange extends SignatureChange {
    public AnnotationChange(Refactoring refactoring) {
        super(Type.ANNOTATION_CHANGE, refactoring);
    }
}
