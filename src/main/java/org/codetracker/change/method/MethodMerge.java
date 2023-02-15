package org.codetracker.change.method;

import org.refactoringminer.api.Refactoring;

public class MethodMerge extends MethodSignatureChange {
    public MethodMerge(Refactoring refactoring) {
        super(Type.METHOD_MERGE, refactoring);
    }
}
