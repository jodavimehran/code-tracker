package org.codetracker.change.attribute;

import org.refactoringminer.api.Refactoring;

public class AttributeAccessModifierChange extends AttributeChange {
    public AttributeAccessModifierChange(Refactoring refactoring) {
        super(Type.ACCESS_MODIFIER_CHANGE, refactoring);
    }
}
