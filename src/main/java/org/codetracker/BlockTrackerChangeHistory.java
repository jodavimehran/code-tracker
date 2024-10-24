package org.codetracker;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.codetracker.api.History;
import org.codetracker.api.History.HistoryInfo;
import org.codetracker.api.Version;
import org.codetracker.change.AbstractChange;
import org.codetracker.change.Change;
import org.codetracker.change.ChangeFactory;
import org.codetracker.change.Introduced;
import org.codetracker.change.block.BlockSignatureFormatChange;
import org.codetracker.change.block.ElseBlockAdded;
import org.codetracker.change.block.ExpressionChange;
import org.codetracker.change.block.MergeBlock;
import org.codetracker.change.block.ReplaceConditionalWithTernary;
import org.codetracker.change.block.ReplaceLoopWithPipeline;
import org.codetracker.change.block.ReplacePipelineWithLoop;
import org.codetracker.change.block.SplitBlock;
import org.codetracker.change.method.BodyChange;
import org.codetracker.element.Block;
import org.codetracker.element.Method;
import org.refactoringminer.api.Refactoring;

import com.github.difflib.DiffUtils;
import com.github.difflib.patch.AbstractDelta;
import com.github.difflib.patch.Chunk;
import com.github.difflib.patch.InsertDelta;
import com.github.difflib.patch.Patch;

import gr.uom.java.xmi.UMLOperation;
import gr.uom.java.xmi.UMLType;
import gr.uom.java.xmi.VariableDeclarationContainer;
import gr.uom.java.xmi.LocationInfo.CodeElementType;
import gr.uom.java.xmi.decomposition.AbstractCodeFragment;
import gr.uom.java.xmi.decomposition.AbstractCodeMapping;
import gr.uom.java.xmi.decomposition.AbstractExpression;
import gr.uom.java.xmi.decomposition.CompositeStatementObject;
import gr.uom.java.xmi.decomposition.CompositeStatementObjectMapping;
import gr.uom.java.xmi.decomposition.LeafMapping;
import gr.uom.java.xmi.decomposition.StatementObject;
import gr.uom.java.xmi.decomposition.TryStatementObject;
import gr.uom.java.xmi.decomposition.UMLOperationBodyMapper;
import gr.uom.java.xmi.decomposition.VariableDeclaration;
import gr.uom.java.xmi.diff.ExtractOperationRefactoring;
import gr.uom.java.xmi.diff.InlineOperationRefactoring;
import gr.uom.java.xmi.diff.MergeOperationRefactoring;
import gr.uom.java.xmi.diff.MoveCodeRefactoring;
import gr.uom.java.xmi.diff.MoveOperationRefactoring;
import gr.uom.java.xmi.diff.PullUpOperationRefactoring;
import gr.uom.java.xmi.diff.PushDownOperationRefactoring;
import gr.uom.java.xmi.diff.RenameOperationRefactoring;
import gr.uom.java.xmi.diff.ReplaceConditionalWithTernaryRefactoring;
import gr.uom.java.xmi.diff.ReplaceLoopWithPipelineRefactoring;
import gr.uom.java.xmi.diff.ReplacePipelineWithLoopRefactoring;
import gr.uom.java.xmi.diff.SplitConditionalRefactoring;
import gr.uom.java.xmi.diff.SplitOperationRefactoring;
import gr.uom.java.xmi.diff.UMLAbstractClassDiff;
import gr.uom.java.xmi.diff.UMLAnonymousClassDiff;

public class BlockTrackerChangeHistory extends AbstractChangeHistory<Block> {
	private final ChangeHistory<Block> blockChangeHistory = new ChangeHistory<>();
    private final String methodName;
    private final int methodDeclarationLineNumber;
    private final CodeElementType blockType;
    private final int blockStartLineNumber;
    private final int blockEndLineNumber;

	public BlockTrackerChangeHistory(String methodName, int methodDeclarationLineNumber, CodeElementType blockType,
			int blockStartLineNumber, int blockEndLineNumber) {
		this.methodName = methodName;
		this.methodDeclarationLineNumber = methodDeclarationLineNumber;
		this.blockType = blockType;
		this.blockStartLineNumber = blockStartLineNumber;
		this.blockEndLineNumber = blockEndLineNumber;
	}

	public ChangeHistory<Block> get() {
		return blockChangeHistory;
	}

	public String getMethodName() {
		return methodName;
	}

	public int getMethodDeclarationLineNumber() {
		return methodDeclarationLineNumber;
	}

	public CodeElementType getBlockType() {
		return blockType;
	}

	public int getBlockStartLineNumber() {
		return blockStartLineNumber;
	}

	public int getBlockEndLineNumber() {
		return blockEndLineNumber;
	}

    public boolean isStartBlock(Block block) {
        return block.getComposite().getLocationInfo().getCodeElementType().equals(blockType) &&
                block.getComposite().getLocationInfo().getStartLine() == blockStartLineNumber &&
                block.getComposite().getLocationInfo().getEndLine() == blockEndLineNumber;
    }

    public boolean isStartMethod(Method method) {
        return method.getUmlOperation().getName().equals(methodName) &&
                method.getUmlOperation().getLocationInfo().getStartLine() <= methodDeclarationLineNumber &&
                method.getUmlOperation().getLocationInfo().getEndLine() >= methodDeclarationLineNumber;
    }

    public boolean checkClassDiffForBlockChange(Version currentVersion, Version parentVersion, Predicate<Method> equalMethod, Predicate<Block> equalBlock, UMLAbstractClassDiff umlClassDiff) {
        for (UMLOperationBodyMapper operationBodyMapper : umlClassDiff.getOperationBodyMapperList()) {
            Method method2 = Method.of(operationBodyMapper.getContainer2(), currentVersion);
            if (equalMethod.test(method2)) {
                if (isBlockRefactored(operationBodyMapper.getRefactoringsAfterPostProcessing(), currentVersion, parentVersion, equalBlock))
                    return true;
                // check if it is in the matched
                if (isMatched(operationBodyMapper, currentVersion, parentVersion, equalBlock))
                    return true;
                //Check if is added
                if (isAdded(operationBodyMapper, currentVersion, parentVersion, equalBlock))
                    return true;
            }
        }
        return false;
    }

    public boolean isMergeMultiMapping(Version currentVersion, Version parentVersion, Predicate<Method> equalMethod, Block rightBlock, List<Refactoring> refactorings) {
    	Set<Pair<Block, Block>> mappings = new LinkedHashSet<>();
    	AbstractCodeFragment fragment2 = null;
    	int fragment2Matches = 0;
    	MergeOperationRefactoring mergeOperationRefactoring = null;
    	for (Refactoring refactoring : refactorings) {
            switch (refactoring.getRefactoringType()) {
	            case MERGE_OPERATION: {
	                mergeOperationRefactoring = (MergeOperationRefactoring) refactoring;
	                Method methodAfter = Method.of(mergeOperationRefactoring.getNewMethodAfterMerge(), currentVersion);
	                if (equalMethod.test(methodAfter)) {
	                    for (UMLOperationBodyMapper bodyMapper : mergeOperationRefactoring.getMappers()) {
	                        for (AbstractCodeMapping mapping : bodyMapper.getMappings()) {
	                            if (mapping instanceof CompositeStatementObjectMapping) {
	                                Block matchedBlockInsideMergedMethodBody = Block.of((CompositeStatementObject) mapping.getFragment2(), bodyMapper.getContainer2(), currentVersion);
	                                if (matchedBlockInsideMergedMethodBody.equalIdentifierIgnoringVersion(rightBlock)) {
                                		Block blockBefore = Block.of((CompositeStatementObject) mapping.getFragment1(), bodyMapper.getContainer1(), parentVersion);
                                    	mappings.add(Pair.of(blockBefore, matchedBlockInsideMergedMethodBody));
                                    	if (fragment2 == null) {
                                    		fragment2 = mapping.getFragment2();
                                    	}
                                    	else if (fragment2.equals(mapping.getFragment2())) {
                                    		fragment2Matches++;
                                    	}
	                                }
	                            }
	                            else if (mapping instanceof LeafMapping && mapping.getFragment1() instanceof StatementObject && mapping.getFragment2() instanceof StatementObject) {
	                                Block matchedBlockInsideMergedMethodBody = Block.of((StatementObject) mapping.getFragment2(), bodyMapper.getContainer2(), currentVersion);
	                                if (matchedBlockInsideMergedMethodBody.equalIdentifierIgnoringVersion(rightBlock)) {
                                    	Block blockBefore = Block.of((StatementObject) mapping.getFragment1(), bodyMapper.getContainer1(), parentVersion);
                                    	mappings.add(Pair.of(blockBefore, matchedBlockInsideMergedMethodBody));
                                    	if (fragment2 == null) {
                                    		fragment2 = mapping.getFragment2();
                                    	}
                                    	else if (fragment2.equals(mapping.getFragment2())) {
                                    		fragment2Matches++;
                                    	}
	                                }
	                            }
	                        }
	                    }
	                }
	                break;
	            }
            }
    	}
    	if (mappings.size() > 1 && mappings.size() == fragment2Matches + 1) {
    		for (Pair<Block, Block> pair : mappings) {
    			blockChangeHistory.handleAdd(pair.getLeft(), pair.getRight(), mergeOperationRefactoring.toString());
                elements.add(pair.getLeft());
    		}
    		blockChangeHistory.connectRelatedNodes();
    		return true;
    	}
    	return false;
    }

