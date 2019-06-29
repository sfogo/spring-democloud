package com.vnet.democloud.m1;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.circuitbreaker.EnableCircuitBreaker;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@SpringBootApplication
@EnableDiscoveryClient
@EnableCircuitBreaker
@EnableFeignClients
@RestController
public class Application {

    @Value("${demo.message}")
    String demoMessage = "Welcome!";

    @Value("${demo.resource}")
    String demoResource;

    @Value("${spring.cloud.config.uri}")
    String configServer;

    @Value("${spring.application.name}")
    String appName;

    @Autowired
    private Items itemService;

    @Autowired
    private CounterService counterService;

    private final UUID uuid = new UUID(System.currentTimeMillis(),getClass().hashCode());
    private String getInstanceId() {return appName + "-" + uuid;}

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @RequestMapping(value = "/items/{id}", method = RequestMethod.GET)
    @ResponseBody
    public Map<String,Object> item(@PathVariable String id) {
        // This is feigned
        final Map counter = counterService.nextValue(appName);

        // This is Hystrix wrapped
        final Map<String,Object> item = itemService.getItem(demoResource +"/"+id);

        item.put("counter", counter);
        item.put("instance-counter", counterService.nextValue(getInstanceId()));
        item.put("message", demoMessage);
        return item;
    }

    @RequestMapping(value = "/", method = RequestMethod.GET)
    @ResponseBody
    public Map<String,Object> home() {
        final Map<String,Object> map = new HashMap<>();
        map.put("message", demoMessage);
        map.put("config.uri", configServer);
        map.put("counter", counterService.getValue(appName));
        map.put("instance-counter", counterService.getValue(getInstanceId()));
        return map;
    }

    @FeignClient("m3-service")
    interface CounterService {

        @RequestMapping(value = "/counters/{key}",
                method = RequestMethod.POST,
                produces = MediaType.APPLICATION_JSON_VALUE)
        Map<String,Object> nextValue(@PathVariable("key") String key);

        @RequestMapping(value = "/counters/{key}",
                method = RequestMethod.GET,
                produces = MediaType.APPLICATION_JSON_VALUE)
        Map<String,Object> getValue(@PathVariable("key") String key);
    }
}
