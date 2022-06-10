package com.yuyue.admin.biz.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.yuyue.admin.biz.service.HosipitalService;
import com.yuyue.admin.dal.dao.HosipitalDao;
import com.yuyue.admin.dal.po.HosipitalPO;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;

/**
 *
 * @author bowen
 * @date 2022-05-31
 */
@Component
public class HosipitalServiceImpl implements HosipitalService {

    @Resource
    private HosipitalDao hosipitalDao;

    @Override
    public List<HosipitalPO> listAll(){
        QueryWrapper<HosipitalPO> queryWrapper = new QueryWrapper<>();
        queryWrapper.select("*");
        return hosipitalDao.selectList(queryWrapper);
    }
}
