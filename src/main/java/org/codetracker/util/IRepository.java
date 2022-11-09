package org.codetracker.util;

import org.codetracker.VersionImpl;
import org.codetracker.api.Version;

/**
 * This interface defines all needed functionalities related to a git repository CodeTracker is
 * depending on this interface, so switching between different implementation is much easier
 */
public interface IRepository {
  /**
   * @param commitId the commit id that you wanted to find its parent ID
   * @return parent's commit ID, if a commit has more than one parent, it returns the first one
   */
  String getParentId(String commitId);

  /**
   * @param commitId the commit ID that you wanted to find its time
   * @return commit time
   */
  long getCommitTime(String commitId);

  /**
   * @param commitId the commit ID for which to find the authored time
   * @return authored time
   */
  long getAuthoredTime(String commitId);

  /**
   * @param commitId the commit ID that you wanted to find its author name
   * @return name of the committer
   */
  String getCommitAuthorName(String commitId);

  /**
   * @param commitId commit id that you want to convert to Version
   * @return Version instance of the provided commit ID
   */
  default Version getVersion(String commitId) {
    return new VersionImpl(commitId, getCommitTime(commitId), getAuthoredTime(commitId), getCommitAuthorName(commitId));
  }
}
