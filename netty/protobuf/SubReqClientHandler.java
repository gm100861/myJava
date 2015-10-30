package org.linuxsogood.netty.protobuf;

import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;

import java.util.ArrayList;

public class SubReqClientHandler extends ChannelHandlerAdapter {

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        for ( int i = 0 ; i < 10 ; i++ ){
            ctx.write(subReq(i));
        }
        ctx.flush();
    }

    
    private SubscribeReqProto.SubscribeReq subReq(int i) {
        SubscribeReqProto.SubscribeReq.Builder builder = SubscribeReqProto.SubscribeReq.newBuilder();
        builder.setProductName("Netty book for protobuf");
        builder.setSubReqID(i);
        builder.setUserName("honway.liu");
        ArrayList<String> address = new ArrayList<String>();
        address.add("NanJing YuHuaTai");
        address.add("ShangHai People's square");
        address.add("Tokoy Hot");
        builder.addAllAddress(address);
        return builder.build();
    }
    
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg)
            throws Exception {
        System.out.println("Recive server response : { " + msg + " }");
    }
    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.flush();
    }
    
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
            throws Exception {
        cause.printStackTrace();
        ctx.close();
    }
}
