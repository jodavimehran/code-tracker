package org.codetracker.change.attribute;

import org.refactoringminer.api.Refactoring;

public class AttributeMove extends AttributeCrossFileChange {
    public AttributeMove(Refactoring refactoring) {
        super(Type.MOVED, refactoring);
    }
}
