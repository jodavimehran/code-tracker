package org.codetracker.change.block;

import org.codetracker.change.Change;

public class FinallyBlockAdded extends BlockChange {
    public FinallyBlockAdded() {
        super(Change.Type.FINALLY_BLOCK_ADDED);
    }

    @Override
    public String toString() {
        return "Finally Block Added";
    }
}
