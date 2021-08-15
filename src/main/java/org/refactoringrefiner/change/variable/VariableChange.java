package org.refactoringrefiner.change.variable;

import org.refactoringrefiner.change.AbstractChange;

public abstract class VariableChange extends AbstractChange {
    public VariableChange(Type type) {
        super(type);
    }
}
