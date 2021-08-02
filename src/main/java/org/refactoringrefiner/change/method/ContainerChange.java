package org.refactoringrefiner.change.method;

import org.refactoringminer.api.Refactoring;

public class ContainerChange extends CrossFileChange {
    public ContainerChange(Refactoring refactoring) {
        super(Type.CONTAINER_CHANGE, refactoring);
    }
}
