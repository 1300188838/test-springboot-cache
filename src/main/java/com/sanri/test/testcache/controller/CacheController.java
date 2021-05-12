package com.sanri.test.testcache.controller;

import com.sanri.test.testcache.configs.CacheCustom;
import com.sanri.test.testcache.po.Vehicle;
import com.sanri.test.testcache.service.DataProvide;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Around;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.*;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@CacheConfig(cacheNames = "vehicle")
public class CacheController {

    @Autowired
    private DataProvide dataProvide;

    @GetMapping("/vehicleList")
    @Cacheable
    public List<Vehicle> vehicleList(){
        return dataProvide.selectAll();
    }

    /**
     * 测试存储 key 冲突
     * @return
     */
    @GetMapping("/testConflict")
    @Cacheable
    public String testConflict(){
        return "12";
    }

    @GetMapping("/testMetaKey")
    @Cacheable(key = "#vin+#name")
    public List<Vehicle> testMetaKey(String vin,String name){
        List<Vehicle> vehicles = dataProvide.selectAll();
        return vehicles.stream().filter(vehicle -> vehicle.getVin().equals(vin) && vehicle.getName().contains(name)).collect(Collectors.toList());
    }

    @GetMapping("/vehicleListUpdate")
    @CachePut
    public List<Vehicle> vehicleListUpdate(){
        return dataProvide.selectAll();
    }

    @CacheEvict(allEntries = true)
    public void delete(){

    }

    @Caching(
            evict = {},
            cacheable = {},
            put = {}
    )
    public void cachinging(){

    }
}
