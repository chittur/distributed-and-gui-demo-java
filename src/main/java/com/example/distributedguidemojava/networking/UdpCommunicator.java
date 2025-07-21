package com.example.distributedguidemojava.networking;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.net.ServerSocket;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Communicator implementation using UDP for network communication.
 */
public class UdpCommunicator implements ICommunicator {

    /** Logger for logging information and errors. */
    private static final Logger LOGGER = Logger.getLogger(UdpCommunicator.class.getName());
    /** The UDP socket used for communication. */
    private final DatagramSocket socket;
    /** The port on which the communicator listens for incoming messages. */
    private final int listenPort;
    /** The thread that listens for incoming messages. */
    private final Thread listenThread;
    /** Map of subscribers to their respective message listeners. */
    private final Map<String, IMessageListener> subscribers;

    /**
     * Constructs a UdpCommunicator, initializing the socket and starting a listener thread.
     */
    public UdpCommunicator() {
        subscribers = new HashMap<>();
        listenPort = getRandomAvailablePort();
        try {
            socket = new DatagramSocket(listenPort);
        } catch (SocketException e) {
            throw new RuntimeException("Failed to create UDP socket on port " + listenPort, e);
        }

        listenThread = new Thread(this::listenerThreadProc);
        listenThread.setDaemon(true); // Stop the thread when the application exits
        listenThread.start();
        LOGGER.log(Level.INFO, "UDP Communicator listening on port {0}", listenPort);
    }

    @Override
    public void addSubscriber(final String id, final IMessageListener subscriber) {
        if (id == null || id.isEmpty() || subscriber == null) {
            throw new IllegalArgumentException("ID and subscriber must not be null or empty");
        }
        synchronized (this) {
            subscribers.put(id, subscriber);
        }
    }

    @Override
    public void removeSubscriber(final String id) {
        if (id == null || id.isEmpty()) {
            throw new IllegalArgumentException("ID must not be null or empty");
        }
        synchronized (this) {
            subscribers.remove(id);
        }
    }

    @Override
    public void sendMessage(final String ipAddress, final int port, final String senderId, final String message) {
        try {
            final InetAddress address = InetAddress.getByName(ipAddress);
            final String payload = senderId + ":" + message;
            final byte[] sendData = payload.getBytes();
            final DatagramPacket packet = new DatagramPacket(sendData, sendData.length, address, port);
            socket.send(packet);
        } catch (UnknownHostException e) {
            LOGGER.log(Level.WARNING, "Unknown host: {0}", ipAddress);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error sending message: {0}", e.getMessage());
        }
    }

    @Override
    public int getListenPort() {
        return listenPort;
    }

    /**
     * Gets a random available port for listening.
     * @return An available port number.
     */
    private static int getRandomAvailablePort() {
        try (ServerSocket serverSocket = new ServerSocket(0)) {
            return serverSocket.getLocalPort();
        } catch (IOException e) {
            throw new RuntimeException("Failed to find an available port", e);
        }
    }

    /**
     * Listens for incoming messages on the UDP socket.
     */
    private void listenerThreadProc() {
        // Buffer size for UDP: handles image chunks + large test messages
        final int bufferSize = 65536; // 64KB buffer - handles chunks and large test cases
        final byte[] receiveData = new byte[bufferSize];
        while (true) {
            try {
                final DatagramPacket packet = new DatagramPacket(receiveData, receiveData.length);
                socket.receive(packet);
                final String payload = new String(packet.getData(), 0, packet.getLength());
                LOGGER.log(Level.FINE, "Received payload: {0}", payload);

                // Expected format: senderId:message
                final String[] tokens = payload.split(":", 2);
                if (tokens.length == 2) {
                    final String id = tokens[0];
                    final String message = tokens[1];
                    synchronized (this) {
                        final IMessageListener listener = subscribers.get(id);
                        if (listener != null) {
                            listener.onMessageReceived(message);
                        } else {
                            LOGGER.log(Level.WARNING, "Received message for unknown subscriber: {0}", id);
                        }
                    }
                }
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Error receiving message: {0}", e.getMessage());
            }
        }
    }
}
