package com.vnet.democloud.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.netflix.zuul.EnableZuulProxy;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.beans.factory.annotation.Value;

import java.util.HashMap;
import java.util.Map;

@SpringBootApplication
@EnableDiscoveryClient
@EnableZuulProxy
@RestController
public class Application {

    @Value("${demo.message}")
    private String message = "Welcome!";

    @Value("${spring.cloud.config.uri}")
    private String configServer;

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @RequestMapping(value = "/", method = RequestMethod.GET)
    @ResponseBody
    public Map home() {
        final Map map = new HashMap<>();
        map.put("message", message);
        map.put("config.uri", configServer);
        return map;
    }
}
