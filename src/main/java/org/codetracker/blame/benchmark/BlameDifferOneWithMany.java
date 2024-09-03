package org.codetracker.blame.benchmark;

import org.codetracker.blame.model.LineBlameResult;
import org.eclipse.jgit.lib.Repository;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

/* Created by pourya on 2024-09-03*/
public class BlameDifferOneWithMany extends BlameDiffer {

    protected final BlamerFactory subject;
    public BlameDifferOneWithMany(EnumSet<BlamerFactory> blamerFactories, BlamerFactory subject) {
        super(blamerFactories);
        this.subject = subject;
    }

    @Override
    protected Map<Integer, EnumMap<BlamerFactory, String>> process(Repository repository, String commitId, String filePath, Map<Integer, EnumMap<BlamerFactory, String>> table) {
        table.entrySet().removeIf(entry -> {
            EnumMap<BlamerFactory, String> factories = entry.getValue();
            String subject_value = factories.get(subject);
            return factories.values().stream().filter(value -> value.equals(subject_value)).count() > 1;
        });
        return table;
    }

    @Override
    protected boolean verify(EnumMap<BlamerFactory, List<LineBlameResult>> results) {
        return true; //TODO:
    }
}
