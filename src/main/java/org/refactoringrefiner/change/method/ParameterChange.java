package org.refactoringrefiner.change.method;

import org.refactoringminer.api.Refactoring;

public class ParameterChange extends MethodSignatureChange {

    public ParameterChange(Refactoring refactoring) {
        super(Type.PARAMETER_CHANGE, refactoring);
    }

}
