package com.yuyue.admin.web.controller;

import com.alibaba.fastjson.JSONArray;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.yuyue.admin.dal.po.CaseInfoPO;
import com.yuyue.admin.dal.po.HosipitalPO;
import com.yuyue.admin.dal.po.ModelPO;
import com.yuyue.admin.dal.po.TaskPO;
//import com.yuyue.admin.stats.Array;
//import com.yuyue.admin.stats.ComputeRequest;
//import com.yuyue.admin.stats.ComputeResponse;
//import com.yuyue.admin.stats.StatsComputeGrpc;
import com.yuyue.admin.web.vo.*;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.*;
import com.alibaba.fastjson.JSONObject;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author bowen
 * @date 2022-03-16
 */
@RestController
@RequestMapping("/bi")
@Api(tags = "后端服务")
@EnableScheduling
public class TaskController {
//    @Resource
//    private StatsServiceGrpc statsServiceGrpc;
    @Resource
    private com.yuyue.admin.biz.service.TaskService taskService;
    @Resource
    private com.yuyue.admin.biz.service.CaseInfoService caseInfoService;
    @Resource
    private com.yuyue.admin.biz.service.HosipitalService hosipitalService;
    @Resource
    private com.yuyue.admin.biz.service.ModelService modelService;
    private static Cache<String, ResultVO> taskCache =
            CacheBuilder.newBuilder().initialCapacity(100000).maximumSize(1000000)
                    .expireAfterAccess(100, TimeUnit.MINUTES).build();
    //private List<CaseInfoPO> caseInfoPOs;
    private List<Map<String , Object>> geoMap;
    public double getEst(int modelId){
        //select avg(updatetime-createtime) from
        ModelPO modelPO=modelService.getModel(modelId);
        if(null==modelPO)
            return -1d;
        return modelPO.getEst();
    }

    public String catStr(String[] arr){
        String str="";
        for(String s:arr)
            str=str+s;
        return str;
    }

    @PostMapping(value="/config", produces = "application/json; charset=utf-8")
    @ApiOperation("获取配置")
    public ResultVO buildTask(@RequestBody ConfigVO configVO) {
        //1. get config of x,y
        //2. get config of model
        //3. get config of other conditions
        JSONObject jsonObject=new JSONObject();
        ResultVO resultVO=new ResultVO();
        JSONObject params=new JSONObject();
        params.put("x",configVO.getX());
        params.put("y",configVO.getY());
        params.put("modelId",configVO.getModelId());
        params.put("condition",configVO.getCondition());
        double est=getEst(configVO.getModelId());
        //3-1. get  config of echart?
        //4. build task

        //5. generate 32 uid
//        String uid = UUID.randomUUID().toString().replace("-", "").toLowerCase();
        String base=catStr(configVO.getX())+catStr(configVO.getY())+configVO.getCondition()+configVO.getModelId();
        String uid= DigestUtils.md5DigestAsHex(base.getBytes());
        String s=getTask(uid);
        //System.out.println("s========"+s);
        resultVO=JSONObject.parseObject(s,ResultVO.class);
        if(resultVO==null)
            return null;
        int code=resultVO.getCode();
        if(code==200)
            return resultVO;
//        ResultVO result=taskCache.getIfPresent(uid);
//        if(null!=result) {
//            return result;
//        }
//        else{
//            TaskPO taskPO=taskService.getTaskByUid(uid);
//            if(null!=taskPO){
//                jsonObject.put("success", true);
//                jsonObject.put("code", 200);
//                jsonObject.put("uid", uid);
//                jsonObject.put("result",taskPO.getResult());
//                resultVO.setCode(200);
//                resultVO.setMessage("get task successfully");
//                resultVO.setUid(uid);
//                resultVO.setEst(est);
//                String res=taskPO.getResult();
//                if(isJsonArray(res)) {
//                    JSONArray array = JSONArray.parseArray(res);
//
//                    resultVO.setResult(array);
//                }
//                else {
//                    JSONArray array = new JSONArray();
//                    array.add(res);
//                    resultVO.setResult(array);
//                }
//                return resultVO;//jsonObject.toJSONString();
//            }
//        }
        TaskPO taskPO=new TaskPO();
        taskPO.setUid(uid);
        taskPO.setModelId(configVO.getModelId());
        taskPO.setParams(params.toJSONString());
        taskPO.setStatus(0);
        if(taskPO.getModelId()==1){
            resultVO=calculateModel1(configVO);
            if(resultVO==null)
                return null;
            resultVO.setStatus(2);
        }
        if(taskPO.getModelId()==13){
            if(geoMap==null||geoMap.size()==0)
                geoMap =caseInfoService.getGeo();//.getCaseInfos();
            jsonObject.put("geolist",geoMap);
            JSONArray array= (JSONArray)JSONArray.toJSON(geoMap);
//            String data=array.toString();
//            data=data.replace("\"","");
            resultVO.setResult(geoMap);
            resultVO.setStatus(2);
            taskPO.setStatus(2);
            taskPO.setResult(array.toString());
        }
        if(taskPO.getModelId()==19){
            ResultVO resultVO1=timebar3(configVO);
            resultVO1.setStatus(2);
            ResultVO resultVO2=timebar4(configVO);
            List<ChartArray> chartArrays1=resultVO1.getChartArray();
            List<ChartArray> chartArrays2=resultVO2.getChartArray();
            chartArrays1.addAll(chartArrays2);
            resultVO.setChartArray(chartArrays1);
            resultVO.setResult(chartArrays1);
            resultVO.setStatus(2);
            resultVO.setInfo("ok");
            resultVO.setTotal(chartArrays1.size());
            taskPO.setStatus(2);
            taskPO.setResult(resultVO.toString());
        }
        if(taskPO.getModelId()==20){
            resultVO = timebar5(configVO);
            resultVO.setStatus(2);
            resultVO.setInfo("ok");

            taskPO.setStatus(2);
            taskPO.setResult(resultVO.toString());
        }
        //6. save task
        //taskCache.put();x+y+condition+chartid,result
        int ret=taskService.save(taskPO);

        //7. return task uid

        if(ret>0) {
            jsonObject.put("success", true);
            jsonObject.put("code", 200);
            jsonObject.put("uid", uid);
            jsonObject.put("est",est);
            resultVO.setMessage("create task successfully");
            resultVO.setCode(200);
            resultVO.setUid(uid);
            resultVO.setEst(est);
            if(resultVO.getResult()!=null)//&&resultVO.getResult().length()!=0)
                taskCache.put(uid,resultVO);
            taskService.updateUid(uid);
        }
        else {
            jsonObject.put("success", false);
            jsonObject.put("code", 500);
            resultVO.setCode(500);
            resultVO.setMessage("create task failed");
        }
        return resultVO;//jsonObject.toJSONString();
    }

