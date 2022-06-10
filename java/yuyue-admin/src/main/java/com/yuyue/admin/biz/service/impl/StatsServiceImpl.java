//package com.yuyue.admin.biz.service.impl;
//
//import com.yuyue.admin.stats.ComputeRequest;
//import com.yuyue.admin.stats.ComputeResponse;
//import io.grpc.stub.StreamObserver;
//import org.lognet.springboot.grpc.GRpcService;
//
//@GRpcService
//public class StatsServiceImpl extends StatsServiceGrpc.StatsServiceImplBase{
//
//    @Override
//    public void executeStats(ComputeRequest request, StreamObserver<ComputeResponse> responseObserver) {
//        ComputeResponse response = ComputeResponse.newBuilder()
//                .build();
//
//        responseObserver.onNext(response);
//        responseObserver.onCompleted();
//    }
//
//}
