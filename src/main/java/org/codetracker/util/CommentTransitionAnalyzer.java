package org.codetracker.util;

import gr.uom.java.xmi.LocationInfo;
import gr.uom.java.xmi.UMLComment;
import gr.uom.java.xmi.decomposition.*;
import org.codetracker.api.Version;
import org.codetracker.change.Change;
import org.codetracker.element.Block;

import java.util.*;

public class CommentTransitionAnalyzer {

    public static Set<Change.Type> DetectCommentTransitionsInsideMatchedBlock(UMLOperationBodyMapper umlOperationBodyMapper, Block blockBefore, Block blockAfter, Version currentVersion, Version parentVersion) {
        Set<Change.Type> changeTypes = new HashSet<>();
        List<UMLComment> deletedComments = umlOperationBodyMapper.getCommentListDiff().getDeletedComments();
        List<UMLComment> addedComments = umlOperationBodyMapper.getCommentListDiff().getAddedComments();
        Map<String, UMLComment> deletedCommentsHash;
        Map<String, UMLComment> addedCommentsHash;

        if (!deletedComments.isEmpty()) {
            deletedCommentsHash = generateCommentTextHashMap(deletedComments);
            if (isUncommentedCodeDetected(deletedCommentsHash, umlOperationBodyMapper, blockAfter))
                changeTypes.add(Change.Type.UNCOMMENTED_STATEMENT);
        }
        if (!addedComments.isEmpty()) {
            addedCommentsHash = generateCommentTextHashMap(addedComments);
            if (isCommentedOutCodeDetected(addedCommentsHash, umlOperationBodyMapper, blockBefore)) {
                changeTypes.add(Change.Type.COMMENTED_OUT_STATEMENT);
            }
        }
        return changeTypes;
    }

