package org.refactoringrefiner.edge;

import org.refactoringminer.api.Refactoring;

import java.util.Objects;

public class Refactored extends AbstractChange {
    private final Refactoring refactoring;

    public Refactored(Refactoring refactoring) {
        super(Type.REFACTORED);
        this.refactoring = refactoring;
    }

    public Refactoring getRefactoring() {
        return refactoring;
    }

    @Override
    public String toSummary() {
        return refactoring.getRefactoringType().getDisplayName();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        Refactored that = (Refactored) o;
        return refactoring.equals(that.refactoring);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), refactoring);
    }

    @Override
    public String toString() {
        return refactoring.toString();
    }
}
