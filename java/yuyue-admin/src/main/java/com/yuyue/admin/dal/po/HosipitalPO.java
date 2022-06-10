package com.yuyue.admin.dal.po;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * task
 *
 * @author bowen
 * @date 2022-05-31
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("hosipital")
public class HosipitalPO {
    @TableField("id")
    private String id;
    @TableField("name")
    private String name;
    @TableField("address")
    private String address;
    @TableField("level")
    private String level;
    @TableField("province")
    private String province;
    @TableField("city")
    private String city;
    @TableField("num_cases")
    private int numCases;//0
    @TableField("is_del")
    private boolean isDel;
}
