package org.refactoringrefiner.edge;

import org.refactoringminer.api.Refactoring;

import java.util.Objects;

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
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        ContainerChange that = (ContainerChange) o;
        return Objects.equals(refactoring, that.refactoring);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), refactoring);
    }

    @Override
    public String toString() {
        return String.format("The container of the code element is changed due to %s.", refactoring.toString());
    }
}
