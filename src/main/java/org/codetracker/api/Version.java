package org.codetracker.api;

public interface Version {

  /** @return commit ID */
  String getId();

  /** @return commit time */
  long getTime();

  /** @return name of the committer */
  String getAuthorName();
}
