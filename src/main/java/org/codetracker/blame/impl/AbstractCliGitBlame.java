package org.codetracker.blame.impl;

/* Created by pourya on 2024-09-27*/

import org.codetracker.blame.model.IBlame;
import org.codetracker.blame.model.LineBlameResult;
import org.eclipse.jgit.lib.Repository;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/* Created by pourya on 2024-08-22*/
public abstract class AbstractCliGitBlame implements IBlame {
    public AbstractCliGitBlame() {

    }

    @Override
    public List<LineBlameResult> blameFile(Repository repository, String commitId, String filePath) throws Exception {
        List<LineBlameResult> blameResults = new ArrayList<>();
        Process process = null;

        try {
            String[] command = getCommand(commitId, filePath);
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.directory(repository.getDirectory());

            // Start the process and get the input stream
            process = processBuilder.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            String line;
            int lineNumber = 1;
            while ((line = reader.readLine()) != null) {
                blameResults.add(getLineBlameResult(line, filePath, lineNumber));
                lineNumber++;
            }
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new Exception("Error executing git blame command");
            }
        } finally {
            if (process != null) {
                process.destroy();
            }
        }

        return blameResults;
    }

    public String[] getCommand(String commitId, String filePath) {
        String[] pre_command = new String[]{"git", "blame", "-n"};
        String[] post_command = new String[] {"--follow", commitId, "--", filePath};
        String[] addons = getAdditionalCommandOptions();
        //Merge pre command, addons, and post command
        String[] result = new String[pre_command.length + addons.length + post_command.length];
        System.arraycopy(pre_command, 0, result, 0, pre_command.length);
        System.arraycopy(addons, 0, result, pre_command.length, addons.length);
        System.arraycopy(post_command, 0, result, pre_command.length + addons.length, post_command.length);
        return result;
    }

    public abstract String[] getAdditionalCommandOptions();

    private static LineBlameResult getLineBlameResult(String line, String filePath, int lineNumber) {
        if (line.charAt(0) == '^') {
            line = line.substring(1);
        }
        // Extract commitId, committer, commitDate, filePath, and beforeFilePath from the blame line
        String[] parts = line.split("\\s+", 3);
        String blameCommitId = parts[0].trim();  // Extract the commit ID
        String prevFilePath = parts[1].trim();

        // Find the index of the first space to separate the number
        int firstSpaceIndex = parts[2].indexOf(" ");
        int resultLineNumber = -1;
        try {
            resultLineNumber = Integer.parseInt(parts[2].substring(0, firstSpaceIndex));
            parts[2] = parts[2].substring(firstSpaceIndex + 1);
        }
        catch (NumberFormatException e) {
//            System.out.println("Error parsing line number: " + parts[2].substring(0, firstSpaceIndex));
            parts[2] = parts[2].substring(firstSpaceIndex + 1);
        }


        String[] meta = parts[2].split("\\s{2,}"); // Split on at least 2 spaces
        String commiter = "";
        long commitTime = 0;

        // Regex to capture the timestamp
        Pattern pattern = Pattern.compile("(.*?)(\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2} [+-]\\d{4})");
        Matcher matcher = pattern.matcher(parts[2]);

        if (matcher.find()) {
            // Extract the committer and timestamp
            commiter = matcher.group(1).trim(); // Everything before the timestamp
            commiter = commiter.substring(1); // Remove the parentheses
            String timestamp = matcher.group(2).trim(); // Timestamp


            // Define the formatter for the timestamp
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss Z");

            // Parse the timestamp string
            OffsetDateTime offsetDateTime = OffsetDateTime.parse(timestamp, formatter);

            // Convert to Instant
            Instant instant = offsetDateTime.toInstant();
            commitTime = instant.toEpochMilli() / 1000;
        }
        return new LineBlameResult(blameCommitId, filePath, prevFilePath, commiter, "", commitTime, resultLineNumber, lineNumber);
    }

    @Override
    public List<LineBlameResult> blameFile(Repository repository, String commitId, String filePath, int fromLine, int toLine) throws Exception {
        // Implement logic to blame a specific range of lines in a file
        // This method is optional and can be implemented based on the requirements
        throw new UnsupportedOperationException("Not implemented");
    }
}