    @PostMapping("/updatetask")
    @ApiOperation("更新任务")
    public String updateTask(String uid,String chartIds,String result,int status) {
        JSONObject jsonObject=new JSONObject();
        TaskPO taskPO=taskService.getTaskByUid(uid);
        if(null!=taskPO) {
            taskService.update(uid,chartIds,result,status);
            jsonObject.put("success", true);
            jsonObject.put("code", 200);
            jsonObject.put("uid", uid);
            double est=taskService.getAvgTime(taskPO.getModelId());
            modelService.update(taskPO.getModelId(),est);

        }
        else {
            jsonObject.put("success", false);
            jsonObject.put("code", 500);
        }
        return jsonObject.toJSONString();
    }
    @GetMapping(value = "/gettask", produces = "application/json; charset=utf-8")
    @ApiOperation("获取任务状态")
    public String getTask(@RequestParam String uid) {
        ResultVO resultVO=taskCache.getIfPresent(uid);

        if(resultVO!=null){
            resultVO.setUid(uid);
            Object obj=JSONObject.toJSON(resultVO);
            return obj.toString();
        }
        resultVO=new ResultVO();
        TaskPO taskPO=taskService.getTaskByUid(uid);

        JSONObject jsonObject=new JSONObject();
        if(taskPO!=null) {
            jsonObject.put("success", true);
            jsonObject.put("code", 200);
            jsonObject.put("uid", uid);
            jsonObject.put("chartIds", taskPO.getChartIds());
            jsonObject.put("status", taskPO.getStatus());
            resultVO.setUid(uid);
            resultVO.setStatus(taskPO.getStatus());
            //JSONArray result=taskPO.getResult();
            String result = taskPO.getResult();
            boolean good = false;
            if (null == result || result.length() == 0) {
                MetricsVO metricsVO = new MetricsVO();
                metricsVO.setUid(uid);
                String params = taskPO.getParams();
                JSONObject obj = JSONObject.parseObject(params);
                JSONArray array = (JSONArray) obj.get("x");
                String[] x = new String[array.size()];
                //metricsVO.setX(array.toArray(x));
                metricsVO.setX(array.toArray(x));
                metricsVO.setX_count(array.size());
                array = (JSONArray) obj.get("y");
                String[] y = new String[array.size()];
                metricsVO.setY(array.toArray(y));
                metricsVO.setY_count(array.size());
                metricsVO.setModelId(taskPO.getModelId());

                resultVO = buildMetrics(metricsVO);

                String info = resultVO.getInfo();
                if (info != null && info.trim().equalsIgnoreCase("ok"))
                    good = true;
            } else {
                //jsonObject.put("result", result);
                if (isJsonArray(result)) {
                    JSONArray array = JSONArray.parseArray(result);
                    resultVO.setResult(array);
                    good = true;
                } else {
                    JSONArray array = new JSONArray();
                    array.add(result);
                    resultVO.setResult(array);
                    good = false;
                }
            }

            if (good){//(resultVO.getResult()!=null&&resultVO.getResult().size()!=0){
                taskCache.put(uid, resultVO);
                resultVO.setCode(200);
                resultVO.setStatus(2);
                resultVO.setMessage("Get task successful");
            }
        }
        else {
            resultVO.setCode(500);
            resultVO.setMessage("task not found");
        }
        Object obj=JSONObject.toJSON(resultVO);
        return obj.toString();//jsonObject.toJSONString();
    }

    @GetMapping("/rmtask")
    @ApiOperation("删除任务")
    public String deleteTask(@RequestParam String uid) {
        int ret=taskService.delete(uid);

        JSONObject jsonObject=new JSONObject();
        if(ret>0) {
            jsonObject.put("success", true);
            jsonObject.put("code", 200);
            jsonObject.put("uid", uid);
            taskCache.invalidate(uid);
        }
        else {
            jsonObject.put("success", false);
            jsonObject.put("code", 500);
        }
        return jsonObject.toJSONString();
    }