    public boolean checkForExtractionOrInline(Version currentVersion, Version parentVersion, Predicate<Method> equalMethod, Block rightBlock, List<Refactoring> refactorings) {
        int extractMatches = 0;
    	for (Refactoring refactoring : refactorings) {
            switch (refactoring.getRefactoringType()) {
                case EXTRACT_AND_MOVE_OPERATION:
                case EXTRACT_OPERATION: {
                    ExtractOperationRefactoring extractOperationRefactoring = (ExtractOperationRefactoring) refactoring;
                    Method extractedMethod = Method.of(extractOperationRefactoring.getExtractedOperation(), currentVersion);
                    if (equalMethod.test(extractedMethod)) {
                        AbstractCodeFragment matchedBlockFromSourceMethod = null;
                        UMLOperationBodyMapper bodyMapper = extractOperationRefactoring.getBodyMapper();
                        for (AbstractCodeMapping mapping : bodyMapper.getMappings()) {
                            if (mapping instanceof CompositeStatementObjectMapping) {
                                Block matchedBlockInsideExtractedMethodBody = Block.of((CompositeStatementObject) mapping.getFragment2(), bodyMapper.getContainer2(), currentVersion);
                                if (matchedBlockInsideExtractedMethodBody.equalIdentifierIgnoringVersion(rightBlock)) {
                                    matchedBlockFromSourceMethod = (CompositeStatementObject) mapping.getFragment1();
                                    Block blockBefore = Block.of((CompositeStatementObject) mapping.getFragment1(), bodyMapper.getContainer1(), parentVersion);
                                    List<String> stringRepresentationBefore = blockBefore.getComposite().stringRepresentation();
                                    List<String> stringRepresentationAfter = matchedBlockInsideExtractedMethodBody.getComposite().stringRepresentation();
                                    if (!stringRepresentationBefore.equals(stringRepresentationAfter)) {
                                        if (!stringRepresentationBefore.get(0).equals(stringRepresentationAfter.get(0))) {
                                            blockChangeHistory.addChange(blockBefore, matchedBlockInsideExtractedMethodBody, ChangeFactory.forBlock(Change.Type.EXPRESSION_CHANGE));
                                        }
                                        List<String> stringRepresentationBodyBefore = stringRepresentationBefore.subList(1, stringRepresentationBefore.size());
                                        List<String> stringRepresentationBodyAfter = stringRepresentationAfter.subList(1, stringRepresentationAfter.size());
                                        if (!stringRepresentationBodyBefore.equals(stringRepresentationBodyAfter)) {
                                            blockChangeHistory.addChange(blockBefore, matchedBlockInsideExtractedMethodBody, ChangeFactory.forBlock(Change.Type.BODY_CHANGE));
                                        }
                                    }
                                    break;
                                }
                            }
                            else if (mapping instanceof LeafMapping && mapping.getFragment1() instanceof StatementObject && mapping.getFragment2() instanceof StatementObject) {
                                Block matchedBlockInsideExtractedMethodBody = Block.of((StatementObject) mapping.getFragment2(), bodyMapper.getContainer2(), currentVersion);
                                if (matchedBlockInsideExtractedMethodBody.equalIdentifierIgnoringVersion(rightBlock)) {
                                    matchedBlockFromSourceMethod = mapping.getFragment1();
                                    Block blockBefore = Block.of((StatementObject) mapping.getFragment1(), bodyMapper.getContainer1(), parentVersion);
                                    List<String> stringRepresentationBefore = blockBefore.getComposite().stringRepresentation();
                                    List<String> stringRepresentationAfter = matchedBlockInsideExtractedMethodBody.getComposite().stringRepresentation();
                                    if (!stringRepresentationBefore.equals(stringRepresentationAfter)) {
                                        //blockChangeHistory.addChange(blockBefore, matchedBlockInsideExtractedMethodBody, ChangeFactory.forBlock(Change.Type.BODY_CHANGE));
                                    	addStatementChange(blockBefore, matchedBlockInsideExtractedMethodBody);
                                    }
                                    break;
                                }
                            }
                        }
                        Block blockBefore = Block.of(rightBlock.getComposite(), rightBlock.getOperation(), parentVersion);
                        if (matchedBlockFromSourceMethod == null) {
                            blockChangeHistory.handleAdd(blockBefore, rightBlock, extractOperationRefactoring.toString());
                            if(extractMatches == 0) {
                            	elements.add(blockBefore);
                            }
                        }
                        else {
                            VariableDeclarationContainer sourceOperation = extractOperationRefactoring.getSourceOperationBeforeExtraction();
                            Method sourceMethod = Method.of(sourceOperation, parentVersion);
                            Block leftBlock = Block.of(matchedBlockFromSourceMethod instanceof StatementObject ? (StatementObject) matchedBlockFromSourceMethod : (CompositeStatementObject) matchedBlockFromSourceMethod, sourceMethod);
                            if(extractMatches == 0) {
                            	elements.add(leftBlock);
                            }
                        }
                        blockChangeHistory.connectRelatedNodes();
                        extractMatches++;
                    }
                    UMLOperationBodyMapper mapper = extractOperationRefactoring.getBodyMapper();
                    Set<UMLAnonymousClassDiff> anonymousClassDiffs = mapper.getAnonymousClassDiffs();
                    for (UMLAnonymousClassDiff diff : anonymousClassDiffs) {
                        for (UMLOperationBodyMapper anonymousMapper : diff.getOperationBodyMapperList()) {
                            Method anonymousExtractedOperationAfter = Method.of(anonymousMapper.getContainer2(), currentVersion);
                            if (equalMethod.test(anonymousExtractedOperationAfter)) {
                                AbstractCodeFragment matchedBlockFromSourceMethod = null;
                                for (AbstractCodeMapping mapping : anonymousMapper.getMappings()) {
                                    if (mapping instanceof CompositeStatementObjectMapping) {
                                        Block matchedBlockInsideExtractedMethodBody = Block.of((CompositeStatementObject) mapping.getFragment2(), anonymousMapper.getContainer2(), currentVersion);
                                        if (matchedBlockInsideExtractedMethodBody.equalIdentifierIgnoringVersion(rightBlock)) {
                                            matchedBlockFromSourceMethod = (CompositeStatementObject) mapping.getFragment1();
                                            Block blockBefore = Block.of((CompositeStatementObject) mapping.getFragment1(), anonymousMapper.getContainer1(), parentVersion);
                                            List<String> stringRepresentationBefore = blockBefore.getComposite().stringRepresentation();
                                            List<String> stringRepresentationAfter = matchedBlockInsideExtractedMethodBody.getComposite().stringRepresentation();
                                            if (!stringRepresentationBefore.equals(stringRepresentationAfter)) {
                                                if (!stringRepresentationBefore.get(0).equals(stringRepresentationAfter.get(0))) {
                                                    blockChangeHistory.addChange(blockBefore, matchedBlockInsideExtractedMethodBody, ChangeFactory.forBlock(Change.Type.EXPRESSION_CHANGE));
                                                }
                                                List<String> stringRepresentationBodyBefore = stringRepresentationBefore.subList(1, stringRepresentationBefore.size());
                                                List<String> stringRepresentationBodyAfter = stringRepresentationAfter.subList(1, stringRepresentationAfter.size());
                                                if (!stringRepresentationBodyBefore.equals(stringRepresentationBodyAfter)) {
                                                    blockChangeHistory.addChange(blockBefore, matchedBlockInsideExtractedMethodBody, ChangeFactory.forBlock(Change.Type.BODY_CHANGE));
                                                }
                                            }
                                            break;
                                        }
                                    }
                                    else if (mapping instanceof LeafMapping && mapping.getFragment1() instanceof StatementObject && mapping.getFragment2() instanceof StatementObject) {
                                        Block matchedBlockInsideExtractedMethodBody = Block.of((StatementObject) mapping.getFragment2(), anonymousMapper.getContainer2(), currentVersion);
                                        if (matchedBlockInsideExtractedMethodBody.equalIdentifierIgnoringVersion(rightBlock)) {
                                            matchedBlockFromSourceMethod = mapping.getFragment1();
                                            Block blockBefore = Block.of((StatementObject) mapping.getFragment1(), anonymousMapper.getContainer1(), parentVersion);
                                            List<String> stringRepresentationBefore = blockBefore.getComposite().stringRepresentation();
                                            List<String> stringRepresentationAfter = matchedBlockInsideExtractedMethodBody.getComposite().stringRepresentation();
                                            if (!stringRepresentationBefore.equals(stringRepresentationAfter)) {
                                                //blockChangeHistory.addChange(blockBefore, matchedBlockInsideExtractedMethodBody, ChangeFactory.forBlock(Change.Type.BODY_CHANGE));
                                            	addStatementChange(blockBefore, matchedBlockInsideExtractedMethodBody);
                                            }
                                            break;
                                        }
                                    }
                                }
                                if (matchedBlockFromSourceMethod == null) {
                                    Block blockBefore = Block.of(rightBlock.getComposite(), rightBlock.getOperation(), parentVersion);
                                    blockChangeHistory.handleAdd(blockBefore, rightBlock, extractOperationRefactoring.toString());
                                    if(extractMatches == 0) {
                                        elements.add(blockBefore);
                                    }
                                }
                                else {
                                    VariableDeclarationContainer sourceOperation = anonymousMapper.getContainer1();
                                    Method sourceMethod = Method.of(sourceOperation, parentVersion);
                                    Block leftBlock = Block.of(matchedBlockFromSourceMethod instanceof StatementObject ? (StatementObject) matchedBlockFromSourceMethod : (CompositeStatementObject) matchedBlockFromSourceMethod, sourceMethod);
                                    if(extractMatches == 0) {
                                        elements.add(leftBlock);
                                    }
                                }
                                blockChangeHistory.connectRelatedNodes();
                                extractMatches++;
                            }
                        }
                    }
                    break;
                }
                case MOVE_CODE: {
                	MoveCodeRefactoring moveCodeRefactoring = (MoveCodeRefactoring) refactoring;
                	Method extractedMethod = Method.of(moveCodeRefactoring.getTargetContainer(), currentVersion);
                    if (equalMethod.test(extractedMethod)) {
                    	AbstractCodeFragment matchedBlockFromSourceMethod = null;
                        UMLOperationBodyMapper bodyMapper = moveCodeRefactoring.getBodyMapper();
                        for (AbstractCodeMapping mapping : bodyMapper.getMappings()) {
                            if (mapping instanceof CompositeStatementObjectMapping) {
                                Block matchedBlockInsideExtractedMethodBody = Block.of((CompositeStatementObject) mapping.getFragment2(), bodyMapper.getContainer2(), currentVersion);
                                if (matchedBlockInsideExtractedMethodBody.equalIdentifierIgnoringVersion(rightBlock)) {
                                    matchedBlockFromSourceMethod = (CompositeStatementObject) mapping.getFragment1();
                                    Block blockBefore = Block.of((CompositeStatementObject) mapping.getFragment1(), bodyMapper.getContainer1(), parentVersion);
                                    List<String> stringRepresentationBefore = blockBefore.getComposite().stringRepresentation();
                                    List<String> stringRepresentationAfter = matchedBlockInsideExtractedMethodBody.getComposite().stringRepresentation();
                                    if (!stringRepresentationBefore.equals(stringRepresentationAfter)) {
                                        if (!stringRepresentationBefore.get(0).equals(stringRepresentationAfter.get(0))) {
                                            blockChangeHistory.addChange(blockBefore, matchedBlockInsideExtractedMethodBody, ChangeFactory.forBlock(Change.Type.EXPRESSION_CHANGE));
                                        }
                                        List<String> stringRepresentationBodyBefore = stringRepresentationBefore.subList(1, stringRepresentationBefore.size());
                                        List<String> stringRepresentationBodyAfter = stringRepresentationAfter.subList(1, stringRepresentationAfter.size());
                                        if (!stringRepresentationBodyBefore.equals(stringRepresentationBodyAfter)) {
                                            blockChangeHistory.addChange(blockBefore, matchedBlockInsideExtractedMethodBody, ChangeFactory.forBlock(Change.Type.BODY_CHANGE));
                                        }
                                    }
                                    break;
                                }
                            }
                            else if (mapping instanceof LeafMapping && mapping.getFragment1() instanceof StatementObject && mapping.getFragment2() instanceof StatementObject) {
                                Block matchedBlockInsideExtractedMethodBody = Block.of((StatementObject) mapping.getFragment2(), bodyMapper.getContainer2(), currentVersion);
                                if (matchedBlockInsideExtractedMethodBody.equalIdentifierIgnoringVersion(rightBlock)) {
                                    matchedBlockFromSourceMethod = mapping.getFragment1();
                                    Block blockBefore = Block.of((StatementObject) mapping.getFragment1(), bodyMapper.getContainer1(), parentVersion);
                                    List<String> stringRepresentationBefore = blockBefore.getComposite().stringRepresentation();
                                    List<String> stringRepresentationAfter = matchedBlockInsideExtractedMethodBody.getComposite().stringRepresentation();
                                    if (!stringRepresentationBefore.equals(stringRepresentationAfter)) {
                                        //blockChangeHistory.addChange(blockBefore, matchedBlockInsideExtractedMethodBody, ChangeFactory.forBlock(Change.Type.BODY_CHANGE));
                                    	addStatementChange(blockBefore, matchedBlockInsideExtractedMethodBody);
                                    }
                                    break;
                                }
                            }
                        }
                        if (matchedBlockFromSourceMethod != null) {
                            VariableDeclarationContainer sourceOperation = moveCodeRefactoring.getSourceContainer();
                            Method sourceMethod = Method.of(sourceOperation, parentVersion);
                            Block leftBlock = Block.of(matchedBlockFromSourceMethod instanceof StatementObject ? (StatementObject) matchedBlockFromSourceMethod : (CompositeStatementObject) matchedBlockFromSourceMethod, sourceMethod);
                            if(extractMatches == 0) {
                            	elements.add(leftBlock);
                            }
                            blockChangeHistory.connectRelatedNodes();
                            extractMatches++;
                        }
                    }
                	break;
                }
                case MOVE_AND_INLINE_OPERATION:
                case INLINE_OPERATION: {
                    InlineOperationRefactoring inlineOperationRefactoring = (InlineOperationRefactoring) refactoring;
                    Method targetOperationAfterInline = Method.of(inlineOperationRefactoring.getTargetOperationAfterInline(), currentVersion);
                    if (equalMethod.test(targetOperationAfterInline)) {
                        UMLOperationBodyMapper bodyMapper = inlineOperationRefactoring.getBodyMapper();
                        for (AbstractCodeMapping mapping : bodyMapper.getMappings()) {
                            if (mapping instanceof CompositeStatementObjectMapping) {
                                Block matchedBlockInsideInlinedMethodBody = Block.of((CompositeStatementObject) mapping.getFragment2(), bodyMapper.getContainer2(), currentVersion);
                                if (matchedBlockInsideInlinedMethodBody.equalIdentifierIgnoringVersion(rightBlock)) {
                                    Block blockBefore = Block.of((CompositeStatementObject) mapping.getFragment1(), bodyMapper.getContainer1(), parentVersion);
                                    blockChangeHistory.handleAdd(blockBefore, matchedBlockInsideInlinedMethodBody, inlineOperationRefactoring.toString());
                                    elements.add(blockBefore);
                                    blockChangeHistory.connectRelatedNodes();
                                    return true;
                                }
                            }
                            else if (mapping instanceof LeafMapping && mapping.getFragment1() instanceof StatementObject && mapping.getFragment2() instanceof StatementObject) {
                                Block matchedBlockInsideInlinedMethodBody = Block.of((StatementObject) mapping.getFragment2(), bodyMapper.getContainer2(), currentVersion);
                                if (matchedBlockInsideInlinedMethodBody.equalIdentifierIgnoringVersion(rightBlock)) {
                                    Block blockBefore = Block.of((StatementObject) mapping.getFragment1(), bodyMapper.getContainer1(), parentVersion);
                                    blockChangeHistory.handleAdd(blockBefore, matchedBlockInsideInlinedMethodBody, inlineOperationRefactoring.toString());
                                    elements.add(blockBefore);
                                    blockChangeHistory.connectRelatedNodes();
                                    return true;
                                }
                            }
                        }
                    }
                    break;
                }
                case MERGE_OPERATION: {
                    MergeOperationRefactoring mergeOperationRefactoring = (MergeOperationRefactoring) refactoring;
                    Method methodAfter = Method.of(mergeOperationRefactoring.getNewMethodAfterMerge(), currentVersion);
                    if (equalMethod.test(methodAfter)) {
                    	int mergeMatches = 0;
                        for (UMLOperationBodyMapper bodyMapper : mergeOperationRefactoring.getMappers()) {
                            for (AbstractCodeMapping mapping : bodyMapper.getMappings()) {
                                if (mapping instanceof CompositeStatementObjectMapping) {
                                    Block matchedBlockInsideMergedMethodBody = Block.of((CompositeStatementObject) mapping.getFragment2(), bodyMapper.getContainer2(), currentVersion);
                                    if (matchedBlockInsideMergedMethodBody.equalIdentifierIgnoringVersion(rightBlock)) {
                                        // implementation for introduced
                                        /*
                                        Block blockBefore = Block.of((CompositeStatementObject) mapping.getFragment1(), bodyMapper.getContainer1(), parentVersion);
                                        blockChangeHistory.handleAdd(blockBefore, matchedBlockInsideMergedMethodBody, mergeOperationRefactoring.toString());
                                        blocks.add(blockBefore);
                                        blockChangeHistory.connectRelatedNodes();
                                        return true;
                                        */
                                        Set<Refactoring> mapperRefactorings = bodyMapper.getRefactoringsAfterPostProcessing();
                                        //Check if refactored
                                        if (isBlockRefactored(mapperRefactorings, currentVersion, parentVersion, rightBlock::equalIdentifierIgnoringVersion))
                                        	mergeMatches++;
                                        // check if it is in the matched
                                        if (isMatched(bodyMapper, currentVersion, parentVersion, rightBlock::equalIdentifierIgnoringVersion))
                                        	mergeMatches++;
                                    }
                                }
                                else if (mapping instanceof LeafMapping && mapping.getFragment1() instanceof StatementObject && mapping.getFragment2() instanceof StatementObject) {
                                    Block matchedBlockInsideMergedMethodBody = Block.of((StatementObject) mapping.getFragment2(), bodyMapper.getContainer2(), currentVersion);
                                    if (matchedBlockInsideMergedMethodBody.equalIdentifierIgnoringVersion(rightBlock)) {
                                        // implementation for introduced
                                        /*
                                        Block blockBefore = Block.of((StatementObject) mapping.getFragment1(), bodyMapper.getContainer1(), parentVersion);
                                        blockChangeHistory.handleAdd(blockBefore, matchedBlockInsideMergedMethodBody, mergeOperationRefactoring.toString());
                                        blocks.add(blockBefore);
                                        blockChangeHistory.connectRelatedNodes();
                                        return true;
                                        */
                                        Set<Refactoring> mapperRefactorings = bodyMapper.getRefactoringsAfterPostProcessing();
                                        //Check if refactored
                                        if (isBlockRefactored(mapperRefactorings, currentVersion, parentVersion, rightBlock::equalIdentifierIgnoringVersion))
                                        	mergeMatches++;
                                        // check if it is in the matched
                                        if (isMatched(bodyMapper, currentVersion, parentVersion, rightBlock::equalIdentifierIgnoringVersion))
                                        	mergeMatches++;
                                    }
                                }
                            }
                        }
                        if(mergeMatches > 0) {
                        	return true;
                        }
                    }
                    break;
                }
                case SPLIT_OPERATION: {
                    SplitOperationRefactoring splitOperationRefactoring = (SplitOperationRefactoring) refactoring;
                    for (VariableDeclarationContainer splitMethod : splitOperationRefactoring.getSplitMethods()) {
                        Method methodAfter = Method.of(splitMethod, currentVersion);
                        if (equalMethod.test(methodAfter)) {
                            for (UMLOperationBodyMapper bodyMapper : splitOperationRefactoring.getMappers()) {
                                for (AbstractCodeMapping mapping : bodyMapper.getMappings()) {
                                    if (mapping instanceof CompositeStatementObjectMapping) {
                                        Block matchedBlockInsideSplitMethodBody = Block.of((CompositeStatementObject) mapping.getFragment2(), bodyMapper.getContainer2(), currentVersion);
                                        if (matchedBlockInsideSplitMethodBody.equalIdentifierIgnoringVersion(rightBlock)) {
                                        // implementation for introduced
                                        /*
                                        Block blockBefore = Block.of((CompositeStatementObject) mapping.getFragment1(), bodyMapper.getContainer1(), parentVersion);
                                        blockChangeHistory.handleAdd(blockBefore, matchedBlockInsideMergedMethodBody, mergeOperationRefactoring.toString());
                                        blocks.add(blockBefore);
                                        blockChangeHistory.connectRelatedNodes();
                                        return true;
                                        */
                                        Set<Refactoring> mapperRefactorings = bodyMapper.getRefactoringsAfterPostProcessing();
                                        //Check if refactored
                                        if (isBlockRefactored(mapperRefactorings, currentVersion, parentVersion, rightBlock::equalIdentifierIgnoringVersion))
                                            return true;
                                        // check if it is in the matched
                                        if (isMatched(bodyMapper, currentVersion, parentVersion, rightBlock::equalIdentifierIgnoringVersion))
                                            return true;
                                        }
                                    }
                                    else if (mapping instanceof LeafMapping && mapping.getFragment1() instanceof StatementObject && mapping.getFragment2() instanceof StatementObject) {
                                        Block matchedBlockInsideSplitMethodBody = Block.of((StatementObject) mapping.getFragment2(), bodyMapper.getContainer2(), currentVersion);
                                        if (matchedBlockInsideSplitMethodBody.equalIdentifierIgnoringVersion(rightBlock)) {
                                        // implementation for introduced
                                        /*
                                        Block blockBefore = Block.of((StatementObject) mapping.getFragment1(), bodyMapper.getContainer1(), parentVersion);
                                        blockChangeHistory.handleAdd(blockBefore, matchedBlockInsideMergedMethodBody, mergeOperationRefactoring.toString());
                                        blocks.add(blockBefore);
                                        blockChangeHistory.connectRelatedNodes();
                                        return true;
                                        */
                                        Set<Refactoring> mapperRefactorings = bodyMapper.getRefactoringsAfterPostProcessing();
                                        //Check if refactored
                                        if (isBlockRefactored(mapperRefactorings, currentVersion, parentVersion, rightBlock::equalIdentifierIgnoringVersion))
                                            return true;
                                        // check if it is in the matched
                                        if (isMatched(bodyMapper, currentVersion, parentVersion, rightBlock::equalIdentifierIgnoringVersion))
                                            return true;
                                        }
                                    }
                                }
                            }
                        }
                    }
                    break;
                }
            }
        }
    	if(extractMatches > 0) {
    		return true;
    	}
        return false;
    }

