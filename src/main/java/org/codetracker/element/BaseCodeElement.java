package org.refactoringrefiner.element;

import org.refactoringrefiner.api.CodeElement;
import org.refactoringrefiner.api.Version;

import java.util.Objects;

public abstract class BaseCodeElement implements CodeElement {
    protected final String identifier;
    protected final String identifierIgnoringVersion;
    protected final String name;
    protected final String filePath;
    protected final Version version;
    protected boolean isRemoved;
    protected boolean isAdded;

    public BaseCodeElement(String identifierIgnoringVersion, String name, String filePath, Version version) {
        this.identifier = version != null ? identifierIgnoringVersion + version : identifierIgnoringVersion;
        this.identifierIgnoringVersion = identifierIgnoringVersion;
        this.name = name;
        this.filePath = filePath;
        this.version = version;
    }


    @Override
    public final int compareTo(CodeElement o) {
        return this.name.compareTo(o.getName());
    }

    @Override
    public final Version getVersion() {
        return version;
    }

    @Override
    public String getFilePath() {
        return filePath;
    }

    public final boolean isRemoved() {
        return isRemoved;
    }

    public final void setRemoved(boolean removed) {
        isRemoved = removed;
    }

    public final boolean isAdded() {
        return isAdded;
    }

    public final void setAdded(boolean added) {
        isAdded = added;
    }

    @Override
    public String getName() {
        return name;
    }

    public boolean equalName(BaseCodeElement baseCodeElement) {
        return this.getName().equals(baseCodeElement.getName());
    }

    public boolean equalIdentifierIgnoringVersion(BaseCodeElement codeElement) {
        return this.identifierIgnoringVersion.equals(codeElement.identifierIgnoringVersion);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BaseCodeElement that = (BaseCodeElement) o;
        return Objects.equals(identifier, that.identifier);
    }

    @Override
    public int hashCode() {
        return Objects.hash(identifier);
    }

    @Override
    public String toString() {
        if (version != null)
            return String.format("%s%s", name, version);
        return name;
    }

    public String getIdentifier() {
        return identifier;
    }

    public String getIdentifierIgnoringVersion() {
        return identifierIgnoringVersion;
    }

//        /**
//     * @param <T> Code Element
//     */
//    public static abstract class BaseElementDiff<T extends CodeElement> {
//        protected final T leftSide;
//        protected final T rightSide;
//
//        public BaseElementDiff(T leftSide, T rightSide) {
//            this.leftSide = leftSide;
//            this.rightSide = rightSide;
//        }
//
//        protected boolean isSameType(String className, UMLType umlType) {
//            if (className == null || umlType == null) {
//                return false;
//            }
//            return umlType.equalClassType(UMLType.extractTypeObject(className));
//        }
//    }

//    public static abstract class BaseClassMemberElementDiff<T extends CodeElement> extends BaseElementDiff<T> {
//
//        public BaseClassMemberElementDiff(T leftSide, T rightSide) {
//            super(leftSide, rightSide);
//        }
//
//        public Set<Refactoring> getRefactorings(HashMap<String, String> renamedOrMovedContainers) {
//            Set<Refactoring> results = new HashSet<>();
//            boolean isRenamed = !this.leftSide.getName().equals(this.rightSide.getName());
//            boolean isMoved = !this.leftSide.getContainerName().equals(this.rightSide.getContainerName());
//
//            if (renamedOrMovedContainers.containsKey(this.leftSide.getContainerName())) {
//                if (renamedOrMovedContainers.get(this.leftSide.getContainerName()).equals(this.rightSide.getContainerName())) {
//                    isMoved = false;
//                }
//            }
//
//            if (isMoved && isRenamed) {
//                results.add(getMoveAndRenameRefactoring());
//            } else if (isRenamed) {
//                results.add(getRenameRefactoring());
//            } else if (isMoved) {
//                results.add(getMoveRefactoring());
//            }
//
//            results.addAll(getOtherRefactorings());
//
//            return results;
//        }
//
//        protected abstract Refactoring getRenameRefactoring();
//
//        protected abstract Refactoring getMoveRefactoring();
//
//        protected abstract Refactoring getMoveAndRenameRefactoring();
//
//        protected abstract Set<Refactoring> getOtherRefactorings();
//    }

}
