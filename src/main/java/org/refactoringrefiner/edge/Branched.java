package org.refactoringrefiner.edge;

import org.refactoringminer.api.Refactoring;

public class Branched extends AbstractChange {
    private final Refactoring refactoring;

    public Branched(Refactoring refactoring, String description) {
        super(Type.BRANCHED, description);
        this.refactoring = refactoring;
    }

    public Refactoring getRefactoring() {
        return refactoring;
    }
}
