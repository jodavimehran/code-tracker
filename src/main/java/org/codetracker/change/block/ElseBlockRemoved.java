package org.codetracker.change.block;

import org.codetracker.change.Change;

public class ElseBlockRemoved extends BlockChange {
	public ElseBlockRemoved() {
		super(Change.Type.ELSE_BLOCK_REMOVED);
	}

    @Override
    public String toString() {
        return "Else Block Removed";
    }
}
