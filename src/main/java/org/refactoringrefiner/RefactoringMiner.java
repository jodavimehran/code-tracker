package org.refactoringrefiner;

import gr.uom.java.xmi.UMLClass;
import gr.uom.java.xmi.UMLModel;
import gr.uom.java.xmi.UMLOperation;
import gr.uom.java.xmi.diff.UMLModelDiff;
import org.apache.commons.lang3.tuple.Pair;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.refactoringminer.rm1.GitHistoryRefactoringMinerImpl;
import org.refactoringminer.util.GitServiceImpl;
import org.refactoringrefiner.api.*;
import org.refactoringrefiner.edge.ChangeFactory;
import org.refactoringrefiner.element.Class;
import org.refactoringrefiner.element.Method;
import org.refactoringrefiner.util.GitRepository;
import org.refactoringrefiner.util.IRepository;

import java.util.*;
import java.util.function.Predicate;

public class RefactoringMiner implements ChangeDetector {
    private final GitHistoryRefactoringMinerImpl gitHistoryRefactoringMiner;
    private final GitServiceImpl gitService;
    private final RefactoringHandlerImpl refactoringHandler;
    private final Repository repository;
    private final HashSet<String> analysedCommits = new HashSet<>();
    private final String repositoryWebURL;

    public RefactoringMiner(Repository repository, String repositoryWebURL) {
        this.repository = repository;
        this.gitHistoryRefactoringMiner = new GitHistoryRefactoringMinerImpl();
        this.gitService = new GitServiceImpl();
        this.refactoringHandler = new RefactoringHandlerImpl(new GitRepository(repository));
        this.repositoryWebURL = repositoryWebURL;
    }

    public static Class getClass(UMLModel umlModel, String key) {
        return getClass(umlModel, key, null);
    }

    public static Class getClass(UMLModel umlModel, String key, Version version) {
        for (UMLClass umlClass : umlModel.getClassList()) {
            Class clazz = Class.of(umlClass, version);
            if (key.equals(clazz.getName())) {
                return clazz;
            }
        }
        return null;
    }

//    public static Attribute getAttribute(UMLModel umlModel, String key) {
//        return getAttribute(umlModel, key, null);
//
//    }

//    public static Attribute getAttribute(UMLModel umlModel, String key, Version version) {
//        for (UMLClass umlClass : umlModel.getClassList()) {
//            for (UMLAttribute umlAttribute : umlClass.getAttributes()) {
//                Attribute attribute = Attribute.of(umlAttribute, version);
//                if (key.equals(attribute.getName())) {
//                    return attribute;
//                }
//            }
//        }
//        return null;
//    }

    public static Method getMethod(UMLModel umlModel, Version version, Predicate<Method> predicate) {
        if (umlModel != null)
            for (UMLClass umlClass : umlModel.getClassList()) {
                for (UMLOperation umlOperation : umlClass.getOperations()) {
                    Method method = Method.of(umlOperation, version);
                    if (predicate.test(method))
                        return method;
                }
            }
        return null;
    }

    public static Method getMethodByName(UMLModel umlModel, Version version, String key) {
        if (umlModel != null)
            for (UMLClass umlClass : umlModel.getClassList()) {
                for (UMLOperation umlOperation : umlClass.getOperations()) {
                    Method method = Method.of(umlOperation, version);
                    if (key.equals(method.getName()))
                        return method;
                }
            }
        return null;
    }

