package org.refactoringrefiner.edge;

import org.refactoringminer.api.Refactoring;
import org.refactoringrefiner.api.CodeElement;

public class Extracted extends Added {
    private final Refactoring extractRefactoring;

    public Extracted(Refactoring extractRefactoring, CodeElement addedElement) {
        super(Type.INTRODUCED, addedElement);
        this.extractRefactoring = extractRefactoring;
    }

    public Refactoring getExtractRefactoring() {
        return extractRefactoring;
    }

    @Override
    public String toString() {
        return extractRefactoring != null ? extractRefactoring.toString() : String.format("A code element with name [%s] is extracted.", addedElement.getName());
    }
}
