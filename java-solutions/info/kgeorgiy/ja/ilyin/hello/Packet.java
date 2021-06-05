package info.kgeorgiy.ja.ilyin.hello;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;

/**
 * DatagramPacket wrapper
 * @author Yaroslav Ilin
 */
public class Packet {
    private final DatagramPacket packet;
    private final byte[] buffer;

    public Packet(int bufferSize, SocketAddress address) {
        this.packet = new DatagramPacket(new byte[bufferSize], bufferSize, address);
        this.buffer = packet.getData();
    }

    public Packet(int bufferSize) {
        this.packet = new DatagramPacket(new byte[bufferSize], bufferSize);
        this.buffer = this.packet.getData();
    }

    public String getData() {
        return new String(packet.getData(), packet.getOffset(), packet.getLength(), StandardCharsets.UTF_8);
    }

    public void send(String request, DatagramSocket socket) throws IOException {
        packet.setData(request.getBytes(StandardCharsets.UTF_8));
        packet.setLength(request.length());
        socket.send(packet);
    }

    public void receive(DatagramSocket socket) throws IOException {
        packet.setData(buffer);
        packet.setLength(buffer.length);
        socket.receive(packet);
    }
}
