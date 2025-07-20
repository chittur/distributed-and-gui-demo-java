package com.example.distributedguidemojava.chatmessaging;

import com.example.distributedguidemojava.networking.ICommunicator;
import com.example.distributedguidemojava.networking.IMessageListener;

import java.util.function.Consumer;

/**
 * Handles sending and receiving chat messages using the networking layer.
 */
public class ChatMessenger {

    /** Identifier for chat messages. */
    private static final String CHAT_ID = "chat";
    
    /** The communicator used for network operations. */
    private final ICommunicator communicator;
    
    /** Callback invoked when a chat message is received. */
    private Consumer<String> onChatMessageReceived;

    /**
     * Constructs a ChatMessenger with the specified communicator.
     * @param communicatorInstance The communicator to use for network operations.
     */
    public ChatMessenger(final ICommunicator communicatorInstance) {
        this.communicator = communicatorInstance;
        this.communicator.addSubscriber(CHAT_ID, new IMessageListener() {
            @Override
            public void onMessageReceived(final String message) {
                if (onChatMessageReceived != null) {
                    onChatMessageReceived.accept(message);
                }
            }
        });
    }

    /**
     * Sets the callback for when a chat message is received.
     * @param callback The callback to invoke with the received message.
     */
    public void setOnChatMessageReceived(final Consumer<String> callback) {
        this.onChatMessageReceived = callback;
    }

    /**
     * Sends a chat message to the specified IP address and port.
     * @param ipAddress IP address of the destination.
     * @param port Port of the destination.
     * @param message Message to send.
     */
    public void sendMessage(final String ipAddress, final int port, final String message) {
        communicator.sendMessage(ipAddress, port, CHAT_ID, message);
    }
}
