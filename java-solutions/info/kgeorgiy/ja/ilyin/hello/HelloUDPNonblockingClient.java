package info.kgeorgiy.ja.ilyin.hello;

import info.kgeorgiy.java.advanced.hello.HelloClient;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

/**
 * @author Yaroslav Ilin
 */
public class HelloUDPNonblockingClient implements HelloClient {
    public static void main(String[] args) {
        HelloUDPClient.checkArgsAndRun(args, new HelloUDPNonblockingClient());
    }
    private static final Pattern TRUE_RESPONSE_REGEXP = Pattern.compile("([^0-9]*)([0-9]+)([^0-9]+)([0-9]+)([^0-9]*)");

    private boolean validateResponse(String response, int threadId, int requestId) {
        final Matcher matcher = TRUE_RESPONSE_REGEXP.matcher(response);
        return matcher.matches() &&
                matcher.group(2).equals(Integer.toString(threadId)) &&
                matcher.group(4).equals(Integer.toString(requestId));
    }

    @Override
    public void run(String host, int port, String prefix, int threads, int requests) {
        try {
            SocketAddress address = new InetSocketAddress(InetAddress.getByName(host), port);
            List<DatagramChannel> datagramChannels = new ArrayList<>();
            try (Selector selector = Selector.open()) {
                int fullRequestsCount = threads * requests;
                int[] requestInd = IntStream.range(0, threads).map(i -> 0).toArray();
                ByteBuffer[] buffers = new ByteBuffer[threads];
                for (int i = 0; i < threads; i++) {
                    DatagramChannel channel = DatagramChannel.open();
                    buffers[i] = ByteBuffer.allocate(channel.socket().getReceiveBufferSize());
                    channel.configureBlocking(false);
                    datagramChannels.add(channel);
                    channel.register(selector, SelectionKey.OP_READ, i);
                }
                while (fullRequestsCount > 0) {
                    fullRequestsCount = sendRequest(prefix, requests, address, selector, fullRequestsCount, requestInd, buffers);
                }
            } catch (IOException e) {
                Logger.exception("Fail in sending request", e);
            } finally {
                datagramChannels.forEach(channel -> {
                    try {
                        channel.close();
                    } catch (final IOException ignored) {
                    }
                });
            }
        } catch (UnknownHostException e) {
            Logger.exception("no IP address for the host could be found", e);
        }
    }

    private int sendRequest(String prefix, int requests, SocketAddress address, Selector selector,
                            int fullRequestsCount, int[] requestInd, ByteBuffer[] buffers) throws IOException {
        if (selector.select(100) == 0) {
            Set<SelectionKey> keys = selector.keys();
            for (SelectionKey key : keys) {
                key.interestOps(SelectionKey.OP_WRITE);
            }
        } else {
            for (final Iterator<SelectionKey> i = selector.selectedKeys().iterator(); i.hasNext(); ) {
                final SelectionKey key = i.next();
                final int thread = (int) key.attachment();
                final DatagramChannel channel = (DatagramChannel) key.channel();
                try {
                    if (key.isReadable()) {
                        final ByteBuffer dst = buffers[thread];
                        dst.clear();
                        channel.receive(dst);
                        dst.flip();

                        key.interestOps(SelectionKey.OP_WRITE);
                        if (validateResponse(
                                new String(dst.array(), dst.arrayOffset(), dst.limit(), StandardCharsets.UTF_8),
                                thread,
                                requestInd[thread])) {
                            fullRequestsCount--;
                            requestInd[thread]++;
                            if (requestInd[thread] == requests) {
                                key.cancel();
                            }
                        }
                    } else if (key.isWritable()) {
                        channel.send(ByteBuffer.wrap((prefix + thread + "_" + requestInd[thread]).getBytes(StandardCharsets.UTF_8)), address);
                        key.interestOps(SelectionKey.OP_READ);
                    }
                } finally {
                    i.remove();
                }
            }
        }
        return fullRequestsCount;
    }
}