    private static boolean isUncommentedCodeDetected(Map<String, UMLComment> deletedCommentsHash, UMLOperationBodyMapper umlOperationBodyMapper, Block blockAfter) {
        if (deletedCommentsHash.isEmpty())
            return false;
        // find unmatched leaf statements afterBlock in deleted comments
        if (!umlOperationBodyMapper.getNonMappedLeavesT2().isEmpty()) {
            for (AbstractCodeFragment abstractCodeFragment : umlOperationBodyMapper.getNonMappedLeavesT2()) {
                if (blockAfter.getComposite().getLeaves().contains(abstractCodeFragment)) { //if nonmappedLeave actually belongs to the target Block
                    String codeFragmentHash = Util.getSHA512(abstractCodeFragment.getString().trim());
                    if (deletedCommentsHash.containsKey(codeFragmentHash)) {
                        return true;
                    }
                }
            }
        }
        //find unmatched composite statements afterBlock in deleted comments
        if (!umlOperationBodyMapper.getNonMappedInnerNodesT2().isEmpty()) {
            for (CompositeStatementObject compositeStatementObject : umlOperationBodyMapper.getNonMappedInnerNodesT2()) {
                if (((CompositeStatementObject) blockAfter.getComposite()).contains(compositeStatementObject)) { //if nonMappedInnerNode actually belongs to the current block
                    String codeFragmentHash = Util.getSHA512(compositeStatementObject.getActualSignature());
                    if (deletedCommentsHash.containsKey(codeFragmentHash)) {
                        return true;
                    }
                }
            }
        }
        // RefactoringMiner may create leaf mappings between semantically different statements when textual similarity is high enough,
        // even when one side is actually a commented/uncommented transition.
        if (!umlOperationBodyMapper.getMappings().isEmpty()) {
            for (AbstractCodeMapping mapping : umlOperationBodyMapper.getMappings()) {
                if (!(mapping instanceof LeafMapping)) // does not happen in composite(i guess)
                    continue;
                if (!isMappingBelongToTrackedBlock(mapping, blockAfter, true))
                    continue;
                //this issue only happens with leaf statements and not composites....
                String codeFragmentHashCurrent = Util.getSHA512(mapping.getFragment2().getString().trim());
                if (deletedCommentsHash.containsKey(codeFragmentHashCurrent)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean isCommentedOutCodeDetected(Map<String, UMLComment> addedCommentsHash, UMLOperationBodyMapper umlOperationBodyMapper, Block blockBefore) {
        if (addedCommentsHash.isEmpty())
            return false;
        if (!umlOperationBodyMapper.getNonMappedLeavesT1().isEmpty()) {
            //find unmatched leaf statements parent1 in added comments
            for (AbstractCodeFragment abstractCodeFragment : umlOperationBodyMapper.getNonMappedLeavesT1()) {
                if (blockBefore.getComposite().getLeaves().contains(abstractCodeFragment)) { // if nonMappedleaf actually belongs to parent block
                    String codeFragmentHash = Util.getSHA512(abstractCodeFragment.getString().trim());
                    if (addedCommentsHash.containsKey(codeFragmentHash)) {
                        return true;
                    }
                }
            }
        }
        if (!umlOperationBodyMapper.getNonMappedInnerNodesT1().isEmpty()) {
            //find unmatched composite statements parent in added comments
            for (CompositeStatementObject compositeStatementObject : umlOperationBodyMapper.getNonMappedInnerNodesT1()) {
                if (((CompositeStatementObject) blockBefore.getComposite()).contains(compositeStatementObject)) { // if nonMappedInnerNode actually belongs to parent block
                    if (!isBracketAndBelongsToStandAloneBlock(compositeStatementObject))
                        continue;
                    String codeFragmentHash = Util.getSHA512(compositeStatementObject.getActualSignature());
                    if (addedCommentsHash.containsKey(codeFragmentHash)) {
                        return true;
                    }
                }
            }
        }
        // RefactoringMiner may create leaf mappings between semantically different statements when textual similarity is high enough,
        // even when one side is actually a commented/uncommented transition.
        if (!umlOperationBodyMapper.getMappings().isEmpty()) {
            for (AbstractCodeMapping mapping : umlOperationBodyMapper.getMappings()) {
                if (!(mapping instanceof LeafMapping))
                    continue;
                if (!isMappingBelongToTrackedBlock(mapping, blockBefore, false))
                    continue;
                String codeFragmentHashParent = Util.getSHA512(mapping.getFragment1().getString().trim());
                if (!addedCommentsHash.isEmpty() && addedCommentsHash.containsKey(codeFragmentHashParent)) {
                    return true;
                }
            }
        }
        return false;
    }
    public static boolean isBlockBodyUnchangedDuringUncomment (UMLOperationBodyMapper umlOperationBodyMapper, CompositeStatementObject uncommentedBlock, Map<String, UMLComment> deletedComments, Block blockAfter) {
        Set<String> mappedFragments = new HashSet<>();
        for (AbstractCodeMapping mapping : umlOperationBodyMapper.getMappings()) {
            if(!isMappingBelongToTrackedBlock(mapping, blockAfter, true))
                continue;
            if(mapping instanceof CompositeStatementObjectMapping) {
                CompositeStatementObject composite = (CompositeStatementObject) mapping.getFragment2();
                if (!isBracketAndBelongsToStandAloneBlock(composite))
                    continue;
                mappedFragments.add(Util.getSHA512(composite.getActualSignature()));
            }
            else if(mapping instanceof LeafMapping && mapping.getFragment2() instanceof StatementObject){ // mappings belong to the currentblock
                mappedFragments.add(Util.getSHA512(mapping.getFragment2().getString().trim()));
            }
        }
        for (CompositeStatementObject inner : uncommentedBlock.getInnerNodes()) {
            if (!isBracketAndBelongsToStandAloneBlock(inner)) {
                continue;
            }
            String hash = Util.getSHA512(inner.getActualSignature().trim());
            if(!existsAsMappedOrComment(hash, mappedFragments, deletedComments)){
                return false;
            }
        }
        for (AbstractCodeFragment leaf : uncommentedBlock.getLeaves()) {
            String hash = Util.getSHA512(((StatementObject) leaf).getActualSignature());
            if(!existsAsMappedOrComment(hash, mappedFragments, deletedComments))
                return false;
        }
        return true;
    }

    private static Change.Type partialBlockUncommented(CompositeStatementObject compositeStatementObject, Block blockBefore, Block blockAfter) {
        //TODO: else/catch/finally uncommented support should be added
        if (blockBefore.getComposite().getLocationInfo().getCodeElementType().equals(LocationInfo.CodeElementType.IF_STATEMENT) &&
                blockAfter.getComposite().getLocationInfo().getCodeElementType().equals(LocationInfo.CodeElementType.IF_STATEMENT)) {
            CompositeStatementObject ifBefore = (CompositeStatementObject) blockBefore.getComposite();
            CompositeStatementObject ifAfter = (CompositeStatementObject) blockAfter.getComposite();
            if (ifBefore.getStatements().size() == 1 && ifAfter.getStatements().size() == 2) {
                return Change.Type.UNCOMMENTED_STATEMENT;
            }
        }
        return null;
    }

    public static boolean isBracketAndBelongsToStandAloneBlock(CompositeStatementObject compositeStatementObject){ //heuristic
        if ((compositeStatementObject.getLocationInfo().getCodeElementType() == LocationInfo.CodeElementType.BLOCK) && (!compositeStatementObject.getParent().getActualSignature().equals("{")))
            return false;
        return true;
    }
    private static boolean isMappingBelongToTrackedBlock(AbstractCodeMapping mapping, Block block, boolean isAfter){
        if(mapping instanceof CompositeStatementObjectMapping){
            CompositeStatementObject composite = (CompositeStatementObject) mapping.getFragment2();
            if(!((CompositeStatementObject) block.getComposite()).contains(composite))
                return false;
        }
        else if(mapping instanceof LeafMapping && (isAfter ? mapping.getFragment2() : mapping.getFragment1()) instanceof StatementObject){
            if(!block.getComposite().getLeaves().contains(isAfter ? mapping.getFragment2() : mapping.getFragment1()))
                return false;
        }
        return true;
    }
    private static boolean existsAsMappedOrComment(String hash, Set<String> mappedFragments, Map<String, UMLComment> comments){
        return mappedFragments.contains(hash) || comments.containsKey(hash);
    }
    public static String normalizeCommentText(String commentText) {
        if (commentText.startsWith("//")) {
            commentText = commentText.substring(2);
        }
        commentText = commentText.trim();
        return commentText;
    }

    public static Map<String, UMLComment> generateCommentTextHashMap(List<UMLComment> commentList) {
        Map<String, UMLComment> commentTextHashMap = new HashMap<>();
        for (UMLComment comment : commentList) {
            String text = comment.getText();
            // remove /* */
            text = text.replace("/*", "").replace("*/", "");
            String[] lines = text.split("\\R");
            for (String line : lines) {
                line = normalizeCommentText(line);
                if (line.isBlank()) {
                    continue;
                }
                commentTextHashMap.put(Util.getSHA512(line), comment);
            }
        }
        return commentTextHashMap;
    }

}
