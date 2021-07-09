package org.refactoringrefiner.edge;

import org.refactoringminer.api.Refactoring;

public class ContainerChange extends AbstractChange {
    private final Refactoring refactoring;

    public ContainerChange(Refactoring refactoring, String description) {
        super(Type.CONTAINER_CHANGE, description);
        this.refactoring = refactoring;
    }

    public Refactoring getRefactoring() {
        return refactoring;
    }

    @Override
    public String toString() {
        if (refactoring != null)
            return String.format("The container of the code element is changed due to %s.", refactoring);
        return description;
    }
}
