package org.refactoringrefiner.api;

import org.eclipse.jgit.lib.Repository;

import java.util.List;

/**
 * The core interface of Refactoring Refiner
 */
public interface RefactoringRefiner {

    /**
     * Analyse all commits in the specified branch of the specified project
     *
     * @param repository The repository of a project
     * @param branch     Name of the branch
     * @return the result of analysis of refactoring for all commits in the specified branch of the specified project
     */
    Result analyseAllCommits(Repository repository, String branch);

    /**
     * Analyse all commits between two specified tags
     *
     * @param repository The repository of a project
     * @param startTag   Start Tag
     * @param endTag     End tag
     * @return the result
     */
    Result analyseBetweenTags(Repository repository, String startTag, String endTag) throws Exception;

    /**
     * Analyse specified commit
     *
     * @param repository Repository of a project
     * @param commitId   specified commit hash code
     * @return the result
     */
    Result analyseCommit(Repository repository, String commitId);

    /**
     * @param repository Repository of a project
     * @param commitList to analyse commit list
     * @return the result
     */
    Result analyseCommits(Repository repository, List<String> commitList);

    /**
     * @param repository    Repository of a project
     * @param startCommitId Start commit
     * @param endCommitId   End commit
     * @return the result
     */
    Result analyseBetweenCommits(Repository repository, String startCommitId, String endCommitId);


    /**
     * @param repository Repository of a project
     * @param filePath   file path
     * @return result
     */
    Result analyseFileCommits(Repository repository, String filePath);

    /**
     * @param projectDirectory     Repository of a project
     * @param startCommitId  Start commit
     * @param filePath       file path
     * @param codeElementKey code element
     * @return the History of specified element
     */
    History findHistory(String projectDirectory, String repositoryWebURL, String startCommitId, String filePath, String codeElementKey, CodeElementType codeElementType, boolean useApiDiffAsChangeDetector);

    enum CodeElementType{
        CLASS,
        METHOD,
        ATTRIBUTE,
        VARIABLE;
    }

}
