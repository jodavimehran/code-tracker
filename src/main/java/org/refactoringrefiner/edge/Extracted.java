package org.refactoringrefiner.edge;

import org.refactoringminer.api.Refactoring;
import org.refactoringrefiner.api.CodeElement;

public class Extracted extends Added {
    private final Refactoring extractRefactoring;

    public Extracted(Refactoring extractRefactoring, CodeElement addedElement, String description) {
        super(Type.EXTRACTED, addedElement, description);
        this.extractRefactoring = extractRefactoring;
    }

    public Refactoring getExtractRefactoring() {
        return extractRefactoring;
    }

    @Override
    public String toString() {
        return extractRefactoring != null ? extractRefactoring.toString() : description;
    }
}
