package org.codetracker.blame.impl;

/* Created by pourya on 2024-08-22*/
public class CliGitBlameCustomizable extends AbstractCliGitBlame {
    private final boolean ignore_whitespace;
    private final String[] moreOptions;

    public CliGitBlameCustomizable(boolean ignore_whitespace) {
        this(ignore_whitespace, new String[0]);
    }
    public CliGitBlameCustomizable(boolean ignore_whitespace, String[] moreOptions) {
        this.ignore_whitespace = ignore_whitespace;
        this.moreOptions = moreOptions;
    }
    @Override
    public String[] getAdditionalCommandOptions() {
        //If whitespace is enabled, combine -w with more option (begin with -w)
        if (ignore_whitespace) {
            String[] newCommand = new String[moreOptions.length + 1];
            System.arraycopy(moreOptions, 0, newCommand, 1, moreOptions.length);
            newCommand[0] = "-w";
            return newCommand;
        }
        return moreOptions;
    }
}
