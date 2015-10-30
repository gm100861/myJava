package org.linuxsogood.netty.echoServer;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.FixedLengthFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;

public class EchoClient {

    public void connect(String host ,int port) throws InterruptedException{
        NioEventLoopGroup group = new NioEventLoopGroup();
        Bootstrap b = new Bootstrap();
        try {
            b.channel(NioSocketChannel.class)
            .group(group)
            .option(ChannelOption.SO_BACKLOG, 1024)
            .handler(new ChannelInitializer<SocketChannel>() {

                @Override
                protected void initChannel(SocketChannel ch) throws Exception {
                    ByteBuf delimiter = Unpooled.copiedBuffer("$_".getBytes());
                    //ch.pipeline().addLast(new DelimiterBasedFrameDecoder(1024, delimiter));
                    ch.pipeline().addLast(new StringDecoder());
                    ch.pipeline().addLast(new EchoClientHandler());
                }
            });
            
            ChannelFuture future = b.connect(host,port).sync();
            future.channel().closeFuture().sync();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            group.shutdownGracefully();
        }
    }
    
    public static void main(String[] args) throws InterruptedException {
        int port = 8080;
        try {
            if( args != null && args.length > 0 ){
                port = Integer.parseInt(args[0]);
            }
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
        new EchoClient().connect("127.0.0.1", port);
    }
}
