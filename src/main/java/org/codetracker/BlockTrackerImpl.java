package org.codetracker;

import gr.uom.java.xmi.LocationInfo.CodeElementType;
import org.codetracker.BaseTracker;
import org.codetracker.ChangeHistory;
import org.codetracker.api.BlockTracker;
import org.codetracker.api.History;
import org.codetracker.element.Block;
import org.eclipse.jgit.lib.Repository;

public class BlockTrackerImpl extends BaseTracker implements BlockTracker {
    private final ChangeHistory<Block> blockChangeHistory = new ChangeHistory<>();
    private final String methodName;
    private final int methodDeclarationLineNumber;
    private final CodeElementType blockType;
    private final int blockStartLineNumber;
    private final int blockEndLineNumber;

    public BlockTrackerImpl(Repository repository, String startCommitId, String filePath,
                            String methodName, int methodDeclarationLineNumber,
                            CodeElementType blockType, int blockStartLineNumber, int blockEndLineNumber) {
        super(repository, startCommitId, filePath);
        this.methodName = methodName;
        this.methodDeclarationLineNumber = methodDeclarationLineNumber;
        this.blockType = blockType;
        this.blockStartLineNumber = blockStartLineNumber;
        this.blockEndLineNumber = blockEndLineNumber;
    }

    @Override
    public History<Block> track() throws Exception {
        return null;
    }
}