    @Override
    public void detectAtCommit(String commitId) {
        if (analysedCommits.contains(commitId))
            return;
        gitHistoryRefactoringMiner.detectAtCommit(repository, commitId, refactoringHandler, 36000);
        analysedCommits.add(commitId);
    }

//    public boolean isChanged(Pair<UMLModel, UMLModel> umlModel, String elementKey, RefactoringRefiner.CodeElementType codeElementType) {
//        if (umlModel == null)
//            return false;
//
//        UMLModel leftSideUMLModel = umlModel.getLeft();
//        UMLModel rightSideUMLModel = umlModel.getRight();
//        if (leftSideUMLModel == null || rightSideUMLModel == null)
//            return true;
//
//        //UMLModelDiff diff = leftSideUMLModel.diff(rightSideUMLModel);
//        //TODO: check local refactorings for more improvement
//        switch (codeElementType) {
//            case METHOD:
//                return isMethodChanged(leftSideUMLModel, rightSideUMLModel, elementKey);
//            case CLASS:
//                return isClassChanged(leftSideUMLModel, rightSideUMLModel, elementKey);
//            case ATTRIBUTE:
//                return isAttributeChanged(leftSideUMLModel, rightSideUMLModel, elementKey);
//            case VARIABLE:
//                return isVariableChanged(leftSideUMLModel, rightSideUMLModel, elementKey);
//        }
//
//        return true;
//    }

//    public Pair<UMLModel, UMLModel> getUMLModel(String commitId) {
//        return getUMLModel(commitId, (s -> true));
//    }
//
//    public Pair<UMLModel, UMLModel> getUMLModel(String commitId, Predicate<String> filterFile) {
//        try (RevWalk walk = new RevWalk(repository)) {
//            RevCommit currentCommit = walk.parseCommit(repository.resolve(commitId));
//            if (currentCommit.getParentCount() != 1)
//                return null;
//            walk.parseCommit(currentCommit.getParent(0));
//            RevCommit parentCommit = currentCommit.getParent(0);
//            List<String> filePathsBefore = new ArrayList<>();
//            List<String> filePathsCurrent = new ArrayList<>();
//            Map<String, String> renamedFilesHint = new HashMap<>();
//            gitService.fileTreeDiff(repository, parentCommit, currentCommit, filePathsBefore, filePathsCurrent, renamedFilesHint);
//
//            List<String> filePathBeforeFilter = filePathsBefore.stream().filter(filterFile).collect(Collectors.toList());
//            List<String> filePathCurrentFilter = filePathsCurrent.stream().filter(filterFile).collect(Collectors.toList());
//            UMLModel leftSideUMLModel = null;
//            UMLModel rightSideUMLModel = null;
//            if (!filePathBeforeFilter.isEmpty())
//                leftSideUMLModel = gitHistoryRefactoringMiner.getUmlModel(repository, parentCommit, filePathBeforeFilter);
//            if (!filePathCurrentFilter.isEmpty())
//                rightSideUMLModel = gitHistoryRefactoringMiner.getUmlModel(repository, currentCommit, filePathCurrentFilter);
//            if (leftSideUMLModel == null && rightSideUMLModel == null)
//                return null;
//
//            return Pair.of(leftSideUMLModel, rightSideUMLModel);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        return null;
//    }

