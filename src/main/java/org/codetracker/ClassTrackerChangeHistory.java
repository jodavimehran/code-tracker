package org.codetracker;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.codetracker.api.History;
import org.codetracker.api.History.HistoryInfo;
import org.codetracker.api.Version;
import org.codetracker.change.Change;
import org.codetracker.change.ChangeFactory;
import org.codetracker.change.Introduced;
import org.codetracker.change.clazz.ClassAnnotationChange;
import org.codetracker.change.clazz.ClassContainerChange;
import org.codetracker.change.clazz.ClassMove;
import org.codetracker.element.Class;
import org.codetracker.element.Package;
import org.refactoringminer.api.Refactoring;

import com.github.difflib.DiffUtils;
import com.github.difflib.patch.AbstractDelta;
import com.github.difflib.patch.Chunk;
import com.github.difflib.patch.InsertDelta;
import com.github.difflib.patch.Patch;

import gr.uom.java.xmi.UMLAbstractClass;
import gr.uom.java.xmi.UMLClass;
import gr.uom.java.xmi.UMLModel;
import gr.uom.java.xmi.UMLOperation;
import gr.uom.java.xmi.diff.AddClassAnnotationRefactoring;
import gr.uom.java.xmi.diff.AddClassModifierRefactoring;
import gr.uom.java.xmi.diff.ChangeClassAccessModifierRefactoring;
import gr.uom.java.xmi.diff.ChangeTypeDeclarationKindRefactoring;
import gr.uom.java.xmi.diff.ExtractClassRefactoring;
import gr.uom.java.xmi.diff.ExtractSuperclassRefactoring;
import gr.uom.java.xmi.diff.ModifyClassAnnotationRefactoring;
import gr.uom.java.xmi.diff.MoveAndRenameClassRefactoring;
import gr.uom.java.xmi.diff.MoveClassRefactoring;
import gr.uom.java.xmi.diff.MoveSourceFolderRefactoring;
import gr.uom.java.xmi.diff.MovedClassToAnotherSourceFolder;
import gr.uom.java.xmi.diff.RemoveClassAnnotationRefactoring;
import gr.uom.java.xmi.diff.RemoveClassModifierRefactoring;
import gr.uom.java.xmi.diff.RenameClassRefactoring;
import gr.uom.java.xmi.diff.ReplaceAnonymousWithClassRefactoring;
import gr.uom.java.xmi.diff.UMLClassMoveDiff;
import gr.uom.java.xmi.diff.UMLModelDiff;

public class ClassTrackerChangeHistory extends AbstractChangeHistory<Class> {
	private final ChangeHistory<Class> classChangeHistory = new ChangeHistory<>();
    private final String className;
    private final int classDeclarationLineNumber;

	public ClassTrackerChangeHistory(String className, int classDeclarationLineNumber) {
		this.className = className;
		this.classDeclarationLineNumber = classDeclarationLineNumber;
	}

	public ChangeHistory<Class> get() {
		return classChangeHistory;
	}

	public String getClassName() {
		return className;
	}

	public int getClassDeclarationLineNumber() {
		return classDeclarationLineNumber;
	}

    boolean isStartClass(Class clazz) {
        return clazz.getUmlClass().getNonQualifiedName().equals(getClassName());
    }

