package org.refactoringrefiner.test;

import gr.uom.java.xmi.diff.UMLModelDiff;
import org.eclipse.jgit.lib.Repository;
import org.refactoringminer.api.GitService;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringHandler;
import org.refactoringminer.rm1.GitHistoryRefactoringMinerImpl;
import org.refactoringminer.util.GitServiceImpl;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class MinerRun {
    private final static String folderToClone = "H:\\Projects\\";

    public static void main(String[] args) {
        new MinerRun().run();
    }

    public void run() {
        String repositoryWebURL = "https://github.com/ReactiveX/RxJava.git";
        String commitId = "7741c59fd05196d40b5a6214314842552290c156";
        GitService gitService = new GitServiceImpl();
        GitHistoryRefactoringMinerImpl miner = new GitHistoryRefactoringMinerImpl();
        String directoryName = folderToClone + repositoryWebURL.replace("https://github.com/", "").replace(".git", "").replace("/", "\\");
        try (Repository repository = gitService.cloneIfNotExists(directoryName, repositoryWebURL)) {
            List<Refactoring> byRepository = new ArrayList<>();
            List<Refactoring> byGitHubApi = new ArrayList<>();
            miner.detectAtCommit(repository, commitId, new RefactoringHandler() {
                @Override
                public void handle(String commitId, List<Refactoring> refactorings) {
                    byRepository.addAll(refactorings);
                }

                @Override
                public void handleExtraInfo(String commitId, UMLModelDiff umlModelDiff) {
                    System.out.println(commitId);
                }
            });
            miner.detectAtCommit(repositoryWebURL, commitId, new RefactoringHandler() {
                @Override
                public void handle(String commitId, List<Refactoring> refactorings) {
                    byGitHubApi.addAll(refactorings);
                }
            }, 1000);

            List<String> byRepo = byRepository.stream().map(Refactoring::toString).collect(Collectors.toList());
            List<String> byApi = byGitHubApi.stream().map(Refactoring::toString).collect(Collectors.toList());
            List<String> minus1 = RefactoringResult.minus(byRepo, byApi);
            List<String> minus2 = RefactoringResult.minus(byApi, byRepo);
            System.out.println("t");
        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }

}
