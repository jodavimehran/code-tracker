package org.codetracker.element;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.codetracker.api.Version;
import org.codetracker.util.Util;

import gr.uom.java.xmi.LocationInfo;
import gr.uom.java.xmi.UMLAbstractClass;
import gr.uom.java.xmi.UMLAnnotation;
import gr.uom.java.xmi.UMLAttribute;
import gr.uom.java.xmi.VariableDeclarationContainer;

public class Annotation extends BaseCodeElement {
	private final UMLAnnotation annotation;
	private final VariableDeclarationContainer operation;
	private final UMLAbstractClass clazz;

	public Annotation(UMLAnnotation annotation, VariableDeclarationContainer operation, String identifierIgnoringVersion, String name, String filePath, Version version) {
		super(identifierIgnoringVersion, name, filePath, version);
		this.annotation = annotation;
		this.operation = operation;
		this.clazz = null;
	}

	public BaseCodeElement of(Version version) {
		if (operation != null)
			return of(this.annotation, this.operation, version);
		if (clazz != null)
			return of(this.annotation, this.clazz, version);
		return null;
	}

	public Annotation(UMLAnnotation annotation, UMLAbstractClass clazz, String identifierIgnoringVersion, String name, String filePath, Version version) {
		super(identifierIgnoringVersion, name, filePath, version);
		this.annotation = annotation;
		this.clazz = clazz;
		this.operation = null;
	}

	public UMLAnnotation getAnnotation() {
		return annotation;
	}

	public Optional<VariableDeclarationContainer> getOperation() {
		return operation != null ? Optional.of(operation) : Optional.empty();
	}

	public Optional<UMLAbstractClass> getClazz() {
		return clazz != null ? Optional.of(clazz) : Optional.empty();
	}

    public static Annotation of(UMLAnnotation annotation, VariableDeclarationContainer operation, Version version) {
    	if (operation instanceof UMLAttribute) {
    		Attribute attribute = Attribute.of((UMLAttribute) operation, version);
    		return of(annotation, attribute);
    	}
        Method method = Method.of(operation, version);
        return of(annotation, method);
    }

    public static Annotation of(UMLAnnotation annotation, UMLAbstractClass clazz, Version version) {
        Class c = Class.of(clazz, version);
        return of(annotation, c);
    }

	public static Annotation of(UMLAnnotation annotation, Class clazz) {
		LocationInfo annotationLocationInfo = annotation.getLocationInfo();
		String statementType = annotationLocationInfo.getCodeElementType().name();
		String name = String.format("%s$%s(%d-%d)", clazz.getName(), statementType, annotationLocationInfo.getStartLine(), annotationLocationInfo.getEndLine());
		String sha512 = Util.getSHA512(annotation.toString());
		String identifierExcludeVersion = String.format(
				"%s$%s:{%s,%s}",
				clazz.getIdentifierIgnoringVersion(),
				statementType,
				sha512,
				getSignature(annotation, clazz.getUmlClass())
				);
		return new Annotation(annotation, clazz.getUmlClass(), identifierExcludeVersion, name, annotationLocationInfo.getFilePath(), clazz.getVersion());
	}

	public static Annotation of(UMLAnnotation annotation, Attribute attribute) {
		LocationInfo annotationLocationInfo = annotation.getLocationInfo();
		String statementType = annotationLocationInfo.getCodeElementType().name();
		String name = String.format("%s$%s(%d-%d)", attribute.getName(), statementType, annotationLocationInfo.getStartLine(), annotationLocationInfo.getEndLine());
		String sha512 = Util.getSHA512(annotation.toString());
		String identifierExcludeVersion = String.format(
				"%s$%s:{%s,%s}",
				attribute.getIdentifierIgnoringVersion(),
				statementType,
				sha512,
				getSignature(annotation, attribute.getUmlAttribute())
				);
		return new Annotation(annotation, attribute.getUmlAttribute(), identifierExcludeVersion, name, annotationLocationInfo.getFilePath(), attribute.getVersion());
	}

	public static Annotation of(UMLAnnotation annotation, Method method) {
		LocationInfo annotationLocationInfo = annotation.getLocationInfo();
		String statementType = annotationLocationInfo.getCodeElementType().name();
		String name = String.format("%s$%s(%d-%d)", method.getName(), statementType, annotationLocationInfo.getStartLine(), annotationLocationInfo.getEndLine());
		String sha512 = Util.getSHA512(annotation.toString());
		String identifierExcludeVersion = String.format(
				"%s$%s:{%s,%s}",
				method.getIdentifierIgnoringVersion(),
				statementType,
				sha512,
				getSignature(annotation, method.getUmlOperation())
				);
		return new Annotation(annotation, method.getUmlOperation(), identifierExcludeVersion, name, annotationLocationInfo.getFilePath(), method.getVersion());
	}

	private static String getSignature(UMLAnnotation annotation, VariableDeclarationContainer operation) {
		String statementType = annotation.getLocationInfo().getCodeElementType().name();
		List<UMLAnnotation> sameTypeSibling = operation.getAnnotations().stream().filter(st -> statementType.equals(st.getLocationInfo().getCodeElementType().name())).collect(Collectors.toList());
		int typeIndex = sameTypeSibling.indexOf(annotation) + 1;
		return String.format("%s%d", statementType, typeIndex);
	}

	private static String getSignature(UMLAnnotation annotation, UMLAbstractClass clazz) {
		String statementType = annotation.getLocationInfo().getCodeElementType().name();
		List<UMLAnnotation> sameTypeSibling = clazz.getAnnotations().stream().filter(st -> statementType.equals(st.getLocationInfo().getCodeElementType().name())).collect(Collectors.toList());
		int typeIndex = sameTypeSibling.indexOf(annotation) + 1;
		return String.format("%s%d", statementType, typeIndex);
	}

	@Override
	public LocationInfo getLocation() {
		return annotation.getLocationInfo();
	}
}
