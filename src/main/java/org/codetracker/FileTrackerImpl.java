package org.codetracker;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.codetracker.api.CodeElement;
import org.codetracker.api.CodeElementNotFoundException;
import org.codetracker.api.History.HistoryInfo;
import org.codetracker.api.Version;
import org.codetracker.change.Change;
import org.codetracker.change.ChangeFactory;
import org.codetracker.element.Attribute;
import org.codetracker.element.BaseCodeElement;
import org.codetracker.element.Block;
import org.codetracker.element.Class;
import org.codetracker.element.Comment;
import org.codetracker.element.Import;
import org.codetracker.element.Method;
import org.codetracker.element.Package;
import org.codetracker.util.CodeElementLocator;
import org.codetracker.util.GitRepository;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringMinerTimedOutException;
import org.refactoringminer.rm1.GitHistoryRefactoringMinerImpl;

import gr.uom.java.xmi.LocationInfo.CodeElementType;
import gr.uom.java.xmi.decomposition.UMLOperationBodyMapper;
import gr.uom.java.xmi.diff.UMLClassBaseDiff;
import gr.uom.java.xmi.diff.UMLModelDiff;
import gr.uom.java.xmi.UMLClass;
import gr.uom.java.xmi.UMLInitializer;
import gr.uom.java.xmi.UMLModel;
import gr.uom.java.xmi.UMLOperation;
import gr.uom.java.xmi.VariableDeclarationContainer;

public class FileTrackerImpl extends BaseTracker {
	private final List<String> lines = new ArrayList<>();
	private final Map<CodeElement, AbstractChangeHistory<? extends BaseCodeElement>> programElementMap = new LinkedHashMap<>();
	private final Map<Integer, HistoryInfo<? extends BaseCodeElement>> blameInfo = new LinkedHashMap<>();

