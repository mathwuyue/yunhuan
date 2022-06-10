package com.yuyue.admin.dal.po;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * model
 *
 * @author bowen
 * @date 2022-03-18
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("bi_model")
public class ModelPO {
    @TableField("model_name")
    private String modelName;
    @TableField("model_id")
    private Integer modelId;//0
    @TableField("estimated_time")
    private Integer est;//-1
    @TableField("create_time")
    private LocalDateTime createTime;
}
