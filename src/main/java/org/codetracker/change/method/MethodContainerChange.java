package org.refactoringrefiner.change.method;

import org.refactoringminer.api.Refactoring;

public class MethodContainerChange extends CrossFileChange {
    public MethodContainerChange(Refactoring refactoring) {
        super(Type.CONTAINER_CHANGE, refactoring);
    }
}
