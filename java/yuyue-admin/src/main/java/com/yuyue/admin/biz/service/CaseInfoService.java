package com.yuyue.admin.biz.service;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.yuyue.admin.dal.po.CaseInfoPO;
import com.yuyue.admin.web.vo.SearchVO;
import org.apache.ibatis.annotations.Param;
import org.springframework.validation.annotation.Validated;

import java.util.List;
import java.util.Map;


/**
 *
 * @author bowen
 * @date 2022-03-16
 */
@Validated
public interface CaseInfoService extends IService<CaseInfoPO> {
    int updateCaseInfo(CaseInfoPO caseInfoPO);

    JSONObject search(SearchVO searchVO);

    List<CaseInfoPO> getCaseInfos();

    JSONObject fullSearch(String text,long current,long size);

    List<CaseInfoPO> getAll();

    List<Map<String, Object>> getGeo();

    List<Object> getFields(String column);

    int getColTotalCount();

    int getColNonNullCount(String col[]);

    int getColNullCount(String col);

    int getColUniqueCount(String col[]);

    int getColType(String col);
    int getCount(String xname,Object xvalue,String yname,Object yvalue);

    Object[] getMin(String xname,Object xvalue,String yname);
    List<Object>  getYvalues(String y);

}