    boolean isStartComment(Package pack) {
    	return pack.getUmlClass().getName().endsWith(getClassName()) &&
    			pack.getUmlPackage().getLocationInfo().getStartLine() == getClassDeclarationLineNumber() &&
    			pack.getUmlPackage().getLocationInfo().getEndLine() == getClassDeclarationLineNumber();
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

    public Set<Class> analyseClassRefactorings(Collection<Refactoring> refactorings, Version currentVersion, Version parentVersion, Predicate<Class> equalOperator) {
        Set<Class> leftClassSet = new HashSet<>();
        for (Refactoring refactoring : refactorings) {
            UMLAbstractClass leftUMLClass = null;
            UMLAbstractClass rightUMLClass = null;
            Change.Type changeType = null;
            Change.Type changeType2 = null;
            switch (refactoring.getRefactoringType()) {
                case MOVE_SOURCE_FOLDER: {
                    MoveSourceFolderRefactoring moveSourceFolderRefactoring = (MoveSourceFolderRefactoring) refactoring;
                    for (MovedClassToAnotherSourceFolder movedClassToAnotherSourceFolder : moveSourceFolderRefactoring.getMovedClassesToAnotherSourceFolder()) {
                        Class classAfter = Class.of(movedClassToAnotherSourceFolder.getMovedClass(), currentVersion);
                        if (equalOperator.test(classAfter)) {
                            leftUMLClass = movedClassToAnotherSourceFolder.getOriginalClass();
                            rightUMLClass = movedClassToAnotherSourceFolder.getMovedClass();
                            changeType = Change.Type.CONTAINER_CHANGE;
                            break;
                        }
                    }
                    break;
                }
                case MOVE_CLASS: {
                    MoveClassRefactoring moveClassRefactoring = (MoveClassRefactoring) refactoring;
                    leftUMLClass = moveClassRefactoring.getOriginalClass();
                    rightUMLClass = moveClassRefactoring.getMovedClass();
                    changeType = Change.Type.MOVED;
                    break;
                }
                case RENAME_CLASS: {
                    RenameClassRefactoring renameClassRefactoring = (RenameClassRefactoring) refactoring;
                    leftUMLClass = renameClassRefactoring.getOriginalClass();
                    rightUMLClass = renameClassRefactoring.getRenamedClass();
                    changeType = Change.Type.RENAME;
                    break;
                }
                case MOVE_RENAME_CLASS: {
                    MoveAndRenameClassRefactoring moveAndRenameClassRefactoring = (MoveAndRenameClassRefactoring) refactoring;
                    leftUMLClass = moveAndRenameClassRefactoring.getOriginalClass();
                    rightUMLClass = moveAndRenameClassRefactoring.getRenamedClass();
                    if (!leftUMLClass.isTopLevel() && rightUMLClass.isTopLevel()) {
                    	changeType = Change.Type.INTRODUCED;
                    }
                    else {
	                    changeType = Change.Type.RENAME;
	                    changeType2 = Change.Type.MOVED;
                    }
                    break;
                }
                case ADD_CLASS_ANNOTATION: {
                    AddClassAnnotationRefactoring addClassAnnotationRefactoring = (AddClassAnnotationRefactoring) refactoring;
                    leftUMLClass = addClassAnnotationRefactoring.getClassBefore();
                    rightUMLClass = addClassAnnotationRefactoring.getClassAfter();
                    changeType = Change.Type.ANNOTATION_CHANGE;
                    break;
                }
                case REMOVE_CLASS_ANNOTATION: {
                    RemoveClassAnnotationRefactoring removeClassAnnotationRefactoring = (RemoveClassAnnotationRefactoring) refactoring;
                    leftUMLClass = removeClassAnnotationRefactoring.getClassBefore();
                    rightUMLClass = removeClassAnnotationRefactoring.getClassAfter();
                    changeType = Change.Type.ANNOTATION_CHANGE;
                    break;
                }
                case MODIFY_CLASS_ANNOTATION: {
                    ModifyClassAnnotationRefactoring modifyClassAnnotationRefactoring = (ModifyClassAnnotationRefactoring) refactoring;
                    leftUMLClass = modifyClassAnnotationRefactoring.getClassBefore();
                    rightUMLClass = modifyClassAnnotationRefactoring.getClassAfter();
                    changeType = Change.Type.ANNOTATION_CHANGE;
                    break;
                }
                case ADD_CLASS_MODIFIER: {
                    AddClassModifierRefactoring addClassModifierRefactoring = (AddClassModifierRefactoring) refactoring;
                    leftUMLClass = addClassModifierRefactoring.getClassBefore();
                    rightUMLClass = addClassModifierRefactoring.getClassAfter();
                    changeType = Change.Type.MODIFIER_CHANGE;
                    break;
                }
                case REMOVE_CLASS_MODIFIER: {
                    RemoveClassModifierRefactoring removeClassModifierRefactoring = (RemoveClassModifierRefactoring) refactoring;
                    leftUMLClass = removeClassModifierRefactoring.getClassBefore();
                    rightUMLClass = removeClassModifierRefactoring.getClassAfter();
                    changeType = Change.Type.MODIFIER_CHANGE;
                    break;
                }
                case CHANGE_CLASS_ACCESS_MODIFIER: {
                    ChangeClassAccessModifierRefactoring changeClassAccessModifierRefactoring = (ChangeClassAccessModifierRefactoring) refactoring;
                    leftUMLClass = changeClassAccessModifierRefactoring.getClassBefore();
                    rightUMLClass = changeClassAccessModifierRefactoring.getClassAfter();
                    changeType = Change.Type.ACCESS_MODIFIER_CHANGE;
                    break;
                }
                case EXTRACT_INTERFACE:
                case EXTRACT_SUPERCLASS: {
                    ExtractSuperclassRefactoring extractSuperclassRefactoring = (ExtractSuperclassRefactoring) refactoring;
                    leftUMLClass = extractSuperclassRefactoring.getExtractedClass();
                    rightUMLClass = extractSuperclassRefactoring.getExtractedClass();
                    changeType = Change.Type.INTRODUCED;
                    break;
                }
                case EXTRACT_SUBCLASS:
                case EXTRACT_CLASS: {
                    ExtractClassRefactoring extractClassRefactoring = (ExtractClassRefactoring) refactoring;
                    leftUMLClass = extractClassRefactoring.getExtractedClass();
                    rightUMLClass = extractClassRefactoring.getExtractedClass();
                    changeType = Change.Type.INTRODUCED;
                    break;
                }
                case CHANGE_TYPE_DECLARATION_KIND: {
                    ChangeTypeDeclarationKindRefactoring changeTypeDeclarationKindRefactoring = (ChangeTypeDeclarationKindRefactoring)refactoring;
                    leftUMLClass = changeTypeDeclarationKindRefactoring.getClassBefore();
                    rightUMLClass = changeTypeDeclarationKindRefactoring.getClassAfter();
                    changeType = Change.Type.TYPE_CHANGE;
                    break;
                }
            }

            if (rightUMLClass != null) {
                Class classAfter = Class.of(rightUMLClass, currentVersion);
                if (equalOperator.test(classAfter)) {
                    Class classBefore = Class.of(leftUMLClass, parentVersion);
                    if (Change.Type.INTRODUCED.equals(changeType)) {
                        classChangeHistory.handleAdd(classBefore, classAfter, refactoring.toString());
                    } else {
                        classChangeHistory.addChange(classBefore, classAfter, ChangeFactory.forClass(changeType).refactoring(refactoring));
                    }
                    if (changeType2 != null)
                        classChangeHistory.addChange(classBefore, classAfter, ChangeFactory.forClass(changeType2).refactoring(refactoring));
                    processChange(classBefore, classAfter);
                    leftClassSet.add(classBefore);
                }
            }
        }

        if (!leftClassSet.isEmpty())
            classChangeHistory.connectRelatedNodes();
        return leftClassSet;
    }

    public boolean isClassAdded(UMLModelDiff modelDiff, Version currentVersion, Version parentVersion, Predicate<Class> equalOperator) {
        List<UMLClass> addedClasses = modelDiff.getAddedClasses();
        for (UMLClass umlClass : addedClasses) {
            Class rightClass = Class.of(umlClass, currentVersion);
            if (equalOperator.test(rightClass)) {
                Class leftClass = Class.of(umlClass, parentVersion);
                classChangeHistory.handleAdd(leftClass, rightClass, "new class");
                classChangeHistory.connectRelatedNodes();
                elements.addFirst(leftClass);
                return true;
            }
        }
        for (Refactoring r : modelDiff.getDetectedRefactorings()) {
        	if (r instanceof ReplaceAnonymousWithClassRefactoring) {
        		ReplaceAnonymousWithClassRefactoring replaceAnonymous = (ReplaceAnonymousWithClassRefactoring)r;
        		Class rightClass = Class.of(replaceAnonymous.getAddedClass(), currentVersion);
                if (equalOperator.test(rightClass)) {
                    Class leftClass = Class.of(replaceAnonymous.getAddedClass(), parentVersion);
                    classChangeHistory.handleAdd(leftClass, rightClass, "new class");
                    classChangeHistory.connectRelatedNodes();
                    elements.addFirst(leftClass);
                    return true;
                }
        	}
        }
        return false;
    }

    public Set<Class> isInnerClassContainerChanged(UMLModelDiff umlModelDiffAll, Collection<Refactoring> refactorings, Version currentVersion, Version parentVersion, Predicate<Class> equalOperator, List<UMLClassMoveDiff> classMoveDiffList) {
        Set<Class> leftClassSet = new HashSet<>();
        Change.Type changeType = Change.Type.CONTAINER_CHANGE;
        for (UMLClassMoveDiff umlClassMoveDiff : classMoveDiffList) {
            Class innerClassAfter = Class.of(umlClassMoveDiff.getMovedClass(), currentVersion);
            if (equalOperator.test(innerClassAfter)) {
                Class innerClassBefore = Class.of(umlClassMoveDiff.getOriginalClass(), parentVersion);
                classChangeHistory.addChange(innerClassBefore, innerClassAfter, ChangeFactory.forClass(changeType).refactoring(new MoveClassRefactoring(umlClassMoveDiff.getOriginalClass(), umlClassMoveDiff.getMovedClass())));
                leftClassSet.add(innerClassBefore);
                classChangeHistory.connectRelatedNodes();
                return leftClassSet;
            }
        }
        return Collections.emptySet();
    }

    private Map<Pair<Class, Class>, List<Integer>> lineChangeMap = new LinkedHashMap<>();

	public void processChange(Class classBefore, Class classAfter) {
		if (classBefore.isMultiLine() || classAfter.isMultiLine()) {
			try {
				Pair<Class, Class> pair = Pair.of(classBefore, classAfter);
				Class startClass = getStart();
				if (startClass != null) {
					List<String> start = IOUtils.readLines(new StringReader(((UMLClass)startClass.getUmlClass()).getActualSignature()));
					List<String> original = IOUtils.readLines(new StringReader(((UMLClass)classBefore.getUmlClass()).getActualSignature()));
					List<String> revised = IOUtils.readLines(new StringReader(((UMLClass)classAfter.getUmlClass()).getActualSignature()));
		
					Patch<String> patch = DiffUtils.diff(original, revised);
					List<AbstractDelta<String>> deltas = patch.getDeltas();
					for (int i=0; i<deltas.size(); i++) {
						AbstractDelta<String> delta = deltas.get(i);
						Chunk<String> target = delta.getTarget();
						List<String> affectedLines = new ArrayList<>(target.getLines());
						boolean subListFound = false;
						if (affectedLines.size() > 1 && !(delta instanceof InsertDelta)) {
							int index = Collections.indexOfSubList(start, affectedLines);
							if (index != -1) {
								subListFound = true;
								for (int j=0; j<affectedLines.size(); j++) {
									int actualLine = startClass.signatureStartLine() + index + j;
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
								for (Integer index : matchingIndices) {
									if (original.size() > index && revised.size() > index &&
											original.get(index).equals(line) && revised.get(index).equals(line)) {
										continue;
									}
									int actualLine = startClass.signatureStartLine() + index;
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
			} catch(IOException e) {
				e.printStackTrace();
			}
		}
	}

	private List<Integer> findAllMatchingIndices(List<String> startCommentLines, String line) {
		List<Integer> matchingIndices = new ArrayList<>();
		for(int i=0; i<startCommentLines.size(); i++) {
			String element = startCommentLines.get(i).trim();
			if(line.equals(element) || element.contains(line.trim())) {
				matchingIndices.add(i);
			}
		}
		return matchingIndices;
	}

	public HistoryInfo<Class> blameReturn(Class startClass) {
		List<HistoryInfo<Class>> history = getHistory();
		for (History.HistoryInfo<Class> historyInfo : history) {
			for (Change change : historyInfo.getChangeList()) {
				if (startClass.isClosingCurlyBracket()) {
					if (change instanceof Introduced) {
						return historyInfo;
					}
				}
				else {
					if (!(change instanceof ClassMove) && !(change instanceof ClassContainerChange) && !(change instanceof ClassAnnotationChange)) {
						return historyInfo;
					}
				}
			}
		}
		return null;
	}

	public HistoryInfo<Class> blameReturn(Class startClass, int exactLineNumber) {
		List<HistoryInfo<Class>> history = getHistory();
		for (History.HistoryInfo<Class> historyInfo : history) {
			Pair<Class, Class> pair = Pair.of(historyInfo.getElementBefore(), historyInfo.getElementAfter());
			boolean multiLine = startClass.isMultiLine();
			for (Change change : historyInfo.getChangeList()) {
				if (startClass.isClosingCurlyBracket()) {
					if (change instanceof Introduced) {
						return historyInfo;
					}
				}
				else {
					if (!(change instanceof ClassMove) && !(change instanceof ClassContainerChange) && !(change instanceof ClassAnnotationChange)) {
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
					if (change instanceof Introduced) {
						return historyInfo;
					}
				}
			}
		}
		return null;
	}

	public HistoryInfo<Class> blameReturn(Package startPackage) {
		List<HistoryInfo<Class>> history = getHistory();
		for (History.HistoryInfo<Class> historyInfo : history) {
			for (Change change : historyInfo.getChangeList()) {
				if (change instanceof Introduced || change instanceof ClassMove) {
					return historyInfo;
				}
			}
		}
		return null;
	}
}
