package org.codetracker.change.attribute;

import org.refactoringminer.api.Refactoring;

public class AttributeContainerChange extends AttributeCrossFileChange {
    public AttributeContainerChange(Refactoring refactoring) {
        super(Type.CONTAINER_CHANGE, refactoring);
    }
}
