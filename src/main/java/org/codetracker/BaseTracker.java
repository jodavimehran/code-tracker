package org.codetracker;

import gr.uom.java.xmi.*;
import gr.uom.java.xmi.diff.*;

import org.codetracker.util.GitRepository;
import org.codetracker.util.IRepository;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.LogCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.revwalk.filter.RevFilter;
import org.refactoringminer.rm1.GitHistoryRefactoringMinerImpl;
import org.refactoringminer.util.GitServiceImpl;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public abstract class BaseTracker extends AbstractTracker {
    protected final GitServiceImpl gitService = new GitServiceImpl();
    protected final Repository repository;
    protected final IRepository gitRepository;
    public BaseTracker(Repository repository, String startCommitId, String filePath) {
        super(startCommitId, filePath);
		this.repository = repository;
        this.gitRepository = new GitRepository(repository);
    }

    protected static List<String> getCommits(Repository repository, String startCommitId, String filePath, Git git) throws IOException, GitAPIException {
        if (startCommitId.equals("0")) {
        	return Collections.emptyList();
        }
    	LogCommand logCommandFile = git.log().add(repository.resolve(startCommitId)).addPath(filePath).setRevFilter(RevFilter.ALL);
        Iterable<RevCommit> fileRevisions = logCommandFile.call();
        List<String> list = StreamSupport.stream(fileRevisions.spliterator(), false).map(revCommit -> revCommit.getId().getName()).collect(Collectors.toList()); 
        /*
        if(jsonFile.exists()) {
        	final ObjectMapper mapper = new ObjectMapper();
			GitLog gitLog = mapper.readValue(jsonFile, GitLog.class);
			Map<String, List<String>> map = gitLog.getCommitLogMap();
			map.put(startCommitId, list);
			jsonFile.delete();
			mapper.writeValue(jsonFile, gitLog);
        }
        else {
        	Map<String, List<String>> map = new LinkedHashMap<>();
        	map.put(startCommitId, list);
        	final ObjectMapper mapper = new ObjectMapper();
    		mapper.writeValue(jsonFile, new GitLog(map));
        }
        */
        return list;
    }

    public static UMLModel getUMLModel(Repository repository, String commitId, Set<String> fileNames) throws Exception {
        if (fileNames == null || fileNames.isEmpty())
            return null;
        try (RevWalk walk = new RevWalk(repository)) {
            RevCommit revCommit = walk.parseCommit(repository.resolve(commitId));
            UMLModel umlModel = getUmlModel(repository, revCommit, fileNames);
            umlModel.setPartial(true);
            return umlModel;
        }
    }

    public static UMLModel getUmlModel(Repository repository, RevCommit commit, Set<String> filePaths) throws Exception {
        Set<String> repositoryDirectories = new LinkedHashSet<>();
        Map<String, String> fileContents = new LinkedHashMap<>();
        GitHistoryRefactoringMinerImpl.populateFileContents(repository, commit, filePaths, fileContents, repositoryDirectories);
        return GitHistoryRefactoringMinerImpl.createModel(fileContents, repositoryDirectories);
    }

    protected UMLModel getUMLModel(String commitId, Set<String> fileNames) throws Exception {
        return getUMLModel(repository, commitId, fileNames);
    }

    public CommitModel getCommitModel(String commitId) throws Exception {
        try (RevWalk walk = new RevWalk(repository)) {
            RevCommit currentCommit = walk.parseCommit(repository.resolve(commitId));
            RevCommit parentCommit1 = null;
            if (currentCommit.getParentCount() == 1 || currentCommit.getParentCount() == 2) {
                walk.parseCommit(currentCommit.getParent(0));
                parentCommit1 = currentCommit.getParent(0);
            }
            RevCommit parentCommit2 = null;
            if (currentCommit.getParentCount() == 2) {
//                walk.parseCommit(currentCommit.getParent(1));
//                parentCommit2 = currentCommit.getParent(1);
            }
            CommitModel commitModel = getCommitModel(parentCommit1, parentCommit2, currentCommit);
            return commitModel;
        }
    }

    private CommitModel getCommitModel(RevCommit parentCommit1, RevCommit parentCommit2, RevCommit currentCommit) throws Exception {
        Map<String, String> renamedFilesHint = new HashMap<>();
        Set<String> filePathsBefore1 = new HashSet<>();
        Set<String> filePathsCurrent1 = new HashSet<>();
        if (parentCommit1 != null) {
            gitService.fileTreeDiff(repository, currentCommit, filePathsBefore1, filePathsCurrent1, renamedFilesHint);
        }

        Set<String> filePathsBefore2 = new HashSet<>();
        Set<String> filePathsCurrent2 = new HashSet<>();
        if (parentCommit2 != null) {
            gitService.fileTreeDiff(repository, currentCommit, filePathsBefore2, filePathsCurrent2, renamedFilesHint);
        }

        Set<String> repositoryDirectoriesBefore = new LinkedHashSet<String>();
        Map<String, String> fileContentsBefore = new LinkedHashMap<String, String>();

        Set<String> repositoryDirectoriesCurrent = new LinkedHashSet<String>();
        Map<String, String> fileContentsCurrent = new LinkedHashMap<String, String>();

        if (parentCommit1 != null) {
            GitHistoryRefactoringMinerImpl.populateFileContents(repository, parentCommit1, filePathsBefore1, fileContentsBefore, repositoryDirectoriesBefore);
        }
        if (parentCommit2 != null) {
            GitHistoryRefactoringMinerImpl.populateFileContents(repository, parentCommit2, filePathsBefore2, fileContentsBefore, repositoryDirectoriesBefore);
        }
        Set<String> filePathsCurrent = new HashSet<>();
        filePathsCurrent.addAll(filePathsCurrent1);
        filePathsCurrent.addAll(filePathsCurrent2);
        GitHistoryRefactoringMinerImpl.populateFileContents(repository, currentCommit, filePathsCurrent, fileContentsCurrent, repositoryDirectoriesCurrent);

        Map<String, String> fileContentsBeforeTrimmed = new HashMap<>(fileContentsBefore);
        Map<String, String> fileContentsCurrentTrimmed = new HashMap<>(fileContentsCurrent);
        List<MoveSourceFolderRefactoring> moveSourceFolderRefactorings = GitHistoryRefactoringMinerImpl.processIdenticalFiles(fileContentsBeforeTrimmed, fileContentsCurrentTrimmed, renamedFilesHint, false);

        return new CommitModel(parentCommit1.getId().getName(), repositoryDirectoriesBefore, fileContentsBefore, fileContentsBeforeTrimmed, repositoryDirectoriesCurrent, fileContentsCurrent, fileContentsCurrentTrimmed, renamedFilesHint, moveSourceFolderRefactorings);
    }
    /*
    private static final String REPOS = System.getProperty("user.dir") + "/oracle/commits";
    protected void populateWithGitHubAPIAndSaveFiles(Repository repository, String commitId) throws IOException, InterruptedException {
    	Set<String> repositoryDirectoriesBefore = ConcurrentHashMap.newKeySet();
		Set<String> repositoryDirectoriesCurrent = ConcurrentHashMap.newKeySet();
		Map<String, String> fileContentsBefore = new ConcurrentHashMap<String, String>();
		Map<String, String> fileContentsCurrent = new ConcurrentHashMap<String, String>();
		Map<String, String> renamedFilesHint = new ConcurrentHashMap<String, String>();
		GitHistoryRefactoringMinerImpl.populateWithLocalRepositoryAndSaveFiles(repository, commitId,
				fileContentsBefore, fileContentsCurrent, renamedFilesHint, repositoryDirectoriesBefore, repositoryDirectoriesCurrent, new File(REPOS));
    }
    */
}
