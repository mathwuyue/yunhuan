package com.yuyue.admin.biz.service;

import com.yuyue.admin.dal.po.ChartPO;
import com.yuyue.admin.dal.po.TaskPO;
import org.springframework.validation.annotation.Validated;


/**
 *
 * @author bowen
 * @date 2022-03-16
 */
@Validated
public interface ChartService {
    ChartPO getChart(String chartId);
}
