package org.codetracker.blame.benchmark;

import org.codetracker.blame.model.IBlame;
import org.codetracker.blame.impl.CliGitBlameCustomizable;
import org.codetracker.blame.impl.CodeTrackerBlame;
import org.codetracker.blame.impl.FileTrackerBlame;
import org.codetracker.blame.impl.JGitBlame;
import org.eclipse.jgit.diff.DiffAlgorithm;

public enum BlamerFactory {
    JGitBlameWithFollow(new JGitBlame()),
    JGitBlameHistogramWithFollow(new JGitBlame(DiffAlgorithm.getAlgorithm(DiffAlgorithm.SupportedAlgorithm.HISTOGRAM))),
    CliGitBlameIgnoringWhiteSpace(new CliGitBlameCustomizable(true)),
    CliGitBlameDefault(new CliGitBlameCustomizable(false)),
    CliGitBlameMoveAware(new CliGitBlameCustomizable(false, new String[]{"-m"})),
    CliGitBlameMoveAwareIgnoringWhiteSpace(new CliGitBlameCustomizable(true, new String[]{"-m"})),
    CliGitBlameCopyAware(new CliGitBlameCustomizable(false, new String[]{"-c"})),
    CliGitBlameCopyAwareIgnoringWhiteSpace(new CliGitBlameCustomizable(true, new String[]{"-c"})),
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
