package com.yuyue.admin.web.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

@Data
@ApiModel
public class ChartArray {
    @ApiModelProperty(example = "[3.1,1.2,2.4]")
    private List<Object[]> x;//private List<Number[]> x;
    @ApiModelProperty(example = "[0.0,1.0,0.0]")
    private List<Object[]> y;
    @ApiModelProperty(example = "[\"a\",\"b\",\"c\"]")
    private String[] xaxis;
    @ApiModelProperty(example = "[\"a\",\"b\",\"c\"]")
    private String[] yaxis;
    @ApiModelProperty(example = "3")
    private int chartId;
}
