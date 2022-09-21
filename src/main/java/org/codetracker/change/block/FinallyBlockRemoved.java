package org.codetracker.change.block;

import org.codetracker.change.Change;

public class FinallyBlockRemoved extends BlockChange {
    public FinallyBlockRemoved() {
        super(Change.Type.FINALLY_BLOCK_REMOVED);
    }

    @Override
    public String toString() {
        return "Finally Block Removed";
    }
}
