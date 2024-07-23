package org.codetracker;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.codetracker.api.Version;
import org.codetracker.element.Attribute;
import org.codetracker.element.BaseCodeElement;
import org.codetracker.element.Class;
import org.codetracker.element.Method;
import org.refactoringminer.rm1.GitHistoryRefactoringMinerImpl;

import gr.uom.java.xmi.UMLAbstractClass;
import gr.uom.java.xmi.UMLAnonymousClass;
import gr.uom.java.xmi.UMLAttribute;
import gr.uom.java.xmi.UMLClass;
import gr.uom.java.xmi.UMLClassMatcher;
import gr.uom.java.xmi.UMLModel;
import gr.uom.java.xmi.UMLOperation;
import gr.uom.java.xmi.UMLParameter;
import gr.uom.java.xmi.UMLType;
import gr.uom.java.xmi.VariableDeclarationContainer;
import gr.uom.java.xmi.decomposition.AbstractCall;
import gr.uom.java.xmi.decomposition.AbstractCodeFragment;
import gr.uom.java.xmi.decomposition.UMLOperationBodyMapper;
import gr.uom.java.xmi.diff.MoveSourceFolderRefactoring;
import gr.uom.java.xmi.diff.UMLAbstractClassDiff;
import gr.uom.java.xmi.diff.UMLClassBaseDiff;
import gr.uom.java.xmi.diff.UMLClassDiff;
import gr.uom.java.xmi.diff.UMLClassMoveDiff;
import gr.uom.java.xmi.diff.UMLClassRenameDiff;
import gr.uom.java.xmi.diff.UMLModelDiff;

public abstract class AbstractTracker {
	private static final Pattern CAMEL_CASE_SPLIT_PATTERN = Pattern.compile("(?<!(^|[A-Z]))(?=[A-Z])|(?<!^)(?=[A-Z][a-z])");
	protected final String startCommitId;
	protected final String filePath;

	protected AbstractTracker(String startCommitId, String filePath) {
		this.startCommitId = startCommitId;
		this.filePath = filePath;
	}

	protected static UMLClassBaseDiff lightweightClassDiff(UMLAbstractClass leftClass, UMLAbstractClass rightClass) {
	    if (leftClass instanceof UMLClass && rightClass instanceof UMLClass) {
	        UMLClassDiff classDiff = new UMLClassDiff((UMLClass)leftClass, (UMLClass)rightClass, null);
	        return classDiff;
	    }
	    else if (leftClass instanceof UMLAnonymousClass && rightClass instanceof UMLAnonymousClass) {
	    	//TODO
	    }
	    return null;
	}

	protected static UMLClassBaseDiff lightweightClassDiff(UMLModel leftModel, UMLModel rightModel, VariableDeclarationContainer leftOperation, VariableDeclarationContainer rightOperation) {
	    UMLClass leftClass = null;
	    for (UMLClass clazz : leftModel.getClassList()) {
	        if (clazz.getName().equals(leftOperation.getClassName())) {
	            leftClass = clazz;
	            break;
	        }
	    }
	    UMLClass rightClass = null;
	    for (UMLClass clazz : rightModel.getClassList()) {
	        if (clazz.getName().equals(rightOperation.getClassName())) {
	            rightClass = clazz;
	            break;
	        }
	    }
	    if (leftClass != null && rightClass != null) {
	        UMLClassDiff classDiff = new UMLClassDiff(leftClass, rightClass, null);
	        for (UMLOperation operation : leftClass.getOperations()) {
	            int index = rightClass.getOperations().indexOf(operation);
	            UMLOperation operation2 = null;
	            if (index != -1) {
	                operation2 = rightClass.getOperations().get(index);
	            }
	            if (index == -1 || differentParameterNames(leftClass, rightClass, operation, operation2))
	                classDiff.getRemovedOperations().add(operation);
	        }
	        for (UMLOperation operation : rightClass.getOperations()) {
	            int index = leftClass.getOperations().indexOf(operation);
	            UMLOperation operation1 = null;
	            if (index != -1) {
	                operation1 = leftClass.getOperations().get(index);
	            }
	            if (index == -1 || differentParameterNames(leftClass, rightClass, operation1, operation))
	                classDiff.getAddedOperations().add(operation);
	        }
	        return classDiff;
	    }
	    return null;
	}

