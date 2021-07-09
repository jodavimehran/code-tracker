package org.refactoringrefiner.edge;

import org.refactoringminer.api.Refactoring;

public class Refactored extends AbstractChange {
    private final Refactoring refactoring;
    private final Refactoring relatedRefactoring;

    public Refactored(Refactoring refactoring, String description) {
        this(refactoring, null, description);
    }

    public Refactored(Refactoring refactoring, Refactoring relatedRefactoring, String description) {
        super(Type.REFACTORED, description);
        this.refactoring = refactoring;
        this.relatedRefactoring = relatedRefactoring;
    }

    public Refactoring getRefactoring() {
        return refactoring;
    }

    public Refactoring getRelatedRefactoring() {
        return relatedRefactoring;
    }

    @Override
    public String toSummary() {
        return refactoring != null ? refactoring.getRefactoringType().getDisplayName() : description;
    }


    @Override
    public String toString() {
        return refactoring != null ? refactoring.toString() : description;
    }
}
