package org.codetracker;

import java.io.BufferedReader;
import java.io.File;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.codetracker.api.CodeElement;
import org.codetracker.api.CodeElementNotFoundException;
import org.codetracker.api.Version;
import org.codetracker.api.History.HistoryInfo;
import org.codetracker.change.Change;
import org.codetracker.change.ChangeFactory;
import org.codetracker.element.Annotation;
import org.codetracker.element.Attribute;
import org.codetracker.element.BaseCodeElement;
import org.codetracker.element.Block;
import org.codetracker.element.Class;
import org.codetracker.element.Comment;
import org.codetracker.element.Import;
import org.codetracker.element.Method;
import org.codetracker.element.Package;
import org.codetracker.util.CodeElementLocatorWithLocalFiles;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringMinerTimedOutException;
import org.refactoringminer.api.RefactoringType;
import org.refactoringminer.rm1.GitHistoryRefactoringMinerImpl;

import gr.uom.java.xmi.UMLAttribute;
import gr.uom.java.xmi.UMLClass;
import gr.uom.java.xmi.UMLEnumConstant;
import gr.uom.java.xmi.UMLInitializer;
import gr.uom.java.xmi.UMLJavadoc;
import gr.uom.java.xmi.UMLModel;
import gr.uom.java.xmi.UMLOperation;
import gr.uom.java.xmi.UMLType;
import gr.uom.java.xmi.VariableDeclarationContainer;
import gr.uom.java.xmi.LocationInfo.CodeElementType;
import gr.uom.java.xmi.UMLAbstractClass;
import gr.uom.java.xmi.UMLAnonymousClass;
import gr.uom.java.xmi.decomposition.OperationBody;
import gr.uom.java.xmi.decomposition.UMLOperationBodyMapper;
import gr.uom.java.xmi.diff.ExtractClassRefactoring;
import gr.uom.java.xmi.diff.ExtractOperationRefactoring;
import gr.uom.java.xmi.diff.ExtractSuperclassRefactoring;
import gr.uom.java.xmi.diff.MoveOperationRefactoring;
import gr.uom.java.xmi.diff.RenameAttributeRefactoring;
import gr.uom.java.xmi.diff.UMLAbstractClassDiff;
import gr.uom.java.xmi.diff.UMLAttributeDiff;
import gr.uom.java.xmi.diff.UMLClassBaseDiff;
import gr.uom.java.xmi.diff.UMLDocumentationDiffProvider;
import gr.uom.java.xmi.diff.UMLEnumConstantDiff;
import gr.uom.java.xmi.diff.UMLModelDiff;

public class FileTrackerWithLocalFilesImpl extends BaseTrackerWithLocalFiles {
	private final Map<String, UMLModelDiff> modelDiffCache = new HashMap<>();
	private final List<String> lines = new ArrayList<>();
	private final Map<CodeElement, AbstractChangeHistory<? extends BaseCodeElement>> programElementMap = new LinkedHashMap<>();
	private final Map<CodeElement, AbstractChangeHistory<? extends BaseCodeElement>> nestedProgramElementMap = new LinkedHashMap<>();
	private final Map<Integer, HistoryInfo<? extends BaseCodeElement>> blameInfo = new LinkedHashMap<>();

	public FileTrackerWithLocalFilesImpl(String cloneURL, String startCommitId, String filePath) {
		super(cloneURL, startCommitId, filePath);
	}

	public List<String> getLines() {
		return lines;
	}

	public Map<Integer, HistoryInfo<? extends BaseCodeElement>> getBlameInfo() {
		return blameInfo;
	}

	private static AbstractChangeHistory<? extends BaseCodeElement> factory(CodeElement codeElement) {
		CodeElementType type = codeElement.getLocation().getCodeElementType();
		int startLine = codeElement.getLocation().getStartLine();
		int endLine = codeElement.getLocation().getEndLine();
		switch (codeElement.getClass().getSimpleName()) {
		case "Class":
			String className = ((Class) codeElement).getUmlClass().getNonQualifiedName();
			ClassTrackerChangeHistory classTrackerChangeHistory = new ClassTrackerChangeHistory(className, startLine);
			classTrackerChangeHistory.setStart((Class) codeElement);
			return classTrackerChangeHistory;
		case "Method":
			String methodName = ((Method) codeElement).getUmlOperation().getName();
			MethodTrackerChangeHistory methodTrackerChangeHistory = new MethodTrackerChangeHistory(methodName, startLine);
			methodTrackerChangeHistory.setStart((Method) codeElement);
			return methodTrackerChangeHistory;
		case "Attribute":
			String attrName = ((Attribute) codeElement).getUmlAttribute().getName();
			AttributeTrackerChangeHistory attributeTrackerChangeHistory = new AttributeTrackerChangeHistory(attrName, startLine);
			attributeTrackerChangeHistory.setStart((Attribute) codeElement);
			return attributeTrackerChangeHistory;
		case "Block":
			Block block = (Block) codeElement;
			BlockTrackerChangeHistory blockTrackerChangeHistory = new BlockTrackerChangeHistory(block.getOperation().getName(), block.getOperation().getLocationInfo().getStartLine(), type, startLine, endLine);
			blockTrackerChangeHistory.setStart(block);
			return blockTrackerChangeHistory;
		case "Comment":
			Comment comment = (Comment) codeElement;
			String containerName = null;
			int containerStartLine = 0;
			if (comment.getOperation().isPresent()) {
				containerName = comment.getOperation().get().getName();
				containerStartLine = comment.getOperation().get().getLocationInfo().getStartLine();
			}
			else if (comment.getClazz().isPresent()) {
				containerName = comment.getClazz().get().getName();
				containerStartLine = comment.getClazz().get().getLocationInfo().getStartLine();
			}
			CommentTrackerChangeHistory commentTrackerChangeHistory = new CommentTrackerChangeHistory(containerName, containerStartLine, type, startLine, endLine);
			commentTrackerChangeHistory.setStart(comment);
			return commentTrackerChangeHistory;
		case "Annotation":
			Annotation annotation = (Annotation) codeElement;
			String annotationContainerName = null;
			int annotationContainerStartLine = 0;
			if (annotation.getOperation().isPresent()) {
				annotationContainerName = annotation.getOperation().get().getName();
				annotationContainerStartLine = annotation.getOperation().get().getLocationInfo().getStartLine();
			}
			else if (annotation.getClazz().isPresent()) {
				annotationContainerName = annotation.getClazz().get().getName();
				annotationContainerStartLine = annotation.getClazz().get().getLocationInfo().getStartLine();
			}
			AnnotationTrackerChangeHistory annotationTrackerChangeHistory = new AnnotationTrackerChangeHistory(annotationContainerName, annotationContainerStartLine, type, startLine, endLine);
			annotationTrackerChangeHistory.setStart(annotation);
			return annotationTrackerChangeHistory;
		case "Import":
			Import imp = (Import) codeElement;
			ImportTrackerChangeHistory importTrackerChangeHistory = new ImportTrackerChangeHistory(imp.getClazz().getName(), imp.getClazz().getLocationInfo().getStartLine(), type, startLine, endLine);
			importTrackerChangeHistory.setStart(imp);
			return importTrackerChangeHistory;
		case "Package":
			Package pack = (Package) codeElement;
			return new ClassTrackerChangeHistory(pack.getUmlClass().getNonQualifiedName(), startLine);
		}
		return null;
	}

	private Set<CodeElement> remainingBlames(Method startMethod) {
		Set<CodeElement> remaining = new LinkedHashSet<CodeElement>();
		for (CodeElement key2 : programElementMap.keySet()) {
			if (key2 instanceof Block) {
				Block startBlock = (Block)key2;
				BlockTrackerChangeHistory startBlockChangeHistory = (BlockTrackerChangeHistory) programElementMap.get(startBlock);
				if (startBlock.getOperation().equals(startMethod.getUmlOperation())) {
					HistoryInfo<Block> historyInfo = startBlockChangeHistory.blameReturn(startBlock);
					if (historyInfo == null) {
						remaining.add(startBlock);
					}
				}
			}
			else if (key2 instanceof Comment) {
				Comment startComment = (Comment)key2;
				CommentTrackerChangeHistory startCommentChangeHistory = (CommentTrackerChangeHistory) programElementMap.get(startComment);
				if (startComment.getOperation().isPresent() && startComment.getOperation().get().equals(startMethod.getUmlOperation())) {
					HistoryInfo<Comment> historyInfo = startCommentChangeHistory.blameReturn();
					if (historyInfo == null) {
						remaining.add(startComment);
					}
				}
			}
		}
		return remaining;
	}

