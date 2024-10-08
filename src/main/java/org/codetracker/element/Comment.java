package org.codetracker.element;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.codetracker.api.Version;
import org.codetracker.util.Util;

import gr.uom.java.xmi.LocationInfo;
import gr.uom.java.xmi.UMLAbstractClass;
import gr.uom.java.xmi.UMLAbstractDocumentation;
import gr.uom.java.xmi.UMLAttribute;
import gr.uom.java.xmi.VariableDeclarationContainer;

public class Comment extends BaseCodeElement {
	private final UMLAbstractDocumentation comment;
	private final VariableDeclarationContainer operation;
	private final UMLAbstractClass clazz;

	public Comment(UMLAbstractDocumentation comment, VariableDeclarationContainer operation, String identifierIgnoringVersion, String name, String filePath, Version version) {
		super(identifierIgnoringVersion, name, filePath, version);
		this.comment = comment;
		this.operation = operation;
		this.clazz = null;
	}

	public BaseCodeElement of(Version version) {
		if (operation != null)
			return of(this.comment, this.operation, version);
		if (clazz != null)
			return of(this.comment, this.clazz, version);
		return null;
	}

	public Comment(UMLAbstractDocumentation comment, UMLAbstractClass clazz, String identifierIgnoringVersion, String name, String filePath, Version version) {
		super(identifierIgnoringVersion, name, filePath, version);
		this.comment = comment;
		this.clazz = clazz;
		this.operation = null;
	}

	public boolean isMultiLine() {
		return getLocation().getEndLine() > getLocation().getStartLine();
	}

	public UMLAbstractDocumentation getComment() {
		return comment;
	}

	public Optional<VariableDeclarationContainer> getOperation() {
		return operation != null ? Optional.of(operation) : Optional.empty();
	}

	public Optional<UMLAbstractClass> getClazz() {
		return clazz != null ? Optional.of(clazz) : Optional.empty();
	}

    public static Comment of(UMLAbstractDocumentation comment, VariableDeclarationContainer operation, Version version) {
    	if (operation instanceof UMLAttribute) {
    		Attribute attribute = Attribute.of((UMLAttribute) operation, version);
    		return of(comment, attribute);
    	}
        Method method = Method.of(operation, version);
        return of(comment, method);
    }

    public static Comment of(UMLAbstractDocumentation comment, UMLAbstractClass clazz, Version version) {
        Class c = Class.of(clazz, version);
        return of(comment, c);
    }

	public static Comment of(UMLAbstractDocumentation comment, Class clazz) {
		LocationInfo commentLocationInfo = comment.getLocationInfo();
		String statementType = commentLocationInfo.getCodeElementType().name();
		String name = String.format("%s$%s(%d-%d)", clazz.getName(), statementType, commentLocationInfo.getStartLine(), commentLocationInfo.getEndLine());
		String sha512 = Util.getSHA512(comment.getText());
		String identifierExcludeVersion = String.format(
				"%s$%s:{%s,%s}",
				clazz.getIdentifierIgnoringVersion(),
				statementType,
				sha512,
				getSignature(comment, clazz.getUmlClass())
				);
		return new Comment(comment, clazz.getUmlClass(), identifierExcludeVersion, name, commentLocationInfo.getFilePath(), clazz.getVersion());
	}

	public static Comment of(UMLAbstractDocumentation comment, Attribute attribute) {
		LocationInfo commentLocationInfo = comment.getLocationInfo();
		String statementType = commentLocationInfo.getCodeElementType().name();
		String name = String.format("%s$%s(%d-%d)", attribute.getName(), statementType, commentLocationInfo.getStartLine(), commentLocationInfo.getEndLine());
		String sha512 = Util.getSHA512(comment.getText());
		String identifierExcludeVersion = String.format(
				"%s$%s:{%s,%s}",
				attribute.getIdentifierIgnoringVersion(),
				statementType,
				sha512,
				getSignature(comment, attribute.getUmlAttribute())
				);
		return new Comment(comment, attribute.getUmlAttribute(), identifierExcludeVersion, name, commentLocationInfo.getFilePath(), attribute.getVersion());
	}

	public static Comment of(UMLAbstractDocumentation comment, Method method) {
		LocationInfo commentLocationInfo = comment.getLocationInfo();
		String statementType = commentLocationInfo.getCodeElementType().name();
		String name = String.format("%s$%s(%d-%d)", method.getName(), statementType, commentLocationInfo.getStartLine(), commentLocationInfo.getEndLine());
		String sha512 = Util.getSHA512(comment.getText());
		String identifierExcludeVersion = String.format(
				"%s$%s:{%s,%s}",
				method.getIdentifierIgnoringVersion(),
				statementType,
				sha512,
				getSignature(comment, method.getUmlOperation())
				);
		return new Comment(comment, method.getUmlOperation(), identifierExcludeVersion, name, commentLocationInfo.getFilePath(), method.getVersion());
	}

	private static String getSignature(UMLAbstractDocumentation comment, VariableDeclarationContainer operation) {
		String statementType = comment.getLocationInfo().getCodeElementType().name();
		List<UMLAbstractDocumentation> sameTypeSibling = operation.getComments().stream().filter(st -> statementType.equals(st.getLocationInfo().getCodeElementType().name())).collect(Collectors.toList());
		int typeIndex = sameTypeSibling.indexOf(comment) + 1;
		return String.format("%s%d", statementType, typeIndex);
	}

	private static String getSignature(UMLAbstractDocumentation comment, UMLAbstractClass clazz) {
		String statementType = comment.getLocationInfo().getCodeElementType().name();
		List<UMLAbstractDocumentation> sameTypeSibling = clazz.getComments().stream().filter(st -> statementType.equals(st.getLocationInfo().getCodeElementType().name())).collect(Collectors.toList());
		int typeIndex = sameTypeSibling.indexOf(comment) + 1;
		return String.format("%s%d", statementType, typeIndex);
	}

	@Override
	public LocationInfo getLocation() {
		return comment.getLocationInfo();
	}
}
