package org.refactoringrefiner.change.variable;

import org.refactoringminer.api.Refactoring;

public class VariableAnnotationChange extends VariableSignatureChange {
    public VariableAnnotationChange(Refactoring refactoring) {
        super(Type.ANNOTATION_CHANGE, refactoring);
    }
}
