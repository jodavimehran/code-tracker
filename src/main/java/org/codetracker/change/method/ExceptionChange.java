package org.codetracker.change.method;

import org.refactoringminer.api.Refactoring;
import org.codetracker.change.Change;

public class ExceptionChange extends MethodSignatureChange {
    public ExceptionChange(Refactoring refactoring) {
        super(Change.Type.EXCEPTION_CHANGE, refactoring);
    }
}
