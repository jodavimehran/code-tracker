package org.codetracker.util;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import gr.uom.java.xmi.UMLModel;
import org.codetracker.VersionImpl;
import org.codetracker.api.CodeElement;
import org.codetracker.api.CodeElementNotFoundException;
import org.codetracker.api.Version;
import org.codetracker.element.Attribute;
import org.codetracker.element.Block;
import org.codetracker.element.Class;
import org.codetracker.element.Method;
import org.codetracker.element.Variable;
import org.refactoringminer.rm1.GitHistoryRefactoringMinerImpl;
import org.refactoringminer.rm1.GitHistoryRefactoringMinerImpl.ChangedFileInfo;

public class CodeElementLocatorWithLocalFiles extends AbstractCodeElementLocator {
	private final UMLModel currentUMLModel;
	private final UMLModel parentUMLModel;
    private static final String REPOS = System.getProperty("user.dir") + "/src/test/resources/oracle/commits";

    public CodeElementLocatorWithLocalFiles(String cloneURL, String commitId, String filePath, String name, int lineNumber) throws Exception {
    	super(commitId, filePath, name, lineNumber);
    	Set<String> repositoryDirectoriesBefore = ConcurrentHashMap.newKeySet();
		Set<String> repositoryDirectoriesCurrent = ConcurrentHashMap.newKeySet();
		Map<String, String> fileContentsBefore = new ConcurrentHashMap<String, String>();
		Map<String, String> fileContentsCurrent = new ConcurrentHashMap<String, String>();
		Map<String, String> renamedFilesHint = new ConcurrentHashMap<String, String>();
		GitHistoryRefactoringMinerImpl miner = new GitHistoryRefactoringMinerImpl();
		ChangedFileInfo info = miner.populateWithGitHubAPIAndSaveFiles(cloneURL, commitId, 
				fileContentsBefore, fileContentsCurrent, renamedFilesHint, repositoryDirectoriesBefore, repositoryDirectoriesCurrent, new File(REPOS));
		Map<String, String> filesBefore = new LinkedHashMap<String, String>();
		Map<String, String> filesCurrent = new LinkedHashMap<String, String>();
		for(String fileName : info.getFilesBefore()) {
			if(fileContentsBefore.containsKey(fileName)) {
				filesBefore.put(fileName, fileContentsBefore.get(fileName));
			}
		}
		for(String fileName : info.getFilesCurrent()) {
			if(fileContentsCurrent.containsKey(fileName)) {
				filesCurrent.put(fileName, fileContentsCurrent.get(fileName));
			}
		}
		fileContentsBefore = filesBefore;
		fileContentsCurrent = filesCurrent;
		GitHistoryRefactoringMinerImpl.processIdenticalFiles(fileContentsBefore, fileContentsCurrent, renamedFilesHint, false);
		this.currentUMLModel = GitHistoryRefactoringMinerImpl.createModel(fileContentsCurrent, repositoryDirectoriesCurrent);
		this.parentUMLModel = GitHistoryRefactoringMinerImpl.createModel(fileContentsBefore, repositoryDirectoriesBefore);
    }

    public CodeElement locate() throws Exception {
        Version version = new VersionImpl(commitId, 0, 0, "");
        UMLModel umlModel = currentUMLModel;
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
}
