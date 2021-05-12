package com.sanri.test.testcache.service;

import com.sanri.test.testcache.po.Vehicle;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Component
public class DataProvide implements InitializingBean {
    private List<Vehicle> vehicles = new ArrayList<>();

    @Override
    public void afterPropertiesSet() throws Exception {
        vehicles.add(new Vehicle(1L, "123456", "sanri", 1, 1D, new Date()));
        for (int i = 0; i < 100; i++) {
            long vehicleId = RandomUtils.nextLong(10, 10000);
            String vin = RandomUtil.vin();
            String username = RandomUtil.username();
            int companId = RandomUtils.nextInt(10, 200);
            double score = RandomUtils.nextDouble(0, 5);
            Date createDate = RandomUtil.date();
            Vehicle vehicle = new Vehicle(vehicleId, vin, username, companId, score, createDate);
            vehicles.add(vehicle);
        }
    }

    public List<Vehicle> selectAll(){
        return vehicles;
    }

    public List<Vehicle> filter(Predicate<Vehicle> predicate){
        return vehicles.stream().filter(predicate).collect(Collectors.toList());
    }
}