	public void blame() throws Exception {
		CommitModel startModel = getCommitModel(startCommitId);
		Version startVersion = new VersionImpl(startCommitId, startModel.commitTime, startModel.authoredTime, startModel.commitAuthorName);
        Set<String> startFileNames = Collections.singleton(filePath);
    	Map<String, String> fileContents = new LinkedHashMap<>();
    	for(String rightFileName : startFileNames) {
    		fileContents.put(rightFileName, startModel.fileContentsCurrentOriginal.get(rightFileName));
    	}
    	UMLModel umlModel = GitHistoryRefactoringMinerImpl.createModel(fileContents, startModel.repositoryDirectoriesCurrent);
    	umlModel.setPartial(true);
		// extract program elements to be blamed
		String fileContentAsString = fileContents.get(filePath);
		Map<Integer, CodeElement> lineNumberToCodeElementMap = new LinkedHashMap<>();
		Class startClass = null;
		try (BufferedReader reader = new BufferedReader(new StringReader(fileContentAsString))) {
			String line;
			int lineNumber = 1;
			while ((line = reader.readLine()) != null) {
				lines.add(line);
				CodeElementLocatorWithLocalFiles locator = new CodeElementLocatorWithLocalFiles(cloneURL, startCommitId, filePath, lineNumber);
				CodeElement codeElement = null;
				try {
					codeElement = locator.locateWithoutName(startVersion, umlModel);
				} catch (CodeElementNotFoundException e) {}
				if (codeElement != null && !StringUtils.isBlank(line)) {
					AbstractChangeHistory<BaseCodeElement> changeHistory = (AbstractChangeHistory<BaseCodeElement>) factory(codeElement);
					changeHistory.addFirst((BaseCodeElement) codeElement);
					changeHistory.get().addNode((BaseCodeElement) codeElement);
					programElementMap.put(codeElement, changeHistory);
					if (startClass == null && codeElement instanceof Class) {
						startClass = (Class)codeElement;
						startClass.setStart(true);
					}
					lineNumberToCodeElementMap.put(lineNumber, codeElement);
				}
				lineNumber++;
			}
		}
		HashSet<String> analysedCommits = new HashSet<>();
		List<String> commits = null;
		String lastFileName = null;
		String startFilePath = startClass.getFilePath();
		ClassTrackerChangeHistory startClassChangeHistory = (ClassTrackerChangeHistory) programElementMap.get(startClass);
		while (!startClassChangeHistory.isEmpty()) {
			Class currentClass = startClassChangeHistory.poll();
			
			if (currentClass.isAdded()) {
				commits = null;
				continue;
			}
			final String currentClassFilePath = currentClass.getFilePath();
			if (commits == null || !currentClass.getFilePath().equals(lastFileName)) {
				lastFileName = currentClass.getFilePath();
				String repoName = cloneURL.substring(cloneURL.lastIndexOf('/') + 1, cloneURL.lastIndexOf('.'));
        		String className = startFilePath.substring(startFilePath.lastIndexOf("/") + 1);
        		className = className.endsWith(".java") ? className.substring(0, className.length()-5) : className;
                String jsonPath = System.getProperty("user.dir") + "/src/test/resources/class/" + repoName + "-" + className + ".json";
                File jsonFile = new File(jsonPath);
                commits = getCommits(currentClass.getVersion().getId(), jsonFile);
				analysedCommits.clear();
			}
			if (analysedCommits.containsAll(commits))
				break;
			for (String commitId : commits) {
				if (analysedCommits.contains(commitId))
					continue;
				analysedCommits.add(commitId);

				CommitModel lightCommitModel = getLightCommitModel(commitId, currentClassFilePath);
                String parentCommitId = lightCommitModel.parentCommitId;
                Version currentVersion = new VersionImpl(commitId, lightCommitModel.commitTime, lightCommitModel.authoredTime, lightCommitModel.commitAuthorName);
                Version parentVersion = new VersionImpl(parentCommitId, 0, 0, "");

                UMLModel leftModel = GitHistoryRefactoringMinerImpl.createModel(lightCommitModel.fileContentsBeforeOriginal, lightCommitModel.repositoryDirectoriesBefore);
            	leftModel.setPartial(true);
            	UMLModel rightModel = GitHistoryRefactoringMinerImpl.createModel(lightCommitModel.fileContentsCurrentOriginal, lightCommitModel.repositoryDirectoriesCurrent);
            	rightModel.setPartial(true);
				Class rightClass = getClass(rightModel, currentVersion, currentClass::equalIdentifierIgnoringVersion);
				if (rightClass == null) {
					continue;
				}
				String rightClassName = rightClass.getUmlClass().getName();
                String rightClassSourceFolder = rightClass.getUmlClass().getLocationInfo().getSourceFolder();
				if ("0".equals(parentCommitId)) {
					Class leftClass = Class.of(rightClass.getUmlClass(), parentVersion);
					startClassChangeHistory.get().handleAdd(leftClass, rightClass, "Initial commit!");
					startClassChangeHistory.get().connectRelatedNodes();
					startClassChangeHistory.add(leftClass);
					for (CodeElement key : programElementMap.keySet()) {
						AbstractChangeHistory<BaseCodeElement> changeHistory = (AbstractChangeHistory<BaseCodeElement>) programElementMap.get(key);
						BaseCodeElement codeElement = changeHistory.poll();
						if (codeElement == null) {
							codeElement = changeHistory.getCurrent();
						}
						if (codeElement != null && !codeElement.isAdded()) {
							BaseCodeElement right = getCodeElement(rightModel, currentVersion, codeElement);
							if (right == null) {
								continue;
							}
							BaseCodeElement left = right.of(parentVersion);
							changeHistory.get().handleAdd(left, right, "Initial commit!");
							changeHistory.get().connectRelatedNodes();
							changeHistory.add(left);
						}
					}
					break;
				}

				Class leftClass = getClass(leftModel, parentVersion, rightClass::equalIdentifierIgnoringVersion);
				boolean annotationChanged = false;
				if(leftClass == null) {
					leftClass = getClass(leftModel, parentVersion, rightClass::equalIdentifierIgnoringVersionAndAnnotation);
					if(leftClass != null)
						annotationChanged = true;
				}
				boolean modifiersChanged = false;
				if(leftClass == null) {
					leftClass = getClass(leftModel, parentVersion, rightClass::equalIdentifierIgnoringVersionAndModifiers);
					if(leftClass != null)
						modifiersChanged = true;
				}
				// No class signature change
				if (leftClass != null && !annotationChanged && !modifiersChanged) {
					UMLType leftSuperclass = leftClass.getUmlClass().getSuperclass();
					UMLType rightSuperclass = rightClass.getUmlClass().getSuperclass();
					if (leftSuperclass != null && rightSuperclass != null) {
                		if (!leftSuperclass.equals(rightSuperclass)) {
                			startClassChangeHistory.get().addChange(leftClass, rightClass, ChangeFactory.forClass(Change.Type.SUPERCLASS_CHANGE));
                			startClassChangeHistory.get().connectRelatedNodes();
                		}
                	}
					else if (leftSuperclass != null && rightSuperclass == null) {
						startClassChangeHistory.get().addChange(leftClass, rightClass, ChangeFactory.forClass(Change.Type.SUPERCLASS_CHANGE));
						startClassChangeHistory.get().connectRelatedNodes();
					}
					else if (leftSuperclass == null && rightSuperclass != null) {
						startClassChangeHistory.get().addChange(leftClass, rightClass, ChangeFactory.forClass(Change.Type.SUPERCLASS_CHANGE));
						startClassChangeHistory.get().connectRelatedNodes();
					}
					if (!leftClass.getUmlClass().getImplementedInterfaces().equals(rightClass.getUmlClass().getImplementedInterfaces())) {
						startClassChangeHistory.get().addChange(leftClass, rightClass, ChangeFactory.forClass(Change.Type.INTERFACE_LIST_CHANGE));
						startClassChangeHistory.get().connectRelatedNodes();
					}
					checkSignatureFormatChange(startClassChangeHistory, leftClass, rightClass);
					Map<Method, MethodTrackerChangeHistory> notFoundMethods = processMethodsWithSameSignature(rightModel, currentVersion, leftModel, parentVersion);
					Map<Attribute, AttributeTrackerChangeHistory> notFoundAttributes = processAttributesWithSameSignature(rightModel, currentVersion, leftModel, parentVersion);
					Map<Class, ClassTrackerChangeHistory> notFoundInnerClasses = new LinkedHashMap<>();
					Set<Pair<Class, Class>> foundInnerClasses = new LinkedHashSet<>();
					processInnerClassesWithSameSignature(rightModel, currentVersion, leftModel, parentVersion, startClass, foundInnerClasses, notFoundInnerClasses);
					if (notFoundMethods.size() > 0 || notFoundAttributes.size() > 0 || notFoundInnerClasses.size() > 0) {
						UMLModelDiff umlModelDiffLocal = null;
						if(modelDiffCache.containsKey(currentVersion.getId())) {
							umlModelDiffLocal = modelDiffCache.get(currentVersion.getId());
						}
						else {
							umlModelDiffLocal = leftModel.diff(rightModel);
							modelDiffCache.put(currentVersion.getId(), umlModelDiffLocal);
						}
						List<Refactoring> refactorings = umlModelDiffLocal.getRefactorings();
						processLocallyRefactoredMethods(notFoundMethods, umlModelDiffLocal, currentVersion, parentVersion, refactorings);
						processLocallyRefactoredAttributes(notFoundAttributes, umlModelDiffLocal, currentVersion, parentVersion, refactorings);
						processLocallyRefactoredInnerClasses(notFoundInnerClasses, umlModelDiffLocal, currentVersion, parentVersion, refactorings);
					}
					UMLClassBaseDiff lightweightClassDiff = lightweightClassDiff(leftClass.getUmlClass(), rightClass.getUmlClass());
					processImportsAndClassComments(lightweightClassDiff, rightClass, currentVersion, parentVersion, Collections.emptyList());
					for (Pair<Class, Class> pair : foundInnerClasses) {
						UMLClassBaseDiff lightweightInnerClassDiff = lightweightClassDiff(pair.getLeft().getUmlClass(), pair.getRight().getUmlClass());
						processImportsAndClassComments(lightweightInnerClassDiff, pair.getRight(), currentVersion, parentVersion, Collections.emptyList());
					}
					if(nestedProgramElementMap.size() > 0) {
						programElementMap.putAll(nestedProgramElementMap);
						nestedProgramElementMap.clear();
					}
					continue;
				}
				else if (leftClass != null && (annotationChanged || modifiersChanged)) {
					UMLModelDiff umlModelDiffLocal = leftModel.diff(rightModel);
					List<Refactoring> refactorings = umlModelDiffLocal.getRefactorings();
					Set<Class> classRefactored = startClassChangeHistory.analyseClassRefactorings(refactorings, currentVersion, parentVersion, rightClass::equalIdentifierIgnoringVersion);
					boolean refactored = !classRefactored.isEmpty();
					if (refactored) {
						UMLType leftSuperclass = leftClass.getUmlClass().getSuperclass();
						UMLType rightSuperclass = rightClass.getUmlClass().getSuperclass();
						if (leftSuperclass != null && rightSuperclass != null) {
                    		if (!leftSuperclass.equals(rightSuperclass)) {
                    			startClassChangeHistory.get().addChange(leftClass, rightClass, ChangeFactory.forClass(Change.Type.SUPERCLASS_CHANGE));
                    			startClassChangeHistory.get().connectRelatedNodes();
                    		}
                    	}
						else if (leftSuperclass != null && rightSuperclass == null) {
							startClassChangeHistory.get().addChange(leftClass, rightClass, ChangeFactory.forClass(Change.Type.SUPERCLASS_CHANGE));
							startClassChangeHistory.get().connectRelatedNodes();
						}
						else if (leftSuperclass == null && rightSuperclass != null) {
							startClassChangeHistory.get().addChange(leftClass, rightClass, ChangeFactory.forClass(Change.Type.SUPERCLASS_CHANGE));
							startClassChangeHistory.get().connectRelatedNodes();
						}
						if (!leftClass.getUmlClass().getImplementedInterfaces().equals(rightClass.getUmlClass().getImplementedInterfaces())) {
							startClassChangeHistory.get().addChange(leftClass, rightClass, ChangeFactory.forClass(Change.Type.INTERFACE_LIST_CHANGE));
							startClassChangeHistory.get().connectRelatedNodes();
						}
						checkSignatureFormatChange(startClassChangeHistory, leftClass, rightClass);
						Map<Method, MethodTrackerChangeHistory> notFoundMethods = processMethodsWithSameSignature(rightModel, currentVersion, leftModel, parentVersion);
						Map<Attribute, AttributeTrackerChangeHistory> notFoundAttributes = processAttributesWithSameSignature(rightModel, currentVersion, leftModel, parentVersion);
						Map<Class, ClassTrackerChangeHistory> notFoundInnerClasses = new LinkedHashMap<>();
						Set<Pair<Class, Class>> foundInnerClasses = new LinkedHashSet<>();
						processInnerClassesWithSameSignature(rightModel, currentVersion, leftModel, parentVersion, startClass, foundInnerClasses, notFoundInnerClasses);
						if (notFoundMethods.size() > 0 || notFoundAttributes.size() > 0 || notFoundInnerClasses.size() > 0) {
							processLocallyRefactoredMethods(notFoundMethods, umlModelDiffLocal, currentVersion, parentVersion, refactorings);
							processLocallyRefactoredAttributes(notFoundAttributes, umlModelDiffLocal, currentVersion, parentVersion, refactorings);
							processLocallyRefactoredInnerClasses(notFoundInnerClasses, umlModelDiffLocal, currentVersion, parentVersion, refactorings);
						}
						UMLAbstractClassDiff umlClassDiff = getUMLClassDiff(umlModelDiffLocal, rightClassSourceFolder, rightClassName);
						processImportsAndClassComments(umlClassDiff, rightClass, currentVersion, parentVersion, refactorings);
						for (Pair<Class, Class> pair : foundInnerClasses) {
							String rightInnerClassName = pair.getRight().getUmlClass().getName();
		                    String rightInnerClassSourceFolder = pair.getRight().getUmlClass().getLocationInfo().getSourceFolder();
							UMLAbstractClassDiff innerClassDiff = getUMLClassDiff(umlModelDiffLocal, rightInnerClassSourceFolder, rightInnerClassName);
							processImportsAndClassComments(innerClassDiff, pair.getRight(), currentVersion, parentVersion, refactorings);
						}
						Set<Class> leftSideClasses = new HashSet<>(classRefactored);
						leftSideClasses.forEach(startClassChangeHistory::addFirst);
						break;
					}
				}
				//All refactorings
				CommitModel commitModel = getCommitModel(commitId);
				if (!commitModel.moveSourceFolderRefactorings.isEmpty()) {
					Pair<UMLModel, UMLModel> umlModelPairPartial = getUMLModelPair(commitModel, rightClass, s -> true, true);
					UMLModelDiff umlModelDiffPartial = umlModelPairPartial.getLeft().diff(umlModelPairPartial.getRight());
					List<Refactoring> refactoringsPartial = umlModelDiffPartial.getRefactorings();
					Set<Class> classRefactored = startClassChangeHistory.analyseClassRefactorings(refactoringsPartial, currentVersion, parentVersion, rightClass::equalIdentifierIgnoringVersion);
					boolean refactored = !classRefactored.isEmpty();
					if (refactored) {
						Map<Method, MethodTrackerChangeHistory> notFoundMethods = processMethodsWithSameSignature(umlModelPairPartial.getRight(), currentVersion, umlModelPairPartial.getLeft(), parentVersion);
						Map<Attribute, AttributeTrackerChangeHistory> notFoundAttributes = processAttributesWithSameSignature(umlModelPairPartial.getRight(), currentVersion, umlModelPairPartial.getLeft(), parentVersion);
						Map<Class, ClassTrackerChangeHistory> notFoundInnerClasses = new LinkedHashMap<>();
						Set<Pair<Class, Class>> foundInnerClasses = new LinkedHashSet<>();
						processInnerClassesWithSameSignature(rightModel, currentVersion, leftModel, parentVersion, startClass, foundInnerClasses, notFoundInnerClasses);
						if (notFoundMethods.size() > 0 || notFoundAttributes.size() > 0 || notFoundInnerClasses.size() > 0) {
							processLocallyRefactoredMethods(notFoundMethods, umlModelDiffPartial, currentVersion, parentVersion, refactoringsPartial);
							processLocallyRefactoredAttributes(notFoundAttributes, umlModelDiffPartial, currentVersion, parentVersion, refactoringsPartial);
							processLocallyRefactoredInnerClasses(notFoundInnerClasses, umlModelDiffPartial, currentVersion, parentVersion, refactoringsPartial);
						}
						UMLAbstractClassDiff umlClassDiff = getUMLClassDiff(umlModelDiffPartial, rightClassSourceFolder, rightClassName);
						processImportsAndClassComments(umlClassDiff, rightClass, currentVersion, parentVersion, refactoringsPartial);
						for (Pair<Class, Class> pair : foundInnerClasses) {
							String rightInnerClassName = pair.getRight().getUmlClass().getName();
		                    String rightInnerClassSourceFolder = pair.getRight().getUmlClass().getLocationInfo().getSourceFolder();
							UMLAbstractClassDiff innerClassDiff = getUMLClassDiff(umlModelDiffPartial, rightInnerClassSourceFolder, rightInnerClassName);
							processImportsAndClassComments(innerClassDiff, pair.getRight(), currentVersion, parentVersion, refactoringsPartial);
						}
						Set<Class> leftSideClasses = new HashSet<>(classRefactored);
						leftSideClasses.forEach(startClassChangeHistory::addFirst);
						break;
					}
				}
				{
					Pair<UMLModel, UMLModel> umlModelPairAll = getUMLModelPair(commitModel, rightClass, s -> true, false);
					UMLModelDiff umlModelDiffAll = umlModelPairAll.getLeft().diff(umlModelPairAll.getRight());

					List<Refactoring> refactorings = umlModelPairAll.getLeft().getClassList().isEmpty() ?
							Collections.emptyList() :
							umlModelDiffAll.getRefactorings();

					Set<Class> classRefactored = startClassChangeHistory.analyseClassRefactorings(refactorings, currentVersion, parentVersion, rightClass::equalIdentifierIgnoringVersion);
					boolean refactored = !classRefactored.isEmpty();
					boolean extractedClass = false;
					for (Class c : classRefactored) {
						if (c.isAdded()) {
							extractedClass = true;
							break;
						}
					}
					if (refactored && !extractedClass) {
						Map<Method, MethodTrackerChangeHistory> notFoundMethods = processMethodsWithSameSignature(umlModelPairAll.getRight(), currentVersion, umlModelPairAll.getLeft(), parentVersion);
						Map<Attribute, AttributeTrackerChangeHistory> notFoundAttributes = processAttributesWithSameSignature(umlModelPairAll.getRight(), currentVersion, umlModelPairAll.getLeft(), parentVersion);
						Map<Class, ClassTrackerChangeHistory> notFoundInnerClasses = new LinkedHashMap<>();
						Set<Pair<Class, Class>> foundInnerClasses = new LinkedHashSet<>();
						processInnerClassesWithSameSignature(umlModelPairAll.getRight(), currentVersion, leftModel, parentVersion, startClass, foundInnerClasses, notFoundInnerClasses);
						if (notFoundMethods.size() > 0 || notFoundAttributes.size() > 0 || notFoundInnerClasses.size() > 0) {
							processLocallyRefactoredMethods(notFoundMethods, umlModelDiffAll, currentVersion, parentVersion, refactorings);
							processLocallyRefactoredAttributes(notFoundAttributes, umlModelDiffAll, currentVersion, parentVersion, refactorings);
							processLocallyRefactoredInnerClasses(notFoundInnerClasses, umlModelDiffAll, currentVersion, parentVersion, refactorings);
						}
						UMLAbstractClassDiff umlClassDiff = getUMLClassDiff(umlModelDiffAll, rightClassSourceFolder, rightClassName);
						processImportsAndClassComments(umlClassDiff, rightClass, currentVersion, parentVersion, refactorings);
						for (Pair<Class, Class> pair : foundInnerClasses) {
							String rightInnerClassName = pair.getRight().getUmlClass().getName();
		                    String rightInnerClassSourceFolder = pair.getRight().getUmlClass().getLocationInfo().getSourceFolder();
							UMLAbstractClassDiff innerClassDiff = getUMLClassDiff(umlModelDiffAll, rightInnerClassSourceFolder, rightInnerClassName);
							processImportsAndClassComments(innerClassDiff, pair.getRight(), currentVersion, parentVersion, refactorings);
						}
						Set<Class> leftSideClasses = new HashSet<>(classRefactored);
						leftSideClasses.forEach(startClassChangeHistory::addFirst);
						break;
					}
					else if(extractedClass) {
						processAddedMethods(umlModelPairAll.getRight(), umlModelDiffAll, currentVersion, parentVersion);
						processAddedAttributes(umlModelPairAll.getRight(), umlModelDiffAll, currentVersion, parentVersion);
						processAddedInnerClasses(umlModelPairAll.getRight(), umlModelDiffAll, currentVersion, parentVersion, startClass);
						processAddedImportsAndClassComments(rightClass, parentVersion);
						break;
					}

					if (startClassChangeHistory.isClassAdded(umlModelDiffAll, currentVersion, parentVersion, rightClass::equalIdentifierIgnoringVersion)) {
						processAddedMethods(umlModelPairAll.getRight(), umlModelDiffAll, currentVersion, parentVersion);
						processAddedAttributes(umlModelPairAll.getRight(), umlModelDiffAll, currentVersion, parentVersion);
						processAddedInnerClasses(umlModelPairAll.getRight(), umlModelDiffAll, currentVersion, parentVersion, startClass);
						processAddedImportsAndClassComments(rightClass, parentVersion);
						break;
					}
				}
			}
		}
		// compute blame information
		for (Integer lineNumber : lineNumberToCodeElementMap.keySet()) {
			CodeElement startElement = lineNumberToCodeElementMap.get(lineNumber);
			if (startElement instanceof Method) {
				Method startMethod = (Method)startElement;
				MethodTrackerChangeHistory startMethodChangeHistory = (MethodTrackerChangeHistory) programElementMap.get(startMethod);
				startMethod.checkClosingBracket(lineNumber);
				HistoryInfo<Method> historyInfo = startMethodChangeHistory.blameReturn(startMethod, lineNumber);
				blameInfo.put(lineNumber, historyInfo);
			}
			else if (startElement instanceof Block) {
				Block startBlock = (Block)startElement;
				BlockTrackerChangeHistory startBlockChangeHistory = (BlockTrackerChangeHistory) programElementMap.get(startBlock);
				startBlock.checkClosingBracket(lineNumber);
				startBlock.checkElseBlockStart(lineNumber);
				startBlock.checkElseBlockEnd(lineNumber);
				startBlock.checkClosingBracketOfAnonymousClassDeclaration(lineNumber);
				startBlock.checkClosingBracketOfLambda(lineNumber);
				startBlock.checkDoWhileConditional(lineNumber);
				HistoryInfo<Block> historyInfo = startBlockChangeHistory.blameReturn(startBlock, lineNumber);
				blameInfo.put(lineNumber, historyInfo);
				if (startBlockChangeHistory.getNested().size() > 0) {
					for (Block nestedBlock : startBlockChangeHistory.getNested().keySet()) {
						BlockTrackerChangeHistory nestedHistory = (BlockTrackerChangeHistory)programElementMap.get(nestedBlock);
						if (nestedHistory.getBlockStartLineNumber() == lineNumber || nestedHistory.getBlockEndLineNumber() == lineNumber) {
							nestedBlock.checkClosingBracket(lineNumber);
							nestedBlock.checkElseBlockStart(lineNumber);
							nestedBlock.checkElseBlockEnd(lineNumber);
							nestedBlock.checkClosingBracketOfAnonymousClassDeclaration(lineNumber);
							nestedBlock.checkClosingBracketOfLambda(lineNumber);
							nestedBlock.checkDoWhileConditional(lineNumber);
							HistoryInfo<Block> nestedHistoryInfo = nestedHistory.blameReturn(nestedBlock, lineNumber);
							blameInfo.put(lineNumber, nestedHistoryInfo);
						}
					}
				}
			}
			else if (startElement instanceof Class) {
				Class clazz = (Class)startElement;
				ClassTrackerChangeHistory classChangeHistory = (ClassTrackerChangeHistory) programElementMap.get(clazz);
				clazz.checkClosingBracket(lineNumber);
				HistoryInfo<Class> historyInfo = classChangeHistory.blameReturn(clazz, lineNumber);
				blameInfo.put(lineNumber, historyInfo);
			}
			else if (startElement instanceof Comment) {
				Comment startComment = (Comment)startElement;
				CommentTrackerChangeHistory commentChangeHistory = (CommentTrackerChangeHistory) programElementMap.get(startComment);
				HistoryInfo<Comment> historyInfo = commentChangeHistory.blameReturn(startComment, lineNumber);
				blameInfo.put(lineNumber, historyInfo);
			}
			else if (startElement instanceof Import) {
				Import startImport = (Import)startElement;
				ImportTrackerChangeHistory importChangeHistory = (ImportTrackerChangeHistory) programElementMap.get(startImport);
				HistoryInfo<Import> historyInfo = importChangeHistory.blameReturn();
				blameInfo.put(lineNumber, historyInfo);
			}
			else if (startElement instanceof Package) {
				Package startPackage = (Package)startElement;
				HistoryInfo<Class> historyInfo = startClassChangeHistory.blameReturn(startPackage);
				blameInfo.put(lineNumber, historyInfo);
			}
			else if (startElement instanceof Attribute) {
				Attribute startAttribute = (Attribute)startElement;
				AttributeTrackerChangeHistory attributeChangeHistory = (AttributeTrackerChangeHistory) programElementMap.get(startAttribute);
				HistoryInfo<Attribute> historyInfo = attributeChangeHistory.blameReturn(startAttribute, lineNumber);
				blameInfo.put(lineNumber, historyInfo);
			}
			else if (startElement instanceof Annotation) {
				Annotation startAnnotation = (Annotation)startElement;
				AnnotationTrackerChangeHistory annotationChangeHistory = (AnnotationTrackerChangeHistory) programElementMap.get(startAnnotation);
				HistoryInfo<Annotation> historyInfo = annotationChangeHistory.blameReturn();
				blameInfo.put(lineNumber, historyInfo);
			}
			else {
				blameInfo.put(lineNumber, null);
			}
		}
	}

