package org.codetracker.blame.benchmark;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

/* Created by pourya on 2024-07-15*/
public class BenchmarkRecordManager<K, V>{
    protected Map<K, EnumMap<BlamerFactory, V>> registry;

    public BenchmarkRecordManager() {
        registry = new HashMap<>();
    }
    public void register(BlamerFactory blamerFactory, K key, V value) {
        EnumMap<BlamerFactory, V> orDefault = registry.getOrDefault(key, new EnumMap<>(BlamerFactory.class));
        orDefault.putIfAbsent(blamerFactory, value);
        registry.put(key, orDefault);
    }

    public Map<K, EnumMap<BlamerFactory, V>> getRegistry() {
        return registry;
    }
}
