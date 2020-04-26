package com.example.demo.socket;


import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.java_websocket.WebSocket;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.enums.ReadyState;
import org.java_websocket.framing.Framedata;
import org.java_websocket.framing.PingFrame;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * @author liu.hongwei
 * @since 2020/4/16 19:30
 */
@Slf4j
public class MyWebSocketClient extends WebSocketClient {

    private static MyWebSocketClient connection;

    private static final String WEBSOCKET_URL = "ws://192.168.3.223:8081/v1";

    private static final Semaphore SEMAPHORE = new Semaphore(1);

    public MyWebSocketClient(URI serverUri) {
        super(serverUri);
    }

    @Override
    public void onOpen(ServerHandshake serverHandshake) {
        log.info("连接成功!!!");
        send("{\"op\":\"subscribe\",\"topic\":\"/result/face_jpeg\"}");
    }

    @Override
    public void onMessage(String s) {
        log.info("收到消息:{}", s);
    }

    @Override
    public void onClose(int i, String s, boolean b) {
        log.info("连接关闭... {}, {}", i, s);
        while (!connection.getReadyState().equals(ReadyState.OPEN)) {
            log.info("连接被关闭了, 尝试重连......");
            close(500, "manual close...");
            try {
                reconnect();
                TimeUnit.SECONDS.sleep(3);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        log.info("连接可能成功???");
    }

    @SneakyThrows
    @Override
    public void onError(Exception e) {
        log.error("异常", e);
        while (!connection.getReadyState().equals(ReadyState.OPEN)) {
            log.info("异常重连...");
            reconnect();
            TimeUnit.SECONDS.sleep(3);
        }
    }

    @SneakyThrows
    @Override
    public void reconnect() {
        if (SEMAPHORE.tryAcquire()) {
            try {
                log.info(connection.getReadyState().toString(), "..................");
                while (!connection.getReadyState().equals(ReadyState.OPEN)) {
                    log.info("连接断开,重连..............................");
                    connectToWebSocket();
                    TimeUnit.SECONDS.sleep(3);
                }
            } catch (Exception e) {
                log.error("exception ", e);
            } finally {
                TimeUnit.SECONDS.sleep(1);
                SEMAPHORE.release();
                log.info("锁释放 .................................");
            }
        } else {
            log.error("获取不到信号量, 放弃执行!!!");
        }
    }

    @Override
    public void onMessage(ByteBuffer bytes) {
        log.info("Receive message: {}", bytes);
    }

    @Override
    public void onWebsocketPong(WebSocket conn, Framedata f) {
        log.info("Receive PONG message: {}", f.toString());
    }

    @Override
    public void onWebsocketPing(WebSocket conn, Framedata f) {
        log.info("Receive PING message: {}", f.toString());
    }

    public static void connectToWebSocket() throws URISyntaxException {
        URI uri = new URI(WEBSOCKET_URL);
        connection = new MyWebSocketClient(uri);
        connection.connect();
    }

    public static void main(String[] args) throws URISyntaxException, InterruptedException {
        connectToWebSocket();
        while (true) {
            try {
                TimeUnit.SECONDS.sleep(3);
                System.out.println("连接状态:" + connection.getReadyState().toString());
                if (connection.getReadyState().equals(ReadyState.OPEN)) {
                    log.info("发送ping消息.");
                    connection.sendFrame(new PingFrame());
                }
            } catch (Exception e) {
                log.info("exception ", e);
            }
        }
    }
}