	private void processAddedImportsAndClassComments(Class rightClass, Version parentVersion) {
		for (CodeElement key : programElementMap.keySet()) {
			if (key instanceof Import) {
				Import startImport = (Import)key;
				ImportTrackerChangeHistory startImportChangeHistory = (ImportTrackerChangeHistory) programElementMap.get(startImport);
				if (rightClass.getUmlClass().getImportedTypes().size() > 0 && rightClass.getUmlClass().isTopLevel()) {
					Import currentImport = startImportChangeHistory.poll();
					if (currentImport == null) {
						continue;
					}
					Import rightImport = rightClass.findImport(currentImport::equalIdentifierIgnoringVersion);
					if (rightImport != null) {
						Import importBefore = Import.of(rightImport.getUmlImport(), rightImport.getClazz(), parentVersion);
						startImportChangeHistory.get().handleAdd(importBefore, rightImport, "added with class");
						startImportChangeHistory.add(importBefore);
						startImportChangeHistory.get().connectRelatedNodes();
					}
				}
			}
			else if (key instanceof Comment) {
				Comment startComment = (Comment)key;
				CommentTrackerChangeHistory startCommentChangeHistory = (CommentTrackerChangeHistory) programElementMap.get(startComment);
				if (startComment.getClazz().isPresent()) {
					Comment currentComment = startCommentChangeHistory.poll();
					if (currentComment == null) {
						continue;
					}
					Comment rightComment = rightClass.findComment(currentComment::equalIdentifierIgnoringVersion);
					if (rightComment != null) {
						Comment commentBefore = Comment.of(rightComment.getComment(), rightComment.getClazz().get(), parentVersion);
						startCommentChangeHistory.get().handleAdd(commentBefore, rightComment, "added with class");
						startCommentChangeHistory.add(commentBefore);
						startCommentChangeHistory.get().connectRelatedNodes();
					}
				}
			}
			else if (key instanceof Annotation) {
				Annotation startAnnotation = (Annotation)key;
				AnnotationTrackerChangeHistory startAnnotationChangeHistory = (AnnotationTrackerChangeHistory) programElementMap.get(startAnnotation);
				if (startAnnotation.getClazz().isPresent()) {
					Annotation currentAnnotation = startAnnotationChangeHistory.poll();
					if (currentAnnotation == null) {
						continue;
					}
					Annotation rightAnnotation = rightClass.findAnnotation(currentAnnotation::equalIdentifierIgnoringVersion);
					if (rightAnnotation != null) {
						Annotation annotationBefore = Annotation.of(rightAnnotation.getAnnotation(), rightAnnotation.getClazz().get(), parentVersion);
						startAnnotationChangeHistory.get().handleAdd(annotationBefore, rightAnnotation, "added with class");
						startAnnotationChangeHistory.add(annotationBefore);
						startAnnotationChangeHistory.get().connectRelatedNodes();
					}
				}
			}
		}
	}

	private void processImportsAndClassComments(UMLAbstractClassDiff classDiff, Class rightClass, Version currentVersion, Version parentVersion, List<Refactoring> refactorings) {
		boolean extracted = isExtracted(rightClass, refactorings);
		for (CodeElement key : programElementMap.keySet()) {
			if (key instanceof Import) {
				Import startImport = (Import)key;
				ImportTrackerChangeHistory startImportChangeHistory = (ImportTrackerChangeHistory) programElementMap.get(startImport);
				if (rightClass.getUmlClass().getImportedTypes().size() > 0 && rightClass.getUmlClass().isTopLevel()) {
					Import currentImport = startImportChangeHistory.peek();
					if (currentImport == null || currentImport.isAdded()) {
						continue;
					}
					Import rightImport = rightClass.findImport(currentImport::equalIdentifierIgnoringVersion);
					if (rightImport == null) {
						continue;
					}
					startImportChangeHistory.poll();
					if (extracted) {
						startImportChangeHistory.addedClass(rightClass, rightImport, parentVersion);
						continue;
					}
					startImportChangeHistory.checkBodyOfMatchedClasses(currentVersion, parentVersion, rightImport::equalIdentifierIgnoringVersion, classDiff);
				}
			}
			else if (key instanceof Comment) {
				Comment startComment = (Comment)key;
				CommentTrackerChangeHistory startCommentChangeHistory = (CommentTrackerChangeHistory) programElementMap.get(startComment);
				if ((startComment.getClazz().isPresent() && startComment.getClazz().get().equals(rightClass.getUmlClass())) ||
						(!startCommentChangeHistory.isEmpty() && startCommentChangeHistory.peek().getClazz().isPresent() && rightClass.getUmlClass().equals(startCommentChangeHistory.peek().getClazz().get()))) {
					Comment currentComment = startCommentChangeHistory.peek();
					if (currentComment == null || currentComment.isAdded()) {
						continue;
					}
					Comment rightComment = rightClass.findComment(currentComment::equalIdentifierIgnoringVersion);
					if (rightComment == null) {
						continue;
					}
					startCommentChangeHistory.poll();
					if (extracted) {
						startCommentChangeHistory.addedClass(rightClass, rightComment, parentVersion);
						continue;
					}
					startCommentChangeHistory.checkBodyOfMatchedClasses(currentVersion, parentVersion, rightComment::equalIdentifierIgnoringVersion, classDiff);
				}
			}
			else if (key instanceof Annotation) {
				Annotation startAnnotation = (Annotation)key;
				AnnotationTrackerChangeHistory startAnnotationChangeHistory = (AnnotationTrackerChangeHistory) programElementMap.get(startAnnotation);
				if ((startAnnotation.getClazz().isPresent() && startAnnotation.getClazz().get().equals(rightClass.getUmlClass())) ||
						(!startAnnotationChangeHistory.isEmpty() && startAnnotationChangeHistory.peek().getClazz().isPresent() && rightClass.getUmlClass().equals(startAnnotationChangeHistory.peek().getClazz().get()))) {
					Annotation currentAnnotation = startAnnotationChangeHistory.peek();
					if (currentAnnotation == null || currentAnnotation.isAdded()) {
						continue;
					}
					Annotation rightAnnotation = rightClass.findAnnotation(currentAnnotation::equalIdentifierIgnoringVersion);
					if (rightAnnotation == null) {
						continue;
					}
					startAnnotationChangeHistory.poll();
					if (extracted) {
						startAnnotationChangeHistory.addedClass(rightClass, rightAnnotation, parentVersion);
						continue;
					}
					startAnnotationChangeHistory.checkBodyOfMatchedClasses(currentVersion, parentVersion, rightAnnotation::equalIdentifierIgnoringVersion, classDiff);
				}
			}
		}
	}

