package org.linuxsogood.netty.protobuf;

import java.util.ArrayList;
import java.util.List;

import org.linuxsogood.netty.protobuf.SubscribeReqProto.SubscribeReq;

import com.google.protobuf.InvalidProtocolBufferException;

public class TestSubscribeReqProto {

    private static byte[] encode(SubscribeReqProto.SubscribeReq req){
        return req.toByteArray();
    }
    
    private static SubscribeReqProto.SubscribeReq decode(byte[] body) throws InvalidProtocolBufferException{
        return SubscribeReqProto.SubscribeReq.parseFrom(body);
    }
    
    private static SubscribeReqProto.SubscribeReq createSubscribeReq(){
        SubscribeReqProto.SubscribeReq.Builder builder = SubscribeReqProto.SubscribeReq.newBuilder();
        builder.setSubReqID(1);
        builder.setUserName("honway");
        builder.setProductName("Netty Book");
        List<String> address = new ArrayList<String>();
        address.add("Shanghai redhat");
        address.add("Beijing tiananmen");
        address.add("ShenZhen HongShuLin");
        builder.addAllAddress(address);
        return builder.build();
    }
    
    public static void main(String[] args) throws InvalidProtocolBufferException {
        SubscribeReq req = createSubscribeReq();
        System.out.println("Before encode : " + req.toString());
        SubscribeReq req2 = decode(encode(req));
        System.out.println("After decode : " + req.toString());
        System.out.println("Assert equal : --> " + req2.equals(req));
        
    }
}
