package com.vnet.democloud.m3;

import java.util.HashMap;
import java.util.Map;

public class Counter  {
    private int value;
    private final String name;
    public Counter(String name) {
        this.name = name;
        this.value = 0;
    }

    public Counter(String name, int value) {
        this.name = name;
        this.value = value;
    }

    public synchronized int next() {return value++;}
    public synchronized void reset() {value = 0;}

    static public Map<String,Object> map(Counter counter) {
        final Map<String,Object> map = new HashMap<>();
        map.put("name", counter.name);
        map.put("value", counter.value);
        return map;
    }
}