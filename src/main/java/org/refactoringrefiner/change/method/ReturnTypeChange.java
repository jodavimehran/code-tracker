package org.refactoringrefiner.change.method;

import org.refactoringminer.api.Refactoring;

public class ReturnTypeChange extends SignatureChange {

    public ReturnTypeChange(Refactoring refactoring) {
        super(Type.RETURN_TYPE_CHANGE, refactoring);

    }

}
