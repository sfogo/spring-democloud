package com.vnet.democloud.m1;

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
    String message = "Welcome!";

    @Value("${demo.resource}")
    String resource;

    @Value("${spring.cloud.config.uri}")
    String configServer;

    @Value("${spring.application.name}")
    String name;

    @Autowired
    Items service;

    @Autowired
    M3Service m3Service;

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @RequestMapping(value = "/items/{id}", method = RequestMethod.GET)
    @ResponseBody
    public Map item(@PathVariable String id) {
        // This is feigned
        final Map counter = m3Service.nextValue(name);

        // This is Hystrix wrapped
        final Map item = service.getItem(resource+"/"+id);

        item.put("counter", counter);
        return item;
    }

    @RequestMapping(value = "/", method = RequestMethod.GET)
    @ResponseBody
    public Map home() {
        final Map<String,String> map = new HashMap<>();
        map.put("message", message);
        map.put("config.uri", configServer);
        return map;
    }

    @FeignClient("m3-service")
    interface M3Service {
        @RequestMapping(value = "/counters/{key}",
                method = RequestMethod.POST,
                produces = MediaType.APPLICATION_JSON_VALUE)
        Map nextValue(@PathVariable("key") String key);
    }
}
