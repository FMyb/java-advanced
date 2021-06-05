package info.kgeorgiy.ja.ilyin.hello;

import info.kgeorgiy.java.advanced.hello.HelloServer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.*;

/**
 * @author Yaroslav Ilin
 */
public class HelloUDPServer implements HelloServer {
    private DatagramSocket socket;
    private ExecutorService workers;
    private int bufferSize;

    public static void main(String[] args) {
        try (final HelloUDPServer server = new HelloUDPServer()) {
            checkArgsAndRun(args, server);
        } catch (final IOException e) {
            Logger.exception("Fail in reading input", e);
        }
    }

    public static void checkArgsAndRun(String[] args, HelloServer server) throws IOException {
        if (args == null || args.length != 2 || Arrays.stream(args).anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("Expected: port threads");
        }
        server.start(Integer.parseInt(args[0]), Integer.parseInt(args[1]));
        final BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        reader.readLine();

    }

    @Override
    public void start(int port, int threads) {
        try {
            socket = new DatagramSocket(port);
            bufferSize = socket.getReceiveBufferSize();
        } catch (SocketException e) {
            Logger.exception("Fail in starting server", e);
            return;
        }
        workers = Executors.newFixedThreadPool(threads);
        listen(threads);
    }

    private void send(Packet packet) {
        try {
            String response = "Hello, " + packet.getData();
            packet.send(response, socket);
        } catch (IOException e) {
            Logger.exception("Fail in sending response", e);
        }
    }

    private void listen(int threads) {
        for (int i = 0; i < threads; i++) {
            workers.submit(() -> {
                Packet packet = new Packet(bufferSize);
                while (!socket.isClosed() && !Thread.interrupted()) {
                    try {
                        packet.receive(socket);
                        send(packet);
                    } catch (IOException e) {
                        Logger.exception("Fail in receiving request", e);
                    }
                }
            });
        }
    }

    @Override
    public void close() {
        socket.close();
        safeShutdown(workers);
    }

    public static void safeShutdown(ExecutorService executor) {
        executor.shutdownNow();
        try {
            if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                System.err.println("Executor did not terminate in the specified time.");
                List<Runnable> droppedTasks = executor.shutdownNow();
                System.err.println("Executor was abruptly shut down. " + droppedTasks.size() + " tasks will not be executed.");
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
