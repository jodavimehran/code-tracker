package org.codetracker.util;

import gr.uom.java.xmi.*;
import org.codetracker.api.CodeElement;
import org.codetracker.api.CodeElementNotFoundException;
import org.codetracker.api.Version;
import org.codetracker.element.*;
import org.codetracker.element.Class;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.refactoringminer.rm1.GitHistoryRefactoringMinerImpl;

import java.util.*;
import java.util.function.Predicate;

public class CodeElementLocator {
    private final Repository repository;
    private final IRepository gitRepository;
    private final String commitId;
    private final String filePath;
    private final String name;
    private final int lineNumber;

    public CodeElementLocator(Repository repository, String commitId, String filePath, String name, int lineNumber) {
        this.repository = repository;
        this.gitRepository = new GitRepository(repository);
        this.commitId = commitId;
        this.filePath = filePath;
        this.name = name;
        this.lineNumber = lineNumber;
    }

    private boolean classPredicate(Class clazz) {
        return clazz.getUmlClass().getNonQualifiedName().equals(name);
    }

    private boolean methodPredicateWithName(Method method) {
        return method.getUmlOperation().getName().equals(name) &&
                method.getUmlOperation().getLocationInfo().getStartLine() <= lineNumber &&
                method.getUmlOperation().getLocationInfo().getEndLine() >= lineNumber;
    }

    private boolean methodPredicateWithoutName(Method method) {
        return method.getUmlOperation().getLocationInfo().getStartLine() <= lineNumber &&
                method.getUmlOperation().getLocationInfo().getEndLine() >= lineNumber;
    }

    private boolean variablePredicate(Variable variable) {
        return variable.getVariableDeclaration().getVariableName().equals(name) &&
                variable.getVariableDeclaration().getLocationInfo().getStartLine() <= lineNumber &&
                variable.getVariableDeclaration().getLocationInfo().getEndLine() >= lineNumber;
    }

    private boolean attributePredicate(Attribute attribute) {
        return attribute.getUmlAttribute().getName().equals(name) &&
                attribute.getUmlAttribute().getLocationInfo().getStartLine() <= lineNumber &&
                attribute.getUmlAttribute().getLocationInfo().getEndLine() >= lineNumber;
    }

    private boolean blockPredicate(Block block) {
        String blockCodeElementTypeName = block.getComposite().getLocationInfo().getCodeElementType().getName();
        if(blockCodeElementTypeName != null) {
            return blockCodeElementTypeName.equals(name) &&
                    block.getComposite().getLocationInfo().getStartLine() == lineNumber &&
                    block.getComposite().getLocationInfo().getEndLine() >= lineNumber;
        }
        return block.getComposite().getLocationInfo().getStartLine() == lineNumber &&
                block.getComposite().getLocationInfo().getEndLine() >= lineNumber;
    }

    public CodeElement locate() throws Exception {
        Version version = gitRepository.getVersion(commitId);
        UMLModel umlModel = getUMLModel(repository, commitId, Collections.singleton(filePath));
        Class clazz = getClass(umlModel, version, this::classPredicate);
        if (clazz != null) {
            return clazz;
        }
        Attribute attribute = getAttribute(umlModel, version, this::attributePredicate);
        if (attribute != null) {
            return attribute;
        }
        Method method = getMethod(umlModel, version, this::methodPredicateWithName);
        if (method != null) {
            return method;
        }
        else {
            method = getMethod(umlModel, version, this::methodPredicateWithoutName);
            if (method != null) {
                Variable variable = method.findVariable(this::variablePredicate);
                if (variable != null) {
                    return variable;
                }
                Block block = method.findBlock(this::blockPredicate);
                if (block != null) {
                    return block;
                }
            }
        }
        throw new CodeElementNotFoundException(filePath, name, lineNumber);
    }

    private static UMLModel getUMLModel(Repository repository, String commitId, Set<String> fileNames) throws Exception {
        if (fileNames == null || fileNames.isEmpty())
            return null;
        try (RevWalk walk = new RevWalk(repository)) {
            RevCommit revCommit = walk.parseCommit(repository.resolve(commitId));
            UMLModel umlModel = getUmlModel(repository, revCommit, fileNames);
            umlModel.setPartial(true);
            return umlModel;
        }
    }

    private static UMLModel getUmlModel(Repository repository, RevCommit commit, Set<String> filePaths) throws Exception {
        Set<String> repositoryDirectories = new LinkedHashSet<>();
        Map<String, String> fileContents = new LinkedHashMap<>();
        GitHistoryRefactoringMinerImpl.populateFileContents(repository, commit, filePaths, fileContents, repositoryDirectories);
        return GitHistoryRefactoringMinerImpl.createModel(fileContents, repositoryDirectories);
    }

    private static Method getMethod(UMLModel umlModel, Version version, Predicate<Method> predicate) {
        if (umlModel != null)
            for (UMLClass umlClass : umlModel.getClassList()) {
                for (UMLAnonymousClass anonymousClass : umlClass.getAnonymousClassList()) {
                    Method method = getMethod(version, predicate, anonymousClass.getOperations());
                    if (method != null) return method;
                }
                Method method = getMethod(version, predicate, umlClass.getOperations());
                if (method != null) return method;
            }
        return null;
    }

    private static Method getMethod(Version version, Predicate<Method> predicate, List<UMLOperation> operations) {
        for (UMLOperation umlOperation : operations) {
            Method method = Method.of(umlOperation, version);
            if (predicate.test(method))
                return method;
        }
        return null;
    }

    private static Attribute getAttribute(UMLModel umlModel, Version version, Predicate<Attribute> predicate) {
        if (umlModel != null)
            for (UMLClass umlClass : umlModel.getClassList()) {
                for (UMLAnonymousClass anonymousClass : umlClass.getAnonymousClassList()) {
                    Attribute attribute = getAttribute(version, predicate, anonymousClass.getAttributes());
                    if (attribute != null) return attribute;
                    attribute = getAttribute(version, predicate, anonymousClass.getEnumConstants());
                    if (attribute != null) return attribute;
                }
                Attribute attribute = getAttribute(version, predicate, umlClass.getAttributes());
                if (attribute != null) return attribute;
                attribute = getAttribute(version, predicate, umlClass.getEnumConstants());
                if (attribute != null) return attribute;
            }
        return null;
    }

    private static Attribute getAttribute(Version version, Predicate<Attribute> predicate, List<? extends UMLAttribute> attributes) {
        for (UMLAttribute umlAttribute : attributes) {
            Attribute attribute = Attribute.of(umlAttribute, version);
            if (predicate.test(attribute))
                return attribute;
        }
        return null;
    }

    private static Class getClass(UMLModel umlModel, Version version, Predicate<Class> predicate) {
        if (umlModel != null)
            for (UMLClass umlClass : umlModel.getClassList()) {
                Class clazz = Class.of(umlClass, version);
                if (predicate.test(clazz))
                    return clazz;
            }
        return null;
    }
}
