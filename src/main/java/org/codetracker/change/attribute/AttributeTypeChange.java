package org.codetracker.change.attribute;

import org.refactoringminer.api.Refactoring;

public class AttributeTypeChange extends AttributeChange {
    public AttributeTypeChange(Refactoring refactoring) {
        super(Type.TYPE_CHANGE, refactoring);
    }
}
