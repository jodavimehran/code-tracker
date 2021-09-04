package org.refactoringrefiner.change;

import org.refactoringminer.api.Refactoring;
import org.refactoringrefiner.api.CodeElement;
import org.refactoringrefiner.change.Added;

public class Extracted extends Added {


    public Extracted(Refactoring extractRefactoring, CodeElement addedElement) {
        super(Type.INTRODUCED, addedElement, extractRefactoring);

    }

    public Refactoring getExtractRefactoring() {
        return refactoring;
    }

    @Override
    public String toString() {
        return refactoring != null ? refactoring.toString() : String.format("A code element with name [%s] is extracted.", addedElement.getName());
    }
}
