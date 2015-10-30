package org.linuxsogood.netty.shop;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

public class SubReqServer {

    public void bind(int port) throws InterruptedException{
        NioEventLoopGroup bossGroup = new NioEventLoopGroup();
        NioEventLoopGroup workerGroup = new NioEventLoopGroup();
        
        ServerBootstrap b = new ServerBootstrap();
        b.group(bossGroup,workerGroup)
        .channel(NioServerSocketChannel.class)
        .option(ChannelOption.SO_BACKLOG, 100)
        .handler(new LoggingHandler(LogLevel.DEBUG))
        .childHandler(new ChannelInitializer<SocketChannel>() {

            @Override
            protected void initChannel(SocketChannel ch) throws Exception {
                ch.pipeline().addLast(new ObjectDecoder(1024*1024,ClassResolvers.weakCachingConcurrentResolver(this.getClass().getClassLoader())));
                ch.pipeline().addLast(new ObjectEncoder());
                ch.pipeline().addLast(new SubReqServerHandler());
            }
        });
        
        ChannelFuture future = b.bind(port).sync();
        
        future.channel().closeFuture();
    }
    
    public static void main(String[] args) throws InterruptedException {
        int port = 8080;
        if(args != null && args.length > 0){
            port = Integer.parseInt(args[0]);
        }
        
        new SubReqServer().bind(port);
        
    }
}