	private void processAddedInnerClasses(UMLModel rightModel, UMLModelDiff umlModelDiffAll, Version currentVersion, Version parentVersion, Class startClass) {
		for (CodeElement key : programElementMap.keySet()) {
			if (key instanceof Class && !key.equals(startClass)) {
				Class startInnerClass = (Class)key;
				ClassTrackerChangeHistory startInnerClassChangeHistory = (ClassTrackerChangeHistory) programElementMap.get(startInnerClass);
				Class currentClass = startInnerClassChangeHistory.peek();
				if (currentClass == null) {
					currentClass = startInnerClassChangeHistory.getCurrent();
				}
				if (currentClass == null || currentClass.isAdded()) {
					continue;
				}
				Class rightClass = getClass(rightModel, currentVersion, currentClass::equalIdentifierIgnoringVersion);
				if (rightClass == null) {
					continue;
				}
				startInnerClassChangeHistory.poll();
				if (startInnerClassChangeHistory.isClassAdded(umlModelDiffAll, currentVersion, parentVersion, rightClass::equalIdentifierIgnoringVersion)) {
					for (CodeElement key2 : programElementMap.keySet()) {
						if (key2 instanceof Comment) {
							Comment startComment = (Comment)key2;
							CommentTrackerChangeHistory startCommentChangeHistory = (CommentTrackerChangeHistory) programElementMap.get(startComment);
							if ((startComment.getClazz().isPresent() && startComment.getClazz().get().equals(startInnerClass.getUmlClass())) ||
									(!startCommentChangeHistory.isEmpty() && startCommentChangeHistory.peek().getClazz().isPresent() && rightClass.getUmlClass().equals(startCommentChangeHistory.peek().getClazz().get()))) {
								Comment currentComment = startCommentChangeHistory.poll();
								Comment rightComment = rightClass.findComment(currentComment::equalIdentifierIgnoringVersion);
								if (rightComment != null) {
									Comment commentBefore = Comment.of(rightComment.getComment(), rightComment.getClazz().get(), parentVersion);
									startCommentChangeHistory.get().handleAdd(commentBefore, rightComment, "added with inner class");
									startCommentChangeHistory.add(commentBefore);
									startCommentChangeHistory.get().connectRelatedNodes();
								}
							}
						}
						else if (key2 instanceof Annotation) {
							Annotation startAnnotation = (Annotation)key2;
							AnnotationTrackerChangeHistory startAnnotationChangeHistory = (AnnotationTrackerChangeHistory) programElementMap.get(startAnnotation);
							if ((startAnnotation.getClazz().isPresent() && startAnnotation.getClazz().get().equals(startInnerClass.getUmlClass())) ||
									(!startAnnotationChangeHistory.isEmpty() && startAnnotationChangeHistory.peek().getClazz().isPresent() && rightClass.getUmlClass().equals(startAnnotationChangeHistory.peek().getClazz().get()))) {
								Annotation currentAnnotation = startAnnotationChangeHistory.poll();
								Annotation rightAnnotation = rightClass.findAnnotation(currentAnnotation::equalIdentifierIgnoringVersion);
								if (rightAnnotation != null) {
									Annotation annotationBefore = Annotation.of(rightAnnotation.getAnnotation(), rightAnnotation.getClazz().get(), parentVersion);
									startAnnotationChangeHistory.get().handleAdd(annotationBefore, rightAnnotation, "added with inner class");
									startAnnotationChangeHistory.add(annotationBefore);
									startAnnotationChangeHistory.get().connectRelatedNodes();
								}
							}
						}
					}
				}
			}
		}
	}

	private void processAddedAttributes(UMLModel rightModel, UMLModelDiff umlModelDiffAll, Version currentVersion, Version parentVersion) {
		for (CodeElement key : programElementMap.keySet()) {
			if (key instanceof Attribute) {
				Attribute startAttribute = (Attribute)key;
				AttributeTrackerChangeHistory startAttributeChangeHistory = (AttributeTrackerChangeHistory) programElementMap.get(startAttribute);
				Attribute currentAttribute = startAttributeChangeHistory.poll();
				if (currentAttribute == null) {
					currentAttribute = startAttributeChangeHistory.getCurrent();
				}
				if (currentAttribute == null || currentAttribute.isAdded()) {
					continue;
				}
				Attribute rightAttribute = getAttribute(rightModel, currentVersion, currentAttribute::equalIdentifierIgnoringVersion);
				if (rightAttribute == null)
					rightAttribute = currentAttribute;
				if (startAttributeChangeHistory.isAttributeAdded(umlModelDiffAll, rightAttribute.getUmlAttribute().getLocationInfo().getSourceFolder(), rightAttribute.getUmlAttribute().getClassName(), currentVersion, parentVersion, rightAttribute::equalIdentifierIgnoringVersion, getAllClassesDiff(umlModelDiffAll))) {
					for (CodeElement key2 : programElementMap.keySet()) {
						if (key2 instanceof Comment) {
							Comment startComment = (Comment)key2;
							CommentTrackerChangeHistory startCommentChangeHistory = (CommentTrackerChangeHistory) programElementMap.get(startComment);
							if ((startComment.getOperation().isPresent() && startComment.getOperation().get().equals(startAttribute.getUmlAttribute())) ||
									(!startCommentChangeHistory.isEmpty() && startCommentChangeHistory.peek().getOperation().isPresent() && rightAttribute.getUmlAttribute().equals(startCommentChangeHistory.peek().getOperation().get()))) {
								Comment currentComment = startCommentChangeHistory.poll();
								Comment rightComment = rightAttribute.findComment(currentComment::equalIdentifierIgnoringVersion);
								if (rightComment != null) {
									Comment commentBefore = Comment.of(rightComment.getComment(), rightComment.getOperation().get(), parentVersion);
									startCommentChangeHistory.get().handleAdd(commentBefore, rightComment, "added with attribute");
									startCommentChangeHistory.add(commentBefore);
									startCommentChangeHistory.get().connectRelatedNodes();
								}
							}
						}
						else if (key2 instanceof Annotation) {
							Annotation startAnnotation = (Annotation)key2;
							AnnotationTrackerChangeHistory startAnnotationChangeHistory = (AnnotationTrackerChangeHistory) programElementMap.get(startAnnotation);
							if ((startAnnotation.getOperation().isPresent() && startAnnotation.getOperation().get().equals(startAttribute.getUmlAttribute())) ||
									(!startAnnotationChangeHistory.isEmpty() && startAnnotationChangeHistory.peek().getOperation().isPresent() && rightAttribute.getUmlAttribute().equals(startAnnotationChangeHistory.peek().getOperation().get()))) {
								Annotation currentAnnotation = startAnnotationChangeHistory.poll();
								Annotation rightAnnotation = rightAttribute.findAnnotation(currentAnnotation::equalIdentifierIgnoringVersion);
								if (rightAnnotation != null) {
									Annotation annotationBefore = Annotation.of(rightAnnotation.getAnnotation(), rightAnnotation.getOperation().get(), parentVersion);
									startAnnotationChangeHistory.get().handleAdd(annotationBefore, rightAnnotation, "added with attribute");
									startAnnotationChangeHistory.add(annotationBefore);
									startAnnotationChangeHistory.get().connectRelatedNodes();
								}
							}
						}
					}
				}
			}
		}
	}

	private void processAddedMethods(UMLModel rightModel, UMLModelDiff umlModelDiffAll, Version currentVersion, Version parentVersion) {
		for (CodeElement key : programElementMap.keySet()) {
			if (key instanceof Method) {
				Method startMethod = (Method)key;
				MethodTrackerChangeHistory startMethodChangeHistory = (MethodTrackerChangeHistory) programElementMap.get(startMethod);
				Method currentMethod = startMethodChangeHistory.poll();
				if (currentMethod == null) {
					currentMethod = startMethodChangeHistory.getCurrent();
				}
				if (currentMethod == null || currentMethod.isAdded()) {
					continue;
				}
				Method rightMethod = getMethod(rightModel, currentVersion, currentMethod::equalIdentifierIgnoringVersion);
				if (rightMethod == null)
					rightMethod = currentMethod;
				if (startMethodChangeHistory.isMethodAdded(umlModelDiffAll, rightMethod.getUmlOperation().getLocationInfo().getSourceFolder(), rightMethod.getUmlOperation().getClassName(), currentVersion, parentVersion, rightMethod::equalIdentifierIgnoringVersion, getAllClassesDiff(umlModelDiffAll))) {
					for (CodeElement key2 : programElementMap.keySet()) {
						if (key2 instanceof Block) {
							Block startBlock = (Block)key2;
							BlockTrackerChangeHistory startBlockChangeHistory = (BlockTrackerChangeHistory) programElementMap.get(startBlock);
							if (startBlock.getOperation().equals(startMethod.getUmlOperation()) ||
									matchingPeekMethod(rightMethod, startBlockChangeHistory)) {
								Block currentBlock = startBlockChangeHistory.poll();
								if (currentBlock == null) {
									continue;
								}
								Block rightBlock = rightMethod.findBlock(currentBlock::equalIdentifierIgnoringVersion);
								if (rightBlock != null) {
									Block blockBefore = Block.of(rightBlock.getComposite(), rightBlock.getOperation(), parentVersion);
									startBlockChangeHistory.get().handleAdd(blockBefore, rightBlock, "added with method");
									startBlockChangeHistory.add(blockBefore);
									startBlockChangeHistory.get().connectRelatedNodes();
								}
							}
						}
						else if (key2 instanceof Comment) {
							Comment startComment = (Comment)key2;
							CommentTrackerChangeHistory startCommentChangeHistory = (CommentTrackerChangeHistory) programElementMap.get(startComment);
							if (matchingMethod(startMethod, startComment) ||
									matchingPeekMethod(rightMethod, startCommentChangeHistory)) {
								Comment currentComment = startCommentChangeHistory.poll();
								if (currentComment == null) {
									continue;
								}
								Comment rightComment = rightMethod.findComment(currentComment::equalIdentifierIgnoringVersion);
								if (rightComment != null) {
									Comment commentBefore = Comment.of(rightComment.getComment(), rightComment.getOperation().get(), parentVersion);
									startCommentChangeHistory.get().handleAdd(commentBefore, rightComment, "added with method");
									startCommentChangeHistory.add(commentBefore);
									startCommentChangeHistory.get().connectRelatedNodes();
								}
							}
						}
						else if (key2 instanceof Annotation) {
							Annotation startAnnotation = (Annotation)key2;
							AnnotationTrackerChangeHistory startAnnotationChangeHistory = (AnnotationTrackerChangeHistory) programElementMap.get(startAnnotation);
							if (matchingMethod(startMethod, startAnnotation) ||
									matchingPeekMethod(rightMethod, startAnnotationChangeHistory)) {
								Annotation currentAnnotation = startAnnotationChangeHistory.poll();
								if (currentAnnotation == null) {
									continue;
								}
								Annotation rightAnnotation = rightMethod.findAnnotation(currentAnnotation::equalIdentifierIgnoringVersion);
								if (rightAnnotation != null) {
									Annotation commentBefore = Annotation.of(rightAnnotation.getAnnotation(), rightAnnotation.getOperation().get(), parentVersion);
									startAnnotationChangeHistory.get().handleAdd(commentBefore, rightAnnotation, "added with method");
									startAnnotationChangeHistory.add(commentBefore);
									startAnnotationChangeHistory.get().connectRelatedNodes();
								}
							}
						}
					}
				}
			}
		}
	}

