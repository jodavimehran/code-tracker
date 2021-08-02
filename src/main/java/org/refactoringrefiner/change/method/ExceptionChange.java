package org.refactoringrefiner.change.method;

import org.refactoringminer.api.Refactoring;
import org.refactoringrefiner.api.Change;

public class ExceptionChange extends SignatureChange {
    public ExceptionChange(Refactoring refactoring) {
        super(Change.Type.EXCEPTION_CHANGE, refactoring);
    }
}
