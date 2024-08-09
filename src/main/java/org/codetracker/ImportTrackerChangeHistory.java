package org.codetracker;

import java.util.List;
import java.util.function.Predicate;

import org.apache.commons.lang3.tuple.Pair;
import org.codetracker.api.History;
import org.codetracker.api.History.HistoryInfo;
import org.codetracker.api.Version;
import org.codetracker.change.AbstractChange;
import org.codetracker.change.Change;
import org.codetracker.change.ChangeFactory;
import org.codetracker.change.Introduced;
import org.codetracker.change.method.BodyChange;
import org.codetracker.element.Class;
import org.codetracker.element.Import;

import gr.uom.java.xmi.UMLImport;
import gr.uom.java.xmi.LocationInfo.CodeElementType;
import gr.uom.java.xmi.diff.UMLAbstractClassDiff;
import gr.uom.java.xmi.diff.UMLClassBaseDiff;
import gr.uom.java.xmi.diff.UMLImportListDiff;

public class ImportTrackerChangeHistory extends AbstractChangeHistory<Import> {
	private final ChangeHistory<Import> importChangeHistory = new ChangeHistory<>();
	private final String className;
	private final int classDeclarationLineNumber; 
    private final CodeElementType importType;
    private final int importStartLineNumber;
    private final int importEndLineNumber;

	public ImportTrackerChangeHistory(String className, int classDeclarationLineNumber, CodeElementType importType, int importStartLineNumber, int importEndLineNumber) {
		this.importType = importType;
		this.className = className;
		this.classDeclarationLineNumber = classDeclarationLineNumber;
		this.importStartLineNumber = importStartLineNumber;
		this.importEndLineNumber = importEndLineNumber;
	}

	public ChangeHistory<Import> get() {
		return importChangeHistory;
	}

	public String getClassName() {
		return className;
	}

	public int getClassDeclarationLineNumber() {
		return classDeclarationLineNumber;
	}

	public CodeElementType getImportType() {
		return importType;
	}

	public int getImportStartLineNumber() {
		return importStartLineNumber;
	}

	public int getImportEndLineNumber() {
		return importEndLineNumber;
	}

    public boolean isStartImport(Import imp) {
        return imp.getUmlImport().getLocationInfo().getCodeElementType().equals(importType) &&
                imp.getUmlImport().getLocationInfo().getStartLine() == importStartLineNumber &&
                imp.getUmlImport().getLocationInfo().getEndLine() == importEndLineNumber;
    }


    public boolean isStartClass(Class clazz) {
        return clazz.getUmlClass().getName().equals(className) &&
        		clazz.getUmlClass().getLocationInfo().getStartLine() <= classDeclarationLineNumber &&
        		clazz.getUmlClass().getLocationInfo().getEndLine() >= classDeclarationLineNumber;
    }

    public boolean checkBodyOfMatchedClasses(Version currentVersion, Version parentVersion, Predicate<Import> equalOperator, UMLAbstractClassDiff classDiff) {
        if (classDiff == null)
            return false;
        // check if it is in the matched
        if (isMatched(classDiff, currentVersion, parentVersion, equalOperator))
            return true;
        //Check if is added
        return isAdded(classDiff, currentVersion, parentVersion, equalOperator);
    }

    public boolean isMatched(UMLAbstractClassDiff classDiff, Version currentVersion, Version parentVersion, Predicate<Import> equalOperator) {
    	int matches = 0;
    	if (classDiff instanceof UMLClassBaseDiff) {
    		UMLImportListDiff diff = ((UMLClassBaseDiff) classDiff).getImportDiffList();
	    	for (Pair<UMLImport, UMLImport> mapping : diff.getCommonImports()) {
	            Import importAfter = Import.of(mapping.getRight(), classDiff.getNextClass(), currentVersion);
	            if (importAfter != null && equalOperator.test(importAfter)) {
	            	Import importBefore = Import.of(mapping.getLeft(), classDiff.getOriginalClass(), parentVersion);
	                importChangeHistory.addChange(importBefore, importAfter, ChangeFactory.of(AbstractChange.Type.NO_CHANGE));
	                if(matches == 0) {
	                	elements.add(importBefore);
	                }
	                importChangeHistory.connectRelatedNodes();
	                matches++;
	            }
	        }
	    	for (Pair<UMLImport, UMLImport> mapping : diff.getChangedImports()) {
	            Import importAfter = Import.of(mapping.getRight(), classDiff.getNextClass(), currentVersion);
	            if (importAfter != null && equalOperator.test(importAfter)) {
	            	Import importBefore = Import.of(mapping.getLeft(), classDiff.getOriginalClass(), parentVersion);
	                importChangeHistory.addChange(importBefore, importAfter, ChangeFactory.forImport(Change.Type.BODY_CHANGE));
	                if(matches == 0) {
	                	elements.add(importBefore);
	                }
	                importChangeHistory.connectRelatedNodes();
	                matches++;
	            }
	        }
    	}
    	if(matches > 0) {
    		return true;
    	}
        return false;
    }

    private boolean isAdded(UMLAbstractClassDiff classDiff, Version currentVersion, Version parentVersion, Predicate<Import> equalOperator) {
    	if (classDiff instanceof UMLClassBaseDiff) {
    		UMLImportListDiff diff = ((UMLClassBaseDiff) classDiff).getImportDiffList();
	    	for (UMLImport imp : diff.getAddedImports()) {
	            Import importAfter = Import.of(imp, classDiff.getNextClass(), currentVersion);
	            if (equalOperator.test(importAfter)) {
	                Import importBefore = Import.of(imp, classDiff.getNextClass(), parentVersion);
	                importChangeHistory.handleAdd(importBefore, importAfter, "new import");
	                elements.add(importBefore);
	                importChangeHistory.connectRelatedNodes();
	                return true;
	            }
	        }
    	}
        return false;
    }

	public HistoryInfo<Import> blameReturn() {
		List<HistoryInfo<Import>> history = getHistory();
		for (History.HistoryInfo<Import> historyInfo : history) {
			for (Change change : historyInfo.getChangeList()) {
				if (change instanceof BodyChange || change instanceof Introduced) {
					return historyInfo;
				}
			}
		}
		return null;
	}
}
