package org.codetracker;

import gr.uom.java.xmi.LocationInfo.CodeElementType;
import gr.uom.java.xmi.UMLModel;
import org.codetracker.api.BlockTracker;
import org.codetracker.api.CodeElementNotFoundException;
import org.codetracker.api.History;
import org.codetracker.api.Version;
import org.codetracker.element.Block;
import org.codetracker.element.Method;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Repository;

import java.util.Collections;

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

    private boolean isStartBlock(Block block) {
        return block.getComposite().getLocationInfo().getCodeElementType().equals(blockType) &&
                block.getComposite().getLocationInfo().getStartLine() == blockStartLineNumber &&
                block.getComposite().getLocationInfo().getEndLine() == blockEndLineNumber;
    }

    private boolean isStartMethod(Method method) {
        return method.getUmlOperation().getName().equals(methodName) &&
                method.getUmlOperation().getLocationInfo().getStartLine() <= methodDeclarationLineNumber &&
                method.getUmlOperation().getLocationInfo().getEndLine() >= methodDeclarationLineNumber;
    }

    @Override
    public History<Block> track() throws Exception {
        HistoryImpl.HistoryReportImpl historyReport = new HistoryImpl.HistoryReportImpl();
        try (Git git = new Git(repository)) {
            Version startVersion = gitRepository.getVersion(startCommitId);
            UMLModel umlModel = getUMLModel(startCommitId, Collections.singleton(filePath));
            Method startMethod = getMethod(umlModel, startVersion, this::isStartMethod);
            if (startMethod == null) {
                throw new CodeElementNotFoundException(filePath, methodName, methodDeclarationLineNumber);
            }
            Block startBlock = startMethod.findBlock(this::isStartBlock);
            if (startBlock == null) {
                throw new CodeElementNotFoundException(filePath, blockType.getName(), blockStartLineNumber);
            }
            blockChangeHistory.addNode(startBlock);


            return new HistoryImpl<>(blockChangeHistory.findSubGraph(startBlock), historyReport);
        }
    }
}
