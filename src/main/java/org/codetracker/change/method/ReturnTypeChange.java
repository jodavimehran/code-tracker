package org.codetracker.change.method;

import org.refactoringminer.api.Refactoring;

public class ReturnTypeChange extends MethodSignatureChange {

    public ReturnTypeChange(Refactoring refactoring) {
        super(Type.RETURN_TYPE_CHANGE, refactoring);

    }

}
