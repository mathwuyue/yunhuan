package com.yuyue.admin.biz.service;

import com.yuyue.admin.dal.po.HosipitalPO;
import com.yuyue.admin.dal.po.TaskPO;
import org.springframework.validation.annotation.Validated;

import java.util.List;


/**
 *
 * @author bowen
 * @date 2022-05-31
 */
@Validated
public interface HosipitalService {
    List<HosipitalPO> listAll();
}
