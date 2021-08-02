package org.refactoringrefiner.edge;

import org.refactoringminer.api.Refactoring;
import org.refactoringrefiner.api.CodeElement;

public class Inlined extends Removed {
    private final Refactoring inlineRefactoring;

    public Inlined(Refactoring inlineRefactoring, CodeElement removedElement, String description) {
        super(Type.REMOVED, removedElement);
        this.inlineRefactoring = inlineRefactoring;
    }

    public Refactoring getInlineRefactoring() {
        return inlineRefactoring;
    }

    @Override
    public String toString() {
        return inlineRefactoring.toString();
    }
}
