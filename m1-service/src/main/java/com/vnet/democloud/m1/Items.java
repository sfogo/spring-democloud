package com.vnet.democloud.m1;

import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

@Service
public class Items {

    static final private Log logger = LogFactory.getLog(Items.class);

    @Bean
    RestTemplate restTemplate() {
        final RestTemplate t = new RestTemplate();
        t.getMessageConverters().add(new MappingJackson2HttpMessageConverter());
        t.getMessageConverters().add(new StringHttpMessageConverter());
        return t;
    }

    @Autowired
    RestTemplate restTemplate;

    @HystrixCommand(fallbackMethod = "reliable")
    public Map getItem(String uri) {
        logger.info(uri);
        return restTemplate.getForObject(URI.create(uri), Map.class);
    }

    private Map reliable(String item) {
        final Map<String,String> map = new HashMap<>();
        map.put("requested.item", item);
        map.put("message", "This is an Hystrix Command fallback.");
        map.put("requestedAt", String.valueOf(System.currentTimeMillis()));
        map.put("class", getClass().getName());
        map.put("method", Thread.currentThread().getStackTrace()[2].getMethodName());
        return map;
    }
}
