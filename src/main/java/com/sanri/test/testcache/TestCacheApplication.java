package com.sanri.test.testcache;

import org.apache.commons.collections.CollectionUtils;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class TestCacheApplication {

    public static void main(String[] args) {
        SpringApplication.run(TestCacheApplication.class, args);
    }

}
