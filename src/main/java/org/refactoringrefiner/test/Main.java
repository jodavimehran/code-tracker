package org.refactoringrefiner.test;

import org.eclipse.jgit.lib.Repository;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringHandler;
import org.refactoringminer.rm1.GitHistoryRefactoringMinerImpl;
import org.refactoringrefiner.RefactoringRefinerImpl;

import java.io.IOException;
import java.util.List;

public class Main {
    private final static String folderToClone = "H:\\Projects\\";
    private final static String repositoryWebURL = "https://github.com/jodavimehran/refactoring-samples.git";

    public static void main(String[] args) throws IOException {



//        List<String> commits = Arrays.asList("b401661c32c1114ef0e621a3ee1ccb8daf3fecf3",
//                "f9923b9632923a99eeded114cc541decc7830834",
//                "badffa764a2671b9471891273a00237a2817ad89",
//                "207247e2324bafe8830681fabcf4291553e5fc4f",
//                "7c29d2bfb2878379768e4db172688de5309ca680");
//        String beforeCommitId = "b401661c32c1114ef0e621a3ee1ccb8daf3fecf3";
//        String currentCommitId = "7c29d2bfb2878379768e4db172688de5309ca680";
//
//        GitService gitService = new GitServiceImpl();
//        GitHistoryRefactoringMinerImpl miner = new GitHistoryRefactoringMinerImpl();
//        String directoryName = folderToClone + repositoryWebURL.replace("https://github.com/", "").replace(".git", "").replace("/", "\\");
////        findRefactoringByGitHubApi(currentCommitId, miner);
//        try (Repository repository = gitService.cloneIfNotExists(directoryName, repositoryWebURL)) {
//            findRefactoringWithRepository(beforeCommitId, currentCommitId, miner, repository);
////            findRefactoringWithRefRefiner(beforeCommitId, currentCommitId, repository);
//            findRefactoringWithRefRefiner(repositoryWebURL, commits);
//        } catch (Exception ex) {
//            ex.printStackTrace();
//        }
    }

    private static void findRefactoringWithRefRefiner(String beforeCommitId, String currentCommitId, Repository repository) throws Exception {
        System.out.println("refactoring-refiner says :");
        List<Refactoring>  refactorings = ((RefactoringRefinerImpl) RefactoringRefinerImpl.factory()).detectAtCommit(repository, beforeCommitId, currentCommitId);
        for (Refactoring ref : refactorings) {
            System.out.println(ref.toString());
        }
    }

    private static void findRefactoringWithRefRefiner(String repositoryWebURL, List<String> commits) throws Exception {
        System.out.println("refactoring-refiner says :");
        List<Refactoring> refactorings = ((RefactoringRefinerImpl) RefactoringRefinerImpl.factory()).detectAtCommits(repositoryWebURL, commits);
        for (Refactoring ref : refactorings) {
            System.out.println(ref.toString());
        }
    }


    private static void findRefactoringWithRepository(String beforeCommitId, String currentCommitId, GitHistoryRefactoringMinerImpl miner, Repository repository) throws Exception {
        System.out.println("RefactoringMiner says :");
        miner.detectAtCommit(repository, beforeCommitId, currentCommitId, new RefactoringHandler() {
            @Override
            public void handle(String commitId, List<Refactoring> refactorings) {
                System.out.println("Refactorings at " + commitId);
                for (Refactoring ref : refactorings) {
                    System.out.println(ref.toString());
                }
            }
        });
    }

    private static void findRefactoringByGitHubApi(String currentCommitId, GitHistoryRefactoringMinerImpl miner) {
        System.out.println("RefactoringMiner says :");
        miner.detectAtCommit(repositoryWebURL, currentCommitId, new RefactoringHandler() {
            @Override
            public void handle(String commitId, List<Refactoring> refactorings) {
                System.out.println("Refactorings at " + commitId);
                for (Refactoring ref : refactorings) {
                    System.out.println(ref.toString());
                }
            }
        }, 1000);
    }
}