    public boolean checkBodyOfMatchedOperations(Version currentVersion, Version parentVersion, Predicate<Block> equalOperator, UMLOperationBodyMapper umlOperationBodyMapper) {
        if (umlOperationBodyMapper == null)
            return false;
        Set<Refactoring> refactorings = umlOperationBodyMapper.getRefactoringsAfterPostProcessing();
        //Check if refactored
        if (isBlockRefactored(refactorings, currentVersion, parentVersion, equalOperator))
            return true;
        // check if it is in the matched
        if (isMatched(umlOperationBodyMapper, currentVersion, parentVersion, equalOperator))
            return true;
        //Check if is added
        return isAdded(umlOperationBodyMapper, currentVersion, parentVersion, equalOperator);
    }

    public boolean isBlockRefactored(Collection<Refactoring> refactorings, Version currentVersion, Version parentVersion, Predicate<Block> equalOperator) {
        Set<Block> leftBlockSet = analyseBlockRefactorings(refactorings, currentVersion, parentVersion, equalOperator);
        for (Block leftBlock : leftBlockSet) {
            elements.add(leftBlock);
            blockChangeHistory.connectRelatedNodes();
            return true;
        }
        return false;
    }

    private Set<Block> analyseBlockRefactorings(Collection<Refactoring> refactorings, Version currentVersion, Version parentVersion, Predicate<Block> equalOperator) {
        Set<Block> leftBlockSet = new HashSet<>();
        for (Refactoring refactoring : refactorings) {
            Block blockBefore = null;
            Block blockAfter = null;
            Change.Type changeType = null;
            switch (refactoring.getRefactoringType()) {
                case REPLACE_LOOP_WITH_PIPELINE: {
                    ReplaceLoopWithPipelineRefactoring loopWithPipelineRefactoring = (ReplaceLoopWithPipelineRefactoring) refactoring;
                    for (AbstractCodeFragment fragment : loopWithPipelineRefactoring.getCodeFragmentsAfter()) {
                        if (fragment instanceof StatementObject) {
                            StatementObject statement = (StatementObject) fragment;
                            Block addedBlockAfter = Block.of(statement, loopWithPipelineRefactoring.getOperationAfter(), currentVersion);
                            if (equalOperator.test(addedBlockAfter)) {
                                Set<AbstractCodeFragment> fragmentsBefore = loopWithPipelineRefactoring.getCodeFragmentsBefore();
                                for (AbstractCodeFragment fragmentBefore : fragmentsBefore) {
                                    if (fragmentBefore instanceof CompositeStatementObject) {
                                        if (fragmentBefore.getLocationInfo().getCodeElementType().equals(CodeElementType.ENHANCED_FOR_STATEMENT) ||
                                                fragmentBefore.getLocationInfo().getCodeElementType().equals(CodeElementType.FOR_STATEMENT) ||
                                                fragmentBefore.getLocationInfo().getCodeElementType().equals(CodeElementType.WHILE_STATEMENT) ||
                                                fragmentBefore.getLocationInfo().getCodeElementType().equals(CodeElementType.DO_STATEMENT)) {
                                            blockBefore = Block.of((CompositeStatementObject) fragmentBefore, loopWithPipelineRefactoring.getOperationBefore(), parentVersion);
                                            blockAfter = addedBlockAfter;
                                            changeType = Change.Type.REPLACE_LOOP_WITH_PIPELINE;
                                        }
                                    }
                                }
                            }
                        }
                    }
                    break;
                }
                case REPLACE_PIPELINE_WITH_LOOP: {
                    ReplacePipelineWithLoopRefactoring pipelineWithLoopRefactoring = (ReplacePipelineWithLoopRefactoring) refactoring;
                    for (AbstractCodeFragment fragment : pipelineWithLoopRefactoring.getCodeFragmentsAfter()) {
                        if (fragment instanceof CompositeStatementObject) {
                            CompositeStatementObject composite = (CompositeStatementObject) fragment;
                            Block addedBlockAfter = Block.of(composite, pipelineWithLoopRefactoring.getOperationAfter(), currentVersion);
                            if (equalOperator.test(addedBlockAfter)) {
                                // implementation for introduced
                                /*
                                Block addedBlockBefore = Block.of(composite, pipelineWithLoopRefactoring.getOperationAfter(), parentVersion);
                                addedBlockBefore.setAdded(true);
                                ChangeFactory changeFactory = ChangeFactory.forBlock(Change.Type.INTRODUCED)
                                        .comment(pipelineWithLoopRefactoring.toString()).refactoring(pipelineWithLoopRefactoring).codeElement(addedBlockAfter);
                                blockChangeHistory.addChange(addedBlockBefore, addedBlockAfter, changeFactory);
                                leftBlockSet.add(addedBlockBefore);
                                blockChangeHistory.connectRelatedNodes();
                                return leftBlockSet;
                                 */
                                Set<AbstractCodeFragment> fragmentsBefore = pipelineWithLoopRefactoring.getCodeFragmentsBefore();
                                if (fragmentsBefore.size() == 1 && fragmentsBefore.iterator().next() instanceof StatementObject) {
                                    StatementObject streamStatement = (StatementObject) fragmentsBefore.iterator().next();
                                    blockBefore = Block.of(streamStatement, pipelineWithLoopRefactoring.getOperationBefore(), parentVersion);
                                    blockAfter = addedBlockAfter;
                                    changeType = Change.Type.REPLACE_PIPELINE_WITH_LOOP;
                                }
                            }
                            else {
                                //check if a nested composite statement matches
                                List<CompositeStatementObject> innerNodes = composite.getInnerNodes();
                                for (CompositeStatementObject innerNode : innerNodes) {
                                    addedBlockAfter = Block.of(innerNode, pipelineWithLoopRefactoring.getOperationAfter(), currentVersion);
                                    if (equalOperator.test(addedBlockAfter)) {
                                        Block addedBlockBefore = Block.of(innerNode, pipelineWithLoopRefactoring.getOperationAfter(), parentVersion);
                                        addedBlockBefore.setAdded(true);
                                        ChangeFactory changeFactory = ChangeFactory.forBlock(Change.Type.INTRODUCED)
                                                .comment(pipelineWithLoopRefactoring.toString()).refactoring(pipelineWithLoopRefactoring).codeElement(addedBlockAfter);
                                        blockChangeHistory.addChange(addedBlockBefore, addedBlockAfter, changeFactory);
                                        leftBlockSet.add(addedBlockBefore);
                                        blockChangeHistory.connectRelatedNodes();
                                        return leftBlockSet;
                                    }
                                }
                            }
                        }
                    }
                    break;
                }
                case SPLIT_CONDITIONAL: {
                    SplitConditionalRefactoring splitConditionalRefactoring = (SplitConditionalRefactoring) refactoring;
                    for (AbstractCodeFragment splitConditional : splitConditionalRefactoring.getSplitConditionals()) {
                        if (splitConditional instanceof CompositeStatementObject) {
                            Block addedBlockAfter = Block.of((CompositeStatementObject) splitConditional, splitConditionalRefactoring.getOperationAfter(), currentVersion);
                            if (equalOperator.test(addedBlockAfter)) {
                                // implementation with evolution hook
                                /*
                                Block addedBlockBefore = Block.of((CompositeStatementObject) splitConditional, splitConditionalRefactoring.getOperationAfter(), parentVersion);
                                addedBlockBefore.setAdded(true);
                                ChangeFactory changeFactory = ChangeFactory.forBlock(Change.Type.BLOCK_SPLIT)
                                        .comment(splitConditionalRefactoring.toString()).refactoring(splitConditionalRefactoring).codeElement(addedBlockAfter);
                                if (splitConditionalRefactoring.getOriginalConditional() instanceof CompositeStatementObject) {
                                    blockBefore = Block.of((CompositeStatementObject) splitConditionalRefactoring.getOriginalConditional(), splitConditionalRefactoring.getOperationBefore(), parentVersion);
                                    changeFactory.hookedElement(blockBefore);
                                }
                                blockChangeHistory.addChange(addedBlockBefore, addedBlockAfter, changeFactory);
                                leftBlockSet.add(addedBlockBefore);
                                blockChangeHistory.connectRelatedNodes();
                                return leftBlockSet;
                                 */
                                // implementation without evolution hook
                                if (splitConditionalRefactoring.getOriginalConditional() instanceof CompositeStatementObject) {
                                    blockBefore = Block.of((CompositeStatementObject) splitConditionalRefactoring.getOriginalConditional(), splitConditionalRefactoring.getOperationBefore(), parentVersion);
                                }
                                blockAfter = addedBlockAfter;
                                changeType = Change.Type.BLOCK_SPLIT;
                            }
                        }
                    }
                    break;
                }
                case REPLACE_CONDITIONAL_WITH_TERNARY: {
                	ReplaceConditionalWithTernaryRefactoring replaceWithTernaryRefactoring = (ReplaceConditionalWithTernaryRefactoring) refactoring;
                	AbstractCodeFragment ternaryConditional = replaceWithTernaryRefactoring.getTernaryConditional();
                	if (ternaryConditional instanceof StatementObject) {
                		Block addedBlockAfter = Block.of((StatementObject) ternaryConditional, replaceWithTernaryRefactoring.getOperationAfter(), currentVersion);
                        if (equalOperator.test(addedBlockAfter)) {
                        	if (replaceWithTernaryRefactoring.getOriginalConditional() instanceof CompositeStatementObject) {
                        		blockBefore = Block.of((CompositeStatementObject) replaceWithTernaryRefactoring.getOriginalConditional(), replaceWithTernaryRefactoring.getOperationBefore(), parentVersion);
                        	}
                        	blockAfter = addedBlockAfter;
                            changeType = Change.Type.REPLACE_CONDITIONAL_WITH_TERNARY;
                        }
                	}
                	break;
                }
                /*case MERGE_CONDITIONAL: {
                    MergeConditionalRefactoring mergeConditionalRefactoring = (MergeConditionalRefactoring) refactoring;
                    if (mergeConditionalRefactoring.getNewConditional() instanceof CompositeStatementObject) {
                        Block addedBlockAfter = Block.of((CompositeStatementObject) mergeConditionalRefactoring.getNewConditional(), mergeConditionalRefactoring.getOperationAfter(), currentVersion);
                        blockAfter = addedBlockAfter;
                        changeType = Change.Type.BLOCK_MERGE;
                        if (equalOperator.test(addedBlockAfter)) {
                            for (AbstractCodeFragment mergedConditional : mergeConditionalRefactoring.getMergedConditionals()) {
                            	if (mergedConditional instanceof CompositeStatementObject) {
                            		blockBefore = Block.of((CompositeStatementObject) mergedConditional, mergeConditionalRefactoring.getOperationBefore(), parentVersion);
                            		blockChangeHistory.addChange(blockBefore, blockAfter, ChangeFactory.forBlock(changeType).refactoring(refactoring));
                                    leftBlockSet.add(blockBefore);
                            	}
                            }
                            blockChangeHistory.connectRelatedNodes();
                            return leftBlockSet;
                        }
                    }
                    break;
                }
                case MERGE_CATCH: {
                    MergeCatchRefactoring mergeCatchRefactoring = (MergeCatchRefactoring) refactoring;
                    if (mergeCatchRefactoring.getNewCatchBlock() instanceof CompositeStatementObject) {
                        Block addedBlockAfter = Block.of((CompositeStatementObject) mergeCatchRefactoring.getNewCatchBlock(), mergeCatchRefactoring.getOperationAfter(), currentVersion);
                        blockAfter = addedBlockAfter;
                        changeType = Change.Type.BLOCK_MERGE;
                        if (equalOperator.test(addedBlockAfter)) {
                            for (AbstractCodeFragment mergedCatchBlock : mergeCatchRefactoring.getMergedCatchBlocks()) {
                            	if (mergedCatchBlock instanceof CompositeStatementObject) {
                            		blockBefore = Block.of((CompositeStatementObject) mergedCatchBlock, mergeCatchRefactoring.getOperationBefore(), parentVersion);
                            		blockChangeHistory.addChange(blockBefore, blockAfter, ChangeFactory.forBlock(changeType).refactoring(refactoring));
                                    leftBlockSet.add(blockBefore);
                            	}
                            }
                            blockChangeHistory.connectRelatedNodes();
                            return leftBlockSet;
                        }
                    }
                    break;
                }*/
            }
            if (changeType != null && blockBefore != null) {
                if (equalOperator.test(blockAfter)) {
                    blockChangeHistory.addChange(blockBefore, blockAfter, ChangeFactory.forBlock(changeType).refactoring(refactoring));
                    leftBlockSet.add(blockBefore);
                }
            }
        }
        blockChangeHistory.connectRelatedNodes();
        return leftBlockSet;
    }

