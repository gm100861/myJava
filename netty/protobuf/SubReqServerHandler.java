package org.linuxsogood.netty.protobuf;

import org.linuxsogood.netty.protobuf.SubscribeReqProto.SubscribeReq;

import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;

public class SubReqServerHandler extends ChannelHandlerAdapter {

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg)
            throws Exception {
        SubscribeReqProto.SubscribeReq req = (SubscribeReq) msg;
        System.out.println("Server accept client subscribe req : [ " + req.toString() + " ]");
        ctx.writeAndFlush(resp(req.getSubReqID()));
    }
    
    private SubscribeRespProto.SubscribeResp resp(int subReqID) {
        SubscribeRespProto.SubscribeResp.Builder builder = SubscribeRespProto.SubscribeResp.newBuilder();
        builder.setSubReqID(subReqID);
        builder.setDesc("Netty book order succeed , 3 days later , sent to the designated address");
        builder.setRespCode(0);
        return builder.build();
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
