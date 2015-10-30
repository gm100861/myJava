package org.linuxsogood.netty.echoServer;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;

public class EchoClientHandler extends ChannelHandlerAdapter {

    private int counter = 0 ;
    static final String message = "Hi , Honway.Liu, Welecome to Netty's word.$_";
    
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        for( int i = 0 ; i < 10 ; i++){
            ctx.writeAndFlush(Unpooled.copiedBuffer(message.getBytes()));
        }
    }
    
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
            throws Exception {
        cause.printStackTrace();
        ctx.close();
    }
    
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg)
            throws Exception {
        String resp = (String) msg;
        System.out.println("This is " + ++counter + " times recevie netty server message [ " + resp + " ]");
    }
    
    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.flush();
    }
}
