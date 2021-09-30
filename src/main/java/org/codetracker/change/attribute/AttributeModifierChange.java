package org.codetracker.change.attribute;

import org.refactoringminer.api.Refactoring;

public class AttributeModifierChange extends AttributeChange {
    public AttributeModifierChange(Refactoring refactoring) {
        super(Type.MODIFIER_CHANGE, refactoring);
    }
}
