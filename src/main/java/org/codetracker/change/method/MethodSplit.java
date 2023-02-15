package org.codetracker.change.method;

import org.refactoringminer.api.Refactoring;

public class MethodSplit extends MethodSignatureChange {
    public MethodSplit(Refactoring refactoring) {
        super(Type.METHOD_SPLIT, refactoring);
    }
}