    public boolean isMatched(UMLOperationBodyMapper umlOperationBodyMapper, Version currentVersion, Version parentVersion, Predicate<Block> equalOperator) {
    	int matches = 0;
    	for (AbstractCodeMapping mapping : umlOperationBodyMapper.getMappings()) {
            if (mapping instanceof CompositeStatementObjectMapping) {
                Block blockAfter = Block.of((CompositeStatementObject) mapping.getFragment2(), umlOperationBodyMapper.getContainer2(), currentVersion);
                if (equalOperator.test(blockAfter)) {
                    boolean bodyChange = false;
                    boolean catchOrFinallyChange = false;
                    boolean elseChange = false;
                    Block blockBefore = Block.of((CompositeStatementObject) mapping.getFragment1(), umlOperationBodyMapper.getContainer1(), parentVersion);
                    List<String> stringRepresentationBefore = blockBefore.getComposite().stringRepresentation();
                    List<String> stringRepresentationAfter = blockAfter.getComposite().stringRepresentation();
                    if (!stringRepresentationBefore.equals(stringRepresentationAfter)) {
                        if (!stringRepresentationBefore.get(0).equals(stringRepresentationAfter.get(0))) {
                            blockChangeHistory.addChange(blockBefore, blockAfter, ChangeFactory.forBlock(Change.Type.EXPRESSION_CHANGE));
                        }
                        List<String> stringRepresentationBodyBefore = stringRepresentationBefore.subList(1, stringRepresentationBefore.size());
                        List<String> stringRepresentationBodyAfter = stringRepresentationAfter.subList(1, stringRepresentationAfter.size());
                        if (!stringRepresentationBodyBefore.equals(stringRepresentationBodyAfter)) {
                            blockChangeHistory.addChange(blockBefore, blockAfter, ChangeFactory.forBlock(Change.Type.BODY_CHANGE));
                        }
                        bodyChange = true;
                    }
                    if (blockBefore.getComposite() instanceof TryStatementObject && blockAfter.getComposite() instanceof TryStatementObject) {
                        TryStatementObject tryBefore = (TryStatementObject) blockBefore.getComposite();
                        TryStatementObject tryAfter = (TryStatementObject) blockAfter.getComposite();
                        List<CompositeStatementObject> catchBlocksBefore = new ArrayList<>(tryBefore.getCatchClauses());
                        List<CompositeStatementObject> catchBlocksAfter = new ArrayList<>(tryAfter.getCatchClauses());
                        for (AbstractCodeMapping m : umlOperationBodyMapper.getMappings()) {
                            if (m instanceof CompositeStatementObjectMapping) {
                                CompositeStatementObject fragment1 = (CompositeStatementObject) m.getFragment1();
                                CompositeStatementObject fragment2 = (CompositeStatementObject) m.getFragment2();
                                if (m.getFragment1().getLocationInfo().getCodeElementType().equals(CodeElementType.CATCH_CLAUSE) &&
                                        m.getFragment2().getLocationInfo().getCodeElementType().equals(CodeElementType.CATCH_CLAUSE) &&
                                        tryBefore.getCatchClauses().contains(fragment1) &&
                                        tryAfter.getCatchClauses().contains(fragment2)) {
                                    List<String> catchStringRepresentationBefore = fragment1.stringRepresentation();
                                    List<String> catchStringRepresentationAfter = fragment2.stringRepresentation();
                                    catchBlocksBefore.remove(fragment1);
                                    catchBlocksAfter.remove(fragment2);
                                    if (!catchStringRepresentationBefore.equals(catchStringRepresentationAfter)) {
                                        blockChangeHistory.addChange(blockBefore, blockAfter, ChangeFactory.forBlock(Change.Type.CATCH_BLOCK_CHANGE));
                                        catchOrFinallyChange = true;
                                    }
                                }
                            }
                        }
                        Set<CompositeStatementObject> catchBlocksBeforeToRemove = new LinkedHashSet<>();
                        Set<CompositeStatementObject> catchBlocksAfterToRemove = new LinkedHashSet<>();
                        for (int i=0; i<Math.min(catchBlocksBefore.size(), catchBlocksAfter.size()); i++) {
                            List<UMLType> typesBefore = new ArrayList<>();
                            for (VariableDeclaration variableDeclaration : catchBlocksBefore.get(i).getVariableDeclarations()) {
                                typesBefore.add(variableDeclaration.getType());
                            }
                            List<UMLType> typesAfter = new ArrayList<>();
                            for (VariableDeclaration variableDeclaration : catchBlocksAfter.get(i).getVariableDeclarations()) {
                                typesAfter.add(variableDeclaration.getType());
                            }
                            if (typesBefore.equals(typesAfter)) {
                                List<String> catchStringRepresentationBefore = catchBlocksBefore.get(i).stringRepresentation();
                                List<String> catchStringRepresentationAfter = catchBlocksAfter.get(i).stringRepresentation();
                                catchBlocksBeforeToRemove.add(catchBlocksBefore.get(i));
                                catchBlocksAfterToRemove.add(catchBlocksAfter.get(i));
                                if (!catchStringRepresentationBefore.equals(catchStringRepresentationAfter)) {
                                    blockChangeHistory.addChange(blockBefore, blockAfter, ChangeFactory.forBlock(Change.Type.CATCH_BLOCK_CHANGE));
                                    catchOrFinallyChange = true;
                                }
                            }
                        }
                        catchBlocksBefore.removeAll(catchBlocksBeforeToRemove);
                        catchBlocksAfter.removeAll(catchBlocksAfterToRemove);
                        for (CompositeStatementObject catchBlockBefore : catchBlocksBefore) {
                            blockChangeHistory.addChange(blockBefore, blockAfter, ChangeFactory.forBlock(Change.Type.CATCH_BLOCK_REMOVED));
                            catchOrFinallyChange = true;
                        }
                        for (CompositeStatementObject catchBlockAfter : catchBlocksAfter) {
                            blockChangeHistory.addChange(blockBefore, blockAfter, ChangeFactory.forBlock(Change.Type.CATCH_BLOCK_ADDED));
                            catchOrFinallyChange = true;
                        }
                        if (tryBefore.getFinallyClause() != null && tryAfter.getFinallyClause() != null) {
                            List<String> finallyStringRepresentationBefore = tryBefore.getFinallyClause().stringRepresentation();
                            List<String> finallyStringRepresentationAfter = tryAfter.getFinallyClause().stringRepresentation();
                            if (!finallyStringRepresentationBefore.equals(finallyStringRepresentationAfter)) {
                                blockChangeHistory.addChange(blockBefore, blockAfter, ChangeFactory.forBlock(Change.Type.FINALLY_BLOCK_CHANGE));
                                catchOrFinallyChange = true;
                            }
                        }
                        else if (tryBefore.getFinallyClause() == null && tryAfter.getFinallyClause() != null) {
                            blockChangeHistory.addChange(blockBefore, blockAfter, ChangeFactory.forBlock(Change.Type.FINALLY_BLOCK_ADDED));
                            catchOrFinallyChange = true;
                        }
                        else if (tryBefore.getFinallyClause() != null && tryAfter.getFinallyClause() == null) {
                            blockChangeHistory.addChange(blockBefore, blockAfter, ChangeFactory.forBlock(Change.Type.FINALLY_BLOCK_REMOVED));
                            catchOrFinallyChange = true;
                        }
                    }
                    if (blockBefore.getComposite().getLocationInfo().getCodeElementType().equals(CodeElementType.IF_STATEMENT) && 
                    		blockAfter.getComposite().getLocationInfo().getCodeElementType().equals(CodeElementType.IF_STATEMENT)) {
                    	CompositeStatementObject ifBefore = (CompositeStatementObject) blockBefore.getComposite();
                    	CompositeStatementObject ifAfter = (CompositeStatementObject) blockAfter.getComposite();
                    	if (ifBefore.getStatements().size() == 1 && ifAfter.getStatements().size() == 2) {
                    		blockChangeHistory.addChange(blockBefore, blockAfter, ChangeFactory.forBlock(Change.Type.ELSE_BLOCK_ADDED));
                    		elseChange = true;
                    	}
                    	else if (ifBefore.getStatements().size() == 2 && ifAfter.getStatements().size() == 1) {
                    		blockChangeHistory.addChange(blockBefore, blockAfter, ChangeFactory.forBlock(Change.Type.ELSE_BLOCK_REMOVED));
                    		elseChange = true;
                    	}
                    }
                    if (!bodyChange && !catchOrFinallyChange && !elseChange) {
                        blockChangeHistory.addChange(blockBefore, blockAfter, ChangeFactory.of(AbstractChange.Type.NO_CHANGE));
                    }
                    if(matches == 0) {
                    	elements.add(blockBefore);
                    }
                    blockChangeHistory.connectRelatedNodes();
                    matches++;
                }
            }
            else if (mapping instanceof LeafMapping && mapping.getFragment1() instanceof StatementObject && mapping.getFragment2() instanceof StatementObject) {
                Block blockAfter = Block.of((StatementObject) mapping.getFragment2(), umlOperationBodyMapper.getContainer2(), currentVersion);
                if (blockAfter != null && equalOperator.test(blockAfter)) {
                    Block blockBefore = Block.of((StatementObject) mapping.getFragment1(), umlOperationBodyMapper.getContainer1(), parentVersion);
                    if (!blockBefore.getComposite().toString().equals(blockAfter.getComposite().toString())) {
                        addStatementChange(blockBefore, blockAfter);
                    }
                    else {
                    	if(blockBefore.differInFormatting(blockAfter)) {
                    		blockChangeHistory.addChange(blockBefore, blockAfter, ChangeFactory.forBlock(Change.Type.SIGNATURE_FORMAT_CHANGE));
                    		processChange(blockBefore, blockAfter);
                    	}
                    	else {
                    		blockChangeHistory.addChange(blockBefore, blockAfter, ChangeFactory.of(AbstractChange.Type.NO_CHANGE));
                    	}
                    }
                    if(matches == 0) {
                    	elements.add(blockBefore);
                    }
                    blockChangeHistory.connectRelatedNodes();
                    matches++;
                }
            }
        }
    	if(matches > 0) {
    		return true;
    	}
        return false;
    }

