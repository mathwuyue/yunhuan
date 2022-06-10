package com.yuyue.admin.biz.service;

import com.yuyue.admin.dal.po.ModelPO;
import org.springframework.validation.annotation.Validated;


/**
 *
 * @author bowen
 * @date 2022-03-18
 */
@Validated
public interface ModelService {
    ModelPO getModel(int modelId);
    int update(int modelId,double est);
}
