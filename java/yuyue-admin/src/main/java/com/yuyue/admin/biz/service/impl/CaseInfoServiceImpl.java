package com.yuyue.admin.biz.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yuyue.admin.biz.service.CaseInfoService;
import com.yuyue.admin.dal.dao.CaseInfoDao;
import com.yuyue.admin.dal.po.CaseInfoPO;
import com.yuyue.admin.mapper.CaseInfoMapper;
import com.yuyue.admin.web.vo.SearchEnum;
import com.yuyue.admin.web.vo.SearchVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;

/**
 *
 * @author bowen
 * @date 2022-03-18
 */
//@Component
@Service
@Slf4j
public class CaseInfoServiceImpl extends ServiceImpl<CaseInfoMapper, CaseInfoPO> implements CaseInfoService {

    private Set<String> stringSet=new HashSet<String>(){{
        add("sex");
        add("job");
        add("disease");
        add("health_care");
        add("surface_part");
        add("surface_type");
        add("metabolic_ulcer");
        add("dm_time");
        add("dm_used");
        add("dm_course");
        add("df_time");
        add("df_cause");
        add("df_part");
        add("operation_part");
        add("amputation_part");
        add("smoke");
        add("drink_years");
        add("drinking");
        add("before_day");
        add("cure_way");
        add("pathogenic_bacteria");
        add("hosipital_address");
        add("doctor");
        add("department");
        add("in_day");
        add("course");
        add("cure_outcome");
        add("operation_course");
        add("case_source");
        add("edu");
        add("dc");
        add("wagner");
        add("crp");

    }};
    private Set<String> doubleSet=new HashSet<String>(){{
        add("weight");
        add("height");
        add("surface_area");
        add("cost");
        add("bmi");
        add("wbc");
        add("hba1c");
        add("procalcitonin");
        add("fpg");
        add("uric_acid");
        add("triglyceride");
        add("total_cholesterol");
        add("hdlc");
        add("ldlc");
        add("hemoglobin");
        add("alb");
        add("abi");
        add("cre");
        add("platelet");
        add("neut");
    }};
    private Set<String> intSet=new HashSet<String>(){{
        add("year");
        add("hosipital");
    }};

    @Resource
    private CaseInfoDao caseInfoDao;

    @Override
    public int updateCaseInfo(CaseInfoPO caseInfoPO){
        UpdateWrapper<CaseInfoPO> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("id",caseInfoPO.getId());
        updateWrapper.set("lgt",caseInfoPO.getLgt());
        updateWrapper.set("lat",caseInfoPO.getLat());
        updateWrapper.set("confidence",caseInfoPO.getConfidence());
        return caseInfoDao.update(null,updateWrapper);
    }

