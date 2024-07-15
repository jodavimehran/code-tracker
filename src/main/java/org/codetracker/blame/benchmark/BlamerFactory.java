package org.codetracker.blame.benchmark;

import org.codetracker.blame.IBlame;
import org.codetracker.blame.impl.CliGitBlame;
import org.codetracker.blame.impl.CodeTrackerBlame;
import org.codetracker.blame.impl.FileTrackerBlame;
import org.codetracker.blame.impl.JGitBlame;

public enum BlamerFactory {

    JGitBlameWithFollow(new JGitBlame()),
    CliGitBlame(new CliGitBlame()),
    CodeTrackerBlame(new CodeTrackerBlame()),
    FileTrackerBlame(new FileTrackerBlame());
    private final IBlame blamer;

    BlamerFactory(IBlame iBlame) {
        this.blamer = iBlame;
    }

    public IBlame getBlamer() {
        return blamer;
    }

    public String getName() {
        return name();
    }
}
