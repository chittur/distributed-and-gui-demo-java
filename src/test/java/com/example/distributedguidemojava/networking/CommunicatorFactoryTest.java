package com.example.distributedguidemojava.networking;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for CommunicatorFactory class.
 */
public class CommunicatorFactoryTest {

    @Test
    public void testCreateCommunicator() {
        ICommunicator communicator = CommunicatorFactory.createCommunicator();
        assertNotNull(communicator, "Communicator should not be null");
        assertTrue(communicator instanceof UdpCommunicator, "Communicator should be an instance of UdpCommunicator");
    }
}