	private static boolean differentParameterNames(UMLClass leftClass, UMLClass rightClass, UMLOperation operation1, UMLOperation operation2) {
	    if (operation1 != null && operation2 != null && !operation1.getParameterNameList().equals(operation2.getParameterNameList())) {
	        int methodsWithIdenticalName1 = 0;
	        for (UMLOperation operation : leftClass.getOperations()) {
	            if (operation != operation1 && operation.getName().equals(operation1.getName()) && !operation.hasVarargsParameter()) {
	                methodsWithIdenticalName1++;
	            }
	        }
	        int methodsWithIdenticalName2 = 0;
	        for (UMLOperation operation : rightClass.getOperations()) {
	            if (operation != operation2 && operation.getName().equals(operation2.getName()) && !operation.hasVarargsParameter()) {
	                methodsWithIdenticalName2++;
	            }
	        }
	        if (methodsWithIdenticalName1 > 0 && methodsWithIdenticalName2 > 0) {
	            return true;
	        }
	    }
	    return false;
	}

	protected static boolean containsCallToExtractedMethod(UMLOperationBodyMapper bodyMapper, UMLAbstractClassDiff classDiff) {
	    if(classDiff != null) {
	        List<UMLOperation> addedOperations = classDiff.getAddedOperations();
	        for(AbstractCodeFragment leaf2 : bodyMapper.getNonMappedLeavesT2()) {
	            AbstractCall invocation = leaf2.invocationCoveringEntireFragment();
	            if(invocation == null) {
	                invocation = leaf2.assignmentInvocationCoveringEntireStatement();
	            }
	            UMLOperation matchingOperation = null;
	            if(invocation != null && (matchingOperation = matchesOperation(invocation, addedOperations, bodyMapper.getContainer2(), classDiff)) != null && matchingOperation.getBody() != null) {
	                return true;
	            }
	        }
	    }
	    return false;
	}

	private static UMLOperation matchesOperation(AbstractCall invocation, List<UMLOperation> operations, VariableDeclarationContainer callerOperation, UMLAbstractClassDiff classDiff) {
	    for(UMLOperation operation : operations) {
	        if(invocation.matchesOperation(operation, callerOperation, classDiff,null))
	            return operation;
	    }
	    return null;
	}

	protected static UMLOperationBodyMapper findBodyMapper(UMLModelDiff umlModelDiff, Method method, Version currentVersion, Version parentVersion) {
	    UMLClassBaseDiff umlClassDiff = getUMLClassDiff(umlModelDiff, method.getUmlOperation().getClassName());
	    if (umlClassDiff != null) {
	        for (UMLOperationBodyMapper operationBodyMapper : umlClassDiff.getOperationBodyMapperList()) {
	            Method methodLeft = Method.of(operationBodyMapper.getContainer1(), parentVersion);
	            if (method.equalIdentifierIgnoringVersion(methodLeft)) {
	                return operationBodyMapper;
	            }
	            Method methodRight = Method.of(operationBodyMapper.getContainer2(), currentVersion);
	            if (method.equalIdentifierIgnoringVersion(methodRight)) {
	                return operationBodyMapper;
	            }
	        }
	    }
	    return null;
	}

