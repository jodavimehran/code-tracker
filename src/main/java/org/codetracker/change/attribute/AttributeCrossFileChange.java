package org.codetracker.change.attribute;

import org.refactoringminer.api.Refactoring;

public abstract class AttributeCrossFileChange extends AttributeChange {
    protected AttributeCrossFileChange(Type type, Refactoring refactoring) {
        super(type, refactoring);
    }
}