	private void addStatementChange(Block blockBefore, Block blockAfter) {
		blockChangeHistory.addChange(blockBefore, blockAfter, ChangeFactory.forBlock(Change.Type.BODY_CHANGE));
		processChange(blockBefore, blockAfter);
	}

    public void addedMethod(Method rightMethod, Block rightBlock, Version parentVersion) {
    	Block blockBefore = Block.of(rightBlock.getComposite(), rightMethod.getUmlOperation(), parentVersion);
    	blockChangeHistory.handleAdd(blockBefore, rightBlock, "added with method");
        elements.add(blockBefore);
        blockChangeHistory.connectRelatedNodes();
    }

    private boolean isAdded(UMLOperationBodyMapper umlOperationBodyMapper, Version currentVersion, Version parentVersion, Predicate<Block> equalOperator) {
        for (CompositeStatementObject composite : umlOperationBodyMapper.getNonMappedInnerNodesT2()) {
            Block blockAfter = Block.of(composite, umlOperationBodyMapper.getContainer2(), currentVersion);
            if (equalOperator.test(blockAfter)) {
                Block blockBefore = Block.of(composite, umlOperationBodyMapper.getContainer2(), parentVersion);
                blockChangeHistory.handleAdd(blockBefore, blockAfter, "new block");
                elements.add(blockBefore);
                blockChangeHistory.connectRelatedNodes();
                return true;
            }
        }
        for (AbstractCodeFragment composite : umlOperationBodyMapper.getNonMappedLeavesT2()) {
        	if(composite instanceof StatementObject) {
	            Block blockAfter = Block.of((StatementObject)composite, umlOperationBodyMapper.getContainer2(), currentVersion);
	            if (equalOperator.test(blockAfter)) {
	                Block blockBefore = Block.of((StatementObject)composite, umlOperationBodyMapper.getContainer2(), parentVersion);
	                blockChangeHistory.handleAdd(blockBefore, blockAfter, "new statement");
	                elements.add(blockBefore);
	                blockChangeHistory.connectRelatedNodes();
	                return true;
	            }
        	}
        }
		//process mappings between expressions and statements
		for (AbstractCodeMapping mapping : umlOperationBodyMapper.getMappings()) {
			if (mapping instanceof LeafMapping && mapping.getFragment1() instanceof AbstractExpression && mapping.getFragment2() instanceof StatementObject) {
                Block blockAfter = Block.of((StatementObject) mapping.getFragment2(), umlOperationBodyMapper.getContainer2(), currentVersion);
                if (blockAfter != null && equalOperator.test(blockAfter)) {
                	Block blockBefore = Block.of((StatementObject)mapping.getFragment2(), umlOperationBodyMapper.getContainer2(), parentVersion);
	                blockChangeHistory.handleAdd(blockBefore, blockAfter, "new statement");
	                elements.add(blockBefore);
	                blockChangeHistory.connectRelatedNodes();
	                return true;
                }
			}
		}
        return false;
    }

