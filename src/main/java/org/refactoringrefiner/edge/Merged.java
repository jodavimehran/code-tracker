package org.refactoringrefiner.edge;

import org.refactoringminer.api.Refactoring;

public class Merged extends AbstractChange {
    private final Refactoring refactoring;

    public Merged(Refactoring refactoring, String description) {
        super(Type.MERGED, description);
        this.refactoring = refactoring;
    }

    public Refactoring getRefactoring() {
        return refactoring;
    }
}
