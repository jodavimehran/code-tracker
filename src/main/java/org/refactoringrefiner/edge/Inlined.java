package org.refactoringrefiner.edge;

import org.refactoringminer.api.Refactoring;
import org.refactoringrefiner.api.CodeElement;

import java.util.Objects;

public class Inlined extends Removed {
    private final Refactoring inlineRefactoring;

    public Inlined(Refactoring inlineRefactoring, CodeElement removedElement) {
        super(Type.INLINED, removedElement);
        this.inlineRefactoring = inlineRefactoring;
    }

    public Refactoring getInlineRefactoring() {
        return inlineRefactoring;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        Inlined inlined = (Inlined) o;
        return inlineRefactoring.equals(inlined.inlineRefactoring);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), inlineRefactoring);
    }

    @Override
    public String toString() {
        return inlineRefactoring.toString();
    }
}
