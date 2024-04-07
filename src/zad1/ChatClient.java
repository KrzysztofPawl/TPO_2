/**
 * @author Pawłowski Krzysztof S22381
 */

package zad1;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class ChatClient extends Thread {
    private SocketChannel channel;
    private final String host;
    private final int port;
    private final String id;
    private final List<String> logsList = new ArrayList<>();

    public ChatClient(String host, int port, String id) {
        this.host = host;
        this.port = port;
        this.id = id;
        logsList.add(String.format("\n=== %s chat view", id));
    }

    public void login() {
        try {
            channel = SocketChannel.open(new InetSocketAddress(host, port));
            channel.configureBlocking(false);
            writeContent("login\t" + id);
            Thread.sleep(50);
            start();
        } catch (IOException | InterruptedException e) {
            exceptionLogger(e);
        }
    }

    public void logout() {
        writeContent("logout\t" + id);
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            exceptionLogger(e);
        }
    }

    public void send(String message) {
        writeContent("message\t" + message);
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            exceptionLogger(e);
        }
    }

    private void writeContent(String content) {
        try {
            ByteBuffer buffer = StandardCharsets.UTF_8.encode(content + "\n");
            while (buffer.hasRemaining()) {
                channel.write(buffer);
            }
        } catch (IOException e) {
            exceptionLogger(e);
        }
    }

    @Override
    public void run() {
        ByteBuffer buffer = ByteBuffer.allocateDirect(1024);
        try {
            while (!isInterrupted()) {
                int bytesRead = channel.read(buffer);
                if (bytesRead < 0) {
                    break;
                }
                if (bytesRead > 0) {
                    buffer.flip();
                    String response = StandardCharsets.UTF_8.decode(buffer).toString().trim();
                    logsList.add(response);
                    buffer.clear();
                }
            }
        } catch (IOException e) {
            if (!isInterrupted()) {
                exceptionLogger(e);
            }
        } finally {
            if (channel != null) {
                try {
                    channel.close();
                } catch (IOException e) {
                    exceptionLogger(e);
                }
            }
        }
    }

    public String getChatView() {
        return String.join("\n", logsList);
    }

    private void exceptionLogger(Exception e) {
        logsList.add("Exception thrown: " + e.toString());
    }
}
