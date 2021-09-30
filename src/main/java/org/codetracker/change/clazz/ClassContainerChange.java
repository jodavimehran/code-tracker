package org.codetracker.change.clazz;

import org.refactoringminer.api.Refactoring;

public class ClassContainerChange extends ClassChange {
    public ClassContainerChange(Refactoring refactoring) {
        super(Type.CONTAINER_CHANGE, refactoring);

    }
}