    public List<UMLModelDiff> getUMLModelDiff(String commitId, List<String> rightSideFileNames) {
        List<UMLModelDiff> result = new ArrayList<>();
        try (RevWalk walk = new RevWalk(repository)) {
            RevCommit currentCommit = walk.parseCommit(repository.resolve(commitId));
            if (currentCommit.getParentCount() == 1 || currentCommit.getParentCount() == 2) {
                walk.parseCommit(currentCommit.getParent(0));
                RevCommit parentCommit = currentCommit.getParent(0);
                result.add(getUMLModelDiff(rightSideFileNames, currentCommit, parentCommit));
            }

            if (currentCommit.getParentCount() == 2) {
                walk.parseCommit(currentCommit.getParent(1));
                RevCommit parentCommit = currentCommit.getParent(1);
                result.add(getUMLModelDiff(rightSideFileNames, currentCommit, parentCommit));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    private UMLModelDiff getUMLModelDiff(List<String> rightSideFileNames, RevCommit currentCommit, RevCommit parentCommit) throws Exception {
        List<String> filePathsBefore = new ArrayList<>();
        List<String> filePathsCurrent = new ArrayList<>();
        Map<String, String> renamedFilesHint = new HashMap<>();
        gitService.fileTreeDiff(repository, parentCommit, currentCommit, filePathsBefore, filePathsCurrent, renamedFilesHint);

        UMLModel leftSideUMLModel = gitHistoryRefactoringMiner.getUmlModel(repository, parentCommit, filePathsBefore);
        UMLModel rightSideUMLModel = gitHistoryRefactoringMiner.getUmlModel(repository, currentCommit, rightSideFileNames);

        return leftSideUMLModel.diff(rightSideUMLModel, renamedFilesHint);
    }

    public Pair<Pair<UMLModel, UMLModel>, UMLModel> getUMLModelPair(String commitId, List<String> rightSideFileNames) {
        Pair<Pair<UMLModel, UMLModel>, UMLModel> result = null;
        if (rightSideFileNames == null)
            throw new IllegalArgumentException("File names could not be null.");

        if (rightSideFileNames.isEmpty())
            return null;

        try (RevWalk walk = new RevWalk(repository)) {
            RevCommit revCommit = walk.parseCommit(repository.resolve(commitId));

            UMLModel rightSideUMLModel = gitHistoryRefactoringMiner.getUmlModel(repository, revCommit, rightSideFileNames);


            UMLModel leftSideUMLModel1 = null;
            UMLModel leftSideUMLModel2 = null;
            switch (revCommit.getParentCount()) {
                case 0: {
                    return Pair.of(null, rightSideUMLModel);
                }
                case 2: {
                    walk.parseCommit(revCommit.getParent(1));
                    RevCommit parentCommit = revCommit.getParent(1);
                    leftSideUMLModel2 = gitHistoryRefactoringMiner.getUmlModel(repository, parentCommit, rightSideFileNames);
                }
                case 1: {
                    walk.parseCommit(revCommit.getParent(0));
                    RevCommit parentCommit = revCommit.getParent(0);
                    leftSideUMLModel1 = gitHistoryRefactoringMiner.getUmlModel(repository, parentCommit, rightSideFileNames);
                    break;
                }

            }

            result = Pair.of(Pair.of(leftSideUMLModel1, leftSideUMLModel2), rightSideUMLModel);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    public UMLModel getUMLModel(String commitId, List<String> fileNames) {
        if (fileNames == null || fileNames.isEmpty())
            return null;
        try (RevWalk walk = new RevWalk(repository)) {
            RevCommit revCommit = walk.parseCommit(repository.resolve(commitId));
            return gitHistoryRefactoringMiner.getUmlModel(repository, revCommit, fileNames);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

//    private <U, E extends CodeElement> boolean isChanged(UMLModel leftSide, UMLModel rightSide, String key, CodeElementFinder<U> codeElementFinder) {
//        CodeElement leftSideCodeElement = codeElementFinder.getCodeElement(leftSide, key);
//        CodeElement rightSideCodeElement = codeElementFinder.getCodeElement(rightSide, key);
//        if (leftSideCodeElement == null || rightSideCodeElement == null)
//            return true;
//
//        return !leftSideCodeElement.getIdentifierIgnoringVersion().equals(rightSideCodeElement.getIdentifierIgnoringVersion());
//    }
//
//    private boolean isClassChanged(UMLModel leftSideUMLModel, UMLModel rightSideUMLModel, String key) {
//        return isChanged(leftSideUMLModel, rightSideUMLModel, key, RefactoringMiner::getClass);
//    }
//
//    private boolean isMethodChanged(UMLModel leftSideUMLModel, UMLModel rightSideUMLModel, String key) {
//        return false;
//        //        return isChanged(leftSideUMLModel, rightSideUMLModel, key, RefactoringMiner::getMethod);
//    }
//
//    private boolean isVariableChanged(UMLModel leftSideUMLModel, UMLModel rightSideUMLModel, String key) {
//        throw new UnsupportedOperationException();
//    }
//
//    private boolean isAttributeChanged(UMLModel leftSideUMLModel, UMLModel rightSideUMLModel, String key) {
//        return isChanged(leftSideUMLModel, rightSideUMLModel, key, RefactoringMiner::getAttribute);
//    }

    public List<CodeElement> findMostLeftElement(RefactoringRefiner.CodeElementType codeElementType, String codeElementKey) {
        switch (codeElementType) {
            case CLASS:
                return refactoringHandler.getClassChangeHistoryGraph().findMostLeftSide(codeElementKey);
            case ATTRIBUTE:
                return refactoringHandler.getAttributeChangeHistory().findMostLeftSide(codeElementKey);
            case METHOD:
                return refactoringHandler.getMethodChangeHistoryGraph().findMostLeftSide(codeElementKey);
            case VARIABLE:
                return refactoringHandler.getVariableChangeHistoryGraph().findMostLeftSide(codeElementKey);
        }
        return Collections.emptyList();
    }

    public Graph<CodeElement, Edge> findSubGraph(RefactoringRefiner.CodeElementType codeElementType, CodeElement start) {
        switch (codeElementType) {
            case CLASS:
                return refactoringHandler.getClassChangeHistoryGraph().findSubGraph(start);
            case ATTRIBUTE:
                return refactoringHandler.getAttributeChangeHistory().findSubGraph(start);
            case METHOD:
                return refactoringHandler.getMethodChangeHistoryGraph().findSubGraph(start);
            case VARIABLE:
                return refactoringHandler.getVariableChangeHistoryGraph().findSubGraph(start);
        }
        return null;
    }

    public IRepository getRepository() {
        return refactoringHandler.getRepository();
    }

    @Override
    public void addNode(RefactoringRefiner.CodeElementType codeElementType, CodeElement codeElement) {
        switch (codeElementType) {
            case CLASS: {
                refactoringHandler.getClassChangeHistoryGraph().addNode(codeElement);
                break;
            }
            case ATTRIBUTE: {
                refactoringHandler.getAttributeChangeHistory().addNode(codeElement);
                break;
            }
            case METHOD: {
                refactoringHandler.getMethodChangeHistoryGraph().addNode(codeElement);
                break;
            }
            case VARIABLE: {
                refactoringHandler.getVariableChangeHistoryGraph().addNode(codeElement);
                break;
            }
        }
    }

    public void addEdge(RefactoringRefiner.CodeElementType codeElementType, CodeElement leftSide, CodeElement rightSide, ChangeFactory changeFactory) {
        switch (codeElementType) {
            case CLASS: {
                refactoringHandler.getClassChangeHistoryGraph().addChange(leftSide, rightSide, changeFactory);
                break;
            }
            case ATTRIBUTE: {
                refactoringHandler.getAttributeChangeHistory().addChange(leftSide, rightSide, changeFactory);
                break;
            }
            case METHOD: {
                refactoringHandler.getMethodChangeHistoryGraph().addChange(leftSide, rightSide, changeFactory);
                break;
            }
            case VARIABLE: {
                refactoringHandler.getVariableChangeHistoryGraph().addChange(leftSide, rightSide, changeFactory);
                break;
            }
        }
    }

    public Set<CodeElement> predecessors(RefactoringRefiner.CodeElementType codeElementType, CodeElement codeElement) {
        switch (codeElementType) {
            case CLASS:
                return refactoringHandler.getClassChangeHistoryGraph().predecessors(codeElement);
            case ATTRIBUTE:
                return refactoringHandler.getAttributeChangeHistory().predecessors(codeElement);
            case METHOD:
                return refactoringHandler.getMethodChangeHistoryGraph().predecessors(codeElement);
            case VARIABLE:
                return refactoringHandler.getVariableChangeHistoryGraph().predecessors(codeElement);
        }
        return Collections.emptySet();
    }

    public Version getVersion(String commitId) {
        return refactoringHandler.getRepository().getVersion(commitId);
    }

    public RefactoringHandlerImpl getRefactoringHandler() {
        return refactoringHandler;
    }

    public void connectRelatedNodes(RefactoringRefiner.CodeElementType codeElementType) {
        switch (codeElementType) {
            case CLASS: {
                refactoringHandler.getClassChangeHistoryGraph().connectRelatedNodes();
                break;
            }
            case ATTRIBUTE: {
                refactoringHandler.getAttributeChangeHistory().connectRelatedNodes();
                break;
            }
            case METHOD: {
                refactoringHandler.getMethodChangeHistoryGraph().connectRelatedNodes();
                break;
            }
            case VARIABLE: {
                refactoringHandler.getVariableChangeHistoryGraph().connectRelatedNodes();
                break;
            }
        }
    }

//    private interface CodeElementFinder<U> {
//        CodeElement getCodeElement(UMLModel umlModel, String key);
//    }
}
