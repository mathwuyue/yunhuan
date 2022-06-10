package com.yuyue.admin.web.vo;

import com.alibaba.fastjson.JSONArray;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.util.List;

/**
 * result
 *
 * @author bowen
 * @date 2022-03-18
 */
@Data
@ApiModel
public class ResultVO {

    @ApiModelProperty(example = "200")
    private int code;
    @ApiModelProperty(example = "abcde")
    private String uid;
    @ApiModelProperty(example = "exception")
    private String message;
    @ApiModelProperty(example = "hahah")
    private String info;
    @ApiModelProperty(example = "3.678")
    private double est;
    @ApiModelProperty(example = "{data}")
    private Object result;
    @ApiModelProperty(example = "{chartArray}")
    private List<ChartArray> chartArray;
    @ApiModelProperty(example = "2")
    private int total;
    @ApiModelProperty(example = "2")
    private Integer status;//0 created, 1 executing, 2 success, 3 fail, 4 cancel


}