	public FileTrackerImpl(Repository repository, String startCommitId, String filePath) {
		super(repository, startCommitId, filePath);
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

	public void blame() throws Exception {
		try (Git git = new Git(repository); RevWalk walk = new RevWalk(repository)) {
			Version startVersion = gitRepository.getVersion(startCommitId);
			RevCommit revCommit = walk.parseCommit(repository.resolve(startCommitId));
			Set<String> repositoryDirectories = new LinkedHashSet<>();
			Map<String, String> fileContents = new LinkedHashMap<>();
			GitHistoryRefactoringMinerImpl.populateFileContents(repository, revCommit, Collections.singleton(filePath), fileContents, repositoryDirectories);
			UMLModel umlModel = GitHistoryRefactoringMinerImpl.createModel(fileContents, repositoryDirectories);
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
					CodeElementLocator locator = new CodeElementLocator((GitRepository) gitRepository, startCommitId, filePath, lineNumber);
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
			// TODO check all blamed program elements if they have an empty element queue
			ClassTrackerChangeHistory startClassChangeHistory = (ClassTrackerChangeHistory) programElementMap.get(startClass);
			while (!startClassChangeHistory.isEmpty()) {
				Class currentClass = startClassChangeHistory.poll();
				if (currentClass.isAdded()) {
					commits = null;
					continue;
				}
				if (commits == null || !currentClass.getFilePath().equals(lastFileName)) {
					lastFileName = currentClass.getFilePath();
					commits = getCommits(repository, currentClass.getVersion().getId(), lastFileName, git);
					analysedCommits.clear();
				}
				if (analysedCommits.containsAll(commits))
					break;
				for (String commitId : commits) {
					if (analysedCommits.contains(commitId))
						continue;
					analysedCommits.add(commitId);

					Version currentVersion = gitRepository.getVersion(commitId);
					String parentCommitId = gitRepository.getParentId(commitId);
					Version parentVersion = gitRepository.getVersion(parentCommitId);

					UMLModel rightModel = getUMLModel(commitId, Collections.singleton(currentClass.getFilePath()));
					Class rightClass = getClass(rightModel, currentVersion, currentClass::equalIdentifierIgnoringVersion);
					if (rightClass == null) {
						continue;
					}
					if ("0".equals(parentCommitId)) {
						Class leftClass = Class.of(rightClass.getUmlClass(), parentVersion);
						startClassChangeHistory.get().handleAdd(leftClass, rightClass, "Initial commit!");
						startClassChangeHistory.get().connectRelatedNodes();
						startClassChangeHistory.add(leftClass);
						// TODO terminate all blamed program element change histories
						break;
					}
					UMLModel leftModel = getUMLModel(parentCommitId, Collections.singleton(rightClass.getFilePath()));

					Class leftClass = getClass(leftModel, parentVersion, rightClass::equalIdentifierIgnoringVersion);
					// No class signature change
					if (leftClass != null) {
						Map<Method, MethodTrackerChangeHistory> notFoundMethods = processMethodsWithSameSignature(rightModel, currentVersion, leftModel, parentVersion);
						if (notFoundMethods.size() > 0) {
							UMLModelDiff umlModelDiffLocal = leftModel.diff(rightModel);
							processLocallyRefactoredMethods(notFoundMethods, umlModelDiffLocal, currentVersion, parentVersion);
						}
						UMLClassBaseDiff lightweightClassDiff = lightweightClassDiff(leftClass.getUmlClass(), rightClass.getUmlClass());
						processImports(lightweightClassDiff, rightClass, currentVersion, parentVersion);
						continue;
					}
					//All refactorings
					CommitModel commitModel = getCommitModel(commitId);
					if (!commitModel.moveSourceFolderRefactorings.isEmpty()) {
						Pair<UMLModel, UMLModel> umlModelPairPartial = getUMLModelPair(commitModel, rightClass.getFilePath(), s -> true, true);
						UMLModelDiff umlModelDiffPartial = umlModelPairPartial.getLeft().diff(umlModelPairPartial.getRight());
						List<Refactoring> refactoringsPartial = umlModelDiffPartial.getRefactorings();
						Set<Class> classRefactored = startClassChangeHistory.analyseClassRefactorings(refactoringsPartial, currentVersion, parentVersion, rightClass::equalIdentifierIgnoringVersion);
						boolean refactored = !classRefactored.isEmpty();
						if (refactored) {
							//TODO Handle methods and blocks
							Set<Class> leftSideClasses = new HashSet<>(classRefactored);
							leftSideClasses.forEach(startClassChangeHistory::addFirst);
							break;
						}
					}
					{
						Pair<UMLModel, UMLModel> umlModelPairAll = getUMLModelPair(commitModel, rightClass.getFilePath(), s -> true, false);
						UMLModelDiff umlModelDiffAll = umlModelPairAll.getLeft().diff(umlModelPairAll.getRight());

						List<Refactoring> refactorings = umlModelDiffAll.getRefactorings();

						Set<Class> classRefactored = startClassChangeHistory.analyseClassRefactorings(refactorings, currentVersion, parentVersion, rightClass::equalIdentifierIgnoringVersion);
						boolean refactored = !classRefactored.isEmpty();
						if (refactored) {
							//TODO Handle methods and blocks
							Set<Class> leftSideClasses = new HashSet<>(classRefactored);
							leftSideClasses.forEach(startClassChangeHistory::addFirst);
							break;
						}

						if (startClassChangeHistory.isClassAdded(umlModelDiffAll, currentVersion, parentVersion, rightClass::equalIdentifierIgnoringVersion)) {
							processAddedMethods(umlModelDiffAll, currentVersion, parentVersion);
							processAddedImports(rightClass, parentVersion);
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
					HistoryInfo<Method> historyInfo = startMethodChangeHistory.blameReturn(startMethod);
					blameInfo.put(lineNumber, historyInfo);
				}
				else if (startElement instanceof Block) {
					Block startBlock = (Block)startElement;
					BlockTrackerChangeHistory startBlockChangeHistory = (BlockTrackerChangeHistory) programElementMap.get(startBlock);
					startBlock.checkClosingBracket(lineNumber);
					startBlock.checkElseBlockStart(lineNumber);
					startBlock.checkElseBlockEnd(lineNumber);
					HistoryInfo<Block> historyInfo = startBlockChangeHistory.blameReturn(startBlock);
					blameInfo.put(lineNumber, historyInfo);
				}
				else if (startElement instanceof Class) {
					Class clazz = (Class)startElement;
					ClassTrackerChangeHistory classChangeHistory = (ClassTrackerChangeHistory) programElementMap.get(clazz);
					clazz.checkClosingBracket(lineNumber);
					HistoryInfo<Class> historyInfo = classChangeHistory.blameReturn(clazz);
					blameInfo.put(lineNumber, historyInfo);
				}
				else if (startElement instanceof Comment) {
					Comment startComment = (Comment)startElement;
					CommentTrackerChangeHistory commentChangeHistory = (CommentTrackerChangeHistory) programElementMap.get(startComment);
					HistoryInfo<Comment> historyInfo = commentChangeHistory.blameReturn();
					blameInfo.put(lineNumber, historyInfo);
				}
				else if (startElement instanceof Import) {
					Import startImport = (Import)startElement;
					ImportTrackerChangeHistory importChangeHistory = (ImportTrackerChangeHistory) programElementMap.get(startImport);
					HistoryInfo<Import> historyInfo = importChangeHistory.blameReturn();
					blameInfo.put(lineNumber, historyInfo);
				}
				else {
					blameInfo.put(lineNumber, null);
				}
			}
		}
	}

	private void processAddedImports(Class rightClass, Version parentVersion) {
		for (CodeElement key : programElementMap.keySet()) {
			if (key instanceof Import) {
				Import startImport = (Import)key;
				ImportTrackerChangeHistory startImportChangeHistory = (ImportTrackerChangeHistory) programElementMap.get(startImport);
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
	}

	private void processImports(UMLClassBaseDiff classDiff, Class rightClass, Version currentVersion, Version parentVersion) throws RefactoringMinerTimedOutException {
		for (CodeElement key : programElementMap.keySet()) {
			if (key instanceof Import) {
				Import startImport = (Import)key;
				ImportTrackerChangeHistory startImportChangeHistory = (ImportTrackerChangeHistory) programElementMap.get(startImport);
				Import currentImport = startImportChangeHistory.poll();
				Import rightImport = rightClass.findImport(currentImport::equalIdentifierIgnoringVersion);
				if (rightImport == null) {
					continue;
				}
				startImportChangeHistory.checkBodyOfMatchedClasses(currentVersion, parentVersion, rightImport::equalIdentifierIgnoringVersion, classDiff);
			}
		}
	}

	private void processAddedMethods(UMLModelDiff umlModelDiffAll, Version currentVersion, Version parentVersion) {
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
				Method rightMethod = currentMethod;
				if (startMethodChangeHistory.isMethodAdded(umlModelDiffAll, rightMethod.getUmlOperation().getClassName(), currentVersion, parentVersion, rightMethod::equalIdentifierIgnoringVersion, getAllClassesDiff(umlModelDiffAll))) {
					for (CodeElement key2 : programElementMap.keySet()) {
						if (key2 instanceof Block) {
							Block startBlock = (Block)key2;
							if (startBlock.getOperation().equals(startMethod.getUmlOperation())) {
								BlockTrackerChangeHistory startBlockChangeHistory = (BlockTrackerChangeHistory) programElementMap.get(startBlock);
								Block currentBlock = startBlockChangeHistory.poll();
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
							if (startComment.getOperation().isPresent() && startComment.getOperation().get().equals(startMethod.getUmlOperation())) {
								CommentTrackerChangeHistory startCommentChangeHistory = (CommentTrackerChangeHistory) programElementMap.get(startComment);
								Comment currentComment = startCommentChangeHistory.poll();
								Comment rightComment = rightMethod.findComment(currentComment::equalIdentifierIgnoringVersion);
								if (rightComment != null) {
									Comment commentBefore = Comment.of(rightComment.getComment(), rightComment.getOperation().get(), parentVersion);
									startCommentChangeHistory.get().handleAdd(commentBefore, rightComment, "added with method");
									startCommentChangeHistory.add(commentBefore);
									startCommentChangeHistory.get().connectRelatedNodes();
								}
							}
						}
					}
				}
			}
		}
	}

	private void processLocallyRefactoredMethods(Map<Method, MethodTrackerChangeHistory> notFoundMethods, UMLModelDiff umlModelDiffLocal, Version currentVersion, Version parentVersion) throws RefactoringMinerTimedOutException {
		List<Refactoring> refactorings = umlModelDiffLocal.getRefactorings();
		for (Method rightMethod : notFoundMethods.keySet()) {
			MethodTrackerChangeHistory startMethodChangeHistory = notFoundMethods.get(rightMethod);
			Method startMethod = startMethodChangeHistory.getStart();
			Set<Method> leftSideMethods = startMethodChangeHistory.analyseMethodRefactorings(refactorings, currentVersion, parentVersion, rightMethod::equalIdentifierIgnoringVersion);
			boolean refactored = !leftSideMethods.isEmpty();
			if (refactored) {
				leftSideMethods.forEach(startMethodChangeHistory::addFirst);
			}
			for (CodeElement key2 : programElementMap.keySet()) {
				if (key2 instanceof Block) {
					Block startBlock = (Block)key2;
					if (startBlock.getOperation().equals(startMethod.getUmlOperation())) {
						BlockTrackerChangeHistory startBlockChangeHistory = (BlockTrackerChangeHistory) programElementMap.get(startBlock);
						Block currentBlock = startBlockChangeHistory.poll();
						Block rightBlock = rightMethod.findBlock(currentBlock::equalIdentifierIgnoringVersion);
						if (rightBlock == null) {
							continue;
						}
						boolean found = startBlockChangeHistory.checkForExtractionOrInline(currentVersion, parentVersion, rightMethod::equalIdentifierIgnoringVersion, rightBlock, refactorings);
						if (found) {
							continue;
						}
						found = startBlockChangeHistory.checkRefactoredMethod(currentVersion, parentVersion, rightMethod::equalIdentifierIgnoringVersion, rightBlock, refactorings);
						if (found) {
							continue;
						}
						found = startBlockChangeHistory.checkBodyOfMatchedOperations(currentVersion, parentVersion, rightBlock::equalIdentifierIgnoringVersion, findBodyMapper(umlModelDiffLocal, rightMethod, currentVersion, parentVersion));
						if (found) {
							continue;
						}
					}
				}
				else if (key2 instanceof Comment) {
					Comment startComment = (Comment)key2;
					if (startComment.getOperation().isPresent() && startComment.getOperation().get().equals(startMethod.getUmlOperation())) {
						CommentTrackerChangeHistory startCommentChangeHistory = (CommentTrackerChangeHistory) programElementMap.get(startComment);
						Comment currentComment = startCommentChangeHistory.poll();
						Comment rightComment = rightMethod.findComment(currentComment::equalIdentifierIgnoringVersion);
						if (rightComment == null) {
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
						found = startCommentChangeHistory.checkBodyOfMatchedOperations(currentVersion, parentVersion, rightComment::equalIdentifierIgnoringVersion, findBodyMapper(umlModelDiffLocal, rightMethod, currentVersion, parentVersion));
						if (found) {
							continue;
						}
					}
				}
			}
		}
	}

	private Map<Method, MethodTrackerChangeHistory> processMethodsWithSameSignature(UMLModel rightModel, Version currentVersion, UMLModel leftModel, Version parentVersion) throws RefactoringMinerTimedOutException {
		Map<Method, MethodTrackerChangeHistory> notFoundMethods = new LinkedHashMap<>();
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
				if (rightMethod == null) {
					continue;
				}
				//NO CHANGE
				Method leftMethod = getMethod(leftModel, parentVersion, rightMethod::equalIdentifierIgnoringVersion);
				if (leftMethod != null) {
					startMethodChangeHistory.setCurrent(leftMethod);
					continue;
				}
				//CHANGE BODY OR DOCUMENT
				leftMethod = getMethod(leftModel, parentVersion, rightMethod::equalIdentifierIgnoringVersionAndDocumentAndBody);
				//check if there is another method in leftModel with identical bodyHashCode to the rightMethod
				boolean otherExactMatchFound = false;
				if (leftMethod != null) {
					for (UMLClass leftClass : leftModel.getClassList()) {
						for (UMLOperation leftOperation : leftClass.getOperations()) {
							if (leftOperation.getBodyHashCode() == rightMethod.getUmlOperation().getBodyHashCode() && !leftOperation.equals(leftMethod.getUmlOperation())) {
								otherExactMatchFound = true;
								break;
							}
						}
						if(otherExactMatchFound) {
							break;
						}
					}
				}
				else {
					notFoundMethods.put(rightMethod, startMethodChangeHistory);
				}
				if (leftMethod != null && !otherExactMatchFound) {
					if (!leftMethod.equalBody(rightMethod))
						startMethodChangeHistory.get().addChange(leftMethod, rightMethod, ChangeFactory.forMethod(Change.Type.BODY_CHANGE));
					if (!leftMethod.equalDocuments(rightMethod))
						startMethodChangeHistory.get().addChange(leftMethod, rightMethod, ChangeFactory.forMethod(Change.Type.DOCUMENTATION_CHANGE));
					startMethodChangeHistory.get().connectRelatedNodes();
					startMethodChangeHistory.setCurrent(leftMethod);
					VariableDeclarationContainer leftOperation = leftMethod.getUmlOperation();
					VariableDeclarationContainer rightOperation = rightMethod.getUmlOperation();
					UMLOperationBodyMapper bodyMapper = null;
					if (leftOperation instanceof UMLOperation && rightOperation instanceof UMLOperation) {
						UMLClassBaseDiff lightweightClassDiff = lightweightClassDiff(leftModel, rightModel, leftOperation, rightOperation);
						bodyMapper = new UMLOperationBodyMapper((UMLOperation) leftOperation, (UMLOperation) rightOperation, lightweightClassDiff);
						if (containsCallToExtractedMethod(bodyMapper, bodyMapper.getClassDiff())) {
							bodyMapper = null;
						}
					}
					else if (leftOperation instanceof UMLInitializer && rightOperation instanceof UMLInitializer) {
						UMLClassBaseDiff lightweightClassDiff = lightweightClassDiff(leftModel, rightModel, leftOperation, rightOperation);
						bodyMapper = new UMLOperationBodyMapper((UMLInitializer) leftOperation, (UMLInitializer) rightOperation, lightweightClassDiff);
						if (containsCallToExtractedMethod(bodyMapper, bodyMapper.getClassDiff())) {
							bodyMapper = null;
						}
					}
					for (CodeElement key2 : programElementMap.keySet()) {
						if (key2 instanceof Block) {
							Block startBlock = (Block)key2;
							if (startBlock.getOperation().equals(startMethod.getUmlOperation())) {
								BlockTrackerChangeHistory startBlockChangeHistory = (BlockTrackerChangeHistory) programElementMap.get(startBlock);
								Block currentBlock = startBlockChangeHistory.poll();
								Block rightBlock = rightMethod.findBlock(currentBlock::equalIdentifierIgnoringVersion);
								if (rightBlock == null) {
									continue;
								}
								if (startBlockChangeHistory.checkBodyOfMatchedOperations(currentVersion, parentVersion, rightBlock::equalIdentifierIgnoringVersion, bodyMapper)) {
									continue;
								}
							}
						}
						else if (key2 instanceof Comment) {
							Comment startComment = (Comment)key2;
							if (startComment.getOperation().isPresent() && startComment.getOperation().get().equals(startMethod.getUmlOperation())) {
								CommentTrackerChangeHistory startCommentChangeHistory = (CommentTrackerChangeHistory) programElementMap.get(startComment);
								Comment currentComment = startCommentChangeHistory.poll();
								Comment rightComment = rightMethod.findComment(currentComment::equalIdentifierIgnoringVersion);
								if (rightComment == null) {
									continue;
								}
								if (startCommentChangeHistory.checkBodyOfMatchedOperations(currentVersion, parentVersion, rightComment::equalIdentifierIgnoringVersion, bodyMapper)) {
									continue;
								}
							}
						}
					}
				}
			}
		}
		return notFoundMethods;
	}
}