	private void processLocallyRefactoredAttributes(Map<Attribute, AttributeTrackerChangeHistory> notFoundAttributes, UMLModelDiff umlModelDiff, Version currentVersion, Version parentVersion, List<Refactoring> refactorings) throws Exception {
		for (Attribute rightAttribute : notFoundAttributes.keySet()) {
			String rightAttributeClassName = rightAttribute.getUmlAttribute().getClassName();
            String rightAttributeSourceFolder = rightAttribute.getUmlAttribute().getLocationInfo().getSourceFolder();
			AttributeTrackerChangeHistory startAttributeChangeHistory = notFoundAttributes.get(rightAttribute);
			Set<Attribute> attributeContainerChanged = startAttributeChangeHistory.isAttributeContainerChanged(umlModelDiff, refactorings, currentVersion, parentVersion, rightAttribute::equalIdentifierIgnoringVersion, getClassMoveDiffList(umlModelDiff));
			boolean containerChanged = !attributeContainerChanged.isEmpty();

			String renamedAttributeClassType = null;
			String extractedClassFilePath = null;
			for (Refactoring r : refactorings) {
				if (r.getRefactoringType().equals(RefactoringType.RENAME_ATTRIBUTE)) {
					RenameAttributeRefactoring renameAttributeRefactoring = (RenameAttributeRefactoring)r;
					if (renameAttributeRefactoring.getRenamedAttribute().getType() != null) {
						renamedAttributeClassType = renameAttributeRefactoring.getRenamedAttribute().getType().getClassType();
					}
					if (renamedAttributeClassType != null) {
						Map<String, String> renamedFilesHint = new HashMap<>();
						Set<String> filePathsBefore = new HashSet<>();
						Set<String> filePathsCurrent = new HashSet<>();
						populateFileSets(currentVersion.getId(), filePathsBefore, filePathsCurrent, renamedFilesHint);
						for (String filePath : filePathsCurrent) {
							if (filePath.endsWith(renamedAttributeClassType + ".java") && !filePathsBefore.contains(filePath)) {
								extractedClassFilePath = filePath;
								break;
							}
						}
					}
				}
			}
			Set<Attribute> attributeRefactored = null;
			if (extractedClassFilePath == null)
				attributeRefactored = startAttributeChangeHistory.analyseAttributeRefactorings(refactorings, currentVersion, parentVersion, rightAttribute::equalIdentifierIgnoringVersion);
			else
				attributeRefactored = Collections.emptySet();
			boolean refactored = !attributeRefactored.isEmpty();

			if (containerChanged || refactored) {
				Set<Attribute> leftSideAttributes = new HashSet<>();
				leftSideAttributes.addAll(attributeContainerChanged);
				leftSideAttributes.addAll(attributeRefactored);
				leftSideAttributes.forEach(startAttributeChangeHistory::addFirst);
				if (leftSideAttributes.size() == 1) {
					startAttributeChangeHistory.setCurrent(leftSideAttributes.iterator().next());
				}
				for (CodeElement key2 : programElementMap.keySet()) {
					if (key2 instanceof Comment) {
						Comment startComment = (Comment)key2;
						CommentTrackerChangeHistory startCommentChangeHistory = (CommentTrackerChangeHistory) programElementMap.get(startComment);
						if ((startComment.getOperation().isPresent() && startComment.getOperation().get().equals(rightAttribute.getUmlAttribute())) ||
								(!startCommentChangeHistory.isEmpty() && startCommentChangeHistory.peek().getOperation().isPresent() && rightAttribute.getUmlAttribute().equals(startCommentChangeHistory.peek().getOperation().get()))) {
							Comment currentComment = startCommentChangeHistory.peek();
							if (currentComment == null) {
								continue;
							}
							Comment rightComment = rightAttribute.findComment(currentComment::equalIdentifierIgnoringVersion);
							if (rightComment == null) {
								continue;
							}
							startCommentChangeHistory.poll();
							boolean found = startCommentChangeHistory.checkRefactoredAttribute(currentVersion, parentVersion, rightAttribute::equalIdentifierIgnoringVersion, rightComment, refactorings);
							if (found) {
								continue;
							}
							UMLAbstractClassDiff umlClassDiff = getUMLClassDiff(umlModelDiff,  rightAttributeSourceFolder, rightAttributeClassName);
						    if (umlClassDiff != null) {
						    	Pair<? extends UMLAttribute, ? extends UMLAttribute> foundPair = null;
						    	for (Pair<UMLAttribute, UMLAttribute> pair : umlClassDiff.getCommonAtrributes()) {
						    		if (pair.getRight().equals(rightAttribute.getUmlAttribute())) {
						    			foundPair = pair;
						    			break;
						    		}
						    	}
						    	for (Pair<UMLEnumConstant, UMLEnumConstant> pair : umlClassDiff.getCommonEnumConstants()) {
						    		if (pair.getRight().equals(rightAttribute.getUmlAttribute())) {
						    			foundPair = pair;
						    			break;
						    		}
						    	}
						    	UMLDocumentationDiffProvider provider = null;
						    	for (UMLAttributeDiff attributeDiff : umlClassDiff.getAttributeDiffList()) {
						    		if (attributeDiff.getContainer2().equals(rightAttribute.getUmlAttribute())) {
						    			provider = attributeDiff;
						    			break;
						    		}
						    	}
						    	for (UMLEnumConstantDiff attributeDiff : umlClassDiff.getEnumConstantDiffList()) {
						    		if (attributeDiff.getContainer2().equals(rightAttribute.getUmlAttribute())) {
						    			provider = attributeDiff;
						    			break;
						    		}
						    	}
						    	startCommentChangeHistory.checkBodyOfMatchedAttributes(currentVersion, parentVersion, currentComment::equalIdentifierIgnoringVersion, foundPair);
						    	startCommentChangeHistory.checkBodyOfMatchedOperations(currentVersion, parentVersion, currentComment::equalIdentifierIgnoringVersion, provider);
						    }
						}
					}
					else if (key2 instanceof Annotation) {
						Annotation startAnnotation = (Annotation)key2;
						AnnotationTrackerChangeHistory startAnnotationChangeHistory = (AnnotationTrackerChangeHistory) programElementMap.get(startAnnotation);
						if ((startAnnotation.getOperation().isPresent() && startAnnotation.getOperation().get().equals(rightAttribute.getUmlAttribute())) ||
								(!startAnnotationChangeHistory.isEmpty() && startAnnotationChangeHistory.peek().getOperation().isPresent() && rightAttribute.getUmlAttribute().equals(startAnnotationChangeHistory.peek().getOperation().get()))) {
							Annotation currentAnnotation = startAnnotationChangeHistory.peek();
							if (currentAnnotation == null) {
								continue;
							}
							Annotation rightAnnotation = rightAttribute.findAnnotation(currentAnnotation::equalIdentifierIgnoringVersion);
							if (rightAnnotation == null) {
								continue;
							}
							startAnnotationChangeHistory.poll();
							boolean found = startAnnotationChangeHistory.checkRefactoredAttribute(currentVersion, parentVersion, rightAttribute::equalIdentifierIgnoringVersion, rightAnnotation, refactorings);
							if (found) {
								continue;
							}
							UMLAbstractClassDiff umlClassDiff = getUMLClassDiff(umlModelDiff,  rightAttributeSourceFolder, rightAttributeClassName);
							startAnnotationChangeHistory.checkClassDiffForAnnotationChange(currentVersion, parentVersion, rightAttribute, currentAnnotation::equalIdentifierIgnoringVersion, umlClassDiff);
						}
					}
				}
			}
			else if(startAttributeChangeHistory.isAttributeAdded(umlModelDiff, rightAttribute.getUmlAttribute().getLocationInfo().getSourceFolder(), rightAttribute.getUmlAttribute().getClassName(), currentVersion, parentVersion, rightAttribute::equalIdentifierIgnoringVersion, getAllClassesDiff(umlModelDiff))) {
				for (CodeElement key2 : programElementMap.keySet()) {
					if (key2 instanceof Comment) {
						Comment startComment = (Comment)key2;
						CommentTrackerChangeHistory startCommentChangeHistory = (CommentTrackerChangeHistory) programElementMap.get(startComment);
						if ((startComment.getOperation().isPresent() && startComment.getOperation().get().equals(rightAttribute.getUmlAttribute())) ||
								(!startCommentChangeHistory.isEmpty() && startCommentChangeHistory.peek().getOperation().isPresent() && rightAttribute.getUmlAttribute().equals(startCommentChangeHistory.peek().getOperation().get()))) {
							Comment currentComment = startCommentChangeHistory.poll();
							if (currentComment == null) {
								continue;
							}
							Comment rightComment = rightAttribute.findComment(currentComment::equalIdentifierIgnoringVersion);
							if (rightComment == null) {
								continue;
							}
							startCommentChangeHistory.addedAttribute(rightAttribute, rightComment, parentVersion);
						}
					}
					else if (key2 instanceof Annotation) {
						Annotation startAnnotation = (Annotation)key2;
						AnnotationTrackerChangeHistory startAnnotationChangeHistory = (AnnotationTrackerChangeHistory) programElementMap.get(startAnnotation);
						if ((startAnnotation.getOperation().isPresent() && startAnnotation.getOperation().get().equals(rightAttribute.getUmlAttribute())) ||
								(!startAnnotationChangeHistory.isEmpty() && startAnnotationChangeHistory.peek().getOperation().isPresent() && rightAttribute.getUmlAttribute().equals(startAnnotationChangeHistory.peek().getOperation().get()))) {
							Annotation currentAnnotation = startAnnotationChangeHistory.poll();
							if (currentAnnotation == null) {
								continue;
							}
							Annotation rightAnnotation = rightAttribute.findAnnotation(currentAnnotation::equalIdentifierIgnoringVersion);
							if (rightAnnotation == null) {
								continue;
							}
							startAnnotationChangeHistory.addedAttribute(rightAttribute, rightAnnotation, parentVersion);
						}
					}
				}
			}
		}
	}

	private boolean isMoved(Method rightMethod, List<Refactoring> refactorings) {
		Set<VariableDeclarationContainer> parentContainers = new LinkedHashSet<>();
		parentContainers.add(rightMethod.getUmlOperation());
		if (rightMethod.getUmlOperation().getAnonymousClassContainer() != null && rightMethod.getUmlOperation().getAnonymousClassContainer().isPresent()) {
			UMLAnonymousClass anonymous = rightMethod.getUmlOperation().getAnonymousClassContainer().get();
			parentContainers.addAll(anonymous.getParentContainers());
		}
		for (Refactoring r : refactorings) {
			if (r instanceof MoveOperationRefactoring) {
				MoveOperationRefactoring move = (MoveOperationRefactoring) r;
				if (parentContainers.contains(move.getMovedOperation()))
					return true;
			}
			else if (r.getRefactoringType().equals(RefactoringType.EXTRACT_AND_MOVE_OPERATION)) {
				ExtractOperationRefactoring extract = (ExtractOperationRefactoring) r;
				if (parentContainers.contains(extract.getExtractedOperation()))
					return true;
			}
		}
		return false;
	}

	private boolean isExtracted(Class rightClass, List<Refactoring> refactorings) {
		for (Refactoring r : refactorings) {
			if (r instanceof ExtractClassRefactoring) {
				ExtractClassRefactoring move = (ExtractClassRefactoring) r;
				if (move.getExtractedClass().equals(rightClass.getUmlClass()))
					return true;
			}
			else if (r instanceof ExtractSuperclassRefactoring) {
				ExtractSuperclassRefactoring move = (ExtractSuperclassRefactoring) r;
				if (move.getExtractedClass().equals(rightClass.getUmlClass()))
					return true;
			}
		}
		return false;
	}

	private void processLocallyRefactoredMethods(Map<Method, MethodTrackerChangeHistory> notFoundMethods, UMLModelDiff umlModelDiff, Version currentVersion, Version parentVersion, List<Refactoring> refactorings) throws RefactoringMinerTimedOutException {
		Set<CodeElement> alreadyProcessed = new HashSet<>();
		for (Method rightMethod : notFoundMethods.keySet()) {
			MethodTrackerChangeHistory startMethodChangeHistory = notFoundMethods.get(rightMethod);
			Method startMethod = startMethodChangeHistory.getStart();
			boolean moved = isMoved(rightMethod, refactorings);
			Set<Method> leftSideMethods = startMethodChangeHistory.analyseMethodRefactorings(refactorings, currentVersion, parentVersion, rightMethod::equalIdentifierIgnoringVersion);
			Set<Method> methodContainerChanged = startMethodChangeHistory.isMethodContainerChanged(umlModelDiff, refactorings, currentVersion, parentVersion, rightMethod::equalIdentifierIgnoringVersion, getClassMoveDiffList(umlModelDiff));
			leftSideMethods.addAll(methodContainerChanged);
			boolean refactored = !leftSideMethods.isEmpty();
			if (refactored || moved) {
				leftSideMethods.forEach(startMethodChangeHistory::addFirst);
				if (leftSideMethods.size() == 1) {
					startMethodChangeHistory.setCurrent(leftSideMethods.iterator().next());
				}
				for (CodeElement key2 : programElementMap.keySet()) {
					if (alreadyProcessed.contains(key2)) {
						continue;
					}
					if (key2 instanceof Block) {
						Block startBlock = (Block)key2;
						BlockTrackerChangeHistory startBlockChangeHistory = (BlockTrackerChangeHistory) programElementMap.get(startBlock);
						if (startBlock.getOperation().equals(startMethod.getUmlOperation()) ||
								matchingPeekMethod(rightMethod, startBlockChangeHistory)) {
							Block currentBlock = startBlockChangeHistory.peek();
							if (currentBlock == null) {
								continue;
							}
							Block rightBlock = rightMethod.findBlock(currentBlock::equalIdentifierIgnoringVersion);
							if (rightBlock == null) {
								continue;
							}
							startBlockChangeHistory.poll();
							alreadyProcessed.add(startBlock);
							if (moved) {
								startBlockChangeHistory.addedMethod(rightMethod, rightBlock, parentVersion);
								continue;
							}
							boolean found = startBlockChangeHistory.isMergeMultiMapping(currentVersion, parentVersion, rightMethod::equalIdentifierIgnoringVersion, rightBlock, refactorings);
							if (found) {
								continue;
							}
							found = startBlockChangeHistory.checkForExtractionOrInline(currentVersion, parentVersion, rightMethod::equalIdentifierIgnoringVersion, rightBlock, refactorings);
							if (found) {
								continue;
							}
							found = startBlockChangeHistory.checkRefactoredMethod(currentVersion, parentVersion, rightMethod::equalIdentifierIgnoringVersion, rightBlock, refactorings);
							if (found) {
								continue;
							}
							found = startBlockChangeHistory.checkBodyOfMatchedOperations(currentVersion, parentVersion, rightBlock::equalIdentifierIgnoringVersion, findBodyMapper(umlModelDiff, rightMethod, currentVersion, parentVersion));
							if (found) {
								nestedProgramElementMap.putAll(startBlockChangeHistory.getNested());
								continue;
							}
						}
					}
					else if (key2 instanceof Comment) {
						Comment startComment = (Comment)key2;
						CommentTrackerChangeHistory startCommentChangeHistory = (CommentTrackerChangeHistory) programElementMap.get(startComment);
						if (matchingMethod(startMethod, startComment) ||
								matchingPeekMethod(rightMethod, startCommentChangeHistory)) {
							Comment currentComment = startCommentChangeHistory.peek();
							if (currentComment == null) {
								continue;
							}
							Comment rightComment = rightMethod.findComment(currentComment::equalIdentifierIgnoringVersion);
							if (rightComment == null) {
								continue;
							}
							startCommentChangeHistory.poll();
							alreadyProcessed.add(startComment);
							if (moved) {
								startCommentChangeHistory.addedMethod(rightMethod, rightComment, parentVersion);
								continue;
							}
							boolean found = startCommentChangeHistory.checkForExtractionOrInline(currentVersion, parentVersion, rightMethod::equalIdentifierIgnoringVersion, rightComment, refactorings);
							if (found) {
								continue;
							}
							found = startCommentChangeHistory.checkRefactoredMethod(currentVersion, parentVersion, rightMethod::equalIdentifierIgnoringVersion, rightComment, refactorings);
							if (found) {
								continue;
							}
							found = startCommentChangeHistory.checkBodyOfMatchedOperations(currentVersion, parentVersion, rightComment::equalIdentifierIgnoringVersion, findBodyMapper(umlModelDiff, rightMethod, currentVersion, parentVersion));
							if (found) {
								continue;
							}
						}
					}
					else if (key2 instanceof Annotation) {
						Annotation startAnnotation = (Annotation)key2;
						AnnotationTrackerChangeHistory startAnnotationChangeHistory = (AnnotationTrackerChangeHistory) programElementMap.get(startAnnotation);
						if (matchingMethod(startMethod, startAnnotation) ||
								matchingPeekMethod(rightMethod, startAnnotationChangeHistory)) {
							Annotation currentAnnotation = startAnnotationChangeHistory.peek();
							if (currentAnnotation == null) {
								continue;
							}
							Annotation rightAnnotation = rightMethod.findAnnotation(currentAnnotation::equalIdentifierIgnoringVersion);
							if (rightAnnotation == null) {
								continue;
							}
							startAnnotationChangeHistory.poll();
							alreadyProcessed.add(startAnnotation);
							if (moved) {
								startAnnotationChangeHistory.addedMethod(rightMethod, rightAnnotation, parentVersion);
								continue;
							}
							boolean found = startAnnotationChangeHistory.checkForExtractionOrInline(currentVersion, parentVersion, rightMethod::equalIdentifierIgnoringVersion, rightAnnotation, refactorings);
							if (found) {
								continue;
							}
							found = startAnnotationChangeHistory.checkRefactoredMethod(currentVersion, parentVersion, rightMethod::equalIdentifierIgnoringVersion, rightAnnotation, refactorings);
							if (found) {
								continue;
							}
							found = startAnnotationChangeHistory.checkBodyOfMatchedOperations(currentVersion, parentVersion, rightAnnotation::equalIdentifierIgnoringVersion, findBodyMapper(umlModelDiff, rightMethod, currentVersion, parentVersion));
							if (found) {
								continue;
							}
						}
					}
				}
				if (startMethodChangeHistory.peek() != null && startMethodChangeHistory.peek().isAdded() && startMethodChangeHistory.getSourceOperation() != null) {
					Method codeElement = startMethodChangeHistory.getSourceOperation();
					boolean found = false;
					for (CodeElement key2 : programElementMap.keySet()) {
						if (key2 instanceof Method) {
							Method method = (Method)key2;
							MethodTrackerChangeHistory methodChangeHistory = (MethodTrackerChangeHistory) programElementMap.get(method);
							if (codeElement.getUmlOperation().equals(method.getUmlOperation())) {
								found = true;
								break;
							}
							if (codeElement.getUmlOperation() instanceof UMLOperation && method.getUmlOperation() instanceof UMLOperation &&
									((UMLOperation)codeElement.getUmlOperation()).equalSignature((UMLOperation)method.getUmlOperation())) {
								found = true;
								break;
							}
							if (!methodChangeHistory.isEmpty() && codeElement.getUmlOperation().equals(methodChangeHistory.peek().getUmlOperation())) {
								found = true;
								break;
							}
							if (methodChangeHistory.getCurrent() != null && codeElement.getUmlOperation().equals(methodChangeHistory.getCurrent().getUmlOperation())) {
								found = true;
								break;
							}
						}
					}
					if (!found && remainingBlames(startMethod).size() > 0) {
						AbstractChangeHistory<BaseCodeElement> changeHistory = (AbstractChangeHistory<BaseCodeElement>) factory(codeElement);
						changeHistory.addFirst((BaseCodeElement) codeElement);
						changeHistory.get().addNode((BaseCodeElement) codeElement);
						programElementMap.put(codeElement, changeHistory);
					}
				}
				if (moved) {
					startMethodChangeHistory.handleAddOperation(currentVersion, parentVersion, rightMethod::equalIdentifierIgnoringVersion, rightMethod.getUmlOperation(), "moved method");
				}
			}
			else if(startMethodChangeHistory.isMethodAdded(umlModelDiff, rightMethod.getUmlOperation().getLocationInfo().getSourceFolder(), rightMethod.getUmlOperation().getClassName(), currentVersion, parentVersion, rightMethod::equalIdentifierIgnoringVersion, getAllClassesDiff(umlModelDiff))) {
				for (CodeElement key2 : programElementMap.keySet()) {
					if (key2 instanceof Block) {
						Block startBlock = (Block)key2;
						BlockTrackerChangeHistory startBlockChangeHistory = (BlockTrackerChangeHistory) programElementMap.get(startBlock);
						if (startBlock.getOperation().equals(startMethod.getUmlOperation()) ||
								matchingPeekMethod(rightMethod, startBlockChangeHistory)) {
							Block currentBlock = startBlockChangeHistory.poll();
							if (currentBlock == null) {
								continue;
							}
							Block rightBlock = rightMethod.findBlock(currentBlock::equalIdentifierIgnoringVersion);
							if (rightBlock == null) {
								continue;
							}
							startBlockChangeHistory.addedMethod(rightMethod, rightBlock, parentVersion);
						}
					}
					else if (key2 instanceof Comment) {
						Comment startComment = (Comment)key2;
						CommentTrackerChangeHistory startCommentChangeHistory = (CommentTrackerChangeHistory) programElementMap.get(startComment);
						if (matchingMethod(rightMethod, startComment) || matchingPeekMethod(rightMethod, startCommentChangeHistory)) {
							Comment currentComment = startCommentChangeHistory.poll();
							if (currentComment == null) {
								continue;
							}
							Comment rightComment = rightMethod.findComment(currentComment::equalIdentifierIgnoringVersion);
							if (rightComment == null) {
								continue;
							}
							startCommentChangeHistory.addedMethod(rightMethod, rightComment, parentVersion);
						}
					}
					else if (key2 instanceof Annotation) {
						Annotation startAnnotation = (Annotation)key2;
						AnnotationTrackerChangeHistory startAnnotationChangeHistory = (AnnotationTrackerChangeHistory) programElementMap.get(startAnnotation);
						if (matchingMethod(rightMethod, startAnnotation) ||
								matchingPeekMethod(rightMethod, startAnnotationChangeHistory)) {
							Annotation currentAnnotation = startAnnotationChangeHistory.poll();
							if (currentAnnotation == null) {
								continue;
							}
							Annotation rightAnnotation = rightMethod.findAnnotation(currentAnnotation::equalIdentifierIgnoringVersion);
							if (rightAnnotation == null) {
								continue;
							}
							startAnnotationChangeHistory.addedMethod(rightMethod, rightAnnotation, parentVersion);
						}
					}
				}
			}
		}
	}

