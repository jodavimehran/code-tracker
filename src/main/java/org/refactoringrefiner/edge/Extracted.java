package org.refactoringrefiner.edge;

import org.refactoringminer.api.Refactoring;
import org.refactoringrefiner.api.CodeElement;

import java.util.Objects;

public class Extracted extends Added {
    private final Refactoring extractRefactoring;

    public Extracted(Refactoring extractRefactoring, CodeElement addedElement) {
        super(Type.EXTRACTED, addedElement);
        this.extractRefactoring = extractRefactoring;
    }

    public Refactoring getExtractRefactoring() {
        return extractRefactoring;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        Extracted extracted = (Extracted) o;
        return extractRefactoring.equals(extracted.extractRefactoring);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), extractRefactoring);
    }

    @Override
    public String toString() {
        return extractRefactoring.toString();
    }
}
