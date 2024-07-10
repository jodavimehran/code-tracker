package org.codetracker.element;

import gr.uom.java.xmi.LocationInfo;
import gr.uom.java.xmi.UMLAbstractClass;
import gr.uom.java.xmi.UMLClass;
import gr.uom.java.xmi.UMLComment;
import gr.uom.java.xmi.UMLJavadoc;

import org.codetracker.api.Version;

import static org.codetracker.util.Util.annotationsToString;
import static org.codetracker.util.Util.getPath;

import java.util.function.Predicate;

public class Class extends BaseCodeElement {
    private final UMLAbstractClass umlClass;

    private Class(UMLAbstractClass umlClass, String identifierExcludeVersion, String name, String filePath, Version version) {
        super(identifierExcludeVersion, name, filePath, version);
        this.umlClass = umlClass;
    }

    public static Class of(UMLAbstractClass umlClass, Version version) {
        String sourceFolder = getPath(umlClass.getLocationInfo().getFilePath(), umlClass.getName());
        String packageName = umlClass.getPackageName();
        String name = umlClass.getName().replace(umlClass.getPackageName(), "").replace(".", "");
        String modifiersString = new ModifiersBuilder()
                .isFinal(umlClass.isFinal())
                .isStatic(umlClass.isStatic())
                .isAbstract(umlClass.isAbstract())
                .build();
        String visibility = umlClass.getVisibility().toString();
        String identifierExcludeVersion = String.format("%s%s.(%s)%s(%s)%s%s", sourceFolder, packageName, visibility, modifiersString, umlClass.getTypeDeclarationKind(), name, annotationsToString(umlClass.getAnnotations()));
        String className = String.format("%s%s.(%s)%s%s(%d)", sourceFolder, packageName, visibility, modifiersString, name, umlClass.getLocationInfo().getStartLine());
        return new Class(umlClass, identifierExcludeVersion, className, umlClass.getLocationInfo().getFilePath(), version);
    }

    public Comment findComment(Predicate<Comment> equalOperator) {
        for (UMLComment umlComment : umlClass.getComments()) {
            Comment comment = Comment.of(umlComment, this);
            if (comment != null && equalOperator.test(comment)) {
                return comment;
            }
        }
        if (umlClass instanceof UMLClass) {
        	UMLJavadoc javadoc = ((UMLClass) umlClass).getJavadoc();
        	if (javadoc != null) {
        		Comment comment = Comment.of(javadoc, this);
        		if (comment != null && equalOperator.test(comment)) {
                    return comment;
                }
        	}
        }
        return null;
    }

    public UMLAbstractClass getUmlClass() {
        return umlClass;
    }

	public void checkClosingBracket(int lineNumber) {
		if (getLocation().getEndLine() == lineNumber) {
			setClosingCurlyBracket(true);
		}
	}

    @Override
    public LocationInfo getLocation() {
        return umlClass.getLocationInfo();
    }
}
