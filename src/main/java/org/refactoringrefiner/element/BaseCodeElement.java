package org.refactoringrefiner.element;

import gr.uom.java.xmi.UMLAnnotation;
import gr.uom.java.xmi.UMLType;
import gr.uom.java.xmi.diff.UMLModelDiff;
import org.refactoringminer.api.Refactoring;
import org.refactoringrefiner.api.CodeElement;
import org.refactoringrefiner.api.Version;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public abstract class BaseCodeElement<T> implements CodeElement {
    protected final T info;
    protected final Version version;
    protected boolean isRemoved;
    protected boolean isAdded;

    public BaseCodeElement(T info, Version version) {
        this.info = info;
        this.version = version;
    }

    protected static String getPath(String filePath, String className) {
        try {
            CharSequence charSequence = longestSubstring(filePath.substring(0, filePath.lastIndexOf("/")), className.replace(".", "/"));
            String srcFile = filePath.toLowerCase().replace(charSequence, "$");
            srcFile = srcFile.substring(0, srcFile.lastIndexOf("$"));
            return srcFile;
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return "";
    }

    public static String longestSubstring(String str1, String str2) {

        StringBuilder sb = new StringBuilder();
        if (str1 == null || str1.isEmpty() || str2 == null || str2.isEmpty())
            return "";

// ignore case
        str1 = str1.toLowerCase();
        str2 = str2.toLowerCase();

// java initializes them already with 0
        int[][] num = new int[str1.length()][str2.length()];
        int maxLength = 0;
        int lastSubsBegin = 0;

        for (int i = 0; i < str1.length(); i++) {
            for (int j = 0; j < str2.length(); j++) {
                if (str1.charAt(i) == str2.charAt(j)) {
                    if ((i == 0) || (j == 0))
                        num[i][j] = 1;
                    else
                        num[i][j] = 1 + num[i - 1][j - 1];

                    if (num[i][j] > maxLength) {
                        maxLength = num[i][j];
                        // generate substring from str1 => i
                        int thisSubsBegin = i - num[i][j] + 1;
                        if (lastSubsBegin == thisSubsBegin) {
                            //if the current LCS is the same as the last time this block ran
                            sb.append(str1.charAt(i));
                        } else {
                            //this block resets the string builder if a different LCS is found
                            lastSubsBegin = thisSubsBegin;
                            sb = new StringBuilder();
                            sb.append(str1, lastSubsBegin, i + 1);
                        }
                    }
                }
            }
        }

        return sb.toString();
    }

    protected static String getPackage(String filePath, String className) {
        try {
            String replace = className.replace(filePath.substring(filePath.lastIndexOf("/") + 1).replace(".java", ""), "$");
            String packageName = replace.substring(0, replace.lastIndexOf("$") - 1);
            packageName = getPath(filePath, className) + packageName;
            return packageName;
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return "";
    }

    protected static String annotationsToString(List<UMLAnnotation> umlAnnotations) {
        StringBuilder sb = new StringBuilder();
        if (umlAnnotations != null && !umlAnnotations.isEmpty()) {
            sb.append("[");
            sb.append(umlAnnotations.stream().map(UMLAnnotation::toString).collect(Collectors.joining(";")));
            sb.append("]");
        }
        return sb.toString();
    }

    protected final String annotationsToString() {
        return annotationsToString(getAnnotations());
    }

    protected abstract List<UMLAnnotation> getAnnotations();

    public T getInfo() {
        return info;
    }


    @Override
    public int compareTo(CodeElement o) {
        return this.getIdentifier().compareTo(o.getIdentifier());
    }

    @Override
    public Version getVersion() {
        return version;
    }

    @Override
    public String getIdentifier() {
        return this.getIdentifierExcludeVersion() + this.getVersion().toString();
    }

    public boolean isRemoved() {
        return isRemoved;
    }

    public void setRemoved(boolean removed) {
        isRemoved = removed;
    }

    public boolean isAdded() {
        return isAdded;
    }

    public void setAdded(boolean added) {
        isAdded = added;
    }

    /**
     * @param <T> Code Element
     */
    public static abstract class BaseElementDiff<T extends CodeElement> {
        protected final T leftSide;
        protected final T rightSide;

        public BaseElementDiff(T leftSide, T rightSide) {
            this.leftSide = leftSide;
            this.rightSide = rightSide;
        }

        protected boolean isSameType(String className, UMLType umlType) {
            if(className == null || umlType == null){
                return false;
            }
            return umlType.equalClassType(UMLType.extractTypeObject(className));
        }
    }

    public static abstract class BaseClassMemberElementDiff<T extends CodeElement> extends BaseElementDiff<T> {

        public BaseClassMemberElementDiff(T leftSide, T rightSide) {
            super(leftSide, rightSide);
        }

        public Set<Refactoring> getRefactorings(HashMap<String, String> renamedOrMovedContainers) {
            Set<Refactoring> results = new HashSet<>();
            boolean isRenamed = !this.leftSide.getName().equals(this.rightSide.getName());
            boolean isMoved = !this.leftSide.getContainerName().equals(this.rightSide.getContainerName());

            if (renamedOrMovedContainers.containsKey(this.leftSide.getContainerName())) {
                if (renamedOrMovedContainers.get(this.leftSide.getContainerName()).equals(this.rightSide.getContainerName())) {
                    isMoved = false;
                }
            }

            if (isMoved && isRenamed) {
                results.add(getMoveAndRenameRefactoring());
            } else if (isRenamed) {
                results.add(getRenameRefactoring());
            } else if (isMoved) {
                results.add(getMoveRefactoring());
            }

            results.addAll(getOtherRefactorings());

            return results;
        }

        protected abstract Refactoring getRenameRefactoring();

        protected abstract Refactoring getMoveRefactoring();

        protected abstract Refactoring getMoveAndRenameRefactoring();

        protected abstract Set<Refactoring> getOtherRefactorings();
    }

}