    @PostMapping(value="/metrics",produces = "application/json; charset=utf-8")
    @ApiOperation("获取向量")
    public ResultVO buildMetrics(@RequestBody MetricsVO metricsVO) {
        //1. get config echart
        //2. get task uid
        //3. build arrow metric
        //4. generate arrow file id
        JSONObject jsonObject=new JSONObject();
        ResultVO resultVO=new ResultVO();
        //jsonObject.put("success", false);
//        jsonObject.put("code", 500);
        resultVO.setCode(500);
        resultVO.setUid(metricsVO.getUid());
//        String r="";
        //JSONObject metrics = (JSONObject) JSONObject.toJSON(metricsVO);
        try {

//            ManagedChannel managedChannel = ManagedChannelBuilder.forAddress("localhost", 50051)
//                    .usePlaintext()
//                    .build();
//
////            StatsServiceGrpc.StatsServiceBlockingStub statsServiceBlockingStub
////                    = StatsServiceGrpc.newBlockingStub(managedChannel);
////            StatsServiceGrpc.StatsServiceStub statsServiceStub=StatsServiceGrpc.newStub(managedChannel);
//            StatsComputeGrpc.StatsComputeBlockingStub statsComputeStub=StatsComputeGrpc.newBlockingStub(managedChannel);
//            //ComputeRequest statsRequest = ComputeRequest.newBuilder();
////                    .a
////                    .setModelId(metricsVO.getModelId())
////                    .build();
//            ComputeRequest.Builder request= ComputeRequest.newBuilder();
//            Array.Builder xarray= Array.newBuilder();
//            int i=0;
//            for(String s:metricsVO.getX()) {
//                List<Object> xlist=caseInfoService.getFields(s);
//                if(xlist==null)
//                    continue;
//                for(Object obj:xlist) {
//                    Double d=Double.valueOf(obj.toString());
//                    xarray.addNum(d);
//                }
//                request.addXarray(i,xarray.build());
//                i++;
//                xarray.clear();
//            }
//            Array.Builder yarray= Array.newBuilder();
//            i=0;
//            for(String s:metricsVO.getY()) {
//                List<Object> ylist=caseInfoService.getFields(s);
//                if(ylist==null)
//                    continue;
//                for(Object obj:ylist) {
//                    Double d=Double.valueOf(obj.toString());
//                    yarray.addNum(d);
//                }
//                request.addYarray(i,yarray.build());
//                i++;
//                yarray.clear();
//            }
//            ComputeRequest statsRequest=request.setModelId(metricsVO.getModelId()).build();
//            ComputeResponse statsResponse = statsComputeStub.getResult(statsRequest);//statsServiceBlockingStub.executeStats(statsRequest);
//
//            //System.out.println("Received response: "+statsResponse.toString());
//            int total=statsResponse.getTotal();
//            String info=statsResponse.getInfo();
//            String chartIds="";
//            jsonObject.put("total",total);
//            jsonObject.put("info",info);
//            resultVO.setTotal(total);
//            resultVO.setInfo(info);
//            if(total==-1)
//                return resultVO;//jsonObject.toJSONString();
//            List<ComputeResponse.ChartData> chartDataList =statsResponse.getChartDataArrayList();
//            JSONArray jsonArray=new JSONArray();
//            for(ComputeResponse.ChartData chartData:chartDataList) {
//                JSONObject object=new JSONObject();
//                int chartId=chartData.getChartId();
//                chartIds=chartIds+","+chartId;
//                List<ByteString> xaxis=chartData.getXaxisList().asByteStringList();
//                List<ByteString> yaxis=chartData.getYaxisList().asByteStringList();
//                List<String> x=new LinkedList<>();
//                List<String> y=new LinkedList<>();
//                for(ByteString s:xaxis){
//                    String str=s.toString("UTF-8");
//                    x.add(str);
//                }
//                for(ByteString s:yaxis){
//                    String str=s.toString("UTF-8");
//                    y.add(str);
//                }
//                object.put("chartId",chartId);
//                List<Array> xarr=chartData.getXarrayList();
//                List<Array> yarr=chartData.getYarrayList();
//                JSONArray xarry=new JSONArray();
//                JSONArray yarry=new JSONArray();
//                //JSONObject js = (JSONObject) JSONObject.toJSON(chartData);
////                JSONArray xaxisArray=(JSONArray)JSONArray.toJSON(xaxis);
////                JSONArray yaxisArray=(JSONArray)JSONArray.toJSON(yaxis);
//                for(Array array:xarr){
//                    List<Double> nums=array.getNumList();
//                    JSONArray numarr=(JSONArray)JSONArray.toJSON(nums);
//                    xarry.add(numarr);
//                }
//                for(Array array:yarr){
//                    List<Double> nums=array.getNumList();
//                    JSONArray numarr=(JSONArray)JSONArray.toJSON(nums);
//                    yarry.add(numarr);
//                }
//                object.put("xaxis",x);
//                object.put("yaxis",y);
//                object.put("xarray",xarry);
//                object.put("yarray",yarry);
//                jsonArray.add(object);
//            }
//            managedChannel.shutdown();



//            ManagedChannel managedChannel = ManagedChannelBuilder.forAddress("localhost", 50051)
//                    .usePlaintext()
//                    .build();
//
////            StatsServiceGrpc.StatsServiceBlockingStub statsServiceBlockingStub
////                    = StatsServiceGrpc.newBlockingStub(managedChannel);
////            StatsServiceGrpc.StatsServiceStub statsServiceStub=StatsServiceGrpc.newStub(managedChannel);
//            StatsComputeGrpc.StatsComputeBlockingStub statsComputeStub=StatsComputeGrpc.newBlockingStub(managedChannel);
//            //ComputeRequest statsRequest = ComputeRequest.newBuilder();
////                    .a
////                    .setModelId(metricsVO.getModelId())
////                    .build();
//            ComputeRequest.Builder request= ComputeRequest.newBuilder();
//            Array.Builder xarray= Array.newBuilder();
//            int i=0;
//            for(String s:metricsVO.getX()) {
//                List<Object> xlist=caseInfoService.getFields(s);
//                if(xlist==null)
//                    continue;
//                for(Object obj:xlist) {
//                    Double d=Double.valueOf(obj.toString());
//                    xarray.addNum(d);
//                }
//                request.addXarray(i,xarray.build());
//                i++;
//                xarray.clear();
//            }
//            Array.Builder yarray= Array.newBuilder();
//            i=0;
//            for(String s:metricsVO.getY()) {
//                List<Object> ylist=caseInfoService.getFields(s);
//                if(ylist==null)
//                    continue;
//                for(Object obj:ylist) {
//                    Double d=Double.valueOf(obj.toString());
//                    yarray.addNum(d);
//                }
//                request.addYarray(i,yarray.build());
//                i++;
//                yarray.clear();
//            }
//            ComputeRequest statsRequest=request.setModelId(metricsVO.getModelId()).build();
//            ComputeResponse statsResponse = statsComputeStub.getResult(statsRequest);//statsServiceBlockingStub.executeStats(statsRequest);
            JSONObject req=new JSONObject();
            //JSONArray array=new JSONArray();
            req.put("condition",metricsVO.getConditon());
            req.put("modelId",metricsVO.getModelId());
            req.put("x_count",metricsVO.getX_count());
            req.put("y_count",metricsVO.getY_count());
            JSONArray x= new JSONArray();
            JSONArray y= new JSONArray();
            for(String xstr:metricsVO.getX()) {
                List<Object> dataa=caseInfoService.getFields(xstr);
                if(dataa==null)
                    continue;
                Object[] data=new Object[dataa.size()];
                for(int i=0;i<dataa.size();i++){
                    //Double d=Double.parseDouble( dataa.get(i).toString());
                    data[i]=dataa.get(i);
                }
                x.add(data);
            }
            req.put("x",x);
            for(String ystr:metricsVO.getY()) {
                List<Object> dataa=caseInfoService.getFields(ystr);
                if(dataa==null)
                    continue;
                Object[] data=new Object[dataa.size()];
                for(int i=0;i<dataa.size();i++){
                    //Double d=Double.parseDouble( dataa.get(i).toString());
                    data[i]=dataa.get(i);
                }
                y.add(data);
            }
            req.put("y",y);
            System.out.println("request: "+req.toString());
            updateTask(metricsVO.getUid(), "-1","", 1);//executing

            String res=HttpUtil.post("http://localhost:50002",req);
//            String res="{\"total\":2,\"chartArray\":[{\"chartId\":0,\"xaxis\":[],\"yaxis\":[\"均值\",\"标准差\",\"最小值\",\"25分位值\",\"50分位值\",\"75分位值\",\"最大值\n" +
//                    "\"],\"x\":[],\"y\":[[0.0,0.0,0.0,0.0,0.0,0.0,0.0],[0.0,0.0,0.0,0.0,0.0,0.0,0.0]]},{\"chartId\":4,\"xaxis\":[],\"yaxis\":[],\"x\":[],\"y\":[[0.0,0.0,0.0,0.0,0.0],[0.0,0.0,0.0,0.0,0.0]]}],\"info\":\"ok\"}";
            System.out.println("Received response: "+res);

//            int total=res.getTotal();
//            String info=statsResponse.getInfo();
            resultVO=JSONObject.parseObject(res,ResultVO.class);
            if(resultVO.getInfo().equalsIgnoreCase("ok")) {
                resultVO.setStatus(2);
                resultVO.setCode(200);
                List<ChartArray> chartArrays=resultVO.getChartArray();
                String chartIds="";
                for(ChartArray e:chartArrays){
                    int chartId=e.getChartId();
                    chartIds=chartIds+chartId+",";
                    //array.add(e);
                }
                resultVO.setResult(chartArrays);
                if(chartIds.endsWith(","))
                    chartIds=chartIds.substring(0,chartIds.length()-1);
                updateTask(metricsVO.getUid(), chartIds,res, 2);//success
            }
            else {
                updateTask(metricsVO.getUid(), "-1",res, 3);//failed
                resultVO.setCode(500);
                resultVO.setResult(res);
            }
//            String r="";
//            String chartIds="";
//            jsonObject.put("total",total);
//            jsonObject.put("info",info);
//            resultVO.setTotal(total);
//            resultVO.setInfo(info);
//            if(total==-1)
//                return resultVO;//jsonObject.toJSONString();
//            List<ComputeResponse.ChartData> chartDataList =statsResponse.getChartDataArrayList();
//            JSONArray jsonArray=new JSONArray();
//            for(ComputeResponse.ChartData chartData:chartDataList) {
//                JSONObject object=new JSONObject();
//                int chartId=chartData.getChartId();
//                chartIds=chartIds+","+chartId;
//                List<ByteString> xaxis=chartData.getXaxisList().asByteStringList();
//                List<ByteString> yaxis=chartData.getYaxisList().asByteStringList();
//                List<String> x=new LinkedList<>();
//                List<String> y=new LinkedList<>();
//                for(ByteString s:xaxis){
//                    String str=s.toString("UTF-8");
//                    x.add(str);
//                }
//                for(ByteString s:yaxis){
//                    String str=s.toString("UTF-8");
//                    y.add(str);
//                }
//                object.put("chartId",chartId);
//                List<Array> xarr=chartData.getXarrayList();
//                List<Array> yarr=chartData.getYarrayList();
//                JSONArray xarry=new JSONArray();
//                JSONArray yarry=new JSONArray();
//                //JSONObject js = (JSONObject) JSONObject.toJSON(chartData);
////                JSONArray xaxisArray=(JSONArray)JSONArray.toJSON(xaxis);
////                JSONArray yaxisArray=(JSONArray)JSONArray.toJSON(yaxis);
//                for(Array array:xarr){
//                    List<Double> nums=array.getNumList();
//                    JSONArray numarr=(JSONArray)JSONArray.toJSON(nums);
//                    xarry.add(numarr);
//                }
//                for(Array array:yarr){
//                    List<Double> nums=array.getNumList();
//                    JSONArray numarr=(JSONArray)JSONArray.toJSON(nums);
//                    yarry.add(numarr);
//                }
//                object.put("xaxis",x);
//                object.put("yaxis",y);
//                object.put("xarray",xarry);
//                object.put("yarray",yarry);
//                jsonArray.add(object);
//            }
//            managedChannel.shutdown();
//            jsonObject.put("message","compute successfully");
//            jsonObject.put("code",200);
//            jsonObject.put("result",jsonArray);
//            resultVO.setMessage("compute successfully");
//            resultVO.setCode(200);
//            resultVO.setResult(jsonArray);//.toJSONString());
//            resultVO.setStatus(2);
//            updateTask(metricsVO.getUid(), chartIds,jsonArray.toJSONString(), 2);//success
            //r = HttpUtil.post("http://127.0.0.1:8787/v1/compute", metrics);
            //r="";
//            updateTask(metricsVO.getUid(), -1,"", 1);//executing
//            if(null==r||r.trim().equals(""))
//                return jsonObject.toString();
//            ResultVO resultVo=JSONObject.parseObject(r,ResultVO.class);
//            if(resultVo.getCode()==200) {
//                jsonObject.put("success", true);
//                jsonObject.put("code", 200);
//                jsonObject.put("uid", metricsVO.getUid());
//                updateTask(metricsVO.getUid(), resultVo.getChartId(),resultVo.getData(), 2);//success
//            }
//            else {
//                jsonObject.put("success", false);
//                jsonObject.put("code", 500);
//                jsonObject.put("uid", metricsVO.getUid());
//                updateTask(metricsVO.getUid(), resultVo.getChartId(),resultVo.getData(), 3);//failed
//            }
        }catch (Exception e){
            e.printStackTrace();
            jsonObject.put("message","compute exception");
            jsonObject.put("code",400);
            jsonObject.put("result",e.getMessage());
            resultVO.setMessage("compute exception");
            resultVO.setCode(400);
            resultVO.setInfo("compute exception");
            //resultVO.setResult(e.getMessage());
            updateTask(metricsVO.getUid(), "",e.getMessage(), 3);//failed
            resultVO.setStatus(3);
        }finally {
            return resultVO;//jsonObject.toString();
        }
    }


