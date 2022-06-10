package com.yuyue.admin.web.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * config
 *
 * @author bowen
 * @date 2022-03-18
 */
@Data
@ApiModel
public class MetricsVO {

    @NotBlank
    @ApiModelProperty(example = "abc234dfcjvf")
    private String uid;

    @ApiModelProperty(example = "[a,b]")
    private String[] x;//String[] x;

    @ApiModelProperty(example = "[c,d]")
    private String[] y;
    @ApiModelProperty(example = "[a,b]")
    private ChartArray[] dataX;//String[] x;

    @ApiModelProperty(example = "[c,d]")
    private ChartArray[] dataY;
    @ApiModelProperty(example = "3")
    private int modelId;
    @ApiModelProperty(example = "limit 2")
    private String conditon;
    @ApiModelProperty(example = "3")
    private int x_count;
    @ApiModelProperty(example = "2")
    private int y_count;
}

