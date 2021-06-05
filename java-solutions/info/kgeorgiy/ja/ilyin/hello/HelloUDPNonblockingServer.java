package info.kgeorgiy.ja.ilyin.hello;

import info.kgeorgiy.java.advanced.hello.HelloServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.*;

/**
 * @author Yaroslav Ilin
 */
public class HelloUDPNonblockingServer implements HelloServer {
    private final Queue<ByteBuffer> buffers = new ConcurrentLinkedQueue<>();
    private final Queue<Packet> responds = new ConcurrentLinkedQueue<>();
    private ExecutorService workers;
    private ExecutorService listener;

    public static void main(String[] args) {
        try (final HelloUDPNonblockingServer server = new HelloUDPNonblockingServer()) {
            HelloUDPServer.checkArgsAndRun(args, server);
        } catch (final IOException e) {
            Logger.exception("Fail in reading input", e);
        }
    }

    synchronized private void check(SelectionKey key, Selector selector) {
        key.interestOps((buffers.isEmpty() ? 0 : SelectionKey.OP_READ) | (responds.isEmpty() ? 0 : SelectionKey.OP_WRITE));
        selector.wakeup();
    }

    @Override
    public void start(int port, int threads) {
        workers = Executors.newFixedThreadPool(threads);
        listener = Executors.newSingleThreadExecutor();
        Phaser phaser = new Phaser(2);
        listener.submit(() -> listen(port, threads, phaser));

        phaser.arriveAndAwaitAdvance();
    }

    private void listen(int port, int threads, Phaser phaser) {
        try (Selector selector = Selector.open()) {
            try (DatagramChannel channel = DatagramChannel.open()) {
                channel.bind(new InetSocketAddress(port));
                channel.configureBlocking(false);
                channel.register(selector, SelectionKey.OP_READ);
                int bufferSize = channel.socket().getReceiveBufferSize();
                for (int thread = 0; thread < threads; thread++) {
                    buffers.add(ByteBuffer.allocate(bufferSize));
                }
                phaser.arrive();
                while (!listener.isShutdown()) {
                    try {
                        work(selector, channel);
                    } catch (IOException ignored) {
                    }
                }
            }
        } catch (IOException ignored) {
        }
    }

    private void work(Selector selector, DatagramChannel channel) throws IOException {
        selector.select();
        for (Iterator<SelectionKey> i = selector.selectedKeys().iterator(); i.hasNext(); ) {
            SelectionKey key = i.next();
            try {
                if (key.isReadable() && key.isValid()) {
                    ByteBuffer dst = buffers.poll();
                    if (dst == null) {
                        check(key, selector);
                        continue;
                    }
                    dst.clear();
                    SocketAddress clientAddress = channel.receive(dst);
                    workers.submit(() -> {
                        dst.flip();
                        String request = new String(dst.array(), dst.arrayOffset(), dst.limit(), StandardCharsets.UTF_8);
                        responds.add(new Packet("Hello, " + request, clientAddress));
                        buffers.add(dst);
                        check(key, selector);
                    });
                }
                if (key.isWritable() && key.isValid()) {
                    Packet pair = responds.poll();
                    if (pair == null) {
                        check(key, selector);
                        continue;
                    }
                    channel.send(ByteBuffer.wrap(pair.message.getBytes(StandardCharsets.UTF_8)), pair.address);
                }
            } finally {
                i.remove();
            }
        }
    }

    @Override
    public void close() {
        HelloUDPServer.safeShutdown(workers);
        HelloUDPServer.safeShutdown(listener);
    }

    private static class Packet {
        public String message;
        public SocketAddress address;

        public Packet(String message, SocketAddress address) {
            this.message = message;
            this.address = address;
        }
    }
}
