package org.codetracker.experiment;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import gr.uom.java.xmi.UMLAnonymousClass;
import gr.uom.java.xmi.UMLClass;
import gr.uom.java.xmi.UMLModel;
import gr.uom.java.xmi.UMLOperation;
import org.codetracker.element.Class;
import org.codetracker.element.Method;
import org.codetracker.experiment.oracle.MethodOracle;
import org.codetracker.experiment.oracle.history.ClassHistoryInfo;
import org.codetracker.experiment.oracle.history.MethodHistoryInfo;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.refactoringminer.api.GitService;
import org.refactoringminer.rm1.GitHistoryRefactoringMinerImpl;
import org.refactoringminer.util.GitServiceImpl;

import java.io.File;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public class AttributeOracleCreator {
    protected final static String FOLDER_TO_CLONE = "H:\\Projects\\";
    protected static final ObjectMapper MAPPER = new ObjectMapper();

    static {
        MAPPER.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    public static void main(String[] args) throws Exception {
        new AttributeOracleCreator().createOracle();
    }

    protected static Method getMethod(UMLModel umlModel, String methodName, int methodDeclarationLineNumber) {
        if (umlModel != null)
            for (UMLClass umlClass : umlModel.getClassList()) {
                Method method = getMethod(methodName, methodDeclarationLineNumber, umlClass.getOperations());
                if (method != null) return method;
                for (UMLAnonymousClass anonymousClass : umlClass.getAnonymousClassList()) {
                    method = getMethod(methodName, methodDeclarationLineNumber, anonymousClass.getOperations());
                    if (method != null) return method;
                }
            }
        return null;
    }

    private static Method getMethod(String methodName, int methodDeclarationLineNumber, List<UMLOperation> operations) {
        for (UMLOperation umlOperation : operations) {
            Method method = Method.of(umlOperation, null);
            if (method.getUmlOperation().getName().equals(methodName) &&
                    method.getUmlOperation().getLocationInfo().getStartLine() <= methodDeclarationLineNumber &&
                    method.getUmlOperation().getLocationInfo().getEndLine() >= methodDeclarationLineNumber)
                return method;
        }
        return null;
    }

    protected UMLModel getUMLModel(Repository repository, String commitId, List<String> fileNames) throws Exception {
        if (fileNames == null || fileNames.isEmpty())
            return null;
        try (RevWalk walk = new RevWalk(repository)) {
            RevCommit revCommit = walk.parseCommit(repository.resolve(commitId));
            return GitHistoryRefactoringMinerImpl.getUmlModel(repository, revCommit, fileNames);
        }
    }

    private void createOracle() throws Exception {
        GitService gitService = new GitServiceImpl();

        for (MethodOracle methodOracle : MethodOracle.all()) {
            String oracleName = methodOracle.getName();
            HashSet<String> classNames = new HashSet<>();
            for (Map.Entry<String, MethodHistoryInfo> oracleEntry : methodOracle.getOracle().entrySet()) {
                String oracleInstanceFileName = oracleEntry.getKey();
                MethodHistoryInfo methodHistoryInfo = oracleEntry.getValue();
                String repositoryWebURL = methodHistoryInfo.getRepositoryWebURL();
                String repositoryName = repositoryWebURL.replace("https://github.com/", "").replace(".git", "").replace("/", "\\");
                String projectDirectory = FOLDER_TO_CLONE + repositoryName;
                try (Repository repository = gitService.cloneIfNotExists(projectDirectory, repositoryWebURL)) {
                    UMLModel umlModel = getUMLModel(repository, methodHistoryInfo.getStartCommitId(), Collections.singletonList(methodHistoryInfo.getFilePath()));
                    Method start = getMethod(umlModel, methodHistoryInfo.getFunctionName(), methodHistoryInfo.getFunctionStartLine());
                    String className = start.getUmlOperation().getClassName();
                    if (classNames.contains(className)) {
                        continue;
                    }
                    classNames.add(className);
                    for (UMLClass umlClass : umlModel.getClassList()) {
                        if (umlClass.getName().equals(className)) {
                            Class aClass = Class.of(umlClass, null);
                            ClassHistoryInfo classHistoryInfo = new ClassHistoryInfo();
                            classHistoryInfo.setRepositoryName(methodHistoryInfo.getRepositoryName());
                            classHistoryInfo.setRepositoryWebURL(methodHistoryInfo.getRepositoryWebURL());
                            classHistoryInfo.setFilePath(methodHistoryInfo.getFilePath());
                            classHistoryInfo.setStartCommitId(methodHistoryInfo.getStartCommitId());
                            classHistoryInfo.setClassKey(aClass.getName());
                            classHistoryInfo.setClassName(umlClass.getNonQualifiedName());
                            classHistoryInfo.setClassDeclarationLine(umlClass.getLocationInfo().getStartLine());

                            ObjectWriter writer = MAPPER.writer(new DefaultPrettyPrinter());
                            File newFile = new File(String.format("E:\\Data\\History\\class\\oracle\\%s\\%s", oracleName, oracleInstanceFileName.replace("-" + start.getUmlOperation().getName(), "")));
                            writer.writeValue(newFile, classHistoryInfo);
//                            for (UMLAttribute umlAttribute : umlClass.getAttributes()) {
//                                Attribute attribute = Attribute.of(umlAttribute, null);
//                                AttributeHistoryInfo attributeHistoryInfo = new AttributeHistoryInfo();
//                                attributeHistoryInfo.setRepositoryName(methodHistoryInfo.getRepositoryName());
//                                attributeHistoryInfo.setRepositoryWebURL(methodHistoryInfo.getRepositoryWebURL());
//                                attributeHistoryInfo.setFilePath(methodHistoryInfo.getFilePath());
//                                attributeHistoryInfo.setStartCommitId(methodHistoryInfo.getStartCommitId());
//                                attributeHistoryInfo.setAttributeName(umlAttribute.getName());
//                                attributeHistoryInfo.setAttributeKey(attribute.getName());
//                                attributeHistoryInfo.setAttributeDeclarationLine(umlAttribute.getLocationInfo().getStartLine());
//
//
//
//                            }
                        }
                    }
                }
            }

        }
    }
}
