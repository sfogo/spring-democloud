package com.vnet.democloud.m3;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@SpringBootApplication
@EnableDiscoveryClient
@RestController
public class Application {

    @Value("${demo.message}")
    String message = "Welcome!";

    @Value("${spring.cloud.config.uri}")
    String configServer;

    @Autowired
    Counters service;

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @RequestMapping(value = "/counters/{id}",
            method = RequestMethod.POST,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String,Object> next(@PathVariable String id) {
        return service.next(id);
    }

    @RequestMapping(value = "/counters/{id}",
            method = RequestMethod.DELETE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String,Object> delete(@PathVariable String id) {
        return service.reset(id);
    }

    @RequestMapping(value = "/counters/{id}",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String,Object> getCounter(@PathVariable String id) {
        return service.get(id);
    }

    @RequestMapping(value = "/counters",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public Collection<Map<String,Object>> getCounters() {
        return service.collect();
    }

    @RequestMapping(value = "/", method = RequestMethod.GET)
    @ResponseBody
    public Map<String,Object> home() {
        final Map<String,Object> map = new HashMap<>();
        map.put("message", message);
        map.put("config.uri", configServer);
        return map;
    }

}
