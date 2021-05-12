package com.sanri.test.testcache.controller;

import com.sanri.test.testcache.configs.CacheCustom;
import com.sanri.test.testcache.po.Vehicle;
import com.sanri.test.testcache.service.DataProvide;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@CacheCustom
@CacheConfig(keyGenerator = "keyGenerator")
@RequestMapping("/customscan")
public class CacheCustomController {

	@Autowired
	private DataProvide dataProvide;

	@GetMapping("/vehicles")
	@Cacheable("vehicles")
	@CacheCustom(threshold = "PT10s")
	public List<Vehicle> vehicles(){
		return dataProvide.selectAll();
	}

	@GetMapping("/testMetaKey")
	@Cacheable(key = "#vin+#name")
	public List<Vehicle> testMetaKey(String vin,String name){
		List<Vehicle> vehicles = dataProvide.selectAll();
		return vehicles.stream().filter(vehicle -> vehicle.getVin().equals(vin) && vehicle.getName().contains(name)).collect(Collectors.toList());
	}
}