	protected static UMLClassBaseDiff getUMLClassDiff(UMLModelDiff umlModelDiff, String className) {
	    int maxMatchedMembers = 0;
	    UMLClassBaseDiff maxRenameDiff = null;
	    UMLClassBaseDiff sameNameDiff = null;
	    for (UMLClassBaseDiff classDiff : getAllClassesDiff(umlModelDiff)) {
	        if (classDiff.getOriginalClass().getName().equals(className) || classDiff.getNextClass().getName().equals(className)) {
	            if (classDiff instanceof UMLClassRenameDiff) {
	                UMLClassMatcher.MatchResult matchResult = ((UMLClassRenameDiff) classDiff).getMatchResult();
	                int matchedMembers = matchResult.getMatchedOperations() + matchResult.getMatchedAttributes();
	                if (matchedMembers > maxMatchedMembers) {
	                    maxMatchedMembers = matchedMembers;
	                    maxRenameDiff = classDiff;
	                }
	            }
	            else if (classDiff instanceof UMLClassMoveDiff) {
	                UMLClassMatcher.MatchResult matchResult = ((UMLClassMoveDiff) classDiff).getMatchResult();
	                int matchedMembers = matchResult.getMatchedOperations() + matchResult.getMatchedAttributes();
	                if (matchedMembers > maxMatchedMembers) {
	                    maxMatchedMembers = matchedMembers;
	                    maxRenameDiff = classDiff;
	                }
	            }
	            else {
	                sameNameDiff = classDiff;
	            }
	        }
	    }
	    return sameNameDiff != null ? sameNameDiff : maxRenameDiff;
	}