	private boolean matchingMethod(Method rightMethod, Comment startComment) {
		if(startComment.getOperation().isPresent()) {
			VariableDeclarationContainer container = startComment.getOperation().get();
			if(container instanceof UMLOperation) {
				return container.equals(rightMethod.getUmlOperation());
			}
			else if(container instanceof UMLInitializer && rightMethod.getUmlOperation() instanceof UMLInitializer) {
				UMLInitializer init1 = (UMLInitializer)container;
				UMLInitializer init2 = (UMLInitializer)rightMethod.getUmlOperation();
				return init1.getClassName().equals(init2.getClassName()) && init1.isStatic() == init2.isStatic();
			}
		}
		return false;
	}

	private boolean matchingMethod(Method rightMethod, Annotation startAnnotation) {
		if(startAnnotation.getOperation().isPresent()) {
			VariableDeclarationContainer container = startAnnotation.getOperation().get();
			if(container instanceof UMLOperation) {
				return container.equals(rightMethod.getUmlOperation());
			}
			else if(container instanceof UMLInitializer && rightMethod.getUmlOperation() instanceof UMLInitializer) {
				UMLInitializer init1 = (UMLInitializer)container;
				UMLInitializer init2 = (UMLInitializer)rightMethod.getUmlOperation();
				return init1.getClassName().equals(init2.getClassName()) && init1.isStatic() == init2.isStatic();
			}
		}
		return false;
	}

	private boolean matchingPeekMethod(Method rightMethod, BlockTrackerChangeHistory startBlockChangeHistory) {
		if(!startBlockChangeHistory.isEmpty()) {
			VariableDeclarationContainer container = startBlockChangeHistory.peek().getOperation();
			if(rightMethod.getUmlOperation().equals(container)) {
				return true;
			}
			if(rightMethod.getUmlOperation() instanceof UMLOperation && container instanceof UMLOperation) {
				return ((UMLOperation)rightMethod.getUmlOperation()).equalsIgoringTypeParameters((UMLOperation)container);
			}
		}
		return false;
	}

	private boolean matchingPeekMethod(Method rightMethod, CommentTrackerChangeHistory startCommentChangeHistory) {
		if(!startCommentChangeHistory.isEmpty() && startCommentChangeHistory.peek().getOperation().isPresent()) {
			VariableDeclarationContainer container = startCommentChangeHistory.peek().getOperation().get();
			if(rightMethod.getUmlOperation().equals(container)) {
				return true;
			}
			if(rightMethod.getUmlOperation() instanceof UMLOperation && container instanceof UMLOperation) {
				return ((UMLOperation)rightMethod.getUmlOperation()).equalsIgoringTypeParameters((UMLOperation)container);
			}
		}
		return false;
	}


	private boolean matchingPeekMethod(Method rightMethod, AnnotationTrackerChangeHistory startAnnotationChangeHistory) {
		if(!startAnnotationChangeHistory.isEmpty() && startAnnotationChangeHistory.peek().getOperation().isPresent()) {
			VariableDeclarationContainer container = startAnnotationChangeHistory.peek().getOperation().get();
			if(rightMethod.getUmlOperation().equals(container)) {
				return true;
			}
			if(rightMethod.getUmlOperation() instanceof UMLOperation && container instanceof UMLOperation) {
				return ((UMLOperation)rightMethod.getUmlOperation()).equalsIgoringTypeParameters((UMLOperation)container);
			}
		}
		return false;
	}

	private void processInnerClassesWithSameSignature(UMLModel rightModel, Version currentVersion, UMLModel leftModel, Version parentVersion, Class startClass, 
			Set<Pair<Class, Class>> foundInnerClasses, Map<Class, ClassTrackerChangeHistory> notFoundInnerClasses) {	
		for (CodeElement key : programElementMap.keySet()) {
			if (key instanceof Class && !key.equals(startClass)) {
				Class startInnerClass = (Class)key;
				ClassTrackerChangeHistory startInnerClassChangeHistory = (ClassTrackerChangeHistory) programElementMap.get(startInnerClass);
				Class currentClass = startInnerClassChangeHistory.peek();
				if (currentClass == null) {
					currentClass = startInnerClassChangeHistory.getCurrent();
				}
				if (currentClass == null || currentClass.isAdded()) {
					continue;
				}
				Class rightClass = getClass(rightModel, currentVersion, currentClass::equalIdentifierIgnoringVersion);
				if (rightClass == null) {
					continue;
				}
				startInnerClassChangeHistory.poll();
				Class leftClass = getClass(leftModel, parentVersion, rightClass::equalIdentifierIgnoringVersion);
				if (leftClass != null) {
					UMLType leftSuperclass = leftClass.getUmlClass().getSuperclass();
					UMLType rightSuperclass = rightClass.getUmlClass().getSuperclass();
					if (leftSuperclass != null && rightSuperclass != null) {
                		if (!leftSuperclass.equals(rightSuperclass)) {
                			startInnerClassChangeHistory.get().addChange(leftClass, rightClass, ChangeFactory.forClass(Change.Type.SUPERCLASS_CHANGE));
                			startInnerClassChangeHistory.get().connectRelatedNodes();
                		}
                	}
					else if (leftSuperclass != null && rightSuperclass == null) {
						startInnerClassChangeHistory.get().addChange(leftClass, rightClass, ChangeFactory.forClass(Change.Type.SUPERCLASS_CHANGE));
						startInnerClassChangeHistory.get().connectRelatedNodes();
					}
					else if (leftSuperclass == null && rightSuperclass != null) {
						startInnerClassChangeHistory.get().addChange(leftClass, rightClass, ChangeFactory.forClass(Change.Type.SUPERCLASS_CHANGE));
						startInnerClassChangeHistory.get().connectRelatedNodes();
					}
					if (!leftClass.getUmlClass().getImplementedInterfaces().equals(rightClass.getUmlClass().getImplementedInterfaces())) {
						startInnerClassChangeHistory.get().addChange(leftClass, rightClass, ChangeFactory.forClass(Change.Type.INTERFACE_LIST_CHANGE));
						startInnerClassChangeHistory.get().connectRelatedNodes();
					}
					checkSignatureFormatChange(startInnerClassChangeHistory, leftClass, rightClass);
					startInnerClassChangeHistory.setCurrent(leftClass);
					startInnerClassChangeHistory.addFirst(leftClass);
					foundInnerClasses.add(Pair.of(leftClass, rightClass));
				}
				else {
					notFoundInnerClasses.put(rightClass, startInnerClassChangeHistory);
				}
			}
		}
	}

	private void processLocallyRefactoredInnerClasses(Map<Class, ClassTrackerChangeHistory> notFoundInnerClasses, UMLModelDiff umlModelDiff, Version currentVersion, Version parentVersion, List<Refactoring> refactorings) throws RefactoringMinerTimedOutException {
		for (Class rightInnerClass : notFoundInnerClasses.keySet()) {
			String rightInnerClassName = rightInnerClass.getUmlClass().getName();
            String rightInnerClassSourceFolder = rightInnerClass.getUmlClass().getLocationInfo().getSourceFolder();
			ClassTrackerChangeHistory startInnerClassChangeHistory = notFoundInnerClasses.get(rightInnerClass);
			Set<Class> classRefactored = startInnerClassChangeHistory.analyseClassRefactorings(refactorings, currentVersion, parentVersion, rightInnerClass::equalIdentifierIgnoringVersion);
			Set<Class> innerClassContainerChanged = startInnerClassChangeHistory.isInnerClassContainerChanged(umlModelDiff, refactorings, currentVersion, parentVersion, rightInnerClass::equalIdentifierIgnoringVersion, getClassMoveDiffList(umlModelDiff));
			classRefactored.addAll(innerClassContainerChanged);
			boolean refactored = !classRefactored.isEmpty();
			if (refactored) {
				Set<Class> leftSideClasses = new HashSet<>(classRefactored);
				leftSideClasses.forEach(startInnerClassChangeHistory::addFirst);
				if (leftSideClasses.size() == 1) {
					startInnerClassChangeHistory.setCurrent(leftSideClasses.iterator().next());
				}
				for (CodeElement key : programElementMap.keySet()) {
					if (key instanceof Comment) {
						Comment startComment = (Comment)key;
						CommentTrackerChangeHistory startCommentChangeHistory = (CommentTrackerChangeHistory) programElementMap.get(startComment);
						if ((startComment.getClazz().isPresent() && startComment.getClazz().get().equals(rightInnerClass.getUmlClass())) ||
								(!startCommentChangeHistory.isEmpty() && startCommentChangeHistory.peek().getClazz().isPresent() && rightInnerClass.getUmlClass().equals(startCommentChangeHistory.peek().getClazz().get()))) {
							Comment currentComment = startCommentChangeHistory.peek();
							if (currentComment == null || currentComment.isAdded()) {
								continue;
							}
							Comment rightComment = rightInnerClass.findComment(currentComment::equalIdentifierIgnoringVersion);
							if (rightComment == null) {
								continue;
							}
							startCommentChangeHistory.poll();
							UMLAbstractClassDiff innerClassDiff = getUMLClassDiff(umlModelDiff, rightInnerClassSourceFolder, rightInnerClassName);
							startCommentChangeHistory.checkBodyOfMatchedClasses(currentVersion, parentVersion, rightComment::equalIdentifierIgnoringVersion, innerClassDiff);
						}
					}
				}
			}
			else if (startInnerClassChangeHistory.isClassAdded(umlModelDiff, currentVersion, parentVersion, rightInnerClass::equalIdentifierIgnoringVersion)) {
				for (CodeElement key : programElementMap.keySet()) {
					if (key instanceof Comment) {
						Comment startComment = (Comment)key;
						CommentTrackerChangeHistory startCommentChangeHistory = (CommentTrackerChangeHistory) programElementMap.get(startComment);
						if ((startComment.getClazz().isPresent() && startComment.getClazz().get().equals(rightInnerClass.getUmlClass())) ||
								(!startCommentChangeHistory.isEmpty() && startCommentChangeHistory.peek().getClazz().isPresent() && rightInnerClass.getUmlClass().equals(startCommentChangeHistory.peek().getClazz().get()))) {
							Comment currentComment = startCommentChangeHistory.peek();
							if (currentComment == null || currentComment.isAdded()) {
								continue;
							}
							Comment rightComment = rightInnerClass.findComment(currentComment::equalIdentifierIgnoringVersion);
							if (rightComment == null) {
								continue;
							}
							startCommentChangeHistory.poll();
							startCommentChangeHistory.addedClass(rightInnerClass, rightComment, parentVersion);
						}
					}
				}
			}
		}
	}

