package com.yuyue.admin.dal.po;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * chart
 *
 * @author bowen
 * @date 2022-03-18
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("bi_chart")
public class ChartPO {
    @TableField("chart_example")
    private String chartExample;
    @TableField("chart_name")
    private String chartName;
    @TableField("chart_id")
    private Integer chartId;//0
    @TableField("create_time")
    private LocalDateTime createTime;
}
