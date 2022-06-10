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
 * @date 2022-03-10
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("bi_task")
public class TaskPO {
//    @TableField("id")
//    private String id;
    @TableField("uid")
    private String uid;
    @TableField("model_id")
    private int modelId;
    @TableField("result")
    private String result;
    @TableField("params")
    private String params;
    @TableField("status")
    private Integer status;//0 created, 1 executing, 2 success, 3 fail, 4 cancel
    @TableField("chart_ids")
    private String chartIds;//0
    @TableField("create_time")
    private LocalDateTime createTime;
    @TableField("update_time")
    private LocalDateTime updateTime;
}
