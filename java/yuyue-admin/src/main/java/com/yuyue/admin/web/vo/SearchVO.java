package com.yuyue.admin.web.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

/**
 * result
 *
 * @author bowen
 * @date 2022-05-31
 */
@Data
@ApiModel
public class SearchVO {

    @ApiModelProperty(example = "20")
    private int minAge;
    @ApiModelProperty(example = "80")
    private int maxAge;
    @ApiModelProperty(example = "0")
    private int sexCode;
    @ApiModelProperty(example = "1")
    private String cureWay;
    @ApiModelProperty(example = "1")
    private int hospital;
    @ApiModelProperty(example = "1")
    private long current;
    @ApiModelProperty(example = "1")
    private long size;
}
