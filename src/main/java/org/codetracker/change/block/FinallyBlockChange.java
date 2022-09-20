package org.codetracker.change.block;

import org.codetracker.change.Change;

public class FinallyBlockChange extends BlockChange {
    public FinallyBlockChange() {
        super(Change.Type.FINALLY_BLOCK_CHANGE);
    }

    @Override
    public String toString() {
        return "Finally Block Change";
    }
}
