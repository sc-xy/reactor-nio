package org.example.reactor;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.locks.ReentrantLock;

public class SubReactorThread extends Thread {
    private Selector selector;
    private ExecutorService businessExecutorPool;
    private List<NioTask> taskList = new ArrayList<NioTask>(512);
    private ReentrantLock taskMainLock = new ReentrantLock();

    public SubReactorThread(ExecutorService businessExecutorPool) {
        try {
            this.businessExecutorPool = businessExecutorPool;
            this.selector = Selector.open();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void register(NioTask task) {
        if (task != null) {
            try {
                taskMainLock.lock();
                taskList.add(task);
            } finally {
                taskMainLock.unlock();
            }
        }
    }

    private void dispatch(SocketChannel sc, ByteBuffer reqBuffer) {
        businessExecutorPool.submit( new Handler(sc, reqBuffer, this)  );
    }

    public void run() {
        while (!Thread.interrupted()) {
            Set<SelectionKey> ops = null;
            try {
                selector.select(1000);
                ops = selector.selectedKeys();
            } catch (IOException e) {
                e.printStackTrace();
                continue;
            }

            for (Iterator<SelectionKey> it = ops.iterator(); it.hasNext(); ) {
                SelectionKey key = it.next();
                it.remove();
                try {
                    if (key.isWritable()) {
                        SocketChannel clientChannel = (SocketChannel) key.channel();
                        ByteBuffer buf = (ByteBuffer) key.attachment();
                        clientChannel.write(buf);

                        clientChannel.register(selector, SelectionKey.OP_READ);
                    } else if (key.isReadable()) {
                        SocketChannel clientChannel = (SocketChannel) key.channel();
                        ByteBuffer buf = ByteBuffer.allocate(1024);

                        try {
                            int rc = clientChannel.read(buf);
                            System.out.println("Server received: " + rc);
                            dispatch(clientChannel, buf);
                        } catch (IOException e) {
                            key.cancel();
                            clientChannel.close();
                            System.out.println("client channel closed");
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    System.out.println("client channel closed");
                }
            }

            if (!taskList.isEmpty()) {
                try {
                    taskMainLock.lock();
                    for (Iterator<NioTask> it = taskList.iterator(); it.hasNext();) {
                        NioTask task = it.next();
                        try {
                            SocketChannel sc = task.getSc();
                            if (task.getData() != null) {
                                ByteBuffer buf = (ByteBuffer) task.getData();
                                buf.flip();
                                int wc = sc.write(buf);
                                System.out.println("send data to client");
                                if (wc < 1 && buf.hasRemaining()) {
                                    sc.register(selector, task.getOp(), task.getData());
                                    continue;
                                }
                            } else {
                                sc.register(selector, task.getOp());
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        it.remove();
                    }
                } finally {
                    taskMainLock.unlock();
                }
            }
        }
    }
}
