package org.refactoringrefiner.change.variable;

import org.refactoringminer.api.Refactoring;

public class VariableContainerChange extends VariableChange {
    private final Refactoring refactoring;

    public VariableContainerChange(Refactoring refactoring) {
        super(Type.CONTAINER_CHANGE);
        this.refactoring = refactoring;
    }

    public Refactoring getRefactoring() {
        return refactoring;
    }
}
