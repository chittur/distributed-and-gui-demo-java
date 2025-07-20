package com.example.distributedguidemojava.networking;

/**
 * Factory class for creating communicators.
 */
public class CommunicatorFactory {

    /**
     * Creates a new communicator instance.
     * @return A new ICommunicator instance.
     */
    public static ICommunicator createCommunicator() {
        return new UdpCommunicator();
    }
}