    @PostMapping("/cancel")
    @ApiOperation("取消任务")
    public String cancelTask(@RequestParam String uid) {
        JSONObject jsonObject=new JSONObject();
        jsonObject.put("success", false);
        jsonObject.put("code", 500);
        String r="";
        JSONObject json = new JSONObject();
        json.put("uid",uid);
        try {
            r = HttpUtil.delete("http://127.0.0.1:8787/v1/compute", "",json.toString());
            if(null==r||r.trim().equals(""))
                return jsonObject.toString();
            jsonObject.put("success", true);
            jsonObject.put("code", 200);
            updateTask(uid, "","", 4);//cancel
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            return jsonObject.toString();
        }
    }

    @GetMapping("/geo")
    @ApiOperation("计算地理位置")
    public String updateGeo() throws IOException, InterruptedException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("code", 500);
        int sum = 0;
        List<CaseInfoPO> list = caseInfoService.getCaseInfos();
        for (CaseInfoPO caseInfoPO : list) {
            Object[] obj = getCoordinate(caseInfoPO.getDetailAddress());
            int confidence=Integer.valueOf( obj[2].toString());
//            if(confidence<60){
//                obj = getCoordinate("温州市"+caseInfoPO.getDetailAddress());
//                int temp=Integer.valueOf( obj[2].toString());
//                if(temp>=60) {
//                    caseInfoPO.setDetailAddress("温州市" + caseInfoPO.getDetailAddress());
//                    confidence = temp;
//                }
//            }
            caseInfoPO.setLgt(Float.valueOf( obj[0].toString()));
            caseInfoPO.setLat(Float.valueOf( obj[1].toString()));
            caseInfoPO.setConfidence(confidence);
            Thread.sleep(200);
            int update = caseInfoService.updateCaseInfo(caseInfoPO);
            sum += update;
        }
        if (sum == list.size()) {
            jsonObject.put("message", sum + " records updated");
            jsonObject.put("code", 200);
        } else if (sum == 0) {
            jsonObject.put("message", sum + " of " + list.size() + " records updated");
            jsonObject.put("code", 201);
        } else
            jsonObject.put("message", "0 records updated");
        return jsonObject.toJSONString();
    }
    //@Scheduled(cron="0 0 0 * * ?") // 每天0点刷新一次地理信息
    public void flushGeo() throws IOException, InterruptedException {
        updateGeo();
        geoMap=caseInfoService.getGeo();
    }
    public static Object[] getCoordinate(String addr) throws IOException {
        String lng = null;//经度
        String lat = null;//纬度
        String confidence="0";
        String address = null;
        try {
            address = java.net.URLEncoder.encode(addr, "UTF-8");
        } catch (UnsupportedEncodingException e1) {
            e1.printStackTrace();
        }
        String key = "qDaws0IfG7VID98VWsDCYZvr8j6fRkGv";
        String url = String.format("http://api.map.baidu.com/geocoder?address=%s&output=json&key=%s", address, key);
        URL myURL = null;
        URLConnection httpsConn = null;
        try {
            myURL = new URL(url);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        InputStreamReader insr = null;
        BufferedReader br = null;
        try {
            httpsConn = (URLConnection) myURL.openConnection();// 不使用代理
            if (httpsConn != null) {
                insr = new InputStreamReader(httpsConn.getInputStream(), "UTF-8");
                br = new BufferedReader(insr);
                String data = null;
                int count = 1;
                while ((data = br.readLine()) != null) {
                    //System.out.println(data);
                    if (count == 5) {
                        int start=data.indexOf(":") ;
                        int end=data.indexOf(",");
                        if(start==-1||end==-1)
                            lng="-1";
                        else
                            lng = (String) data.subSequence(start+1, end);//经度
                        count++;
                    } else if (count == 6) {
                        int start=data.indexOf(":") ;
                        if(start==-1)
                            lat="-1";
                        else
                            lat = data.substring(start + 1);//
                        count++;
                    }else if(count==9){
                        int start=data.indexOf(":") ;
                        int end=data.indexOf(",");
                        if(start==-1||end==-1)
                            confidence="-1";
                        else
                            confidence= data.substring(start+1, end);
                        count++;
                    }
                    else {
                        count++;
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            return new Object[]{-1,-1,-1};
        } finally {
            if (insr != null) {
                insr.close();
            }
            if (br != null) {
                br.close();
            }
        }
        return new Object[]{lng, lat,confidence};
    }

    public static boolean isJsonArray(String content) {
        if(StringUtils.isBlank(content))
            return false;
        StringUtils.isEmpty(content);
        try {
            JSONArray jsonStr = JSONArray.parseArray(content);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    public ResultVO calculateModel1(ConfigVO configVO){
        //get x and y
        //get 3 records of x
        //build xaxis and xarray
        //return
        ResultVO resultVO=new ResultVO();
        String x[]=configVO.getX();
        if(x==null||x.length<1)
            return null;
        int count=caseInfoService.getColTotalCount();
        int nonNullCount=caseInfoService.getColNonNullCount(x);
        int uniqueCount=caseInfoService.getColUniqueCount(x);
        List<ChartArray> list=new ArrayList<>(2);
        ChartArray chart1=new ChartArray();
        chart1.setChartId(0);
        chart1.setYaxis(new String[]{"总条目数","总体完成条目数","unique条目数"});
        Integer[] y1=new Integer[]{count,nonNullCount,uniqueCount};
        List<Object[]> list1=new ArrayList<>(1);
        list1.add(0,y1);
        chart1.setY(list1);

        ChartArray chart2=new ChartArray();
        chart2.setChartId(0);
        chart2.setYaxis(new String[]{"条目数","缺失条目数","变量类型"});
        List<Object[]> list2=new LinkedList<>();

        for(String s:x) {
            int nullCount=caseInfoService.getColNullCount(s);
            int colType=caseInfoService.getColType(s);
            Integer[] y2 = new Integer[]{count, nullCount, colType};
            list2.add(y2);
        }
        chart2.setY(list2);
        list.add(0,chart1);
        list.add(1,chart2);
        resultVO.setInfo("ok");
        resultVO.setTotal(2);
        resultVO.setResult(list);
        resultVO.setChartArray(list);
        return resultVO;
    }

//    @RequestMapping(value = "/excelDown", method = RequestMethod.GET)
//    public void generateLedger(HttpServletRequest request, HttpServletResponse response) throws Exception {
//        Map<String, Object> searchParams = CreepersUtil.getParameterMap(request, false);
//        List<CaseInfoPO> caseInfoPOS = caseInfoService.getAll();
//        Map<String, Object> map = new HashMap<>();
//        map.put("faePrdList", faePrdList);
//        new ExcelUtil().export("财富中心报表", map, "/excel/product_fae.xlsx", response);
//    }

    //@RequestMapping(value = "/timebar3", method = RequestMethod.GET)
    public ResultVO timebar3(ConfigVO configVO) {
        if(configVO.getX()==null||configVO.getY()==null)
            return null;
        List<ChartArray> list=new LinkedList<>();
        String x[]=configVO.getX();
        if(x.length>1)
            return null;
        String xs=x[0];
        List<Object> xvalues=caseInfoService.getYvalues(xs);
        if(xvalues==null||xvalues.size()<1)
            return null;
        String xaxis[]=new String[xvalues.size()];
        for(int i=0;i<xvalues.size();i++)
            xaxis[i]=xvalues.get(i).toString();
        String y[]= configVO.getY();
        ResultVO resultVO=new ResultVO();
        for(String ys:y){
            ChartArray chartArray=new ChartArray();
            List<Object> yvalues=caseInfoService.getYvalues(ys);
            String yaxis[]=new String[yvalues.size()];
            for(int i=0;i<yvalues.size();i++)
                yaxis[i]=yvalues.get(i).toString();
            chartArray.setChartId(3);
            chartArray.setXaxis(xaxis);
            chartArray.setYaxis(yaxis);
            List<Object[]> ylist=new LinkedList<>();
            for(Object xvalue:xvalues) {
                Object[] yo=new Object[yvalues.size()];
                for(int i=0;i<yvalues.size();i++) {
                    Object yvalue = yvalues.get(i);
                    int xo = caseInfoService.getCount(xs, xvalue, ys, yvalue);//select * from caseInfo where x=xs and y=ys;
                    yo[i] = xo;
                }
                ylist.add(yo);
            }
            chartArray.setY(ylist);
            list.add(chartArray);
        }
        resultVO.setInfo("ok");
        resultVO.setTotal(list.size());
        resultVO.setResult(list);
        resultVO.setChartArray(list);
        return resultVO;
    }

    //@RequestMapping(value = "/timebar4", method = RequestMethod.GET)
    public ResultVO timebar4(ConfigVO configVO) {
        if(configVO.getX()==null||configVO.getY()==null)
            return null;
        List<ChartArray> list=new LinkedList<>();
        String x[]=configVO.getX();
        if(x.length>1)
            return null;
        String xs=x[0];
        List<Object> xvalues=caseInfoService.getYvalues(xs);
        if(xvalues==null||xvalues.size()<1)
            return null;
        String xaxis[]=new String[xvalues.size()];
        for(int i=0;i<xvalues.size();i++)
            xaxis[i]=xvalues.get(i).toString();
        String y[]= configVO.getY();
        ResultVO resultVO=new ResultVO();
        for(String ys:y){
            ChartArray chartArray=new ChartArray();
            List<Object> yvalues=caseInfoService.getYvalues(ys);
            String yaxis[]=new String[yvalues.size()];
            for(int i=0;i<yvalues.size();i++)
                yaxis[i]=yvalues.get(i).toString();
            chartArray.setChartId(15);
            chartArray.setXaxis(yaxis);
            chartArray.setYaxis(xaxis);
            List<Object[]> xlist=new LinkedList<>();
            for(Object xvalue:xvalues) {
                Object[] yo=new Object[yvalues.size()];
                for(int i=0;i<yvalues.size();i++) {
                    Object yvalue = yvalues.get(i);
                    int xo = caseInfoService.getCount(xs, xvalue, ys, yvalue);//select * from caseInfo where x=xs and y=ys;
                    yo[i] = xo;
                }
                xlist.add(yo);
            }
            chartArray.setX(xlist);
            list.add(chartArray);
        }
        resultVO.setInfo("ok");
        resultVO.setTotal(list.size());
        resultVO.setResult(list);
        resultVO.setChartArray(list);
        return resultVO;
    }

    public ResultVO timebar5(ConfigVO configVO) {
        if(configVO.getX()==null||configVO.getY()==null)
            return null;
        List<ChartArray> list=new LinkedList<>();
        String x[]=configVO.getX();
        if(x.length>1)
            return null;
        String xs=x[0];
        List<Object> xvalues=caseInfoService.getYvalues(xs);
        if(xvalues==null||xvalues.size()<1)
            return null;
        String xaxis[]=new String[xvalues.size()];
        for(int i=0;i<xvalues.size();i++)
            xaxis[i]=xvalues.get(i).toString();
        String y[]= configVO.getY();
        String label[]=new String[]{"min","avg","max"};
        ResultVO resultVO=new ResultVO();
        for(String ys:y) {
            ChartArray chartArray = new ChartArray();
            chartArray.setChartId(1);
            chartArray.setXaxis(xaxis);
            chartArray.setYaxis(label);
            List<Object[]> xlist = new LinkedList<>();
            List<Object[]> ylist = new LinkedList<>();
            for (int j = 0; j < xaxis.length; j++) {
                Object xvalue = xaxis[j];
                //Object[] xo = new Object[xaxis.length];
                //Object[] yo = new Object[3];
                Object o[] = caseInfoService.getMin(xs, xvalue, ys);//select min(ys) from caseInfo where x=xs;
//            for (int i = 0; i < y.length; i++) {
//                String ys = y[i];
//                yo[i] = o[i];
//            }
                //xo[j]=xvalue;
                ylist.add(o);
                //xlist.add(xo);
            }
            xlist.add(xaxis);
            chartArray.setX(xlist);
            chartArray.setY(ylist);
            list.add(chartArray);
        }
        resultVO.setInfo("ok");
        resultVO.setTotal(list.size());
        resultVO.setResult(convertToLine(list));
        resultVO.setChartArray(list);
        return resultVO;
    }
    public JSONArray convertToLine(List<ChartArray> list){
        JSONArray series=new JSONArray();
        JSONObject result=new JSONObject();

        for(ChartArray chartArray:list){
            List<LineVO> lines=new LinkedList<>();
            //JSONArray jsonArray=new JSONArray();
            JSONObject jsonObject=new JSONObject();

            jsonObject.put("xaxis",chartArray.getXaxis());
            jsonObject.put("chartId",1);
            //for(int z=0;z<list.size();z++) {
            List<Object[]> y = chartArray.getY();
            LineVO lineVO1 = new LineVO();
            //lineVO1.setXaxis(chartArray.getXaxis());
            lineVO1.setName("20");
            lineVO1.setStack("Min");
            lineVO1.setType("line");
            //lineVO1.setChartId(1);
            LineVO lineVO2 = new LineVO();
            //lineVO2.setXaxis(chartArray.getXaxis());
            lineVO2.setName("20");
            lineVO2.setStack("Max");
            lineVO2.setType("line");
            //lineVO2.setChartId(1);
            LineVO lineVO3 = new LineVO();
            //lineVO3.setXaxis(chartArray.getXaxis());
            lineVO3.setName("20");
            lineVO3.setStack("Avg");
            lineVO3.setType("line");
            //lineVO3.setChartId(1);
            Object[] min = new Object[chartArray.getXaxis().length];
            Object[] max = new Object[chartArray.getXaxis().length];
            Object[] avg = new Object[chartArray.getXaxis().length];
            for (int i = 0; i < y.size(); i++) {
                Object[] o = y.get(i);
                min[i] = o[0];
                max[i] = o[1];
                avg[i] = o[2];
            }
            lineVO1.setData(min);
            lineVO2.setData(max);
            lineVO3.setData(avg);
            lines.add(lineVO1);
            lines.add(lineVO2);
            lines.add(lineVO3);
            //jsonArray.add(lines);
            //}
            jsonObject.put("series",lines);
            series.add(jsonObject);
        }
        result.put("result",series);
        return series;
    }
    @RequestMapping(value = "/search", method = RequestMethod.POST)
    public JSONObject search(@RequestBody SearchVO searchVO) {
        return caseInfoService.search(searchVO);
    }
    @RequestMapping(value = "/hospital", method = RequestMethod.GET)
    public List<HosipitalPO> fulltext() {
        List<HosipitalPO> list= hosipitalService.listAll();
        return list;
    }
    @RequestMapping(value = "/fullsearch", method = RequestMethod.GET)
    public JSONObject fulltext(@RequestParam String text,@RequestParam long current,@RequestParam long size) {
        return caseInfoService.fullSearch(text,current,size);
    }
    @RequestMapping(value = "/excel", method = RequestMethod.GET)
    public void downloadsExcelDown(HttpServletResponse response) throws IOException {
        List<CaseInfoPO> caseInfoPOS = caseInfoService.getAll();
        System.out.printf("------------" + caseInfoPOS.toString());
        HSSFWorkbook wb = new HSSFWorkbook();

        HSSFSheet sheet = wb.createSheet("病人信息表格");

        HSSFRow row = null;

        row = sheet.createRow(0);//创建第一个单元格
        row.setHeight((short) (26.25 * 20));
        row.createCell(0).setCellValue("用户信息列表");//为第一行单元格设值

        /*为标题设计空间
         * firstRow从第1行开始
         * lastRow从第0行结束
         *
         *从第1个单元格开始
         * 从第3个单元格结束
         */
        CellRangeAddress rowRegion = new CellRangeAddress(0, 0, 0, 20);
        sheet.addMergedRegion(rowRegion);

      /*CellRangeAddress columnRegion = new CellRangeAddress(1,4,0,0);
      sheet.addMergedRegion(columnRegion);*/

        row = sheet.createRow(1);
        row.setHeight((short) (22.50 * 20));//设置行高
        row.createCell(0).setCellValue("No.");//为第一个单元格设值
        row.createCell(1).setCellValue("Name");
        row.createCell(2).setCellValue("PhoneNum1");
        row.createCell(3).setCellValue("PhoneNum2");
        row.createCell(4).setCellValue("Sex");
        row.createCell(5).setCellValue("Height");
        row.createCell(6).setCellValue("Weight");
        row.createCell(7).setCellValue("IdCard");
        row.createCell(8).setCellValue("Province");
        row.createCell(9).setCellValue("City");
        row.createCell(10).setCellValue("District");
        row.createCell(11).setCellValue("Town");
        row.createCell(12).setCellValue("Street");
        row.createCell(13).setCellValue("DetailAddress");
        row.createCell(14).setCellValue("Job");
        row.createCell(15).setCellValue("Disease");
        row.createCell(16).setCellValue("Health_care");
        row.createCell(17).setCellValue("Surface_part");
        row.createCell(18).setCellValue("Surface_area");
        row.createCell(19).setCellValue("Surface_type");
        row.createCell(20).setCellValue("Metabolic_ulcer");
        row.createCell(21).setCellValue("Dm_time");
        row.createCell(22).setCellValue("Dm_used");
        row.createCell(23).setCellValue("Dm_course");
        row.createCell(24).setCellValue("Df_part");
        row.createCell(25).setCellValue("Operation_part");
        row.createCell(26).setCellValue("Amputation_part");
        row.createCell(27).setCellValue("Smoke");
        row.createCell(28).setCellValue("Drink_years");
        row.createCell(29).setCellValue("Drinking");
        row.createCell(30).setCellValue("Start_time");
        row.createCell(31).setCellValue("Before_day");
        row.createCell(32).setCellValue("Cure_way");
        row.createCell(33).setCellValue("Pathogenic_bacteria");
        row.createCell(34).setCellValue("Hosipital");
        row.createCell(35).setCellValue("Hosipital_address");
        row.createCell(36).setCellValue("Doctor");
        row.createCell(37).setCellValue("Department");
        row.createCell(38).setCellValue("In_day");
        row.createCell(39).setCellValue("Cost");
        row.createCell(40).setCellValue("Course");
        row.createCell(41).setCellValue("Cure_outcome");
        row.createCell(42).setCellValue("Operation_course");
//            row.createCell(38).setCellValue("File");
        row.createCell(43).setCellValue("SaveTime");
        row.createCell(44).setCellValue("SaveUser");
        row.createCell(45).setCellValue("IsDel");
        row.createCell(46).setCellValue("Birthday");
        row.createCell(47).setCellValue("CaseSource");
        row.createCell(48).setCellValue("Edu");
        row.createCell(49).setCellValue("Year");
        row.createCell(50).setCellValue("Bmi");
        row.createCell(51).setCellValue("Dc");
        row.createCell(52).setCellValue("Hba1c");
        row.createCell(53).setCellValue("Wagner");
        row.createCell(54).setCellValue("Wbc");
        row.createCell(55).setCellValue("Crp");
        row.createCell(56).setCellValue("Procalcitonin");
        row.createCell(57).setCellValue("Fpg");
        row.createCell(58).setCellValue("Uric_acid");
        row.createCell(59).setCellValue("Triglyceride");
        row.createCell(60).setCellValue("Total_cholesterol");
        row.createCell(61).setCellValue("Hdlc");
        row.createCell(62).setCellValue("Ldlc");
        row.createCell(63).setCellValue("Hemoglobin");
        row.createCell(64).setCellValue("Alb");
        row.createCell(65).setCellValue("Abi");
        row.createCell(66).setCellValue("Cre");
        row.createCell(67).setCellValue("Tg");
        row.createCell(68).setCellValue("Platelet");
        row.createCell(69).setCellValue("Neut");
        row.createCell(70).setCellValue("Lgt");
        row.createCell(71).setCellValue("Lat");
        row.createCell(72).setCellValue("Confidence");
        //遍历所获取的数据
        for (int i = 0; i < caseInfoPOS.size(); i++) {
            row = sheet.createRow(i + 2);
            CaseInfoPO caseInfoPO = caseInfoPOS.get(i);
            //row.createCell(0).setCellValue(caseInfoPO.getId());
            row.createCell(0).setCellValue(i+1);
            row.createCell(1).setCellValue(caseInfoPO.getName());
            row.createCell(2).setCellValue(caseInfoPO.getPhoneNum1());
            row.createCell(3).setCellValue(caseInfoPO.getPhoneNum2());
            row.createCell(4).setCellValue(caseInfoPO.getSex());
            row.createCell(5).setCellValue(caseInfoPO.getHeight());
            row.createCell(6).setCellValue(caseInfoPO.getWeight());
            row.createCell(7).setCellValue(caseInfoPO.getIdCard());
            row.createCell(8).setCellValue(caseInfoPO.getProvince());
            row.createCell(9).setCellValue(caseInfoPO.getCity());
            row.createCell(10).setCellValue(caseInfoPO.getDistrict());
            row.createCell(11).setCellValue(caseInfoPO.getTown());
            row.createCell(12).setCellValue(caseInfoPO.getStreet());
            row.createCell(13).setCellValue(caseInfoPO.getDetailAddress());
            row.createCell(14).setCellValue(caseInfoPO.getJob());
            row.createCell(15).setCellValue(caseInfoPO.getDisease());
            row.createCell(16).setCellValue(caseInfoPO.getHealthCare());
            row.createCell(17).setCellValue(caseInfoPO.getSurfacePart());
            row.createCell(18).setCellValue(caseInfoPO.getSurfaceArea());
            row.createCell(19).setCellValue(caseInfoPO.getSurfaceType());
            row.createCell(20).setCellValue(caseInfoPO.getMetabolicUlcer());
            row.createCell(21).setCellValue(caseInfoPO.getDmTime());
            row.createCell(22).setCellValue(caseInfoPO.getDmUsed());
            row.createCell(23).setCellValue(caseInfoPO.getDmCourse());
            row.createCell(24).setCellValue(caseInfoPO.getDfPart());
            row.createCell(25).setCellValue(caseInfoPO.getOperationPart());
            row.createCell(26).setCellValue(caseInfoPO.getAmputationPart());
            row.createCell(27).setCellValue(caseInfoPO.getSmoke());
            row.createCell(28).setCellValue(caseInfoPO.getDrinkYears());
            row.createCell(29).setCellValue(caseInfoPO.getDrinking());
            row.createCell(30).setCellValue(caseInfoPO.getStartTime());
            row.createCell(31).setCellValue(caseInfoPO.getBeforeDay());
            row.createCell(32).setCellValue(caseInfoPO.getCureWay());
            row.createCell(33).setCellValue(caseInfoPO.getPathogenicBacteria());
            row.createCell(34).setCellValue(caseInfoPO.getHosipital());
            row.createCell(35).setCellValue(caseInfoPO.getHosipitalAddress());
            row.createCell(36).setCellValue(caseInfoPO.getDoctor());
            row.createCell(37).setCellValue(caseInfoPO.getDepartment());
            row.createCell(38).setCellValue(caseInfoPO.getInDay());
            row.createCell(39).setCellValue(caseInfoPO.getCost());
            row.createCell(40).setCellValue(caseInfoPO.getCourse());
            row.createCell(41).setCellValue(caseInfoPO.getCureOutcome());
            row.createCell(42).setCellValue(caseInfoPO.getOperationCourse());
//            row.createCell(38).setCellValue(caseInfoPO.getFile());
            row.createCell(43).setCellValue(caseInfoPO.getSaveTime());
            row.createCell(44).setCellValue(caseInfoPO.getSaveUser());
            row.createCell(45).setCellValue(caseInfoPO.getIsDel());
            row.createCell(46).setCellValue(caseInfoPO.getBirthday());
            row.createCell(47).setCellValue(caseInfoPO.getCaseSource());
            row.createCell(48).setCellValue(caseInfoPO.getEdu());
            row.createCell(49).setCellValue(caseInfoPO.getYear());
            row.createCell(50).setCellValue(caseInfoPO.getBmi());
            row.createCell(51).setCellValue(caseInfoPO.getDc());
            row.createCell(52).setCellValue(caseInfoPO.getHba1c());
            row.createCell(53).setCellValue(caseInfoPO.getWagner());
            row.createCell(54).setCellValue(caseInfoPO.getWbc());
            row.createCell(55).setCellValue(caseInfoPO.getCrp());
            row.createCell(56).setCellValue(caseInfoPO.getProcalcitonin());
            row.createCell(57).setCellValue(caseInfoPO.getFpg());
            row.createCell(58).setCellValue(caseInfoPO.getUricAcid());
            row.createCell(59).setCellValue(caseInfoPO.getTriglyceride());
            row.createCell(60).setCellValue(caseInfoPO.getTotalCholesterol());
            row.createCell(61).setCellValue(caseInfoPO.getHdlc());
            row.createCell(62).setCellValue(caseInfoPO.getLdlc());
            row.createCell(63).setCellValue(caseInfoPO.getHemoglobin());
            row.createCell(64).setCellValue(caseInfoPO.getAlb());
            row.createCell(65).setCellValue(caseInfoPO.getAbi());
            row.createCell(66).setCellValue(caseInfoPO.getCre());
            row.createCell(67).setCellValue(caseInfoPO.getTg());
            row.createCell(68).setCellValue(caseInfoPO.getPlatelet());
            row.createCell(69).setCellValue(caseInfoPO.getNeut());
            row.createCell(70).setCellValue(caseInfoPO.getLgt());
            row.createCell(71).setCellValue(caseInfoPO.getLat());
            row.createCell(72).setCellValue(caseInfoPO.getConfidence());
        }
        sheet.setDefaultRowHeight((short) (16.5 * 20));
        //列宽自适应
        for (int i = 0; i < 72; i++) {
            sheet.autoSizeColumn(i);
        }
        response.setContentType("application/vnd.ms-excel;charset=utf-8");
        OutputStream os = response.getOutputStream();
        response.setHeader("Content-disposition", "attachment;filename=CaseInfo.xls");//默认Excel名称
        wb.write(os);
        os.flush();
        os.close();
    }

    public static void main(String[] args) throws IOException {
//        Object[] o = getLatAndLngByBaidu.getCoordinate("山东省烟台市机场路2号东方电子");
        Object[] o = getCoordinate("浙江省温州市");

        System.out.println(o[0]);//经度
        System.out.println(o[1]);//纬度
        System.out.println(o[2]);//confidence
//        String res="{\"total\":2,\"chartArray\":[{\"chartId\":0,\"xaxis\":[],\"yaxis\":[\"均值\",\"标准差\",\"最小值\",\"25分位值\",\"50分位值\",\"75分位值\",\"最大值\n" +
//                "\"],\"x\":[],\"y\":[[0.0,0.0,0.0,0.0,0.0,0.0,0.0],[0.0,0.0,0.0,0.0,0.0,0.0,0.0]]},{\"chartId\":4,\"xaxis\":[],\"yaxis\":[],\"x\":[],\"y\":[[0.0,0.0,0.0,0.0,0.0],[0.0,0.0,0.0,0.0,0.0]]}],\"info\":\"ok\"}";
////        res="{\"total\":2,\"chartArray\":[{\"chartId\":0,\"xaxis\":[],\"yaxis\":[\"均值\",\"标准差\",\"最小值\",\"25分位值\",\"50分位值\",\"75分位值\",\"最大值\n" +
////                "\"],\"x\":[],\"y\":[[0.0,0.0,0.0,0.0,0.0,0.0]]},{\"chartId\":4,\"xaxis\":[],\"yaxis\":[],\"x\":[],\"y\":[]}],\"info\":\"ok\"}";
//        System.out.println("Received response: "+res);
//
////            int total=res.getTotal();
////            String info=statsResponse.getInfo();
//        ResultVO resultVO=JSONObject.parseObject(res,ResultVO.class);
//        //List<ChartArray> array=new LinkedList<>() ;
//        if(resultVO.getInfo().equalsIgnoreCase("ok")) {
//            resultVO.setStatus(2);
//            resultVO.setCode(200);
//            List<ChartArray> chartArrays=resultVO.getChartArray();
//            String chartIds="";
//            for(ChartArray e:chartArrays){
//                int chartId=e.getChartId();
//                chartIds=chartIds+chartId+",";
//                //array.add(e);
//            }
//            if(chartIds.endsWith(","))
//                chartIds=chartIds.substring(0,chartIds.length()-1);
//            System.out.println("dsfsdfsdf"+chartIds);
//
//            resultVO.setResult(chartArrays);
//            //updateTask(metricsVO.getUid(), chartIds,res, 2);//success
//        }
//        else {
//            //updateTask(metricsVO.getUid(), "-1",res, 3);//failed
//            resultVO.setCode(500);
//        }
//        System.out.println("ffffff"+resultVO);
//        Object obj=JSONObject.toJSON(resultVO);
//        String ss= obj.toString();//jsonObject.toJSONString();
//        System.out.println("zzzz"+ss);
    }
}
