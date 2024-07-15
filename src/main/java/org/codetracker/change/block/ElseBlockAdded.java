package org.codetracker.change.block;

import org.codetracker.change.Change;

public class ElseBlockAdded extends BlockChange {
	public ElseBlockAdded() {
		super(Change.Type.ELSE_BLOCK_ADDED);
	}

    @Override
    public String toString() {
        return "Else Block Added";
    }
}
