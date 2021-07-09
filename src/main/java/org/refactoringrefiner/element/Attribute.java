package org.refactoringrefiner.element;

import gr.uom.java.xmi.UMLAttribute;
import org.refactoringrefiner.api.Version;
import org.refactoringrefiner.util.Util;

public class Attribute extends BaseCodeElement {
    private final UMLAttribute umlAttribute;

    private Attribute(UMLAttribute umlAttribute, String identifierExcludeVersion, String name, String filePath, Version version) {
        super(identifierExcludeVersion, name, filePath, version);
        this.umlAttribute = umlAttribute;
    }

    public static Attribute of(UMLAttribute umlAttribute, Version version) {
        String sourceFolder = Util.getPath(umlAttribute.getLocationInfo().getFilePath(), umlAttribute.getClassName());
        String name = umlAttribute.getName();
        String className = umlAttribute.getClassName();
        String visibility = umlAttribute.getVisibility();
        String type = umlAttribute.getType().toQualifiedString();

        String identifierExcludeVersion = String.format("%s%s@(%s)%s:%s%s", sourceFolder, className, visibility, name, type, Util.annotationsToString(umlAttribute.getAnnotations()));
        return new Attribute(umlAttribute, identifierExcludeVersion, String.format("%s%s@%s", sourceFolder, className, name), umlAttribute.getLocationInfo().getFilePath(), version);
    }

    public UMLAttribute getUmlAttribute() {
        return umlAttribute;
    }
//    public static class AttributeElementDiff extends BaseClassMemberElementDiff<Attribute> {
//
//        public AttributeElementDiff(Attribute leftSide, Attribute rightSide) {
//            super(leftSide, rightSide);
//        }
//
//        protected Refactoring getRenameRefactoring() {
//            return new RenameAttributeRefactoring(leftSide.info, rightSide.info, new HashSet<>());
//        }
//
//        protected Refactoring getMoveRefactoring() {
//            MoveAttributeRefactoring pullUpOrPushDownRefactoring = getPullUpOrPushDownRefactoring();
//            if (pullUpOrPushDownRefactoring != null) return pullUpOrPushDownRefactoring;
//            return new MoveAttributeRefactoring(leftSide.info, rightSide.info);
//        }
//
//        private MoveAttributeRefactoring getPullUpOrPushDownRefactoring() {
//            if (isSameType(rightSide.info.getClassName(), leftSide.info.getSuperclass())) {
//                return new PullUpAttributeRefactoring(leftSide.info, rightSide.info);
//            }
//            if (isSameType(leftSide.info.getClassName(), rightSide.info.getSuperclass())) {
//                return new PushDownAttributeRefactoring(leftSide.info, rightSide.info);
//            }
//            return null;
//        }
//
//        protected Refactoring getMoveAndRenameRefactoring() {
//            MoveAttributeRefactoring pullUpOrPushDownRefactoring = getPullUpOrPushDownRefactoring();
//            if (pullUpOrPushDownRefactoring != null) return pullUpOrPushDownRefactoring;
//            return new MoveAndRenameAttributeRefactoring(leftSide.info, rightSide.info, new HashSet<>());
//        }
//
//        protected Set<Refactoring> getOtherRefactorings() {
//            return new UMLAttributeDiff(leftSide.info, rightSide.info, new ArrayList<>()).getRefactorings();
//        }
//    }
}
