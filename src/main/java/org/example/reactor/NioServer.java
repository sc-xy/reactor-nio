package org.example.reactor;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class NioServer {
    private static final int SERVER_PORT = 9080;

    public static void main(String[] args) {
        (new Thread(new Acceptor())).start();
    }

    private static class Acceptor implements Runnable {

        // main Reactor 线程池，处理连接请求
        private static final ExecutorService mainReactor = Executors.newSingleThreadExecutor(
                new ThreadFactory() {
                    private final AtomicInteger threadNumber = new AtomicInteger(0);
                    @Override
                    public Thread newThread(Runnable r) {
                        Thread t = new Thread(r);
                        t.setName("reactor-main-" + threadNumber.getAndIncrement());
                        return t;
                    }
                }
        );

        public void run() {
            ServerSocketChannel ssc = null;
            try {
                ssc = ServerSocketChannel.open();
                ssc.configureBlocking(false);
                ssc.bind(new InetSocketAddress(SERVER_PORT));

                dispatch(ssc);
                System.out.println("Server listening on port " + SERVER_PORT);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private void dispatch(ServerSocketChannel ssc) throws IOException {
            mainReactor.submit(new MainReactor(ssc));
        }
    }
}
