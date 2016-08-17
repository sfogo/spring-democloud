package com.vnet.democloud.m2;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.circuitbreaker.EnableCircuitBreaker;
import org.springframework.cloud.netflix.feign.EnableFeignClients;
import org.springframework.cloud.netflix.feign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

import java.util.HashMap;
import java.util.Map;

@SpringBootApplication
@EnableDiscoveryClient
@EnableCircuitBreaker
@EnableFeignClients
@RestController
public class Application {

    @Value("${demo.message}")
    String demoMessage = "Welcome!";

    @Value("${demo.resource}")
    String resource;

    @Value("${spring.cloud.config.uri}")
    String configServer;

    @Value("${spring.application.name}")
    String name;

    @Autowired
    Items itemService;

    @Autowired
    CounterService counterService;

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @RequestMapping(value = "/items/{id}", method = RequestMethod.GET)
    @ResponseBody
    public Map item(@PathVariable String id) {
        // This is feigned
        final Map counter = counterService.nextValue(name);

        // This is Hystrix wrapped
        final Map item = itemService.getItem(resource+"/"+id);

        item.put("counter", counter);
        item.put("message", demoMessage);
        return item;
    }

    @RequestMapping(value = "/", method = RequestMethod.GET)
    @ResponseBody
    public Map home() {
        final Map<String,String> map = new HashMap<>();
        map.put("message", demoMessage);
        map.put("config.uri", configServer);
        return map;
    }

    @FeignClient("m3-service")
    interface CounterService {
        @RequestMapping(value = "/counters/{key}",
                method = RequestMethod.POST,
                produces = MediaType.APPLICATION_JSON_VALUE)
        Map nextValue(@PathVariable("key") String key);
    }
}
