package org.codetracker;

import gr.uom.java.xmi.diff.*;

import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.refactoringminer.rm1.GitHistoryRefactoringMinerImpl;
import org.refactoringminer.rm1.GitHistoryRefactoringMinerImpl.ChangedFileInfo;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;

public abstract class BaseTrackerWithLocalFiles extends AbstractTracker {
    protected final String cloneURL;
    private static final String REPOS = System.getProperty("user.dir") + "/oracle/commits";

    public BaseTrackerWithLocalFiles(String cloneURL, String startCommitId, String filePath) {
        super(startCommitId, filePath);
        this.cloneURL = cloneURL;
    }

    protected static List<String> getCommits(String commitId, File jsonFile) throws IOException, GitAPIException {
    	if (commitId.equals("0")) {
        	return Collections.emptyList();
        }
    	if(jsonFile.exists()) {
        	final ObjectMapper mapper = new ObjectMapper();
			GitLog gitLog = mapper.readValue(jsonFile, GitLog.class);
			Map<String, List<String>> map = gitLog.getCommitLogMap();
			return map.get(commitId);
    	}
    	return Collections.emptyList();
    }

    public void populateFileSets(String commitId, Set<String> filePathsBefore, Set<String> filePathsCurrent, Map<String, String> renamedFilesHint) throws Exception {
    	File rootFolder = new File(REPOS);
    	String repoName = cloneURL.substring(cloneURL.lastIndexOf('/') + 1, cloneURL.lastIndexOf('.'));
		String jsonFilePath = repoName + "-" + commitId + ".json";
		File jsonFile = new File(rootFolder, jsonFilePath);
		if(jsonFile.exists()) {
			final ObjectMapper mapper = new ObjectMapper();
			ChangedFileInfo changedFileInfo = mapper.readValue(jsonFile, ChangedFileInfo.class);
			filePathsBefore.addAll(changedFileInfo.getFilesBefore());
			filePathsCurrent.addAll(changedFileInfo.getFilesCurrent());
			renamedFilesHint.putAll(changedFileInfo.getRenamedFilesHint());
		}
    }

    public CommitModel getLightCommitModel(String commitId, String currentMethodFilePath) throws Exception {
    	File rootFolder = new File(REPOS);
    	final String systemFileSeparator = Matcher.quoteReplacement(File.separator);
    	String repoName = cloneURL.substring(cloneURL.lastIndexOf('/') + 1, cloneURL.lastIndexOf('.'));
		String jsonFilePath = repoName + "-" + commitId + ".json";
		File jsonFile = new File(rootFolder, jsonFilePath);
		if(jsonFile.exists()) {
			final ObjectMapper mapper = new ObjectMapper();
			ChangedFileInfo changedFileInfo = mapper.readValue(jsonFile, ChangedFileInfo.class);
			String parentCommitId = changedFileInfo.getParentCommitId();
			Set<String> repositoryDirectoriesBefore = ConcurrentHashMap.newKeySet();
			Set<String> repositoryDirectoriesCurrent = ConcurrentHashMap.newKeySet();
			Map<String, String> fileContentsBefore = new ConcurrentHashMap<String, String>();
			Map<String, String> fileContentsCurrent = new ConcurrentHashMap<String, String>();
			Map<String, String> renamedFilesHint = new ConcurrentHashMap<String, String>();
			repositoryDirectoriesBefore.addAll(changedFileInfo.getRepositoryDirectoriesBefore());
			repositoryDirectoriesCurrent.addAll(changedFileInfo.getRepositoryDirectoriesCurrent());
			renamedFilesHint.putAll(changedFileInfo.getRenamedFilesHint());
			for(String filePathBefore : changedFileInfo.getFilesBefore()) {
				if(filePathBefore.equals(currentMethodFilePath)) {
					String fullPath = rootFolder + File.separator + repoName + "-" + parentCommitId + File.separator + filePathBefore.replaceAll("/", systemFileSeparator);
					String contents = FileUtils.readFileToString(new File(fullPath));
					fileContentsBefore.put(filePathBefore, contents);
				}
			}
			for(String filePathCurrent : changedFileInfo.getFilesCurrent()) {
				if(filePathCurrent.equals(currentMethodFilePath)) {
					String fullPath = rootFolder + File.separator + repoName + "-" + commitId + File.separator + filePathCurrent.replaceAll("/", systemFileSeparator);
					String contents = FileUtils.readFileToString(new File(fullPath));
					fileContentsCurrent.put(filePathCurrent, contents);
				}
			}
			return new CommitModel(parentCommitId, repositoryDirectoriesBefore, fileContentsBefore, fileContentsBefore, repositoryDirectoriesCurrent, fileContentsCurrent, fileContentsCurrent, renamedFilesHint, Collections.emptyList(),
					changedFileInfo.getCommitTime(), changedFileInfo.getAuthoredTime(), changedFileInfo.getCommitAuthorName());
		}
		return null;
    }

    public CommitModel getCommitModel(String commitId) throws Exception {
    	Set<String> repositoryDirectoriesBefore = ConcurrentHashMap.newKeySet();
		Set<String> repositoryDirectoriesCurrent = ConcurrentHashMap.newKeySet();
		Map<String, String> fileContentsBefore = new ConcurrentHashMap<String, String>();
		Map<String, String> fileContentsCurrent = new ConcurrentHashMap<String, String>();
		Map<String, String> renamedFilesHint = new ConcurrentHashMap<String, String>();
		GitHistoryRefactoringMinerImpl miner = new GitHistoryRefactoringMinerImpl();
		ChangedFileInfo info = miner.populateWithGitHubAPIAndSaveFiles(cloneURL, commitId, 
				fileContentsBefore, fileContentsCurrent, renamedFilesHint, repositoryDirectoriesBefore, repositoryDirectoriesCurrent, new File(REPOS));

        Map<String, String> fileContentsBeforeTrimmed = new HashMap<>(fileContentsBefore);
        Map<String, String> fileContentsCurrentTrimmed = new HashMap<>(fileContentsCurrent);
        List<MoveSourceFolderRefactoring> moveSourceFolderRefactorings = GitHistoryRefactoringMinerImpl.processIdenticalFiles(fileContentsBeforeTrimmed, fileContentsCurrentTrimmed, renamedFilesHint, false);

        return new CommitModel(info.getParentCommitId(), repositoryDirectoriesBefore, fileContentsBefore, fileContentsBeforeTrimmed, repositoryDirectoriesCurrent, fileContentsCurrent, fileContentsCurrentTrimmed, renamedFilesHint, moveSourceFolderRefactorings,
        		info.getCommitTime(), info.getAuthoredTime(), info.getCommitAuthorName());
    }
}
