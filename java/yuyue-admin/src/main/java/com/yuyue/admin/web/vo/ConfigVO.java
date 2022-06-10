package com.yuyue.admin.web.vo;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotBlank;
import java.time.LocalDateTime;

/**
 * config
 *
 * @author bowen
 * @date 2022-03-18
 */
@Data
@ApiModel
public class ConfigVO {

    @NotBlank
    @ApiModelProperty(example = "[\"Allen\"]")
    private String[] x;

    @ApiModelProperty(example = "[John]")
    private String[] y;

    @ApiModelProperty(example = "limit 10")
    private String condition;

    @ApiModelProperty(example = "3")
    private int modelId;


}