	protected static Pair<UMLModel, UMLModel> getUMLModelPair(final CommitModel commitModel, final String rightSideFileName, final Predicate<String> rightSideFileNamePredicate, final boolean filterLeftSide) throws Exception {
	    if (rightSideFileName == null)
	        throw new IllegalArgumentException("File name could not be null.");
	
	    if (filterLeftSide) {
	        String leftSideFileName = rightSideFileName;
	        if (commitModel.moveSourceFolderRefactorings != null) {
	            boolean found = false;
	            for (MoveSourceFolderRefactoring moveSourceFolderRefactoring : commitModel.moveSourceFolderRefactorings) {
	                if (found)
	                    break;
	                for (Map.Entry<String, String> identicalPath : moveSourceFolderRefactoring.getIdenticalFilePaths().entrySet()) {
	                    if (identicalPath.getValue().equals(rightSideFileName)) {
	                        leftSideFileName = identicalPath.getKey();
	                        found = true;
	                        break;
	                    }
	                }
	            }
	        }
	
	        final String leftSideFileNameFinal = leftSideFileName;
	        UMLModel leftSideUMLModel = GitHistoryRefactoringMinerImpl.createModel(commitModel.fileContentsBeforeOriginal.entrySet().stream().filter(map -> map.getKey().equals(leftSideFileNameFinal)).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)), commitModel.repositoryDirectoriesBefore);
	        UMLModel rightSideUMLModel = GitHistoryRefactoringMinerImpl.createModel(commitModel.fileContentsCurrentOriginal.entrySet().stream().filter(map -> map.getKey().equals(rightSideFileName)).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)), commitModel.repositoryDirectoriesCurrent);
	        optimizeUMLModelPair(leftSideUMLModel, rightSideUMLModel, rightSideFileName, commitModel.renamedFilesHint);
	        return Pair.of(leftSideUMLModel, rightSideUMLModel);
	    } else {
	        UMLModel leftSideUMLModel = GitHistoryRefactoringMinerImpl.createModel(commitModel.fileContentsBeforeTrimmed, commitModel.repositoryDirectoriesBefore);
	        UMLModel rightSideUMLModel = GitHistoryRefactoringMinerImpl.createModel(commitModel.fileContentsCurrentTrimmed, commitModel.repositoryDirectoriesCurrent);
	        optimizeUMLModelPair(leftSideUMLModel, rightSideUMLModel, rightSideFileName, commitModel.renamedFilesHint);
	        //remove from rightSideModel the classes not matching the rightSideFileNamePredicate
	        Set<UMLClass> rightClassesToBeRemoved = new HashSet<>();
	        for (UMLClass rightClass : rightSideUMLModel.getClassList()) {
	            if (!rightSideFileNamePredicate.test(rightClass.getSourceFile())) {
	                rightClassesToBeRemoved.add(rightClass);
	            }
	        }
	        rightSideUMLModel.getClassList().removeAll(rightClassesToBeRemoved);
	        return Pair.of(leftSideUMLModel, rightSideUMLModel);
	    }
	
	}

	private static void optimizeUMLModelPair(UMLModel leftSideUMLModel, UMLModel rightSideUMLModel, final String rightSideFileName, Map<String, String> renamedFilesHint) {
	    for (UMLClass leftClass : leftSideUMLModel.getClassList()) {
	        UMLClass rightClass = rightSideUMLModel.getClass(leftClass);
	        if (rightClass == null && renamedFilesHint.containsKey(leftClass.getSourceFile()) && !renamedFilesHint.get(leftClass.getSourceFile()).equals(rightSideFileName)) {
	            String rightSideFile = renamedFilesHint.get(leftClass.getSourceFile());
	            List<UMLClass> matchingRightClasses = new ArrayList<>();
	            for (UMLClass c : rightSideUMLModel.getClassList()) {
	                if (c.getSourceFile().equals(rightSideFile)) {
	                    matchingRightClasses.add(c);
	                }
	            }
	            if (matchingRightClasses.size() == 1) {
	                rightClass = matchingRightClasses.get(0);
	            }
	            else if (matchingRightClasses.size() > 1) {
	                for (UMLClass c : matchingRightClasses) {
	                    if (c.getName().equals(leftClass.getName())) {
	                        rightClass = c;
	                        break;
	                    }
	                }
	            }
	        }
	        if (rightClass != null) {
	            List<UMLOperation> leftOperationsToBeRemoved = new ArrayList<>();
	            List<UMLOperation> rightOperationsToBeRemoved = new ArrayList<>();
	            for (UMLOperation leftOperation : leftClass.getOperations()) {
	                int index = rightClass.getOperations().indexOf(leftOperation);
	                if (index != -1) {
	                    UMLOperation rightOperation = rightClass.getOperations().get(index);
	                    if (leftOperation.getBodyHashCode() == rightOperation.getBodyHashCode()) {
	                        leftOperationsToBeRemoved.add(leftOperation);
	                        rightOperationsToBeRemoved.add(rightOperation);
	                    }
	                }
	            }
	            leftClass.getOperations().removeAll(leftOperationsToBeRemoved);
	            rightClass.getOperations().removeAll(rightOperationsToBeRemoved);
	            List<UMLAttribute> leftAttributesToBeRemoved = new ArrayList<>();
	            List<UMLAttribute> rightAttributesToBeRemoved = new ArrayList<>();
	            for (UMLAttribute leftAttribute : leftClass.getAttributes()) {
	                int index = rightClass.getAttributes().indexOf(leftAttribute);
	                if (index != -1) {
	                    UMLAttribute rightAttribute = rightClass.getAttributes().get(index);
	                    leftAttributesToBeRemoved.add(leftAttribute);
	                    rightAttributesToBeRemoved.add(rightAttribute);
	                }
	            }
	            leftClass.getAttributes().removeAll(leftAttributesToBeRemoved);
	            rightClass.getAttributes().removeAll(rightAttributesToBeRemoved);
	        }
	    }
	    leftSideUMLModel.setPartial(true);
	    rightSideUMLModel.setPartial(true);
	}

	protected static boolean isNewlyAddedFile(CommitModel commitModel, String currentMethodFilePath) {
	    return commitModel.fileContentsCurrentTrimmed.containsKey(currentMethodFilePath) && !commitModel.fileContentsBeforeTrimmed.containsKey(currentMethodFilePath) && commitModel.renamedFilesHint.values().stream().noneMatch(s -> s.equals(currentMethodFilePath));
	}

	protected static Set<String> getRightSideFileNames(Method currentMethod, CommitModel commitModel, UMLModelDiff umlModelDiff) {
	    String currentFilePath = currentMethod.getFilePath();
	    String currentClassName = currentMethod.getUmlOperation().getClassName();
	    Set<String> toBeAddedFileNamesIfTheyAreNewFiles = new HashSet<>();
	    if (currentMethod.getUmlOperation() instanceof UMLOperation) {
	        UMLOperation operation = (UMLOperation) currentMethod.getUmlOperation();
	        UMLParameter returnParameter = operation.getReturnParameter();
	        if (returnParameter != null) {
	            String parameterType = returnParameter.getType().getClassType();
	            if (!"void".equals(parameterType)) {
	                toBeAddedFileNamesIfTheyAreNewFiles.add(parameterType + ".java");
	            }
	        }
	    }
	    for (UMLType parameter : currentMethod.getUmlOperation().getParameterTypeList()) {
	        String parameterType = parameter.getClassType();
	        if ("void".equals(parameterType))
	            continue;
	        toBeAddedFileNamesIfTheyAreNewFiles.add(parameterType + ".java");
	    }
	    Set<String> rightSideFileNames = getRightSideFileNames(currentFilePath, currentClassName, toBeAddedFileNamesIfTheyAreNewFiles, commitModel, umlModelDiff);
		//add all right side files having a main method
	    if (currentMethod.getUmlOperation().isMain()) {
	    	for (String filePath : commitModel.fileContentsCurrentOriginal.keySet()) {
	    		String fileContents = commitModel.fileContentsCurrentOriginal.get(filePath);
	    		if (fileContents.contains("public static void main(String")) {
	    			rightSideFileNames.add(filePath);
	    		}
	    	}
		}
	    return rightSideFileNames;
	}

	protected static Set<String> getRightSideFileNames(String currentFilePath, String currentClassName, Set<String> toBeAddedFileNamesIfTheyAreNewFiles, CommitModel commitModel, UMLModelDiff umlModelDiff) {
	    Set<String> fileNames = new HashSet<>();
	    fileNames.add(currentFilePath);
	    UMLAbstractClass classInChildModel = umlModelDiff.findClassInChildModel(currentClassName);
	    boolean newlyAddedFile = isNewlyAddedFile(commitModel, currentFilePath);
	    if (classInChildModel instanceof UMLClass) {
	        UMLClass umlClass = (UMLClass) classInChildModel;
	
	        StringBuilder regxSb = new StringBuilder();
	
	        String orChar = "";
	        if (umlClass.getSuperclass() != null) {
	            regxSb.append(orChar).append("\\s*extends\\s*").append(umlClass.getSuperclass().getClassType());
	            orChar = "|";
	            if (newlyAddedFile) {
	                regxSb.append(orChar).append("\\s*class\\s*").append(umlClass.getSuperclass().getClassType()).append("\\s\\s*");
	            }
	        }
	
	        for (UMLType implementedInterface : umlClass.getImplementedInterfaces()) {
	            regxSb.append(orChar).append("\\s*implements\\s*.*").append(implementedInterface).append("\\s*");
	            orChar = "|";
	            if (newlyAddedFile) {
	                regxSb.append(orChar).append("\\s*interface\\s*").append(implementedInterface.getClassType()).append("\\s*\\{");
	            }
	        }
	
	        //newly added file
	        if (newlyAddedFile) {
	            regxSb.append(orChar).append("@link\\s*").append(umlClass.getNonQualifiedName());
	            orChar = "|";
	            regxSb.append(orChar).append("new\\s*").append(umlClass.getNonQualifiedName()).append("\\(");
	            regxSb.append(orChar).append("@deprecated\\s*.*").append(umlClass.getNonQualifiedName()).append("\\s*.*\n");
	            regxSb.append(orChar).append("\\s*extends\\s*").append(umlClass.getNonQualifiedName()).append("\\s*\\{");
	        }
	
	        String regx = regxSb.toString();
	        if (!regx.isEmpty()) {
	            Pattern pattern = Pattern.compile(regx);
	            for (Map.Entry<String, String> entry : commitModel.fileContentsCurrentTrimmed.entrySet()) {
	                Matcher matcher = pattern.matcher(entry.getValue());
	                if (matcher.find()) {
	                    String matcherGroup = matcher.group().trim();
	                    String filePath = entry.getKey();
	                    boolean isAnExistingFile = commitModel.fileContentsBeforeTrimmed.containsKey(filePath) || commitModel.renamedFilesHint.values().stream().anyMatch(s -> s.equals(filePath));
	                    if (matcherGroup.startsWith("extends") && matcherGroup.contains(umlClass.getNonQualifiedName())) {
	                        if (isAnExistingFile) {
	                            fileNames.add(filePath);
	                        }
	                    } else if (matcherGroup.startsWith("implements") || matcherGroup.startsWith("extends")) {
	                        if (isAnExistingFile) {
	                            String[] split = matcherGroup.split("\\s");
	                            String className = split[split.length - 1];
	                            if (className.contains(".")) {
	                                className = className.substring(0, className.indexOf("."));
	                            }
	                            String[] tokens = CAMEL_CASE_SPLIT_PATTERN.split(className);
	                            final String fileName = className + ".java";
	                            if (commitModel.fileContentsCurrentTrimmed.keySet().stream().anyMatch(s -> s.endsWith(fileName) || s.endsWith(tokens[tokens.length - 1] + ".java"))) {
	                                fileNames.add(filePath);
	                            }
	                        }
	                    } else if (matcherGroup.startsWith("new")) {
	                        if (isAnExistingFile) {
	                            fileNames.add(filePath);
	                        }
	                    } else if (matcherGroup.startsWith("@link")) {
	                        fileNames.add(filePath); //TODO: add existing file condition and test
	                    } else if (matcherGroup.startsWith("class")) {
	                        if (isAnExistingFile) {
	                            fileNames.add(filePath);
	                        }
	                    } else if (matcherGroup.startsWith("@deprecated")) {
	                        if (isAnExistingFile) {
	                            fileNames.add(filePath);
	                        }
	                    } else if (matcherGroup.startsWith("interface")) {
	                        if (isAnExistingFile) {
	                            fileNames.add(filePath);
	                        }
	                    }
	
	                }
	            }
	        }
	        if (!umlClass.isTopLevel()) {
	            fileNames.addAll(getRightSideFileNames(currentFilePath, umlClass.getPackageName(), toBeAddedFileNamesIfTheyAreNewFiles, commitModel, umlModelDiff));
	        }
	    }
	
	
	    fileNames.addAll(
	            commitModel.fileContentsCurrentTrimmed.keySet().stream()
	                    .filter(filePath -> toBeAddedFileNamesIfTheyAreNewFiles.stream().anyMatch(filePath::endsWith))
	                    .filter(filePath -> isNewlyAddedFile(commitModel, filePath))
	                    .collect(Collectors.toSet())
	    );
	
	    if (newlyAddedFile) {
	        final String currentMethodFileName = currentFilePath.substring(currentFilePath.lastIndexOf("/"));
	        fileNames.addAll(commitModel.fileContentsCurrentTrimmed.keySet().stream().filter(filePath -> filePath.endsWith(currentMethodFileName)).collect(Collectors.toSet()));
	    }
	
	    return fileNames;
	}

	protected static boolean isClassAdded(UMLModelDiff modelDiff, String className) {
		UMLClass addedClass = modelDiff.getAddedClass(className);
		if (addedClass != null) {
			return true;
		}
		return false;
	}

	protected static boolean isMethodAdded(UMLModelDiff modelDiff, String className, Predicate<Method> equalOperator, Consumer<Method> addedMethodHandler, Version currentVersion) {
	    List<UMLOperation> addedOperations = getAllClassesDiff(modelDiff)
	            .stream()
	            .map(UMLClassBaseDiff::getAddedOperations)
	            .flatMap(List::stream)
	            .collect(Collectors.toList());
	    for (UMLOperation operation : addedOperations) {
	        if (isMethodAdded(operation, equalOperator, addedMethodHandler, currentVersion))
	            return true;
	    }
	
	    UMLClass addedClass = modelDiff.getAddedClass(className);
	    if (addedClass != null) {
	        for (UMLOperation operation : addedClass.getOperations()) {
	            if (isMethodAdded(operation, equalOperator, addedMethodHandler, currentVersion))
	                return true;
	        }
	    }
	
	    for (UMLClassRenameDiff classRenameDiffList : modelDiff.getClassRenameDiffList()) {
	        for (UMLAnonymousClass addedAnonymousClasses : classRenameDiffList.getAddedAnonymousClasses()) {
	            for (UMLOperation operation : addedAnonymousClasses.getOperations()) {
	                if (isMethodAdded(operation, equalOperator, addedMethodHandler, currentVersion))
	                    return true;
	            }
	        }
	    }
	    return false;
	}

	protected static boolean isMethodAdded(UMLOperation addedOperation, Predicate<Method> equalOperator, Consumer<Method> addedMethodHandler, Version currentVersion) {
	    Method rightMethod = Method.of(addedOperation, currentVersion);
	    if (equalOperator.test(rightMethod)) {
	        addedMethodHandler.accept(rightMethod);
	        return true;
	    }
	    return false;
	}

	protected static BaseCodeElement getCodeElement(UMLModel umlModel, Version version, BaseCodeElement current) {
		if (current instanceof Attribute) {
			return getAttribute(umlModel, version, current::equalIdentifierIgnoringVersion);
		}
		return current;
	}

	protected static Method getMethod(UMLModel umlModel, Version version, Predicate<Method> predicate) {
	    if (umlModel != null)
	        for (UMLClass umlClass : umlModel.getClassList()) {
	            Method method = getMethod(version, predicate, umlClass.getOperations());
	            if (method != null) return method;
	            for (UMLAnonymousClass anonymousClass : umlClass.getAnonymousClassList()) {
	                method = getMethod(version, predicate, anonymousClass.getOperations());
	                if (method != null) return method;
	            }
	        }
	    return null;
	}

	protected static Class getClass(UMLModel umlModel, Version version, Predicate<Class> predicate) {
	    if (umlModel != null)
	        for (UMLClass umlClass : umlModel.getClassList()) {
	            Class clazz = Class.of(umlClass, version);
	            if (predicate.test(clazz))
		            return clazz;
	        }
	    return null;
	}

	private static Method getMethod(Version version, Predicate<Method> predicate, List<UMLOperation> operations) {
	    for (UMLOperation umlOperation : operations) {
	        Method method = Method.of(umlOperation, version);
	        if (predicate.test(method))
	            return method;
	    }
	    return null;
	}

    protected static Attribute getAttribute(UMLModel umlModel, Version version, Predicate<Attribute> predicate) {
        if (umlModel != null)
            for (UMLClass umlClass : umlModel.getClassList()) {
                Attribute attribute = getAttribute(version, predicate, umlClass.getAttributes());
                if (attribute != null) return attribute;
                attribute = getAttribute(version, predicate, umlClass.getEnumConstants());
                if (attribute != null) return attribute;
                for (UMLAnonymousClass anonymousClass : umlClass.getAnonymousClassList()) {
                    attribute = getAttribute(version, predicate, anonymousClass.getAttributes());
                    if (attribute != null) return attribute;
                    attribute = getAttribute(version, predicate, anonymousClass.getEnumConstants());
                    if (attribute != null) return attribute;
                }
            }
        return null;
    }

    private static Attribute getAttribute(Version version, Predicate<Attribute> predicate, List<? extends UMLAttribute> attributes) {
        for (UMLAttribute umlAttribute : attributes) {
            Attribute attribute = Attribute.of(umlAttribute, version);
            if (predicate.test(attribute))
                return attribute;
        }
        return null;
    }

	public static List<UMLClassBaseDiff> getAllClassesDiff(UMLModelDiff modelDiff) {
	    List<UMLClassBaseDiff> allClassesDiff = new ArrayList<>();
	    allClassesDiff.addAll(modelDiff.getCommonClassDiffList());
	    allClassesDiff.addAll(modelDiff.getClassMoveDiffList());
	    allClassesDiff.addAll(modelDiff.getInnerClassMoveDiffList());
	    allClassesDiff.addAll(modelDiff.getClassRenameDiffList());
	    return allClassesDiff;
	}

	public static class CommitModel {
		public final String parentCommitId;
	    public final Set<String> repositoryDirectoriesBefore;
	    public final Map<String, String> fileContentsBeforeOriginal;
	    public final Map<String, String> fileContentsBeforeTrimmed;
	
	    public final Set<String> repositoryDirectoriesCurrent;
	    public final Map<String, String> fileContentsCurrentOriginal;
	    public final Map<String, String> fileContentsCurrentTrimmed;
	
	    public final Map<String, String> renamedFilesHint;
	    public final List<MoveSourceFolderRefactoring> moveSourceFolderRefactorings;
	
	    public CommitModel(String parentCommitId, Set<String> repositoryDirectoriesBefore, Map<String, String> fileContentsBeforeOriginal, Map<String, String> fileContentsBeforeTrimmed, Set<String> repositoryDirectoriesCurrent, Map<String, String> fileContentsCurrentOriginal, Map<String, String> fileContentsCurrentTrimmed, Map<String, String> renamedFilesHint, List<MoveSourceFolderRefactoring> moveSourceFolderRefactorings) {
	        this.parentCommitId = parentCommitId;
	        this.repositoryDirectoriesBefore = repositoryDirectoriesBefore;
	        this.fileContentsBeforeOriginal = fileContentsBeforeOriginal;
	        this.fileContentsBeforeTrimmed = fileContentsBeforeTrimmed;
	        this.repositoryDirectoriesCurrent = repositoryDirectoriesCurrent;
	        this.fileContentsCurrentOriginal = fileContentsCurrentOriginal;
	        this.fileContentsCurrentTrimmed = fileContentsCurrentTrimmed;
	        this.renamedFilesHint = renamedFilesHint;
	        this.moveSourceFolderRefactorings = moveSourceFolderRefactorings;
	    }
	}

	public List<UMLClassMoveDiff> getClassMoveDiffList(UMLModelDiff umlModelDiff) {
	    List<UMLClassMoveDiff> allMoveClassesDiff = new ArrayList<>();
	    allMoveClassesDiff.addAll(umlModelDiff.getClassMoveDiffList());
	    allMoveClassesDiff.addAll(umlModelDiff.getInnerClassMoveDiffList());
	    return allMoveClassesDiff;
	}

	public static class ModelDiff {
	    public final UMLModelDiff umlModelDiff;
	    public final List<String> filePathsBefore;
	    public final List<String> filePathsCurrent;
	    public final Map<String, String> renamedFilesHint;
	    public final List<MoveSourceFolderRefactoring> moveSourceFolderRefactorings;
	
	    public ModelDiff(UMLModelDiff umlModelDiff, List<String> filePathsBefore, List<String> filePathsCurrent, Map<String, String> renamedFilesHint, List<MoveSourceFolderRefactoring> moveSourceFolderRefactorings) {
	        this.umlModelDiff = umlModelDiff;
	        this.filePathsBefore = filePathsBefore;
	        this.filePathsCurrent = filePathsCurrent;
	        this.renamedFilesHint = renamedFilesHint;
	        this.moveSourceFolderRefactorings = moveSourceFolderRefactorings;
	    }
	}
}