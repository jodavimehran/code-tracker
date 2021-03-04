package org.refactoringrefiner;

import com.google.common.graph.EndpointPair;
import com.google.common.graph.ImmutableValueGraph;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.LogCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.filter.RevFilter;
import org.refactoringminer.api.GitHistoryRefactoringMiner;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.rm1.GitHistoryRefactoringMinerImpl;
import org.refactoringrefiner.api.*;
import org.refactoringrefiner.edge.EdgeImpl;
import org.refactoringrefiner.element.Method;
import org.refactoringrefiner.util.GitHubRepository;
import org.refactoringrefiner.util.GitRepository;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 *
 */
public class RefactoringRefinerImpl implements RefactoringRefiner {

    private final static int TIMEOUT = 120;

    public static RefactoringRefiner factory() {
        return new RefactoringRefinerImpl();
    }

    private static List<CodeElement> findMostLeftSide(CodeElement codeElement, ImmutableValueGraph<CodeElement, Edge> graph, Set<EndpointPair<CodeElement>> analyzed) {
        List<CodeElement> codeElementList = new ArrayList<>();
        Set<CodeElement> predecessors = graph.predecessors(codeElement);
        if (predecessors.isEmpty() || codeElement.isAdded()) {
            codeElementList.add(codeElement);
            return codeElementList;
        }
        for (CodeElement leftElement : predecessors) {
            EndpointPair<CodeElement> endpointPair = EndpointPair.ordered(leftElement, codeElement);
            if (!analyzed.contains(endpointPair)) {
                analyzed.add(endpointPair);
                EdgeImpl edge = (EdgeImpl) graph.edgeValue(leftElement, codeElement).get();
                codeElementList.addAll(findMostLeftSide(leftElement, graph, analyzed));
            }
        }
        return codeElementList;
    }


    @Override
    public Result analyseAllCommits(Repository repository, String branch) {
        try {
            GitHistoryRefactoringMiner miner = new GitHistoryRefactoringMinerImpl();
            RefactoringHandlerImpl refactoringHandler = new RefactoringHandlerImpl(new GitRepository(repository));
            miner.detectAll(repository, branch, refactoringHandler);
            return new ResultImpl(refactoringHandler);
        } catch (Exception ex) {
            return null;
        }
    }

    @Override
    public Result analyseBetweenTags(Repository repository, String startTag, String endTag) throws Exception {
        GitHistoryRefactoringMiner miner = new GitHistoryRefactoringMinerImpl();
        RefactoringHandlerImpl refactoringHandler = new RefactoringHandlerImpl(new GitRepository(repository));

        long startTime = System.nanoTime();
        miner.detectBetweenTags(repository, startTag, endTag, refactoringHandler);
        long refactoringMinerEndTime = System.nanoTime();

        ResultImpl result = new ResultImpl(refactoringHandler);
        long refactoringRefinerEndTime = System.nanoTime();
        result.setRefactoringMinerProcessTime((refactoringMinerEndTime - startTime) / 1000000);
        result.setRefactoringRefinerProcessTime((refactoringRefinerEndTime - startTime) / 1000000);
        return result;
    }

    @Override
    public Result analyseBetweenCommits(Repository repository, String startCommitId, String endCommitId) {
        try {
            GitHistoryRefactoringMiner miner = new GitHistoryRefactoringMinerImpl();
            RefactoringHandlerImpl refactoringHandler = new RefactoringHandlerImpl(new GitRepository(repository));
            miner.detectBetweenCommits(repository, startCommitId, endCommitId, refactoringHandler);
            return new ResultImpl(refactoringHandler);
        } catch (Exception ex) {
            return null;
        }
    }

    @Override
    public Result analyseCommit(Repository repository, String commitId) {
        try {
            GitHistoryRefactoringMiner miner = new GitHistoryRefactoringMinerImpl();
            RefactoringHandlerImpl refactoringHandler = new RefactoringHandlerImpl(new GitRepository(repository));
            miner.detectAtCommit(repository, commitId, refactoringHandler, TIMEOUT);
            return new ResultImpl(refactoringHandler);
        } catch (Exception ex) {
            return null;
        }
    }

    @Override
    public Result analyseCommits(Repository repository, List<String> commitList) {
        try {
            GitHistoryRefactoringMiner miner = new GitHistoryRefactoringMinerImpl();
            RefactoringHandlerImpl refactoringHandler = new RefactoringHandlerImpl(new GitRepository(repository));
            for (String commitId : commitList)
                miner.detectAtCommit(repository, commitId, refactoringHandler, TIMEOUT);
            return new ResultImpl(refactoringHandler);
        } catch (Exception ex) {
            return null;
        }
    }

