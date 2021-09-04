package org.codetracker.change;

import org.refactoringminer.api.Refactoring;

public class ContainerChange extends AbstractChange {
    private final Refactoring refactoring;

    public ContainerChange(Refactoring refactoring) {
        super(Type.CONTAINER_CHANGE);
        this.refactoring = refactoring;
    }

    public Refactoring getRefactoring() {
        return refactoring;
    }

    @Override
    public String toString() {
        if (refactoring != null)
            return String.format("The container of the code element is changed due to %s.", refactoring);
        return refactoring.toString();
    }
}
