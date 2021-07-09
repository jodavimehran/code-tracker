package org.refactoringrefiner;

public class APIDiffChangeDetector
//        implements ChangeDetector
{
//    private final static String folderToClone = "H:\\Projects\\";
//    private final ChangeHistory attributeChangeHistory = new ChangeHistory();
//    private final ChangeHistory classChangeHistory = new ChangeHistory();
//    private final ChangeHistory methodChangeHistory = new ChangeHistory();
//    private final APIDiff diff;
//    private final RefactoringMiner refactoringMiner;
//    private final HashSet<String> analysedCommits = new HashSet<>();
//
//    public APIDiffChangeDetector(RefactoringMiner refactoringMiner, String projectDirectory, String repositoryWebURL) {
//        diff = new APIDiff(projectDirectory.replace(folderToClone, ""), repositoryWebURL);
//        diff.setPath(folderToClone);
//        this.refactoringMiner = refactoringMiner;
//    }
//
//    @Override
//    public void detectAtCommit(String commitId) {
//        if (analysedCommits.contains(commitId))
//            return;
//        Pair<UMLModel, UMLModel> umlModel = refactoringMiner.getUMLModel(commitId);
//        apidiff.Result result = diff.detectChangeAtCommit(commitId, Classifier.API);
//        for (apidiff.Change change : result.getChangeField()) {
//            switch (change.getCategory()) {
//                case FIELD_ADD:
//                    fieldAdd(umlModel.getRight(), change, commitId);
//                    break;
//                case FIELD_REMOVE:
//                    fieldRemove(umlModel.getLeft(), change, commitId);
//                    break;
//                case FIELD_MOVE:
//                case FIELD_PULL_UP:
//                case FIELD_PUSH_DOWN:
//                case FIELD_DEPRECATED:
//                case FIELD_CHANGE_DEFAULT_VALUE:
//                case FIELD_CHANGE_TYPE:
//                case FIELD_LOST_VISIBILITY:
//                case FIELD_GAIN_VISIBILITY:
//                case FIELD_REMOVE_MODIFIER_FINAL:
//                case FIELD_ADD_MODIFIER_FINAL:
//                    fieldRefactored(umlModel.getLeft(), umlModel.getRight(), change, commitId);
//                    break;
//            }
//        }
//        for (apidiff.Change change : result.getChangeMethod()) {
//            switch (change.getCategory()) {
//                case METHOD_ADD:
//                    methodAdd(umlModel.getRight(), change, commitId);
//                    break;
//                case METHOD_REMOVE:
//                    methodRemove(umlModel.getLeft(), change, commitId);
//                    break;
//                case METHOD_MOVE:
//                case METHOD_RENAME:
//                case METHOD_PULL_UP:
//                case METHOD_PUSH_DOWN:
//                case METHOD_CHANGE_PARAMETER_LIST:
//                case METHOD_CHANGE_EXCEPTION_LIST:
//                case METHOD_CHANGE_RETURN_TYPE:
//                case METHOD_GAIN_VISIBILITY:
//                case METHOD_LOST_VISIBILITY:
//                case METHOD_REMOVE_MODIFIER_FINAL:
//                case METHOD_ADD_MODIFIER_FINAL:
//                case METHOD_REMOVE_MODIFIER_STATIC:
//                case METHOD_ADD_MODIFIER_STATIC:
//                case METHOD_DEPRECATED:
//                    methodRefactored(umlModel.getLeft(), umlModel.getRight(), change, commitId);
//                    break;
//                case METHOD_EXTRACT:
//                case METHOD_INLINE:
//                    System.out.println(change.getRef());
//                    break;
//            }
//        }
//        for (apidiff.Change change : result.getChangeType()) {
//            switch (change.getCategory()) {
//                case TYPE_ADD:
//                    typeAdd(umlModel.getRight(), change, commitId);
//                    break;
//                case TYPE_REMOVE:
//                    typeRemove(umlModel.getLeft(), change, commitId);
//                    break;
//                case TYPE_MOVE_AND_RENAME:
//                case TYPE_RENAME:
//                case TYPE_MOVE:
//                    typeRefactored(umlModel.getLeft(), umlModel.getRight(), change, commitId);
//                    break;
//                case TYPE_EXTRACT_SUPERTYPE:
//                case TYPE_LOST_VISIBILITY:
//                case TYPE_GAIN_VISIBILITY:
//                case TYPE_REMOVE_MODIFIER_FINAL:
//                case TYPE_ADD_MODIFIER_FINAL:
//                case TYPE_REMOVE_MODIFIER_STATIC:
//                case TYPE_ADD_MODIFIER_STATIC:
//                case TYPE_CHANGE_SUPERCLASS:
//                case TYPE_REMOVE_SUPERCLASS:
//                case TYPE_ADD_SUPER_CLASS:
//                case TYPE_DEPRECATED:
//                    typeRefactored(umlModel.getLeft(), umlModel.getRight(), change, commitId);
//                    break;
//            }
//        }
//        analysedCommits.add(commitId);
//    }
//
//    @Override
//    public List<CodeElement> findMostLeftElement(RefactoringRefiner.CodeElementType codeElementType, String codeElementKey) {
//        switch (codeElementType) {
//            case CLASS:
//                return classChangeHistory.findMostLeftSide(codeElementKey);
//            case ATTRIBUTE:
//                return attributeChangeHistory.findMostLeftSide(codeElementKey);
//            case METHOD:
//                return methodChangeHistory.findMostLeftSide(codeElementKey);
//        }
//        return Collections.emptyList();
//    }
//
//    private void fieldAdd(UMLModel rightModel, apidiff.Change change, String commitId) {
//        String key = getAttributeKey(change);
//        Attribute leftAttribute = RefactoringMiner.getAttribute(rightModel, key, RefactoringMiner::contains, refactoringMiner.getRepository().getVersion(refactoringMiner.getRepository().getParentId(commitId)));
//        Attribute rightAttribute = RefactoringMiner.getAttribute(rightModel, key, RefactoringMiner::contains, refactoringMiner.getRepository().getVersion(commitId));
//        handleAddedField(leftAttribute, rightAttribute, getDescription(change));
//    }
//
//    private void fieldRemove(UMLModel leftModel, apidiff.Change change, String commitId) {
//        String key = getAttributeKey(change);
//        Attribute leftAttribute = RefactoringMiner.getAttribute(leftModel, key, RefactoringMiner::contains, refactoringMiner.getRepository().getVersion(refactoringMiner.getRepository().getParentId(commitId)));
//        Attribute rightAttribute = RefactoringMiner.getAttribute(leftModel, key, RefactoringMiner::contains, refactoringMiner.getRepository().getVersion(commitId));
//        handleRemovedField(leftAttribute, rightAttribute, getDescription(change));
//    }
//
//    private void handleAddedField(Attribute leftAttribute, Attribute rightAttribute, String description) {
//        if (leftAttribute != null && rightAttribute != null) {
//            attributeChangeHistory.addNode(leftAttribute);
//            attributeChangeHistory.addNode(rightAttribute);
//            attributeChangeHistory.handleAdd(leftAttribute, rightAttribute, description);
//        }
//    }
//
//    private void handleRemovedField(Attribute leftAttribute, Attribute rightAttribute, String description) {
//        if (leftAttribute != null && rightAttribute != null) {
//            attributeChangeHistory.addNode(leftAttribute);
//            attributeChangeHistory.addNode(rightAttribute);
//            attributeChangeHistory.handleRemoved(leftAttribute, rightAttribute, description);
//        }
//    }
//
//    private void fieldRefactored(UMLModel leftModel, UMLModel rightModel, apidiff.Change change, String commitId) {
//        String key = getAttributeKey(change);
//        Attribute leftAttribute = RefactoringMiner.getAttribute(leftModel, key, RefactoringMiner::contains, refactoringMiner.getRepository().getVersion(refactoringMiner.getRepository().getParentId(commitId)));
//        Attribute rightAttribute = RefactoringMiner.getAttribute(rightModel, key, RefactoringMiner::contains, refactoringMiner.getRepository().getVersion(commitId));
//        if (leftAttribute != null && rightAttribute != null) {
//            attributeChangeHistory.addNode(leftAttribute);
//            attributeChangeHistory.addNode(rightAttribute);
//            attributeChangeHistory.addChange(leftAttribute, rightAttribute, ChangeFactory.of(Change.Type.REFACTORED).description(getDescription(change)));
//        }
//    }
//
//    private void methodRefactored(UMLModel leftModel, UMLModel rightModel, apidiff.Change change, String commitId) {
//        String key = getMethodKey(change);
//        String leftKey = change.getRef() != null ? change.getRef().getEntityBefore().key().toString() : key;
//        String rightKey = change.getRef() != null ? change.getRef().getEntityAfter().key().toString() : key;
//        Method leftMethod = RefactoringMiner.getMethod(leftModel, leftKey, RefactoringMiner::contains, refactoringMiner.getRepository().getVersion(refactoringMiner.getRepository().getParentId(commitId)), false);
//        Method rightMethod = RefactoringMiner.getMethod(rightModel, rightKey, RefactoringMiner::contains, refactoringMiner.getRepository().getVersion(commitId), false);
//        if (leftMethod != null && rightMethod != null) {
//            methodChangeHistory.addNode(leftMethod);
//            methodChangeHistory.addNode(rightMethod);
//            methodChangeHistory.addChange(leftMethod, rightMethod, ChangeFactory.of(Change.Type.REFACTORED).description(getDescription(change)));
//        }
//    }
//
//    private String getDescription(apidiff.Change change) {
//        return String.format("%s:%s", change.getCategory().getDisplayName(), change.getDescription());
//    }
//
//    private void methodRemove(UMLModel leftModel, apidiff.Change change, String commitId) {
//        String key = getMethodKey(change);
//        Method leftMethod = RefactoringMiner.getMethod(leftModel, key, RefactoringMiner::contains, refactoringMiner.getRepository().getVersion(refactoringMiner.getRepository().getParentId(commitId)), false);
//        Method rightMethod = RefactoringMiner.getMethod(leftModel, key, RefactoringMiner::contains, refactoringMiner.getRepository().getVersion(commitId), false);
//        handleRemovedMethod(leftMethod, rightMethod, getDescription(change));
//    }
//
//    private void methodAdd(UMLModel rightModel, apidiff.Change change, String commitId) {
//        String key = getMethodKey(change);
//        Method leftMethod = RefactoringMiner.getMethod(rightModel, key, RefactoringMiner::contains, refactoringMiner.getRepository().getVersion(refactoringMiner.getRepository().getParentId(commitId)), false);
//        Method rightMethod = RefactoringMiner.getMethod(rightModel, key, RefactoringMiner::contains, refactoringMiner.getRepository().getVersion(commitId), false);
//        handleAddedMethod(leftMethod, rightMethod, getDescription(change));
//    }
//
//    private void handleAddedMethod(Method leftMethod, Method rightMethod, String description) {
//        if (leftMethod != null && rightMethod != null) {
//            methodChangeHistory.addNode(leftMethod);
//            methodChangeHistory.addNode(rightMethod);
//            methodChangeHistory.handleAdd(leftMethod, rightMethod, description);
//        }
//    }
//
//    private void handleRemovedMethod(Method leftMethod, Method rightMethod, String description) {
//        if (leftMethod != null && rightMethod != null) {
//            methodChangeHistory.addNode(leftMethod);
//            methodChangeHistory.addNode(rightMethod);
//            methodChangeHistory.handleRemoved(leftMethod, rightMethod, description);
//        }
//    }
//
//    private void typeRemove(UMLModel leftModel, apidiff.Change change, String commitId) {
//        String key = getClassKey(change);
//        String description = getDescription(change);
//        Class leftClass = RefactoringMiner.getClass(leftModel, key, RefactoringMiner::contains, refactoringMiner.getRepository().getVersion(refactoringMiner.getRepository().getParentId(commitId)));
//        Class rightClass = RefactoringMiner.getClass(leftModel, key, RefactoringMiner::contains, refactoringMiner.getRepository().getVersion(commitId));
//        if (leftClass != null && rightClass != null) {
//            classChangeHistory.addNode(leftClass);
//            classChangeHistory.addNode(rightClass);
//            classChangeHistory.handleRemoved(leftClass, rightClass, description);
//
//            for (UMLOperation operation : leftClass.getOperations()) {
//                Method leftMethod = Method.of(operation, refactoringMiner.getRepository().getVersion(refactoringMiner.getRepository().getParentId(commitId)));
//                Method rightMethod = Method.of(operation, refactoringMiner.getRepository().getVersion(commitId));
//                handleRemovedMethod(leftMethod, rightMethod, description);
//            }
//            for (UMLAttribute attribute : leftClass.getAttributes()) {
//                Attribute leftAttribute = Attribute.of(attribute, refactoringMiner.getRepository().getVersion(refactoringMiner.getRepository().getParentId(commitId)));
//                Attribute rightAttribute = Attribute.of(attribute, refactoringMiner.getRepository().getVersion(commitId));
//                handleRemovedField(leftAttribute, rightAttribute, description);
//            }
//        }
//    }
//
//    private void typeAdd(UMLModel rightModel, apidiff.Change change, String commitId) {
//        String key = getClassKey(change);
//        String description = getDescription(change);
//        Class leftClass = RefactoringMiner.getClass(rightModel, key, RefactoringMiner::contains, refactoringMiner.getRepository().getVersion(refactoringMiner.getRepository().getParentId(commitId)));
//        Class rightClass = RefactoringMiner.getClass(rightModel, key, RefactoringMiner::contains, refactoringMiner.getRepository().getVersion(commitId));
//
//        if (leftClass != null && rightClass != null) {
//            classChangeHistory.addNode(leftClass);
//            classChangeHistory.addNode(rightClass);
//            classChangeHistory.handleAdd(leftClass, rightClass, description);
//
//            for (UMLOperation operation : leftClass.getOperations()) {
//                Method leftMethod = Method.of(operation, refactoringMiner.getRepository().getVersion(refactoringMiner.getRepository().getParentId(commitId)));
//                Method rightMethod = Method.of(operation, refactoringMiner.getRepository().getVersion(commitId));
//                handleAddedMethod(leftMethod, rightMethod, description);
//            }
//            for (UMLAttribute attribute : leftClass.getAttributes()) {
//                Attribute leftAttribute = Attribute.of(attribute, refactoringMiner.getRepository().getVersion(refactoringMiner.getRepository().getParentId(commitId)));
//                Attribute rightAttribute = Attribute.of(attribute, refactoringMiner.getRepository().getVersion(commitId));
//                handleAddedField(leftAttribute, rightAttribute, description);
//            }
//        }
//    }
//
//    private void typeRefactored(UMLModel leftModel, UMLModel rightModel, apidiff.Change change, String commitId) {
//        String key = getClassKey(change);
//        String description = getDescription(change);
//        String leftKey = change.getRef() != null ? change.getRef().getEntityBefore().key().toString() : key;
//        String rightKey = change.getRef() != null ? change.getRef().getEntityAfter().key().toString() : key;
//
//        String parentCommitId = refactoringMiner.getRepository().getParentId(commitId);
//        VersionImpl parentVersion = refactoringMiner.getRepository().getVersion(parentCommitId);
//        VersionImpl currentVersion = refactoringMiner.getRepository().getVersion(commitId);
//
//        Class leftClass = RefactoringMiner.getClass(leftModel, leftKey, RefactoringMiner::contains, parentVersion);
//        Class rightClass = RefactoringMiner.getClass(rightModel, rightKey, RefactoringMiner::contains, currentVersion);
//        if (leftClass != null && rightClass != null) {
//            classChangeHistory.addNode(leftClass);
//            classChangeHistory.addNode(rightClass);
//            classChangeHistory.addChange(leftClass, rightClass, ChangeFactory.of(Change.Type.REFACTORED).description(description));
//            if (Category.TYPE_RENAME.equals(change.getCategory()) || Category.TYPE_MOVE.equals(change.getCategory()) || Category.TYPE_MOVE_AND_RENAME.equals(change.getCategory())) {
//                matchOperations(description, parentVersion, currentVersion, leftClass.getOperations(), rightClass.getOperations());
//                matchAttributes(description, parentVersion, currentVersion, leftClass.getAttributes(), rightClass.getAttributes());
//            }
//        }
//    }
//
//    private String getAttributeKey(apidiff.Change change) {
//        return String.format("%s@%s", change.getPath(), change.getElement());
//    }
//
//    private String getMethodKey(apidiff.Change change) {
//        return change.getElement().replace("...", "[]");
//    }
//
//    private String getClassKey(apidiff.Change change) {
//        return change.getElement();
//    }
//
//    private void matchAttributes(String description, VersionImpl parentVersion, VersionImpl currentVersion, List<UMLAttribute> leftSide, List<UMLAttribute> rightSide) {
//        for (UMLAttribute attributeBefore : leftSide)
//            for (UMLAttribute attributeAfter : rightSide)
//                if (attributeBefore.getName().equals(attributeAfter.getName())) {
//                    Attribute leftAttribute = Attribute.of(attributeBefore, parentVersion);
//                    Attribute rightAttribute = Attribute.of(attributeAfter, currentVersion);
//
//                    attributeChangeHistory.addNode(leftAttribute);
//                    attributeChangeHistory.addNode(rightAttribute);
//                    attributeChangeHistory.addChange(leftAttribute, rightAttribute, ChangeFactory.of(AbstractChange.Type.CONTAINER_CHANGE).description(description));
//                }
//
//    }
//
//    public void matchOperations(String description, VersionImpl parentVersion, VersionImpl currentVersion, List<UMLOperation> leftSide, List<UMLOperation> rightSide) {
//        Map<UMLOperation, List<UMLOperation>> matched = new HashMap<>();
//        for (UMLOperation operationBefore : leftSide) {
//            ArrayList<UMLOperation> maybe = new ArrayList<>();
//            matched.put(operationBefore, maybe);
//            for (UMLOperation operationAfter : rightSide) {
//                if (operationBefore.getName().equals(operationAfter.getName())) {
//                    maybe.add(operationAfter);
//                }
//            }
//        }
//        for (Map.Entry<UMLOperation, List<UMLOperation>> entry : matched.entrySet()) {
//            UMLOperation umlOperationBefore = entry.getKey();
//            if (entry.getValue().size() == 1) {
//                UMLOperation umlOperationAfter = entry.getValue().get(0);
//
//                addMethodChange(description, parentVersion, currentVersion, umlOperationBefore, umlOperationAfter);
//            } else {
//                for (UMLOperation operationAfter : entry.getValue()) {
//                    if (umlOperationBefore.equalSignature(operationAfter)) {
//                        addMethodChange(description, parentVersion, currentVersion, umlOperationBefore, operationAfter);
//                    }
//                }
//            }
//        }
//    }
//
//    private void addMethodChange(String description, VersionImpl parentVersion, VersionImpl currentVersion, UMLOperation umlOperationBefore, UMLOperation umlOperationAfter) {
//        Method leftMethod = Method.of(umlOperationBefore, parentVersion);
//        Method rightMethod = Method.of(umlOperationAfter, currentVersion);
//        methodChangeHistory.addNode(leftMethod);
//        methodChangeHistory.addNode(rightMethod);
//
//        methodChangeHistory.addChange(leftMethod, rightMethod, ChangeFactory.of(Change.Type.CONTAINER_CHANGE).description(description));
//    }
//
//    public Graph<CodeElement, Edge> findSubGraph(RefactoringRefiner.CodeElementType codeElementType, CodeElement start) {
//        switch (codeElementType) {
//            case CLASS:
//                return classChangeHistory.findSubGraph(start);
//            case ATTRIBUTE:
//                return attributeChangeHistory.findSubGraph(start);
//            case METHOD:
//                return methodChangeHistory.findSubGraph(start);
//        }
//        return null;
//    }
//
//    @Override
//    public void addNode(RefactoringRefiner.CodeElementType codeElementType, CodeElement codeElement) {
//        switch (codeElementType) {
//            case CLASS:
//                classChangeHistory.addNode(codeElement);
//            case ATTRIBUTE:
//                attributeChangeHistory.addNode(codeElement);
//            case METHOD:
//                methodChangeHistory.addNode(codeElement);
//        }
//    }
}
