import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.*;

/**
This server creates a ServerSocketChannel to listen for incoming connections, and registers it with a Selector to handle concurrency. It also creates N children as specified by the command line argument, each of which connects to the server and sends random strings of length 5 every 2 seconds.

When the server receives a message from a client, it sends it to all other connected clients. It also keeps track of the number of messages received and sends a "hello" message to all clients after every 20th message. The children simply reply with
**/
public class ConcurrentServer {
    private static final int BUFFER_SIZE = 1024;
    private static final int MAX_CLIENTS = 10;
    private static final int MAX_MESSAGES = 20;
    private static final int MESSAGE_LENGTH = 5;

    public static void main(String[] args) throws IOException {
        int port = Integer.parseInt(args[0]);

        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.bind(new InetSocketAddress(port));
        serverSocketChannel.configureBlocking(false);

        Selector selector = Selector.open();
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);

        int numChildren = Integer.parseInt(args[1]);
        List<SocketChannel> childChannels = new ArrayList<>();

        for (int i = 0; i < numChildren; i++) {
            SocketChannel childChannel = SocketChannel.open(new InetSocketAddress("localhost", port));
            childChannel.configureBlocking(false);
            childChannels.add(childChannel);
        }

        ByteBuffer messageBuffer = ByteBuffer.allocate(BUFFER_SIZE);

        int messageCount = 0;
        while (true) {
            int readyChannels = selector.select();

            if (readyChannels == 0) {
                continue;
            }

            Set<SelectionKey> selectedKeys = selector.selectedKeys();
            Iterator<SelectionKey> keyIterator = selectedKeys.iterator();

            while (keyIterator.hasNext()) {
                SelectionKey key = keyIterator.next();

                if (key.isAcceptable()) {
                    SocketChannel clientChannel = serverSocketChannel.accept();
                    clientChannel.configureBlocking(false);
                    clientChannel.register(selector, SelectionKey.OP_READ);
                    System.out.println("New client connected: " + clientChannel.getRemoteAddress());
                } else if (key.isReadable()) {
                    SocketChannel clientChannel = (SocketChannel) key.channel();
                    messageBuffer.clear();

                    int bytesRead = clientChannel.read(messageBuffer);
                    if (bytesRead == -1) {
                        key.cancel();
                        clientChannel.close();
                        System.out.println("Client disconnected: " + clientChannel.getRemoteAddress());
                        continue;
                    }

                    String message = new String(messageBuffer.array()).trim();
                    System.out.println("Message received: " + message);

                    for (SocketChannel otherChannel : childChannels) {
                        if (otherChannel != clientChannel) {
                            messageBuffer.rewind();
                            messageBuffer.put(message.getBytes());
                            messageBuffer.flip();
                            otherChannel.write(messageBuffer);
                        }
                    }

                    messageCount++;
                    if (messageCount % MAX_MESSAGES == 0) {
                        messageBuffer.clear();
                        messageBuffer.put("hello".getBytes());
                        messageBuffer.flip();

                        for (SocketChannel childChannel : childChannels) {
                            childChannel.write(messageBuffer);

                            ByteBuffer replyBuffer = ByteBuffer.allocate(BUFFER_SIZE);
                            childChannel.read(replyBuffer);

                            String reply = new String(replyBuffer.array()).trim();
                            System.out.println("Child reply received: " + reply);
                        }
                    }
                }

                keyIterator.remove();
            }
        }
    }
}
