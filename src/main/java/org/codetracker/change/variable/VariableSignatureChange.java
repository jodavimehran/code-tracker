package org.refactoringrefiner.change.variable;

import org.refactoringminer.api.Refactoring;

public abstract class VariableSignatureChange extends VariableChange {

    private final Refactoring refactoring;

    public VariableSignatureChange(Type type, Refactoring refactoring) {
        super(type);
        this.refactoring = refactoring;
    }

    public final Refactoring getRefactoring() {
        return refactoring;
    }

    @Override
    public String toString() {
        return refactoring.toString();
    }
}
