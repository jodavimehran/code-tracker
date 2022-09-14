package org.codetracker.change.block;
import org.codetracker.change.Change;

public class ParenthesisChange extends BlockChange {
    public ParenthesisChange() {
        super(Change.Type.PARENTHESIS_CHANGE);
    }

    @Override
    public String toString() {
        return "Parenthesis Change";
    }
}
