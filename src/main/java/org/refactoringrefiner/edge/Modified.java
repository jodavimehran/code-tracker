package org.refactoringrefiner.edge;

import org.refactoringminer.api.Refactoring;
import org.refactoringrefiner.api.Change;

public class Modified extends AbstractChange {
    private final Refactoring refactoring;

    public Modified(Refactoring refactoring) {
        super(Change.Type.MODIFIED);
        this.refactoring = refactoring;
    }

    public Refactoring getRefactoring() {
        return refactoring;
    }

    @Override
    public String toString() {
        if (refactoring == null)
            return "The body of the code element is changed.";
        return String.format("The body of the code element is changed due to %s.", refactoring.toString());
    }
}
