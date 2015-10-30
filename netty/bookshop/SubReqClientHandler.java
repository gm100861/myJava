package org.linuxsogood.netty.shop;

import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;

public class SubReqClientHandler extends ChannelHandlerAdapter {

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        for ( int i = 0 ; i < 10 ; i++ ){
            ctx.write(subReq(i));
        }
        ctx.flush();
    }
    
    private SubscribeReq subReq(int i ){
        SubscribeReq req = new SubscribeReq();
        req.setAddress("上海市柳州路399号甲G层");
        req.setPhoneNumber("18000009999");
        req.setProductName("Netty权威指南");
        req.setSubReqID(i);
        req.setUsername("honway");
        return req;
    }
    
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg)
            throws Exception {
        System.out.println("Recive server response : [ " + msg + " ]");
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
