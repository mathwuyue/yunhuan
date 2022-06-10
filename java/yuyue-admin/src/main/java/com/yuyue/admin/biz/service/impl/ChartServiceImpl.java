package com.yuyue.admin.biz.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.yuyue.admin.biz.service.ChartService;
import com.yuyue.admin.dal.dao.ChartDao;
import com.yuyue.admin.dal.po.ChartPO;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 *
 * @author bowen
 * @date 2022-03-18
 */
@Component
public class ChartServiceImpl implements ChartService {

    @Resource
    private ChartDao chartDao;

    @Override
    public ChartPO getChart(String chartId){
        QueryWrapper<ChartPO> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("chart_id",chartId);
        return chartDao.selectOne(queryWrapper);
    }
}
