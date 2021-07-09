package org.refactoringrefiner.edge;

import org.refactoringminer.api.Refactoring;
import org.refactoringrefiner.api.Change;

public class Modified extends AbstractChange {
    private final Refactoring refactoring;

    public Modified(Refactoring refactoring, String description) {
        super(Change.Type.MODIFIED, description);
        this.refactoring = refactoring;
    }

    public Refactoring getRefactoring() {
        return refactoring;
    }

    @Override
    public String toString() {
        if (refactoring != null)
            return String.format("The body of the code element is changed due to %s.", refactoring);
        if (description != null)
            return description;
        return "The body of the code element is changed.";
    }
}
