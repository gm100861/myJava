package org.linuxsogood.nio;


import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Date;
import java.util.Iterator;
import java.util.Set;

public class NIOTest implements Runnable{

    private Selector selector;
    private ServerSocketChannel servChannel;
    //用volatile修饰的变量，线程在每次使用变量的时候，都会读取变量修改后的最的值
    private volatile boolean stop;
    
    public NIOTest(Integer port){
        try {
            selector = Selector.open();
            servChannel = ServerSocketChannel.open();
            servChannel.configureBlocking(false);
            servChannel.socket().bind(new InetSocketAddress(port));
            servChannel.register(selector, SelectionKey.OP_ACCEPT);
            System.out.println("The time server is start in port : " + port);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public void stop(){
        this.stop = true;
    }
    
    public void run() {
        while(!stop){
            try {
                selector.select(1000);
                Set<SelectionKey> selectedKeys = selector.selectedKeys();
                Iterator<SelectionKey> iterator = selectedKeys.iterator();
                SelectionKey key = null;
                while(iterator.hasNext()){
                    key = iterator.next();
                    iterator.remove();
                    try {
                        handleInput(key);
                    } catch (Exception e) {
                        if(key != null){
                            key.cancel();
                            if(key.channel() != null){
                                key.channel().close();
                            }
                        }
                    }
                }
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
        if(selector != null){
            try {
                selector.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    
    private void handleInput(SelectionKey key) throws IOException{
        if(key.isValid()){
            //处理新接入的请求消息
            if(key.isAcceptable()){
                //accept the new connection
                ServerSocketChannel ssc = (ServerSocketChannel) key.channel();
                SocketChannel sc = ssc.accept();
                sc.configureBlocking(false);
                // add new connection to the selector
                sc.register(selector, SelectionKey.OP_READ);
            }
            if(key.isReadable()){
                //read the data
                SocketChannel sc = (SocketChannel) key.channel();
                ByteBuffer readBuffer = ByteBuffer.allocate(1024);
                int readBytes = sc.read(readBuffer);
                if(readBytes > 0){
                    readBuffer.flip();
                    byte[] bytes = new byte[readBuffer.remaining()];
                    readBuffer.get(bytes);
                    String body = new String(bytes,"UTF-8");
                    System.out.println("The time server receive order : " + body);
                    String currentTime = "QUERY THE ORDER".equalsIgnoreCase(body) ? new Date(System.currentTimeMillis()).toString() : "BAD ORDER";
                    doWrite(sc,currentTime);
                } else if(readBytes < 0){
                    //对端链路关闭
                    key.cancel();
                    sc.close();
                }else {
                    ;//读到0字节,忽略
                }
            }
        }
    }
    
    private void doWrite(SocketChannel channel, String response) throws IOException{
        if(response != null && response.trim().length() > 0){
            byte[] bytes = response.getBytes();
            ByteBuffer writeBuffer = ByteBuffer.allocate(bytes.length);
            writeBuffer.put(bytes);
            writeBuffer.flip();
            channel.write(writeBuffer);
        }
    }

}
