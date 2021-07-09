package org.refactoringrefiner.test;

import gr.uom.java.xmi.decomposition.replacement.VariableDeclarationReplacement;
import gr.uom.java.xmi.diff.UMLModelDiff;
import org.eclipse.jgit.lib.Repository;
import org.refactoringminer.api.GitHistoryRefactoringMiner;
import org.refactoringminer.api.GitService;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringHandler;
import org.refactoringminer.rm1.GitHistoryRefactoringMinerImpl;
import org.refactoringminer.util.GitServiceImpl;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;

public class ChangeScopeVariable {
    private final static String FOLDER_TO_CLONE = "H:\\Projects\\";

    public static void main(String[] args) throws Exception {
//        detectAll("https://github.com/ReactiveX/RxJava.git");
//        detectAll("https://github.com/apache/incubator-pinot.git");

        detectAll("https://github.com/PhilJay/MPAndroidChart.git");

    }

    private static void detectAll(String repositoryWebURL) throws Exception {
        String repositoryName = repositoryWebURL.replace("https://github.com/", "").replace(".git", "").replace("/", "\\");
        String projectDirectory = FOLDER_TO_CLONE + repositoryName;
        String resultFile = "E:\\Data\\ChangeVar\\result.csv";
        GitService gitService = new GitServiceImpl();
        GitHistoryRefactoringMiner miner = new GitHistoryRefactoringMinerImpl();

        Repository repo = gitService.cloneIfNotExists(projectDirectory, repositoryWebURL);

        miner.detectAll(repo, "master", new RefactoringHandler() {
//        miner.detectAtCommit(repo, "ea6b0e8e1c3f2567ad5724807a31493b7b88e629", new RefactoringHandler() {
            @Override
            public void handleExtraInfo(String commitId, UMLModelDiff umlModelDiff) {
//                for(VariableDeclarationReplacement variableInfoPair : umlModelDiff.get()){
//                    writeToFile(resultFile, String.format(
//                            "%s;%s;%s;%s;%s;%s;%s;%s;%s;%s" + System.lineSeparator(),
//                            repositoryWebURL,
//                            commitId,
//                            variableInfoPair.getOperation1().getClassName(),
//                            variableInfoPair.getOperation2().getClassName(),
//                            variableInfoPair.getOperation1().toString(),
//                            variableInfoPair.getOperation2().toString(),
//                            variableInfoPair.getVariableDeclaration1().toString(),
//                            variableInfoPair.getVariableDeclaration2().toString(),
//                            variableInfoPair.getVariableDeclaration1().getScope().getParentSignature(),
//                            variableInfoPair.getVariableDeclaration2().getScope().getParentSignature()
//                            )
//                            , StandardOpenOption.APPEND);
//                }
//                System.out.println(commitId);
            }

            @Override
            public void handle(String commitId, List<Refactoring> refactorings) {
//                int count = 0;
//                for (Refactoring ref : refactorings) {
//                    if (RefactoringType.CHANGE_VARIABLE_SCOPE.equals(ref.getRefactoringType())) {
//                        ChangeVariableScopeRefactoring changeVariableScopeRefactoring = (ChangeVariableScopeRefactoring) ref;
//                        System.out.printf(ref.toString());
//                        //repository, commit_id, class_name_before, operation_before, class_name_after, operation_after, variable, parent_signature_before, parent_signature_after, method_body_similarity_score, method_body_nesting_structure_similarity_score, scope_similarity_score, scope_nesting_structure_similarity_score
////                        writeToFile(resultFile, String.format(
////                                "%s;%s;%s;%s;%s;%s;%s;%s;%s;%.2f;%.2f;%.2f;%.2f;%.2f;%.2f" + System.lineSeparator(),
////                                repositoryWebURL,
////                                commitId,
////                                changeVariableScopeRefactoring.getOperationBefore().getClassName(),
////                                changeVariableScopeRefactoring.getOperationBefore().toString(),
////                                changeVariableScopeRefactoring.getOperationAfter().getClassName(),
////                                changeVariableScopeRefactoring.getOperationAfter().toString(),
////                                changeVariableScopeRefactoring.getOriginalVariable().toString(),
////                                changeVariableScopeRefactoring.getOriginalVariable().getScope().getParentSignature(),
////                                changeVariableScopeRefactoring.getChangedScopeVariable().getScope().getParentSignature(),
////                                changeVariableScopeRefactoring.getSimilarityScoreMethodBody() * 100.0,
////                                changeVariableScopeRefactoring.getSimilarityScoreMethodNestingStructure() * 100.0,
////                                changeVariableScopeRefactoring.getSimilarityScoreScope() * 100.0,
////                                changeVariableScopeRefactoring.getSimilarityScoreScopeNestingStructure() * 100.0,
////                                changeVariableScopeRefactoring.getSimilarityScoreScopeNestingStructureSameDepth() * 100.0,
////                                changeVariableScopeRefactoring.getSimilarityScoreScopeNestingStructureSameDepthWithDepth() * 100.0
////                                )
////                                , StandardOpenOption.APPEND);
//                    }else {
//                        count++;
//                    }
//                }
//                System.out.println(count);
            }
        });
    }

    private static void writeToFile(String pathString, String content, StandardOpenOption standardOpenOption) {
        try {
            Path path = Paths.get(pathString);
            if (!path.toFile().exists())
                Files.createFile(path);
            Files.write(path, content.getBytes(), standardOpenOption);
        } catch (Exception exception) {
            exception.printStackTrace();
        }

    }
}
