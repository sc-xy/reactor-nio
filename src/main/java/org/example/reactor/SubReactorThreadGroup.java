package org.example.reactor;

import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class SubReactorThreadGroup {
    private static final AtomicInteger requestCounter = new AtomicInteger(0);
    private final int ioThreadCount;
    private final int businessThreadCount;
    private static final int DEFAULT_NIO_THREAD_COUNT;
    private SubReactorThread[] subThreads;
    private ExecutorService businessExecutePool;

    static {
        DEFAULT_NIO_THREAD_COUNT = 4;
    }

    public SubReactorThreadGroup(int ioThreadCount) {
        if (ioThreadCount <= 0) {
            ioThreadCount = DEFAULT_NIO_THREAD_COUNT;
        }

        //暂时固定为10
        businessThreadCount = 10;
        businessExecutePool = Executors.newFixedThreadPool(businessThreadCount, new ThreadFactory() {
            private AtomicInteger num = new AtomicInteger(0);
            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r);
                t.setName("business-thread-" + num.incrementAndGet());
                return t;
            }
        });
        this.ioThreadCount = ioThreadCount;
        this.subThreads = new SubReactorThread[ioThreadCount];
        for(int i = 0; i < ioThreadCount; i ++ ) {
            this.subThreads[i] = new SubReactorThread(businessExecutePool);
            this.subThreads[i].start(); //构造方法中启动线程，由于nioThreads不会对外暴露，故不会引起线程逃逸
        }
        System.out.println("Nio 线程数量：" + ioThreadCount);
    }

    public void dispatch(SocketChannel socketChannel) {
        if(socketChannel != null ) {
            next().register(new NioTask(socketChannel, SelectionKey.OP_READ));
        }
    }
    protected SubReactorThread next() {
        return this.subThreads[ requestCounter.getAndIncrement() %  ioThreadCount ];
    }
}
