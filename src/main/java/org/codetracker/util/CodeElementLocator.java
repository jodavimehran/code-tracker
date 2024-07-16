package org.codetracker.util;

import gr.uom.java.xmi.UMLModel;
import org.codetracker.api.CodeElement;
import org.codetracker.api.Version;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.refactoringminer.rm1.GitHistoryRefactoringMinerImpl;

import java.util.*;

public class CodeElementLocator extends AbstractCodeElementLocator {
    private final Repository repository;
    private final IRepository gitRepository;
    public CodeElementLocator(Repository repository, String commitId, String filePath, String name, int lineNumber) {
        super(commitId, filePath, name, lineNumber);
		this.repository = repository;
        this.gitRepository = new GitRepository(repository);
    }

    public CodeElementLocator(Repository repository, String commitId, String filePath, int lineNumber) {
        super(commitId, filePath, lineNumber);
		this.repository = repository;
        this.gitRepository = new GitRepository(repository);
    }

    public CodeElementLocator(GitRepository repository, String commitId, String filePath, int lineNumber) {
        super(commitId, filePath, lineNumber);
        this.repository = repository.getRepository();
        this.gitRepository = repository;
    }

    @Override
	public CodeElement locate() throws Exception {
    	if (name == null) {
    		return locateWithoutName();
    	}
        Version version = gitRepository.getVersion(commitId);
        UMLModel umlModel = getUMLModel(repository, commitId, Collections.singleton(filePath));
        return locateWithName(version, umlModel);
    }

	private CodeElement locateWithoutName() throws Exception {
        Version version = gitRepository.getVersion(commitId);
        UMLModel umlModel = getUMLModel(repository, commitId, Collections.singleton(filePath));
        return locateWithoutName(version, umlModel);
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
}