    public boolean checkRefactoredMethod(Version currentVersion, Version parentVersion, Predicate<Method> equalMethod, Block rightBlock, List<Refactoring> refactorings) {
        for (Refactoring refactoring : refactorings) {
            UMLOperation operationBefore = null;
            UMLOperation operationAfter = null;
            UMLOperationBodyMapper umlOperationBodyMapper = null;
            switch (refactoring.getRefactoringType()) {
                case PULL_UP_OPERATION: {
                    PullUpOperationRefactoring pullUpOperationRefactoring = (PullUpOperationRefactoring) refactoring;
                    operationBefore = pullUpOperationRefactoring.getOriginalOperation();
                    operationAfter = pullUpOperationRefactoring.getMovedOperation();
                    umlOperationBodyMapper = pullUpOperationRefactoring.getBodyMapper();
                    break;
                }
                case PUSH_DOWN_OPERATION: {
                    PushDownOperationRefactoring pushDownOperationRefactoring = (PushDownOperationRefactoring) refactoring;
                    operationBefore = pushDownOperationRefactoring.getOriginalOperation();
                    operationAfter = pushDownOperationRefactoring.getMovedOperation();
                    umlOperationBodyMapper = pushDownOperationRefactoring.getBodyMapper();
                    break;
                }
                case MOVE_AND_RENAME_OPERATION:
                case MOVE_OPERATION: {
                    MoveOperationRefactoring moveOperationRefactoring = (MoveOperationRefactoring) refactoring;
                    operationBefore = moveOperationRefactoring.getOriginalOperation();
                    operationAfter = moveOperationRefactoring.getMovedOperation();
                    umlOperationBodyMapper = moveOperationRefactoring.getBodyMapper();
                    break;
                }
                case RENAME_METHOD: {
                    RenameOperationRefactoring renameOperationRefactoring = (RenameOperationRefactoring) refactoring;
                    operationBefore = renameOperationRefactoring.getOriginalOperation();
                    operationAfter = renameOperationRefactoring.getRenamedOperation();
                    umlOperationBodyMapper = renameOperationRefactoring.getBodyMapper();
                    break;
                }
            }
            if (operationAfter != null) {
                Method methodAfter = Method.of(operationAfter, currentVersion);
                if (equalMethod.test(methodAfter)) {
                    boolean found = checkBodyOfMatchedOperations(currentVersion, parentVersion, rightBlock::equalIdentifierIgnoringVersion, umlOperationBodyMapper);
                    if (found)
                        return true;
                }
            }
        }
        return false;
    }

