package com.vnet.democloud.m3;

import org.springframework.stereotype.Service;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class Counters {

    static final private Log logger = LogFactory.getLog(Counters.class);
    static final private CounterPool pool = new CounterPool();

    public Map<String,Object> get(String key) {
        logger.info("get:"+key);
        return Counter.map(pool.get(key));
    }

    public Collection<Map<String,Object>> collect() {
        logger.info("collect");
        return pool.collect();
    }

    public Map<String,Object> next(String key) {
        logger.info("next:"+key);
        final Counter counter = pool.get(key);
        counter.next();
        return Counter.map(counter);
    }

    public Map<String,Object> reset(String key) {
        logger.info("reset:"+key);
        final Counter counter = pool.get(key);
        counter.reset();
        return Counter.map(counter);
    }

    public void resetAll() {
        logger.info("resetAll");
        pool.resetAll();
    }

    // ==================
    // Counter Pool
    // ==================
    static private class CounterPool {
        private final HashMap<String,Counter> counters = new HashMap<>();
        synchronized Counter get(String key) {
            Counter counter = counters.get(key);
            if (counter==null) {
                counter = new Counter(key);
                counters.put(key, counter);
            }
            return counter;
        }
        synchronized Collection<Map<String,Object>> collect() {
            // return counters.keySet().stream().map(counters::get).collect(Collectors.toCollection(LinkedList::new));
            return counters.keySet().stream().map(s -> Counter.map(counters.get(s))).collect(Collectors.toCollection(LinkedList::new));
        }
        synchronized void resetAll() {
            for (String key : counters.keySet()) {
                counters.get(key).reset();
            }
        }
    }
}