	private Map<Attribute, AttributeTrackerChangeHistory> processAttributesWithSameSignature(UMLModel rightModel, Version currentVersion, UMLModel leftModel, Version parentVersion) {
		Map<Attribute, AttributeTrackerChangeHistory> notFoundAttributes = new LinkedHashMap<>();
		for (CodeElement key : programElementMap.keySet()) {
			if (key instanceof Attribute) {
				Attribute startAttribute = (Attribute)key;
				AttributeTrackerChangeHistory startAttributeChangeHistory = (AttributeTrackerChangeHistory) programElementMap.get(startAttribute);
				Attribute currentAttribute = startAttributeChangeHistory.poll();
				if (currentAttribute == null) {
					currentAttribute = startAttributeChangeHistory.getCurrent();
				}
				if (currentAttribute == null || currentAttribute.isAdded()) {
					continue;
				}
				Attribute rightAttribute = getAttribute(rightModel, currentVersion, currentAttribute::equalIdentifierIgnoringVersion);
				if (rightAttribute == null) {
					continue;
				}
				Attribute leftAttribute = getAttribute(leftModel, parentVersion, rightAttribute::equalIdentifierIgnoringVersion);
				if (leftAttribute != null) {
					startAttributeChangeHistory.setCurrent(leftAttribute);
					startAttributeChangeHistory.checkInitializerChange(rightAttribute, leftAttribute);
					checkSignatureFormatChange(startAttributeChangeHistory, leftAttribute, rightAttribute);
					for (CodeElement key2 : programElementMap.keySet()) {
						if (key2 instanceof Comment) {
							Comment startComment = (Comment)key2;
							CommentTrackerChangeHistory startCommentChangeHistory = (CommentTrackerChangeHistory) programElementMap.get(startComment);
							if ((startComment.getOperation().isPresent() && startComment.getOperation().get().equals(rightAttribute.getUmlAttribute())) ||
									(!startCommentChangeHistory.isEmpty() && startCommentChangeHistory.peek().getOperation().isPresent() && rightAttribute.getUmlAttribute().equals(startCommentChangeHistory.peek().getOperation().get()))) {
								Comment currentComment = startCommentChangeHistory.peek();
								if (currentComment == null) {
									continue;
								}
								Comment rightComment = rightAttribute.findComment(currentComment::equalIdentifierIgnoringVersion);
								if (rightComment == null) {
									continue;
								}
								startCommentChangeHistory.poll();
								Pair<UMLAttribute, UMLAttribute> pair = Pair.of(leftAttribute.getUmlAttribute(), rightAttribute.getUmlAttribute());
							    startCommentChangeHistory.checkBodyOfMatchedAttributes(currentVersion, parentVersion, rightComment::equalIdentifierIgnoringVersion, pair);
							}
						}
						else if (key2 instanceof Annotation) {
							Annotation startAnnotation = (Annotation)key2;
							AnnotationTrackerChangeHistory startAnnotationChangeHistory = (AnnotationTrackerChangeHistory) programElementMap.get(startAnnotation);
							if ((startAnnotation.getOperation().isPresent() && startAnnotation.getOperation().get().equals(rightAttribute.getUmlAttribute())) ||
									(!startAnnotationChangeHistory.isEmpty() && startAnnotationChangeHistory.peek().getOperation().isPresent() && rightAttribute.getUmlAttribute().equals(startAnnotationChangeHistory.peek().getOperation().get()))) {
								Annotation currentAnnotation = startAnnotationChangeHistory.peek();
								if (currentAnnotation == null) {
									continue;
								}
								Annotation rightAnnotation = rightAttribute.findAnnotation(currentAnnotation::equalIdentifierIgnoringVersion);
								if (rightAnnotation == null) {
									continue;
								}
								startAnnotationChangeHistory.poll();
								Pair<VariableDeclarationContainer, VariableDeclarationContainer> pair = Pair.of(leftAttribute.getUmlAttribute(), rightAttribute.getUmlAttribute());
							    startAnnotationChangeHistory.checkBodyOfMatched(currentVersion, parentVersion, rightAnnotation::equalIdentifierIgnoringVersion, pair);
							}
						}
					}
				}
				else {
					notFoundAttributes.put(rightAttribute, startAttributeChangeHistory);
				}
			}
		}
		return notFoundAttributes;
	}

	private Map<Method, MethodTrackerChangeHistory> processMethodsWithSameSignature(UMLModel rightModel, Version currentVersion, UMLModel leftModel, Version parentVersion) throws RefactoringMinerTimedOutException {
		Map<Method, MethodTrackerChangeHistory> notFoundMethods = new LinkedHashMap<>();
		for (CodeElement key : programElementMap.keySet()) {
			if (key instanceof Method) {
				Method startMethod = (Method)key;
				MethodTrackerChangeHistory startMethodChangeHistory = (MethodTrackerChangeHistory) programElementMap.get(startMethod);
				if (startMethodChangeHistory.elements.size() > 1) {
					Iterator<Method> iterator = startMethodChangeHistory.elements.iterator();
					Set<Pair<Method, Method>> methodPairs = new LinkedHashSet<Pair<Method,Method>>();
					while (iterator.hasNext()) {
						Method currentMethod = iterator.next();
						Method rightMethod = getMethod(rightModel, currentVersion, currentMethod::equalIdentifierIgnoringVersion);
						if (rightMethod == null) {
							continue;
						}
						//NO CHANGE
						Method leftMethod = getMethod(leftModel, parentVersion, rightMethod::equalIdentifierIgnoringVersion);
						if (leftMethod != null) {
							checkIfJavadocChanged(currentVersion, parentVersion, startMethod, rightMethod, leftMethod);
							checkSignatureFormatChange(startMethodChangeHistory, leftMethod, rightMethod);
							if (leftMethod.getUmlOperation() instanceof UMLOperation && rightMethod.getUmlOperation() instanceof UMLOperation) {
	                			UMLOperation leftOperation = (UMLOperation)leftMethod.getUmlOperation();
	                			UMLOperation rightOperation = (UMLOperation)rightMethod.getUmlOperation();
	                			if (!leftOperation.getTypeParameters().equals(rightOperation.getTypeParameters())) {
	                				startMethodChangeHistory.get().addChange(leftMethod, rightMethod, ChangeFactory.forMethod(Change.Type.TYPE_PARAMETER_CHANGE));
	                			}
	                		}
							continue;
						}
						//CHANGE BODY OR DOCUMENT
						leftMethod = getMethod(leftModel, parentVersion, rightMethod::equalIdentifierIgnoringVersionAndDocumentAndBody);
						if (leftMethod == null) {
							leftMethod = getMethod(leftModel, parentVersion, rightMethod::equalIdentifierIgnoringVersionAndAnnotation);
						}
						if (leftMethod == null) {
							notFoundMethods.put(rightMethod, startMethodChangeHistory);
						}
						else {
							if (!leftMethod.equalBody(rightMethod))
								startMethodChangeHistory.get().addChange(leftMethod, rightMethod, ChangeFactory.forMethod(Change.Type.BODY_CHANGE));
							if (!leftMethod.equalDocuments(rightMethod))
								startMethodChangeHistory.get().addChange(leftMethod, rightMethod, ChangeFactory.forMethod(Change.Type.DOCUMENTATION_CHANGE));
							checkIfJavadocChanged(currentVersion, parentVersion, startMethod, rightMethod, leftMethod);
							checkSignatureFormatChange(startMethodChangeHistory, leftMethod, rightMethod);
							if (leftMethod.getUmlOperation() instanceof UMLOperation && rightMethod.getUmlOperation() instanceof UMLOperation) {
	                			UMLOperation leftOperation = (UMLOperation)leftMethod.getUmlOperation();
	                			UMLOperation rightOperation = (UMLOperation)rightMethod.getUmlOperation();
	                			if (!leftOperation.getTypeParameters().equals(rightOperation.getTypeParameters())) {
	                				startMethodChangeHistory.get().addChange(leftMethod, rightMethod, ChangeFactory.forMethod(Change.Type.TYPE_PARAMETER_CHANGE));
	                			}
	                		}
							startMethodChangeHistory.get().connectRelatedNodes();
							startMethodChangeHistory.elements.remove(currentMethod);
							startMethodChangeHistory.elements.add(leftMethod);
							methodPairs.add(Pair.of(leftMethod, rightMethod));
						}
					}
					if (methodPairs.size() > 0) {
						processNestedStatementsAndComments(rightModel, currentVersion, leftModel, parentVersion,
								startMethod, methodPairs);
					}
				}
				else {
					Method currentMethod = startMethodChangeHistory.poll();
					if (currentMethod == null) {
						currentMethod = startMethodChangeHistory.getCurrent();
					}
					if (currentMethod == null || currentMethod.isAdded()) {
						continue;
					}
					Method rightMethod = getMethod(rightModel, currentVersion, currentMethod::equalIdentifierIgnoringVersion);
					if (rightMethod == null) {
						continue;
					}
					//NO CHANGE
					Method leftMethod = getMethod(leftModel, parentVersion, rightMethod::equalIdentifierIgnoringVersion);
					if (leftMethod != null) {
						checkIfJavadocChanged(currentVersion, parentVersion, startMethod, rightMethod, leftMethod);
						checkSignatureFormatChange(startMethodChangeHistory, leftMethod, rightMethod);
						if (leftMethod.getUmlOperation() instanceof UMLOperation && rightMethod.getUmlOperation() instanceof UMLOperation) {
                			UMLOperation leftOperation = (UMLOperation)leftMethod.getUmlOperation();
                			UMLOperation rightOperation = (UMLOperation)rightMethod.getUmlOperation();
                			if (!leftOperation.getTypeParameters().equals(rightOperation.getTypeParameters())) {
                				startMethodChangeHistory.get().addChange(leftMethod, rightMethod, ChangeFactory.forMethod(Change.Type.TYPE_PARAMETER_CHANGE));
                			}
                		}
						startMethodChangeHistory.setCurrent(leftMethod);
						int leftLines = leftMethod.getLocation().getEndLine() - leftMethod.getLocation().getStartLine();
						int rightLines = rightMethod.getLocation().getEndLine() - rightMethod.getLocation().getStartLine();
						OperationBody leftBody = leftMethod.getUmlOperation().getBody();
						OperationBody rightBody = rightMethod.getUmlOperation().getBody();
						boolean differInBodyLines = false;
						if(leftBody != null && rightBody != null) {
							int leftBodyLines = leftBody.getCompositeStatement().getLocationInfo().getEndLine() - leftBody.getCompositeStatement().getLocationInfo().getStartLine();
							int rightBodyLines = rightBody.getCompositeStatement().getLocationInfo().getEndLine() - rightBody.getCompositeStatement().getLocationInfo().getStartLine();
							differInBodyLines = leftBodyLines != rightBodyLines;
						}
						if (leftLines != rightLines || differInBodyLines) {
							processNestedStatementsAndComments(rightModel, currentVersion, leftModel, parentVersion,
									startMethod, rightMethod, leftMethod);
						}
						continue;
					}
					//CHANGE BODY OR DOCUMENT
					leftMethod = getMethod(leftModel, parentVersion, rightMethod::equalIdentifierIgnoringVersionAndDocumentAndBody);
					if (leftMethod == null) {
						notFoundMethods.put(rightMethod, startMethodChangeHistory);
					}
					else {
						if (!leftMethod.equalBody(rightMethod))
							startMethodChangeHistory.get().addChange(leftMethod, rightMethod, ChangeFactory.forMethod(Change.Type.BODY_CHANGE));
						if (!leftMethod.equalDocuments(rightMethod))
							startMethodChangeHistory.get().addChange(leftMethod, rightMethod, ChangeFactory.forMethod(Change.Type.DOCUMENTATION_CHANGE));
						checkIfJavadocChanged(currentVersion, parentVersion, startMethod, rightMethod, leftMethod);
						checkSignatureFormatChange(startMethodChangeHistory, leftMethod, rightMethod);
						if (leftMethod.getUmlOperation() instanceof UMLOperation && rightMethod.getUmlOperation() instanceof UMLOperation) {
                			UMLOperation leftOperation = (UMLOperation)leftMethod.getUmlOperation();
                			UMLOperation rightOperation = (UMLOperation)rightMethod.getUmlOperation();
                			if (!leftOperation.getTypeParameters().equals(rightOperation.getTypeParameters())) {
                				startMethodChangeHistory.get().addChange(leftMethod, rightMethod, ChangeFactory.forMethod(Change.Type.TYPE_PARAMETER_CHANGE));
                			}
                		}
						startMethodChangeHistory.get().connectRelatedNodes();
						startMethodChangeHistory.setCurrent(leftMethod);
						processNestedStatementsAndComments(rightModel, currentVersion, leftModel, parentVersion,
								startMethod, rightMethod, leftMethod);
					}
				}
			}
		}
		return notFoundMethods;
	}

	private void checkSignatureFormatChange(ClassTrackerChangeHistory startClassChangeHistory, Class leftClass, Class rightClass) {
		if (leftClass.differInFormatting(rightClass)) {
			startClassChangeHistory.get().addChange(leftClass, rightClass, ChangeFactory.forClass(Change.Type.SIGNATURE_FORMAT_CHANGE));
			startClassChangeHistory.get().connectRelatedNodes();
		}
		startClassChangeHistory.processChange(leftClass, rightClass);
	}

