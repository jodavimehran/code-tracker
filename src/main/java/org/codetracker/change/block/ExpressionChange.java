package org.codetracker.change.block;

import org.codetracker.change.Change;

public class ExpressionChange extends BlockChange {
    public ExpressionChange() { super(Type.EXPRESSION_CHANGE); }

    @Override
    public String toString() {
        return "Expression Change";
    }
}