    private Map<Pair<Block, Block>, List<Integer>> lineChangeMap = new LinkedHashMap<>();

	public void processChange(Block blockBefore, Block blockAfter) {
		if (blockBefore.isMultiLine() || blockAfter.isMultiLine()) {
			try {
				Pair<Block, Block> pair = Pair.of(blockBefore, blockAfter);
				Block startBlock = getStart();
				if (startBlock != null && startBlock.isMultiLine()) {
					List<String> start = IOUtils.readLines(new StringReader(((StatementObject)startBlock.getComposite()).getActualSignature()));
					List<String> startNoWhitespace = new ArrayList<String>();
					for(String s : start) {
						startNoWhitespace.add(s.replaceAll("\\s+", ""));
					}
					List<String> original = IOUtils.readLines(new StringReader(((StatementObject)blockBefore.getComposite()).getActualSignature()));
					List<String> revised = IOUtils.readLines(new StringReader(((StatementObject)blockAfter.getComposite()).getActualSignature()));
		
					Patch<String> patch = DiffUtils.diff(original, revised);
					List<AbstractDelta<String>> deltas = patch.getDeltas();
					for (int i=0; i<deltas.size(); i++) {
						AbstractDelta<String> delta = deltas.get(i);
						if (indentationChange(delta)) {
							continue;
						}
						Chunk<String> target = delta.getTarget();
						List<String> affectedLines = new ArrayList<>(target.getLines());
						boolean subListFound = false;
						if (affectedLines.size() > 1 && !(delta instanceof InsertDelta)) {
							int index = Collections.indexOfSubList(start, affectedLines);
							if (index != -1) {
								subListFound = true;
								for (int j=0; j<affectedLines.size(); j++) {
									int actualLine = startBlock.signatureStartLine() + index + j;
									if (lineChangeMap.containsKey(pair)) {
										lineChangeMap.get(pair).add(actualLine);
									}
									else {
										List<Integer> list = new ArrayList<>();
										list.add(actualLine);
										lineChangeMap.put(pair, list);
									}
								}
							}
						}
						if (!subListFound) {
							for (String line : affectedLines) {
								List<Integer> matchingIndices = findAllMatchingIndices(start, line);
								if (matchingIndices.isEmpty()) {
									matchingIndices = findAllMatchingIndices(startNoWhitespace, line.replaceAll("\\s+", ""));
								}
								for (Integer index : matchingIndices) {
									if (original.size() > index && revised.size() > index && equalOrStripEqual(original, revised, line, index)) {
										continue;
									}
									if(equalOrStripEqual(original, revised, line)) {
										continue;
									}
									int actualLine = startBlock.signatureStartLine() + index;
									if (lineChangeMap.containsKey(pair)) {
										lineChangeMap.get(pair).add(actualLine);
									}
									else {
										List<Integer> list = new ArrayList<>();
										list.add(actualLine);
										lineChangeMap.put(pair, list);
									}
									//break;
								}
								if (matchingIndices.isEmpty() && !line.isBlank()) {
									matchingIndices = findAllMatchingIndicesRelaxed(start, line);
									for (Integer index : matchingIndices) {
										if (original.size() > index && revised.size() > index && equalOrStripEqual(original, revised, line, index)) {
											continue;
										}
										if(equalOrStripEqual(original, revised, line)) {
											continue;
										}
										int actualLine = startBlock.signatureStartLine() + index;
										if (lineChangeMap.containsKey(pair)) {
											lineChangeMap.get(pair).add(actualLine);
										}
										else {
											List<Integer> list = new ArrayList<>();
											list.add(actualLine);
											lineChangeMap.put(pair, list);
										}
										break;
									}
								}
							}
						}
					}
				}
			} catch(IOException e) {
				e.printStackTrace();
			}
		}
	}

