package org.codetracker.blame.benchmark.impl.record;

import org.codetracker.blame.benchmark.EBlamer;
import org.codetracker.blame.model.IBlameTool;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/* Created by pourya on 2024-07-15*/
public class BenchmarkRecordManager<K, V>{
    protected Map<K, Map<IBlameTool, V>> registry;

    public BenchmarkRecordManager() {
        registry = new HashMap<>();
    }
    public void register(IBlameTool blamerFactory, K key, V value) {
        Map<IBlameTool, V> orDefault = registry.getOrDefault(key, new LinkedHashMap<>());
        orDefault.putIfAbsent(blamerFactory, value);
        registry.put(key, orDefault);
    }

    public Map<K, Map<IBlameTool, V>> getRegistry() {
        return registry;
    }
}
