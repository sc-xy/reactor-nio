package org.example.reactor;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

public class MainReactor implements Runnable {
    private Selector selector;
    private SubReactorThreadGroup subReactorThreadGroup;
    private static final int DEFAULT_IO_THREAD_COUNT = 4;
    private int ioThreadCount = DEFAULT_IO_THREAD_COUNT;

    public MainReactor(ServerSocketChannel channel) {
        try {
            selector = Selector.open();
            channel.register(selector, SelectionKey.OP_ACCEPT);
        } catch (IOException e) {
            e.printStackTrace();
        }
        subReactorThreadGroup = new SubReactorThreadGroup(ioThreadCount);
    }

    public void run() {
        System.out.println("MainReactor started");
        while (!Thread.interrupted()) {
            Set<SelectionKey> ops = null;

            try {
                selector.select(1000);
                ops = selector.selectedKeys();
            } catch (IOException e) {
                e.printStackTrace();
            }

            for (Iterator<SelectionKey> it = ops.iterator(); it.hasNext();) {
                SelectionKey key = it.next();
                it.remove();
                try {
                    if (key.isAcceptable()) {
                        System.out.println("Accept new client connection");
                        ServerSocketChannel ssc = (ServerSocketChannel) key.channel();
                        SocketChannel clientChannel = ssc.accept();
                        clientChannel.configureBlocking(false);
                        subReactorThreadGroup.dispatch(clientChannel);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    System.out.println("Accept client connection failed");
                }
            }
        }
    }
}
