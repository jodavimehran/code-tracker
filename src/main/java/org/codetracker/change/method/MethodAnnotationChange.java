package org.codetracker.change.method;

import org.refactoringminer.api.Refactoring;

public class MethodAnnotationChange extends MethodSignatureChange {
    public MethodAnnotationChange(Refactoring refactoring) {
        super(Type.ANNOTATION_CHANGE, refactoring);
    }
}
