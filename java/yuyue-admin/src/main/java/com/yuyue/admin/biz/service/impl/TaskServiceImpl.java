package com.yuyue.admin.biz.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.yuyue.admin.biz.service.TaskService;
import com.yuyue.admin.dal.dao.TaskDao;
import com.yuyue.admin.dal.po.TaskPO;
import org.springframework.stereotype.Component;


import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 *
 * @author bowen
 * @date 2022-03-16
 */
@Component
public class TaskServiceImpl implements TaskService {

    @Resource
    private TaskDao taskDao;

//    @Override
//    public void save(List<com.yuyue.admin.dal.po.TaskPO> taskPOS) {
//        for(com.yuyue.admin.dal.po.TaskPO taskPO : taskPOS) {
//            taskDao.insert(taskPO);
//        }
//    }

    @Override
    public int save(com.yuyue.admin.dal.po.TaskPO taskPO){
        return taskDao.insert(taskPO);
    }


    @Override
    public int update(String uid,String chartIds,String result,int status){
        //TaskPO taskPO=new TaskPO();
        UpdateWrapper<TaskPO> updateWrapper = new UpdateWrapper<>();
        if(null==result||result.length()==0)
            updateWrapper.eq("uid", uid).set("status", status).set("chart_ids",chartIds).set("update_time", LocalDateTime.now());
        else
            updateWrapper.eq("uid", uid).set("result", result).set("status", status).set("chart_ids",chartIds).set("update_time", LocalDateTime.now());;
        return taskDao.update(null,updateWrapper);
    }
    @Override
    public int updateUid(String uid){
        UpdateWrapper<TaskPO> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("uid", uid).set("update_time", LocalDateTime.now());;
        return taskDao.update(null,updateWrapper);
    }
    @Override
    public int delete(String uid){
        QueryWrapper<com.yuyue.admin.dal.po.TaskPO> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("uid",uid);
        return taskDao.delete(queryWrapper);
    }
    @Override
    public TaskPO getTaskByUid(String uid){
        QueryWrapper<com.yuyue.admin.dal.po.TaskPO> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("uid",uid);
        return taskDao.selectOne(queryWrapper);
    }
    @Override
    public double getAvgTime(int modelId){
        QueryWrapper<com.yuyue.admin.dal.po.TaskPO> queryWrapper = new QueryWrapper<>();
        queryWrapper.select("AVG(EXTRACT(EPOCH FROM update_time-create_time)) AS est")
                .groupBy("model_id")
                .having("model_id = {0}", modelId);
        //TaskPO taskPO= taskDao.selectOne(queryWrapper);
        List<Map<String, Object>> mapList = taskDao.selectMaps(queryWrapper);
        if(mapList==null||mapList.size()<1)
            return -1d;
        Map<String, Object> map=mapList.get(0);
        Object est=map.get("est");
        return (double) est;
    }
}
