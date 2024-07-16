package org.codetracker;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.codetracker.api.CodeElement;
import org.codetracker.api.Version;
import org.codetracker.util.CodeElementLocator;
import org.codetracker.util.GitRepository;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.refactoringminer.rm1.GitHistoryRefactoringMinerImpl;

import gr.uom.java.xmi.UMLModel;

public class FileTrackerImpl extends BaseTracker {

	public FileTrackerImpl(Repository repository, String startCommitId, String filePath) {
		super(repository, startCommitId, filePath);
	}

	public void blame() throws Exception {
		try (Git git = new Git(repository); RevWalk walk = new RevWalk(repository)) {
            Version startVersion = gitRepository.getVersion(startCommitId);
            
            RevCommit revCommit = walk.parseCommit(repository.resolve(startCommitId));
            Set<String> repositoryDirectories = new LinkedHashSet<>();
            Map<String, String> fileContents = new LinkedHashMap<>();
            GitHistoryRefactoringMinerImpl.populateFileContents(repository, revCommit, Collections.singleton(filePath), fileContents, repositoryDirectories);
            UMLModel umlModel = GitHistoryRefactoringMinerImpl.createModel(fileContents, repositoryDirectories);
            umlModel.setPartial(true);
            // extract program elements to be blamed
            String fileContentAsString = fileContents.get(filePath);
            List<String> lines = new ArrayList<>();
            Map<Integer, CodeElement> lineNumberToCodeElementMap = new LinkedHashMap<>();
            Set<CodeElement> programElements = new LinkedHashSet<>();
            try (BufferedReader reader = new BufferedReader(new StringReader(fileContentAsString))) {
                String line;
                int lineNumber = 1;
                while ((line = reader.readLine()) != null) {
                    lines.add(line);
                    CodeElementLocator locator = new CodeElementLocator((GitRepository) gitRepository, startCommitId, filePath, lineNumber);
                    CodeElement codeElement = locator.locateWithoutName(startVersion, umlModel);
                    programElements.add(codeElement);
                    lineNumberToCodeElementMap.put(lineNumber, codeElement);
                    lineNumber++;
                }
            }
		}
	}
}
