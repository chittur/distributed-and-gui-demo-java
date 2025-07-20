package com.example.distributedguidemojava.networking;

/**
 * Interface for listening to received messages.
 */
public interface IMessageListener {

    /**
     * Called when a message is received.
     * @param message The received message.
     */
    void onMessageReceived(String message);
}