    @Override
    public JSONObject fullSearch(String text,long current,long size){
        QueryWrapper<CaseInfoPO> queryWrapper = new QueryWrapper<>();
        if(text!=null&&text.length()>0)
            queryWrapper.apply(" to_tsvector(case_info::text) @@ to_tsquery({0})",text);
        else queryWrapper.select("*");
        long total=caseInfoDao.selectCount(queryWrapper);
        queryWrapper.last(" limit "+size+" offset "+size*(current-1));
//        Page<CaseInfoPO> pages = new Page<>(searchVO.getCurrent(),searchVO.getSize());
//        IPage<CaseInfoPO> page = caseInfoDao.selectPage(pages, queryWrapper);
//        return page;
        List<CaseInfoPO> list= caseInfoDao.selectList(queryWrapper);
        JSONObject jsonObject=new JSONObject();
        jsonObject.put("total",total);
        jsonObject.put("current",current);
        jsonObject.put("size",size);
        jsonObject.put("data",list);
        return jsonObject;
    }
    @Override
    public JSONObject search(SearchVO searchVO){
        QueryWrapper<CaseInfoPO> queryWrapper = new QueryWrapper<>();
        if(searchVO==null)
            return null;
        if(searchVO.getSexCode()!=0){
            if(SearchEnum.MALE.getCode()==searchVO.getSexCode())
                queryWrapper.and(wq->wq.eq("sex",SearchEnum.MALE.getValue()));
            else queryWrapper.and(wq->wq.eq("sex",SearchEnum.FEMALE.getValue()));
        }
        if(searchVO.getCureWay()!=null){
            queryWrapper.lambda().and(wq->wq.like(CaseInfoPO::getCureWay,searchVO.getCureWay()));
        }
        if(searchVO.getHospital()!=0){
            queryWrapper.lambda().and(wq->wq.eq(CaseInfoPO::getHosipital,searchVO.getHospital()));
        }
        if(searchVO.getMaxAge()==0) {
            queryWrapper.lambda().and(wq->wq.ge(CaseInfoPO::getYear, searchVO.getMinAge()));
            queryWrapper.lambda().and(wq->wq.le(CaseInfoPO::getYear, Integer.MAX_VALUE));
        }
        else{
            queryWrapper.lambda().and(wq->wq.ge(CaseInfoPO::getYear, searchVO.getMinAge()));
            queryWrapper.lambda().and(wq->wq.le(CaseInfoPO::getYear, searchVO.getMaxAge()));
        }
        long total=caseInfoDao.selectCount(queryWrapper);
        queryWrapper.last(" limit "+searchVO.getSize()+" offset "+searchVO.getSize()*(searchVO.getCurrent()-1));
//        Page<CaseInfoPO> pages = new Page<>(searchVO.getCurrent(),searchVO.getSize());
//        IPage<CaseInfoPO> page = caseInfoDao.selectPage(pages, queryWrapper);
//        return page;
        List<CaseInfoPO> list= caseInfoDao.selectList(queryWrapper);
        JSONObject jsonObject=new JSONObject();
        jsonObject.put("total",total);
        jsonObject.put("current",searchVO.getCurrent());
        jsonObject.put("size",searchVO.getSize());
        jsonObject.put("data",list);
        return jsonObject;
    }
    @Override
    public List<CaseInfoPO> getCaseInfos(){
        QueryWrapper<CaseInfoPO> queryWrapper = new QueryWrapper<>();
        queryWrapper.select("id","detail_address","lgt","lat");
        return caseInfoDao.selectList(queryWrapper);
    }
    @Override
    public List<CaseInfoPO> getAll(){
        QueryWrapper<CaseInfoPO> queryWrapper = new QueryWrapper<>();
        queryWrapper.select("*");
        return caseInfoDao.selectList(queryWrapper);
    }
    @Override
    public List<Map<String , Object>> getGeo(){
        QueryWrapper<CaseInfoPO> queryWrapper = new QueryWrapper<>();
        queryWrapper.select("lgt","lat");
        List<Map<String , Object>> mapList= caseInfoDao.selectMaps(queryWrapper);
        mapList.forEach(System.out::println);
        return mapList;
    }
    @Override
    public List<Object> getFields(String column){
        QueryWrapper<CaseInfoPO> queryWrapper = new QueryWrapper<>();
        if(column.equalsIgnoreCase("sex")) {
            queryWrapper.select(column).isNotNull(column);
            List<Object> list = caseInfoDao.selectObjs(queryWrapper);
            for (int i=0;i< list.size();i++) {
                String s = list.get(i).toString();
                if (s.equalsIgnoreCase("男"))
                    list.set(i,1);
                else
                    list.set(i,0);
            }
            return list;
        }
        if(intSet.contains(column))
            queryWrapper.select(column).isNotNull(column).ge(column,0);
        else if(doubleSet.contains(column))
            queryWrapper.select(column).isNotNull(column).ge(column,0.0d);
        else if(stringSet.contains(column))
            queryWrapper.select(column).isNotNull(column).ne(column,"");
        else
            queryWrapper.select(column);
        return caseInfoDao.selectObjs(queryWrapper);
    }
    @Override
    public int getColTotalCount(){
        return caseInfoDao.selectCount(null).intValue();
    }
    @Override
    public int getColNonNullCount(String col[]){
        QueryWrapper<CaseInfoPO> queryWrapper = new QueryWrapper<>();
        for(String s:col)
            queryWrapper.isNotNull(s);
        return caseInfoDao.selectCount(queryWrapper).intValue();
    }
    @Override
    public int getColUniqueCount(String col[]){
        QueryWrapper<CaseInfoPO> queryWrapper = new QueryWrapper<>();
        String cols="";
        for(String s:col)
            cols=cols+s+",";
        cols="DISTINCT ("+cols.substring(0,cols.length()-1)+")";
        queryWrapper.select(cols);
        return caseInfoDao.selectCount(queryWrapper).intValue();
    }
    @Override
    public int getColNullCount(String col){
        QueryWrapper<CaseInfoPO> queryWrapper = new QueryWrapper<>();
        queryWrapper.isNull(col);
        return caseInfoDao.selectCount(queryWrapper).intValue();
    }
    @Override
    public int getColType(String col){
        if(doubleSet.contains(col))
            return 0;//"连续变量";
        return 1;//"分类变量";
    }

    @Override
    public Object[] getMin(String xname,Object xvalue,String yname){
        QueryWrapper<CaseInfoPO> queryWrapper = new QueryWrapper<>();
        queryWrapper.select("min("+yname+") as min_y, max("+yname+") as max_y, avg("+yname+") as avg_y").eq(xname,xvalue);
        List<Map<String , Object>> mapList = caseInfoDao.selectMaps(queryWrapper);
        mapList.forEach(System.out::println);
        if(mapList==null||mapList.size()==0)
            return null;
        Map<String , Object> map=mapList.get(0);
        Object o[]=new Object[3];
        o[0]=map.get("min_y");
        o[1]=map.get("max_y");
        o[2]=map.get("avg_y");
        return o;
        //return caseInfoDao.se(queryWrapper).intValue();
    }

    @Override
    public int getCount(String xname,Object xvalue,String yname,Object yvalue){
        QueryWrapper<CaseInfoPO> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(yname,yvalue).eq(xname,xvalue);
//        if(yvalue.toString().equals("")){
//            if(xvalue.toString().startsWith("2017-03-31 08:28:17"))
//                System.out.println("====="+yvalue+"===="+xvalue);
//            else if(xvalue.toString().startsWith("2017-05-14 05:15:24"))
//                System.out.println("====="+yvalue+"===="+xvalue);
//            else if(xvalue.toString().startsWith("2016-07-12 20:27:35"))
//                System.out.println("====="+yvalue+"===="+xvalue);
//            else if(xvalue.toString().startsWith("2016-03-28 18:00:45"))
//                System.out.println("====="+yvalue+"===="+xvalue);
//        }

        return caseInfoDao.selectCount(queryWrapper).intValue();
    }

    @Override
    public List<Object> getYvalues(String y){
        QueryWrapper<CaseInfoPO> queryWrapper = new QueryWrapper<>();
        queryWrapper.select("DISTINCT "+y).orderByAsc(y);
        return caseInfoDao.selectObjs(queryWrapper);
    }
    @Override
    public boolean save(CaseInfoPO entity) {
        return super.save(entity);
    }

    @Override
    public boolean saveBatch(Collection<CaseInfoPO> entityList) {
        return super.saveBatch(entityList);
    }
}
