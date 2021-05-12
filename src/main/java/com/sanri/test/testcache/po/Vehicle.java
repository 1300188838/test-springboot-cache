package com.sanri.test.testcache.po;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Vehicle implements Serializable {
    private Long vehicleId;
    //车架号
    private String vin;
    // 车主姓名
    private String name;
    // 所属车租车公司
    private Integer companyId;
    // 个人评分
    private Double score;
    // 购车日期
    private Date createDate;
}
