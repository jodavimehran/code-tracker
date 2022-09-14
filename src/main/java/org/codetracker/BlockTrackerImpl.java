package org.codetracker;

import gr.uom.java.xmi.*;
import gr.uom.java.xmi.LocationInfo.CodeElementType;
import gr.uom.java.xmi.decomposition.CompositeStatementObject;
import gr.uom.java.xmi.decomposition.OperationBody;
import gr.uom.java.xmi.decomposition.UMLOperationBodyMapper;
import gr.uom.java.xmi.diff.*;
import org.apache.commons.lang3.tuple.Pair;
import org.codetracker.api.History;
import org.codetracker.api.MethodTracker;
import org.codetracker.api.BlockTracker;
import org.codetracker.api.CodeElementNotFoundException;
import org.codetracker.api.Version;
import org.codetracker.change.Change;
import org.codetracker.change.ChangeFactory;
import org.codetracker.element.Block;
import org.codetracker.element.Method;
import org.codetracker.util.Util;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Repository;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringType;
import org.refactoringminer.rm1.GitHistoryRefactoringMinerImpl;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class BlockTrackerImpl extends BaseTracker implements BlockTracker {
    private final ChangeHistory<Block> blockChangeHistory = new ChangeHistory<>();
    private final String methodName;
    private final int methodDeclarationLineNumber;
    private final CodeElementType blockType;
    private final int blockDeclarationLineNumber;
    
    public BlockTrackerImpl(Repository repository, String startCommitId, String filePath, String methodName, int methodDeclarationLineNumber, CodeElementType blockType, int blockDeclarationLineNumber) {
        super(repository, startCommitId, filePath);
        this.methodName = methodName;
        this.methodDeclarationLineNumber = methodDeclarationLineNumber;
        this.blockType = blockType;
        this.blockDeclarationLineNumber = blockDeclarationLineNumber;
    }

    public static boolean checkOperationBodyChanged(OperationBody body1, OperationBody body2) {
        if (body1 == null && body2 == null) return false;

        if (body1 == null || body2 == null) {
            return true;
        }
        return body1.getBodyHashCode() != body2.getBodyHashCode();
    }

    public static boolean checkOperationDocumentationChanged(VariableDeclarationContainer operation1, VariableDeclarationContainer operation2) {
        String comments1 = Util.getSHA512(operation1.getComments().stream().map(UMLComment::getText).collect(Collectors.joining(";")));
        String comments2 = Util.getSHA512(operation2.getComments().stream().map(UMLComment::getText).collect(Collectors.joining(";")));
        return !comments1.equals(comments2);
    }

    private boolean isStartBlock(Block block) {
        return block.getLocation().getCodeElementType().equals(blockType) &&
        block.getLocation().getStartLine() <= blockDeclarationLineNumber &&
                block.getLocation().getEndLine() >= blockDeclarationLineNumber;
    }

    private boolean isStartMethod(Method method) {
        return method.getUmlOperation().getName().equals(methodName) &&
                method.getUmlOperation().getLocationInfo().getStartLine() <= methodDeclarationLineNumber &&
                method.getUmlOperation().getLocationInfo().getEndLine() >= methodDeclarationLineNumber;
    }

    @Override
    public History<Block> track() throws Exception {
        CompositeStatementObject composite;
        HistoryImpl.HistoryReportImpl historyReport = new HistoryImpl.HistoryReportImpl();
        try (Git git = new Git(repository)) {
            Version startVersion = gitRepository.getVersion(startCommitId);
            UMLModel umlModel = getUMLModel(startCommitId, Collections.singleton(filePath));
            Method startMethod = getMethod(umlModel, startVersion, this::isStartMethod);
            if (startMethod == null) {
                throw new CodeElementNotFoundException(filePath, methodName, methodDeclarationLineNumber);
            }

            Block startBlock = getBlock(composite, startMethod);
            if (startBlock == null) {
                throw new CodeElementNotFoundException(filePath, blockType.getName(), blockDeclarationLineNumber);
            }
            
            blockChangeHistory.addNode(startBlock);
            // TODO
            return new HistoryImpl<>(blockChangeHistory.findSubGraph(startBlock), historyReport);
        }
    }
}