	private boolean equalOrStripEqual(List<String> original, List<String> revised, String line) {
		List<Integer> originalMatchingIndices = new ArrayList<>();
		for(int i=0; i<original.size(); i++) {
			String s = original.get(i);
			if(s.strip().equals(line.strip())) {
				originalMatchingIndices.add(i);
			}
		}
		List<Integer> revisedMatchingIndices = new ArrayList<>();
		for(int i=0; i<revised.size(); i++) {
			String s = revised.get(i);
			if(s.strip().equals(line.strip())) {
				revisedMatchingIndices.add(i);
			}
		}
		if(revisedMatchingIndices.equals(originalMatchingIndices) && originalMatchingIndices.size() > 0) {
			return true;
		}
		if(revisedMatchingIndices.size() == originalMatchingIndices.size() && originalMatchingIndices.size() > 0) {
			int matches = 0;
			for(int i=0; i<originalMatchingIndices.size(); i++) {
				int originalIndex = originalMatchingIndices.get(i);
				int revisedIndex = revisedMatchingIndices.get(i);
				if(originalIndex == revisedIndex) {
					matches++;
				}
				else if(revisedIndex - originalIndex == revised.size() - original.size()) {
					matches++;
				}
			}
			return matches == originalMatchingIndices.size();
		}
		return false;
	}

	private boolean equalOrStripEqual(List<String> original, List<String> revised, String line, Integer index) {
		String originalAtIndex = original.get(index);
		String revisedAtIndex = revised.get(index);
		return equalOrStripEqual(originalAtIndex, revisedAtIndex, line);
	}

	private boolean equalOrStripEqual(String originalAtIndex, String revisedAtIndex, String line) {
		if(originalAtIndex.equals(line) && revisedAtIndex.equals(line))
			return true;
		String originalStripped = originalAtIndex.strip();
		String revisedStripped = revisedAtIndex.strip();
		if(originalStripped.equals(revisedStripped) && originalStripped.equals(line.strip()))
			return true;
		return false;
	}

	private boolean indentationChange(AbstractDelta<String> delta) {
		Chunk<String> source = delta.getSource();
		Chunk<String> target = delta.getTarget();
		if (source.getLines().size() == target.getLines().size() && source.getLines().size() > 0) {
			List<String> sourceStrippedLines = new ArrayList<String>();
			List<String> targetStrippedLines = new ArrayList<String>();
			for(int i=0; i<source.getLines().size(); i++) {
				sourceStrippedLines.add(source.getLines().get(i).strip());
				targetStrippedLines.add(target.getLines().get(i).strip());
			}
			if (sourceStrippedLines.equals(targetStrippedLines)) {
				return true;
			}
		}
		return false;
	}

	private List<Integer> findAllMatchingIndices(List<String> lines, String line) {
		List<Integer> matchingIndices = new ArrayList<>();
		for(int i=0; i<lines.size(); i++) {
			String element = lines.get(i);
			if(element.strip().equals(line.strip())) {
				matchingIndices.add(i);
			}
		}
		return matchingIndices;
	}

	private List<Integer> findAllMatchingIndicesRelaxed(List<String> lines, String line) {
		List<Integer> matchingIndices = new ArrayList<>();
		for(int i=0; i<lines.size(); i++) {
			String element = lines.get(i);
			if(element.strip().contains(line.strip())) {
				matchingIndices.add(i);
			}
		}
		return matchingIndices;
	}

	public HistoryInfo<Block> blameReturn(Block startBlock) {
		List<HistoryInfo<Block>> history = getHistory();
		for (History.HistoryInfo<Block> historyInfo : history) {
			for (Change change : historyInfo.getChangeList()) {
				if (startBlock.isElseBlockStart() || startBlock.isElseBlockEnd()) {
					if (change instanceof Introduced || change instanceof ElseBlockAdded) {
						return historyInfo;
					}
				}
				else if (startBlock.isClosingCurlyBracket()) {
					if (change instanceof Introduced || change instanceof ReplacePipelineWithLoop) {
						return historyInfo;
					}
				}
				else {
					if (change instanceof ExpressionChange || change instanceof Introduced || change instanceof MergeBlock || change instanceof SplitBlock ||
							change instanceof ReplaceLoopWithPipeline || change instanceof ReplacePipelineWithLoop || change instanceof ReplaceConditionalWithTernary) {
						return historyInfo;
					}
					if (startBlock.getComposite() instanceof StatementObject && change instanceof BodyChange) {
						return historyInfo;
					}
				}
			}
		}
		return null;
	}

	public HistoryInfo<Block> blameReturn(Block startBlock, int exactLineNumber) {
		List<HistoryInfo<Block>> history = getHistory();
		for (History.HistoryInfo<Block> historyInfo : history) {
			Pair<Block, Block> pair = Pair.of(historyInfo.getElementBefore(), historyInfo.getElementAfter());
			boolean multiLine = startBlock.isMultiLine();
			for (Change change : historyInfo.getChangeList()) {
				if (startBlock.isElseBlockStart() || startBlock.isElseBlockEnd()) {
					if (change instanceof Introduced || change instanceof ElseBlockAdded) {
						return historyInfo;
					}
				}
				else if (startBlock.isClosingCurlyBracket()) {
					if (change instanceof Introduced || change instanceof ReplacePipelineWithLoop) {
						return historyInfo;
					}
				}
				else {
					if (change instanceof ExpressionChange || change instanceof Introduced || change instanceof MergeBlock || change instanceof SplitBlock ||
							change instanceof ReplaceLoopWithPipeline || change instanceof ReplacePipelineWithLoop || change instanceof ReplaceConditionalWithTernary) {
						return historyInfo;
					}
					if (startBlock.getComposite() instanceof StatementObject && (change instanceof BodyChange || change instanceof BlockSignatureFormatChange)) {
						if (multiLine) {
							if (lineChangeMap.containsKey(pair)) {
								if (lineChangeMap.get(pair).contains(exactLineNumber)) {
									return historyInfo;
								}
							}
						}
						else {
							return historyInfo;
						}
					}
				}
			}
		}
		return null;
	}
}
