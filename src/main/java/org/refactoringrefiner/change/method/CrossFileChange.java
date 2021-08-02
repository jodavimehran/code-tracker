package org.refactoringrefiner.change.method;

import org.refactoringminer.api.Refactoring;

public abstract class CrossFileChange extends MethodChange {
    private final Refactoring refactoring;

    public CrossFileChange(Type type, Refactoring refactoring) {
        super(type);
        this.refactoring = refactoring;
    }

    public final Refactoring getRefactoring() {
        return refactoring;
    }
}
