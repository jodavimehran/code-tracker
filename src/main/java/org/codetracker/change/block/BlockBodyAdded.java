package org.codetracker.change.block;

import org.codetracker.change.Change;

public class BlockBodyAdded extends BlockChange {

	public BlockBodyAdded() {
		super(Change.Type.BLOCK_BODY_ADDED);
	}

    @Override
    public String toString() {
        return "Block Body Added";
    }
}