    @Override
    public Result analyseFileCommits(Repository repository, String filePath) {
        try (Git git = new Git(repository)) {
            git.pull().call();
            List<String> commitList = getCommits(filePath, git);
            GitHistoryRefactoringMiner miner = new GitHistoryRefactoringMinerImpl();
            RefactoringHandlerImpl refactoringHandler = new RefactoringHandlerImpl(new GitRepository(repository));
            for (String commitId : commitList)
                miner.detectAtCommit(repository, commitId, refactoringHandler, TIMEOUT);
            return new ResultImpl(refactoringHandler);
        } catch (Exception ex) {
            return null;
        }
    }

    public Result analyseCommits(String gitURL, List<String> commitList) throws Exception {
        GitHistoryRefactoringMinerImpl miner = new GitHistoryRefactoringMinerImpl();
        RefactoringHandlerImpl refactoringHandler = new RefactoringHandlerImpl(new GitHubRepository(miner.getGitHubRepository(gitURL)));
        for (String commitId : commitList)
            miner.detectAtCommit(gitURL, commitId, refactoringHandler, TIMEOUT);
        return new ResultImpl(refactoringHandler);
    }

    public List<Refactoring> detectAtCommits(String gitURL, List<String> commitIdList) throws Exception {
        Result result = analyseCommits(gitURL, commitIdList);
        return result.getAggregatedRefactorings();
    }

    public List<Refactoring> detectAtCommit(Repository repository, String startCommitId, String endCommitId) throws Exception {
        Result result = analyseBetweenCommits(repository, startCommitId, endCommitId);
        return result.getAggregatedRefactorings();
    }

    public History findHistory(Repository repository, String startCommitId, String filePath, String methodKey) {
        try (Git git = new Git(repository)) {
            git.pull().call();
            GitHistoryRefactoringMiner miner = new GitHistoryRefactoringMinerImpl();
            RefactoringHandlerImpl refactoringHandler = new RefactoringHandlerImpl(new GitRepository(repository));
            refactoringHandler.setTrackMethods(true);
            boolean flag = true;
            Method start = null;
            HashSet<String> analysedCommits = new HashSet<>();
            int numberOfCommit = 0;
            while (flag) {
                List<String> commits = getCommits(repository, startCommitId, filePath, git);

                if (analysedCommits.containsAll(commits))
                    break;

                refactoringHandler.getFiles().add(filePath);
                for (String commitId : commits) {
                    if (analysedCommits.contains(commitId))
                        continue;
                    System.out.println("processing " + commitId);
                    analysedCommits.add(commitId);
                    numberOfCommit++;
                    miner.detectAtCommit(repository, commitId, refactoringHandler);
                    String key = String.format("%s>%s", filePath, methodKey);
                    if (refactoringHandler.getMethodElements().containsKey(key)) {
                        Method method = refactoringHandler.getMethodElements().get(key);
                        ImmutableValueGraph<CodeElement, Edge> methodChangeHistoryGraph = refactoringHandler.getMethodChangeHistoryGraph();

                        List<CodeElement> mostLeftSide = findMostLeftSide(method, methodChangeHistoryGraph, new HashSet<>());
                        if (!mostLeftSide.isEmpty()) {
                            Method mostLeftSideMethod = (Method) mostLeftSide.get(0);
                            filePath = mostLeftSideMethod.getInfo().getLocationInfo().getFilePath();
                            methodKey = mostLeftSideMethod.getInfo().getKey();
                            startCommitId = mostLeftSideMethod.getVersion().getId();
                            if (mostLeftSideMethod.isAdded()) {
                                flag = false;
                                start = mostLeftSideMethod;
                            }
                            break;
                        }
                    }
                }
            }
            HistoryImpl historyImpl = new HistoryImpl(numberOfCommit);
            if (start != null) {
                historyImpl.findHistory(start, refactoringHandler.getMethodChangeHistoryGraph());
            }
            return historyImpl;
        } catch (Exception exception) {
            return null;
        }
    }

    private List<String> getCommits(String filePath, Git git) throws GitAPIException {
        LogCommand logCommandFile = git.log().addPath(filePath).setRevFilter(RevFilter.NO_MERGES);
        Iterable<RevCommit> fileRevisions = logCommandFile.call();
        List<String> commits = StreamSupport.stream(fileRevisions.spliterator(), false).map(revCommit -> revCommit.getId().getName()).collect(Collectors.toList());
        return commits;
    }

    private List<String> getCommits(Repository repository, String startCommitId, String filePath, Git git) throws IOException, GitAPIException {
        LogCommand logCommandFile = git.log().add(repository.resolve(startCommitId)).addPath(filePath).setRevFilter(RevFilter.NO_MERGES);
        Iterable<RevCommit> fileRevisions = logCommandFile.call();
        List<String> commits = StreamSupport.stream(fileRevisions.spliterator(), false).map(revCommit -> revCommit.getId().getName()).collect(Collectors.toList());
        return commits;
    }
}
