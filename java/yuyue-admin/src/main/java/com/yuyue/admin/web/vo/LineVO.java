package com.yuyue.admin.web.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

/**
 * result
 *
 * @author bowen
 * @date 2022-03-18
 */
@Data
@ApiModel
public class LineVO {

    @ApiModelProperty(example = "abcde")
    private String name;
    @ApiModelProperty(example = "line")
    private String type;
    @ApiModelProperty(example = "total")
    private String stack;
//    @ApiModelProperty(example = "[1,2,4,5]")
//    private Object[] xaxis;
    @ApiModelProperty(example = "[1,3,4,6]")
    private Object[] data;
//    @ApiModelProperty(example = "1")
//    private int chartId;


}
