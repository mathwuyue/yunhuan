package com.yuyue.admin.biz.service;

import com.yuyue.admin.dal.po.TaskPO;
import org.springframework.validation.annotation.Validated;


/**
 *
 * @author bowen
 * @date 2022-03-16
 */
@Validated
public interface TaskService {
    int save(TaskPO taskPO);
    int update(String uid,String chartIds,String result,int status);
    int updateUid(String uid);
    int delete(String uid);
    TaskPO getTaskByUid(String uid);
    double getAvgTime(int modelId);
}
