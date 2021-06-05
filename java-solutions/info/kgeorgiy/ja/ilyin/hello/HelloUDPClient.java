package info.kgeorgiy.ja.ilyin.hello;

import info.kgeorgiy.java.advanced.hello.HelloClient;

import java.io.IOException;
import java.net.*;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Yaroslav Ilin
 */
public class HelloUDPClient implements HelloClient {
    private static final Pattern TRUE_RESPONSE_REGEXP = Pattern.compile("([^0-9]*)([0-9]+)([^0-9]+)([0-9]+)([^0-9]*)");

    public static void main(String[] args) {
        checkArgsAndRun(args, new HelloUDPClient());
    }

    public static void checkArgsAndRun(String[] args, HelloClient helloClient) {
        if (args == null || args.length != 5 || Arrays.stream(args).anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("Expected: host port prefix threads requests");
        }
        int port = Integer.parseInt(args[1]);
        int threads = Integer.parseInt(args[3]);
        int requests = Integer.parseInt(args[4]);
        helloClient.run(args[0], port, args[2], threads, requests);
    }

    @Override
    public void run(String host, int port, String prefix, int threads, int requests) {
        try {
            SocketAddress address = new InetSocketAddress(InetAddress.getByName(host), port);
            ExecutorService workers = Executors.newFixedThreadPool(threads);
            for (int i = 0; i < threads; i++) {
                final int ind = i;
                workers.submit(() -> sendRequest(prefix, ind, requests, address));
            }
            HelloUDPServer.safeShutdown(workers);
        } catch (UnknownHostException e) {
            Logger.exception("no IP address for the host could be found.", e);
        }
    }

    private boolean validateResponse(String response, int threadId, int requestId) {
        final Matcher matcher = TRUE_RESPONSE_REGEXP.matcher(response);
        return matcher.matches() &&
                matcher.group(2).equals(Integer.toString(threadId)) &&
                matcher.group(4).equals(Integer.toString(requestId));
    }

    private void sendRequest(String prefix, int thread, int requests, SocketAddress address) {
        try (DatagramSocket socket = new DatagramSocket()) {
            socket.setSoTimeout(100);
            Packet packet = new Packet(socket.getReceiveBufferSize(), address);

            for (int i = 0; i < requests; i++) {
                final String request = prefix + thread + "_" + i;
                Logger.request(request);
                boolean finished = false;
                while (!finished && !socket.isClosed() && !Thread.interrupted()) {
                    try {
                        packet.send(request, socket);
                    } catch (IOException e) {
                        Logger.exception("Fail in sending request", e);
                        continue;
                    }
                    try {
                        packet.receive(socket);
                        String response = packet.getData();
                        Logger.response(response);
                        finished = validateResponse(response, thread, i);
                    } catch (IOException e) {
                        Logger.exception("Fail in receiving a response", e);
                    }
                }
            }
        } catch (SocketException e) {
            Logger.exception("Fail socket " + thread, e);
        }
    }
}
