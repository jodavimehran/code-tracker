package org.codetracker.change.method;

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

    @Override
    public String toString() {
        if (refactoring != null)
            return refactoring.toString();
        return type.getTitle();
    }
}
