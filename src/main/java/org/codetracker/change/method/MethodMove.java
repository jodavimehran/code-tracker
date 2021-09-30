package org.codetracker.change.method;

import org.refactoringminer.api.Refactoring;

public class MethodMove extends CrossFileChange {
    public MethodMove(Refactoring refactoring) {
        super(Type.MOVED, refactoring);
    }
}