	private void checkSignatureFormatChange(MethodTrackerChangeHistory startMethodChangeHistory, Method leftMethod, Method rightMethod) {
		if (leftMethod.differInFormatting(rightMethod)) {
			startMethodChangeHistory.get().addChange(leftMethod, rightMethod, ChangeFactory.forMethod(Change.Type.SIGNATURE_FORMAT_CHANGE));
			startMethodChangeHistory.processChange(leftMethod, rightMethod);
			startMethodChangeHistory.get().connectRelatedNodes();
		}
	}

	private void checkSignatureFormatChange(AttributeTrackerChangeHistory startAtributeChangeHistory, Attribute leftAttribute, Attribute rightAttribute) {
		if (leftAttribute.differInFormatting(rightAttribute)) {
			startAtributeChangeHistory.get().addChange(leftAttribute, rightAttribute, ChangeFactory.forAttribute(Change.Type.SIGNATURE_FORMAT_CHANGE));
			startAtributeChangeHistory.processChange(leftAttribute, rightAttribute);
			startAtributeChangeHistory.get().connectRelatedNodes();
		}
	}

	private void checkIfJavadocChanged(Version currentVersion, Version parentVersion, Method startMethod,
			Method rightMethod, Method leftMethod) {
		UMLJavadoc leftJavadoc = leftMethod.getUmlOperation().getJavadoc();
		UMLJavadoc rightJavadoc = rightMethod.getUmlOperation().getJavadoc();
		if (leftJavadoc != null && rightJavadoc != null && !leftJavadoc.getFullText().equals(rightJavadoc.getFullText())) {
			Comment leftComment = Comment.of(leftJavadoc, leftMethod.getUmlOperation(), parentVersion);
			Comment rightComment = Comment.of(rightJavadoc, rightMethod.getUmlOperation(), currentVersion);
			for (CodeElement key2 : programElementMap.keySet()) {
				if (key2 instanceof Comment) {
					Comment startComment = (Comment)key2;
					if (startComment.getLocation().getCodeElementType().equals(CodeElementType.JAVADOC)) {
						CommentTrackerChangeHistory startCommentChangeHistory = (CommentTrackerChangeHistory) programElementMap.get(startComment);
						if (matchingMethod(startMethod, startComment) ||
								matchingPeekMethod(rightMethod, startCommentChangeHistory)) {
							Comment currentComment = startCommentChangeHistory.peek();
							if (currentComment == null) {
								continue;
							}
							startCommentChangeHistory.poll();
							startCommentChangeHistory.processChange(leftComment, rightComment);
							startCommentChangeHistory.addFirst(leftComment);
							startCommentChangeHistory.get().connectRelatedNodes();
						}
					}
				}
			}
		}
		else if (leftJavadoc == null && rightJavadoc != null) {
			Comment rightComment = Comment.of(rightJavadoc, rightMethod.getUmlOperation(), currentVersion);
			for (CodeElement key2 : programElementMap.keySet()) {
				if (key2 instanceof Comment) {
					Comment startComment = (Comment)key2;
					if (startComment.getLocation().getCodeElementType().equals(CodeElementType.JAVADOC)) {
						CommentTrackerChangeHistory startCommentChangeHistory = (CommentTrackerChangeHistory) programElementMap.get(startComment);
						if (matchingMethod(startMethod, startComment) ||
								matchingPeekMethod(rightMethod, startCommentChangeHistory)) {
							Comment currentComment = startCommentChangeHistory.peek();
							if (currentComment == null) {
								continue;
							}
							startCommentChangeHistory.poll();
							Comment commentBefore = Comment.of(rightComment.getComment(), rightComment.getOperation().get(), parentVersion);
							startCommentChangeHistory.get().handleAdd(commentBefore, rightComment, "new javadoc");
							startCommentChangeHistory.add(commentBefore);
							startCommentChangeHistory.get().connectRelatedNodes();
						}
					}
				}
			}
		}
	}

	private void processNestedStatementsAndComments(UMLModel rightModel, Version currentVersion, UMLModel leftModel,
			Version parentVersion, Method startMethod, Set<Pair<Method, Method>> methodPairs)
			throws RefactoringMinerTimedOutException {
		Map<Pair<Method, Method>, UMLOperationBodyMapper> mappers = new LinkedHashMap<Pair<Method,Method>, UMLOperationBodyMapper>();
		for (Pair<Method, Method> pair : methodPairs) {
			VariableDeclarationContainer leftOperation = pair.getLeft().getUmlOperation();
			VariableDeclarationContainer rightOperation = pair.getRight().getUmlOperation();
			UMLOperationBodyMapper bodyMapper = null;
			if (leftOperation instanceof UMLOperation && rightOperation instanceof UMLOperation) {
				UMLClassBaseDiff lightweightClassDiff = lightweightClassDiff(leftModel, rightModel, leftOperation, rightOperation);
				bodyMapper = new UMLOperationBodyMapper((UMLOperation) leftOperation, (UMLOperation) rightOperation, lightweightClassDiff);
			}
			else if (leftOperation instanceof UMLInitializer && rightOperation instanceof UMLInitializer) {
				UMLClassBaseDiff lightweightClassDiff = lightweightClassDiff(leftModel, rightModel, leftOperation, rightOperation);
				bodyMapper = new UMLOperationBodyMapper((UMLInitializer) leftOperation, (UMLInitializer) rightOperation, lightweightClassDiff);
			}
			mappers.put(pair, bodyMapper);
		}
		Set<CodeElement> alreadyProcessed = new HashSet<>();
		for (Pair<Method, Method> pair : methodPairs) {
			Method rightMethod = pair.getRight();
			UMLOperationBodyMapper bodyMapper = mappers.get(pair);
			for (CodeElement key2 : programElementMap.keySet()) {
				if (alreadyProcessed.contains(key2)) {
					continue;
				}
				if (key2 instanceof Block) {
					Block startBlock = (Block)key2;
					BlockTrackerChangeHistory startBlockChangeHistory = (BlockTrackerChangeHistory) programElementMap.get(startBlock);
					if (startBlock.getOperation().equals(startMethod.getUmlOperation()) ||
							matchingPeekMethod(rightMethod, startBlockChangeHistory)) {
						Block currentBlock = startBlockChangeHistory.peek();
						if (currentBlock == null) {
							continue;
						}
						Block rightBlock = rightMethod.findBlock(currentBlock::equalIdentifierIgnoringVersion);
						if (rightBlock == null) {
							continue;
						}
						startBlockChangeHistory.poll();
						alreadyProcessed.add(startBlock);
						if (startBlockChangeHistory.checkBodyOfMatchedOperations(currentVersion, parentVersion, rightBlock::equalIdentifierIgnoringVersion, bodyMapper)) {
							nestedProgramElementMap.putAll(startBlockChangeHistory.getNested());
							continue;
						}
					}
				}
				else if (key2 instanceof Comment) {
					Comment startComment = (Comment)key2;
					CommentTrackerChangeHistory startCommentChangeHistory = (CommentTrackerChangeHistory) programElementMap.get(startComment);
					if (matchingMethod(startMethod, startComment) ||
							matchingPeekMethod(rightMethod, startCommentChangeHistory)) {
						Comment currentComment = startCommentChangeHistory.peek();
						if (currentComment == null) {
							continue;
						}
						Comment rightComment = rightMethod.findComment(currentComment::equalIdentifierIgnoringVersion);
						if (rightComment == null) {
							continue;
						}
						startCommentChangeHistory.poll();
						alreadyProcessed.add(startComment);
						if (startCommentChangeHistory.checkBodyOfMatchedOperations(currentVersion, parentVersion, rightComment::equalIdentifierIgnoringVersion, bodyMapper)) {
							continue;
						}
					}
				}
				else if (key2 instanceof Annotation) {
					Annotation startAnnotation = (Annotation)key2;
					AnnotationTrackerChangeHistory startAnnotationChangeHistory = (AnnotationTrackerChangeHistory) programElementMap.get(startAnnotation);
					if (matchingMethod(startMethod, startAnnotation) ||
							matchingPeekMethod(rightMethod, startAnnotationChangeHistory)) {
						Annotation currentAnnotation = startAnnotationChangeHistory.peek();
						if (currentAnnotation == null) {
							continue;
						}
						Annotation rightAnnotation = rightMethod.findAnnotation(currentAnnotation::equalIdentifierIgnoringVersion);
						if (rightAnnotation == null) {
							continue;
						}
						startAnnotationChangeHistory.poll();
						alreadyProcessed.add(startAnnotation);
						if (startAnnotationChangeHistory.checkBodyOfMatchedOperations(currentVersion, parentVersion, rightAnnotation::equalIdentifierIgnoringVersion, bodyMapper)) {
							continue;
						}
					}
				}
			}
		}
	}

	private void processNestedStatementsAndComments(UMLModel rightModel, Version currentVersion, UMLModel leftModel,
			Version parentVersion, Method startMethod, Method rightMethod, Method leftMethod)
			throws RefactoringMinerTimedOutException {
		VariableDeclarationContainer leftOperation = leftMethod.getUmlOperation();
		VariableDeclarationContainer rightOperation = rightMethod.getUmlOperation();
		UMLOperationBodyMapper bodyMapper = null;
		if (leftOperation instanceof UMLOperation && rightOperation instanceof UMLOperation) {
			UMLClassBaseDiff lightweightClassDiff = lightweightClassDiff(leftModel, rightModel, leftOperation, rightOperation);
			bodyMapper = new UMLOperationBodyMapper((UMLOperation) leftOperation, (UMLOperation) rightOperation, lightweightClassDiff);
			List<UMLOperation> rightSideOperations = new ArrayList<UMLOperation>();
			for(UMLAbstractClass umlClass : rightModel.getClassList()) {
				if(umlClass.getName().equals(rightMethod.getUmlOperation().getClassName())) {
					UMLClass leftClass = leftModel.getClass((UMLClass)umlClass);
					if(leftClass != null) {
						for(UMLOperation operation : umlClass.getOperations()) {
							if(!leftClass.containsOperationWithTheSameSignature(operation)) {
								rightSideOperations.add(operation);
							}
						}
						rightSideOperations.remove(rightMethod.getUmlOperation());
					}
				}
			}
			boolean allMapped = 
					bodyMapper.getNonMappedLeavesT1().size() == 0 &&
					bodyMapper.getNonMappedLeavesT2().size() == 0 &&
					bodyMapper.getNonMappedInnerNodesT1().size() == 0 &&
					bodyMapper.getNonMappedInnerNodesT2().size() == 0 &&
					bodyMapper.getMappings().size() > 0;
			if (!allMapped && containsCallToExtractedMethod(bodyMapper, rightSideOperations)) {
				UMLModelDiff umlModelDiffLocal = null;
				if(modelDiffCache.containsKey(currentVersion.getId())) {
					umlModelDiffLocal = modelDiffCache.get(currentVersion.getId());
				}
				else {
					umlModelDiffLocal = leftModel.diff(rightModel);
					modelDiffCache.put(currentVersion.getId(), umlModelDiffLocal);
				}
				//this bodyMapper has mapping optimization
				bodyMapper = findBodyMapper(umlModelDiffLocal, rightMethod, currentVersion, parentVersion);
			}
		}
		else if (leftOperation instanceof UMLInitializer && rightOperation instanceof UMLInitializer) {
			UMLClassBaseDiff lightweightClassDiff = lightweightClassDiff(leftModel, rightModel, leftOperation, rightOperation);
			bodyMapper = new UMLOperationBodyMapper((UMLInitializer) leftOperation, (UMLInitializer) rightOperation, lightweightClassDiff);
		}
		for (CodeElement key2 : programElementMap.keySet()) {
			if (key2 instanceof Block) {
				Block startBlock = (Block)key2;
				BlockTrackerChangeHistory startBlockChangeHistory = (BlockTrackerChangeHistory) programElementMap.get(startBlock);
				if (startBlock.getOperation().equals(startMethod.getUmlOperation()) ||
						matchingPeekMethod(rightMethod, startBlockChangeHistory)) {
					Block currentBlock = startBlockChangeHistory.peek();
					if (currentBlock == null) {
						continue;
					}
					Block rightBlock = rightMethod.findBlock(currentBlock::equalIdentifierIgnoringVersion);
					if (rightBlock == null) {
						continue;
					}
					startBlockChangeHistory.poll();
					if (startBlockChangeHistory.checkBodyOfMatchedOperations(currentVersion, parentVersion, rightBlock::equalIdentifierIgnoringVersion, bodyMapper)) {
						nestedProgramElementMap.putAll(startBlockChangeHistory.getNested());
						continue;
					}
				}
			}
			else if (key2 instanceof Comment) {
				Comment startComment = (Comment)key2;
				CommentTrackerChangeHistory startCommentChangeHistory = (CommentTrackerChangeHistory) programElementMap.get(startComment);
				if (matchingMethod(startMethod, startComment) ||
						matchingPeekMethod(rightMethod, startCommentChangeHistory)) {
					Comment currentComment = startCommentChangeHistory.peek();
					if (currentComment == null) {
						continue;
					}
					Comment rightComment = rightMethod.findComment(currentComment::equalIdentifierIgnoringVersion);
					if (rightComment == null) {
						continue;
					}
					startCommentChangeHistory.poll();
					if (startCommentChangeHistory.checkBodyOfMatchedOperations(currentVersion, parentVersion, rightComment::equalIdentifierIgnoringVersion, bodyMapper)) {
						continue;
					}
				}
			}
			else if (key2 instanceof Annotation) {
				Annotation startAnnotation = (Annotation)key2;
				AnnotationTrackerChangeHistory startAnnotationChangeHistory = (AnnotationTrackerChangeHistory) programElementMap.get(startAnnotation);
				if (matchingMethod(startMethod, startAnnotation) ||
						matchingPeekMethod(rightMethod, startAnnotationChangeHistory)) {
					Annotation currentAnnotation = startAnnotationChangeHistory.peek();
					if (currentAnnotation == null) {
						continue;
					}
					Annotation rightAnnotation = rightMethod.findAnnotation(currentAnnotation::equalIdentifierIgnoringVersion);
					if (rightAnnotation == null) {
						continue;
					}
					startAnnotationChangeHistory.poll();
					if (startAnnotationChangeHistory.checkBodyOfMatchedOperations(currentVersion, parentVersion, rightAnnotation::equalIdentifierIgnoringVersion, bodyMapper)) {
						continue;
					}
				}
			}
		}
	}
}
