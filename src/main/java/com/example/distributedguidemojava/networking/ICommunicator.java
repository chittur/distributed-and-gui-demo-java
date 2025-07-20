package com.example.distributedguidemojava.networking;

/**
 * Interface for communication over the network.
 */
public interface ICommunicator {

    /**
     * Adds a subscriber to listen for messages.
     * @param id Unique identifier for the subscriber.
     * @param subscriber The subscriber to add.
     */
    void addSubscriber(String id, IMessageListener subscriber);

    /**
     * Removes a subscriber.
     * @param id Unique identifier of the subscriber to remove.
     */
    void removeSubscriber(String id);

    /**
     * Sends a message to the specified IP address and port.
     * @param ipAddress IP address of the destination.
     * @param port Port of the destination.
     * @param senderId Identifier of the sender.
     * @param message Message to send.
     */
    void sendMessage(String ipAddress, int port, String senderId, String message);

    /**
     * Gets the port on which this communicator is listening for messages.
     * @return The listening port.
     */
    int getListenPort();
}
