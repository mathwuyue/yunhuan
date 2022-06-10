package com.yuyue.admin.biz.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.yuyue.admin.biz.service.ModelService;
import com.yuyue.admin.dal.dao.ModelDao;
import com.yuyue.admin.dal.po.ModelPO;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 *
 * @author bowen
 * @date 2022-03-18
 */
@Component
public class ModelServiceImpl implements ModelService {

    @Resource
    private ModelDao modelDao;

    @Override
    public ModelPO getModel(int modelId){
        QueryWrapper<ModelPO> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("model_id",modelId);
        return modelDao.selectOne(queryWrapper);
    }
    @Override
    public int update(int modelId,double est){
        UpdateWrapper<ModelPO> updateWrapper=new UpdateWrapper<>();
        updateWrapper.eq("model_id",modelId).set("estimated_time",est);
        return modelDao.update(null,updateWrapper);
    }
}
