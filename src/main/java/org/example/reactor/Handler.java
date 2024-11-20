package org.example.reactor;

import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

public class Handler implements Runnable {
    private static final byte[] b = "hello, server received your message".getBytes(); // 服务端给客户端的响应

    private SocketChannel sc;
    private ByteBuffer reqBuffer;
    private SubReactorThread parent;

    public Handler(SocketChannel sc, ByteBuffer reqBuffer,
                   SubReactorThread parent) {
        super();
        this.sc = sc;
        this.reqBuffer = reqBuffer;
        this.parent = parent;
    }

    public void run() {
        System.out.println("business handler start processing...");
        // TODO Auto-generated method stub
        //业务处理
        reqBuffer.put(b);
        parent.register(new NioTask(sc, SelectionKey.OP_WRITE, reqBuffer));
        System.out.println("business handler processing complete.");
    }
}
