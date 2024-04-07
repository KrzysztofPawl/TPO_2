/**
 * @author Pawłowski Krzysztof S22381
 */

package zad1;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChatServer {
    private final String host;
    private final int port;
    private Selector selector;
    private ServerSocketChannel serverChannel;
    private final List<String> logsList = Collections.synchronizedList(new ArrayList<>());
    private final Map<SocketChannel, String> clientsMap = new ConcurrentHashMap<>();
    private volatile boolean running = true;

    public ChatServer(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public void startServer() throws IOException {
        this.serverChannel = ServerSocketChannel.open();
        this.serverChannel.bind(new InetSocketAddress(this.host, this.port));
        this.serverChannel.configureBlocking(false);

        this.selector = Selector.open();
        this.serverChannel.register(this.selector, SelectionKey.OP_ACCEPT);

        new Thread(this::runServer).start();
        System.out.println("\nServer started");
    }

    public void stopServer() {
        running = false;
        try {
            selector.wakeup();
            selector.close();
            serverChannel.close();
            System.out.println("\nServer stopped");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void runServer() {
        while (running) {
            try {
                selector.select();
                if (!selector.isOpen()) {
                    break;
                }

                Set<SelectionKey> keys = selector.selectedKeys();
                Iterator<SelectionKey> iter = keys.iterator();

                while (iter.hasNext()) {
                    SelectionKey key = iter.next();
                    iter.remove();

                    if (key.isAcceptable()) {
                        handleAccept();
                    } else if (key.isReadable()) {
                        handleRead((SocketChannel) key.channel());
                    }
                }
            } catch (IOException e) {
                if (running) {
                    e.printStackTrace();
                }
            }
        }
        cleanupResources();
    }


    private void cleanupResources() {
        try {
            selector.close();
            serverChannel.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleAccept() throws IOException {
        SocketChannel clientChannel = serverChannel.accept();
        clientChannel.configureBlocking(false);
        clientChannel.register(selector, SelectionKey.OP_READ);
    }

    private void handleRead(SocketChannel clientChannel) throws IOException {
        String request = readSocket(clientChannel);
        Matcher matcher = Pattern.compile("([^\\t]+)\\t(.+)").matcher(request);

        if (!matcher.find()) {
            return;
        }

        String event = matcher.group(1);
        String message = matcher.group(2);

        processEvent(clientChannel, event, message);
    }

    private String readSocket(SocketChannel socketChannel) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        int bytesRead = socketChannel.read(buffer);
        if (bytesRead <= 0) {
            return "";
        }

        buffer.flip();
        return StandardCharsets.UTF_8.decode(buffer).toString().trim();
    }

    private void processEvent(SocketChannel clientChannel, String action, String message) throws IOException {
        String clientId = clientsMap.getOrDefault(clientChannel, "Unknown");
        String logMessage;

        switch (action) {
            case "login":
                clientsMap.put(clientChannel, message);
                clientId = message;
                logMessage = clientId + " logged in";
                break;
            case "logout":
                clientsMap.remove(clientChannel);
                logMessage = clientId + " logged out";
                break;
            case "message":
                logMessage = clientId + ": " + message;
                break;
            default:
                logMessage = "Unknown action";
        }

        logEvent(logMessage);
        send(logMessage);
    }

    private void logEvent(String message) {
        String timestamp = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS"));
        logsList.add(timestamp + " " + message);
    }

    private void send(String message) throws IOException {
        ByteBuffer buffer = StandardCharsets.UTF_8.encode(message + "\n");
        for (SocketChannel clientSocket : clientsMap.keySet()) {
            while (buffer.hasRemaining()) {
                clientSocket.write(buffer);
            }
            buffer.rewind();
        }
    }

    public String getServerLog() {
        return String.join("\n", logsList);
    }
}
