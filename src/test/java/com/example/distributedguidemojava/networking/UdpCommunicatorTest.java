package com.example.distributedguidemojava.networking;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for UdpCommunicator class.
 */
public class UdpCommunicatorTest {

    private UdpCommunicator communicator;
    private UdpCommunicator receiverCommunicator;
    private IMessageListener mockListener;

    @Captor
    private ArgumentCaptor<String> messageCaptor;

    @BeforeEach
    public void setUp() {
        communicator = new UdpCommunicator();
        mockListener = mock(IMessageListener.class);
    }

    @AfterEach
    public void tearDown() {
        // Allow some time for cleanup
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Test
    public void testAddSubscriber() {
        String subscriberId = "testSubscriber";
        assertDoesNotThrow(() -> communicator.addSubscriber(subscriberId, mockListener));
        
        // Test adding multiple subscribers
        IMessageListener mockListener2 = mock(IMessageListener.class);
        assertDoesNotThrow(() -> communicator.addSubscriber("subscriber2", mockListener2));
    }

    @Test
    public void testAddSubscriberNullId() {
        assertThrows(IllegalArgumentException.class, 
            () -> communicator.addSubscriber(null, mockListener));
    }

    @Test
    public void testAddSubscriberEmptyId() {
        assertThrows(IllegalArgumentException.class, 
            () -> communicator.addSubscriber("", mockListener));
    }

    @Test
    public void testAddSubscriberNullListener() {
        assertThrows(IllegalArgumentException.class, 
            () -> communicator.addSubscriber("testId", null));
    }

    @Test
    public void testRemoveSubscriber() {
        String subscriberId = "testSubscriber";
        communicator.addSubscriber(subscriberId, mockListener);
        assertDoesNotThrow(() -> communicator.removeSubscriber(subscriberId));
    }

    @Test
    public void testRemoveSubscriberNullId() {
        assertThrows(IllegalArgumentException.class, 
            () -> communicator.removeSubscriber(null));
    }

    @Test
    public void testRemoveSubscriberEmptyId() {
        assertThrows(IllegalArgumentException.class, 
            () -> communicator.removeSubscriber(""));
    }

    @Test
    public void testRemoveNonExistentSubscriber() {
        assertDoesNotThrow(() -> communicator.removeSubscriber("nonExistent"));
    }

    @Test
    public void testGetListenPort() {
        int port = communicator.getListenPort();
        assertTrue(port > 0, "Listen port should be greater than 0");
        assertTrue(port <= 65535, "Listen port should be valid");
    }

    @Test
    public void testSendMessageToLocalhost() throws InterruptedException {
        receiverCommunicator = new UdpCommunicator();
        CountDownLatch latch = new CountDownLatch(1);
        String expectedMessage = "Hello, World!";
        String senderId = "testSender";
        
        receiverCommunicator.addSubscriber(senderId, message -> {
            assertEquals(expectedMessage, message);
            latch.countDown();
        });
        
        communicator.sendMessage("127.0.0.1", receiverCommunicator.getListenPort(), senderId, expectedMessage);
        
        assertTrue(latch.await(2, TimeUnit.SECONDS), "Message should be received within 2 seconds");
    }

    @Test
    public void testSendMessageToInvalidHost() {
        // Should not throw exception, but should handle UnknownHostException internally
        assertDoesNotThrow(() -> 
            communicator.sendMessage("invalid.host.name.that.does.not.exist", 12345, "test", "message"));
    }

    @Test
    public void testSendMessageToInvalidPort() {
        // Should not throw exception, but should handle IOException internally
        assertDoesNotThrow(() -> 
            communicator.sendMessage("127.0.0.1", 0, "test", "message"));
    }

    @Test
    public void testMultipleMessageTypes() throws InterruptedException {
        receiverCommunicator = new UdpCommunicator();
        CountDownLatch latch = new CountDownLatch(2);
        
        IMessageListener chatListener = mock(IMessageListener.class);
        IMessageListener imageListener = mock(IMessageListener.class);
        
        doAnswer(invocation -> {
            latch.countDown();
            return null;
        }).when(chatListener).onMessageReceived(anyString());
        
        doAnswer(invocation -> {
            latch.countDown();
            return null;
        }).when(imageListener).onMessageReceived(anyString());
        
        receiverCommunicator.addSubscriber("chat", chatListener);
        receiverCommunicator.addSubscriber("image", imageListener);
        
        communicator.sendMessage("127.0.0.1", receiverCommunicator.getListenPort(), "chat", "Hello from chat");
        communicator.sendMessage("127.0.0.1", receiverCommunicator.getListenPort(), "image", "Image data");
        
        assertTrue(latch.await(3, TimeUnit.SECONDS), "Both messages should be received");
        
        verify(chatListener).onMessageReceived("Hello from chat");
        verify(imageListener).onMessageReceived("Image data");
    }

    @Test
    public void testMessageToUnknownSubscriber() throws InterruptedException {
        receiverCommunicator = new UdpCommunicator();
        
        // Add a small delay to ensure the receiver is ready
        Thread.sleep(100);
        
        // Send message to unknown subscriber - should not cause any errors but won't be delivered
        assertDoesNotThrow(() -> 
            communicator.sendMessage("127.0.0.1", receiverCommunicator.getListenPort(), "unknown", "message"));
        
        // Give some time for message to be processed
        Thread.sleep(200);
    }

    @Test
    public void testLargeMessage() throws InterruptedException {
        receiverCommunicator = new UdpCommunicator();
        CountDownLatch latch = new CountDownLatch(1);
        
        StringBuilder largeMessage = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            largeMessage.append("This is a large message part ").append(i).append(" ");
        }
        
        String expectedMessage = largeMessage.toString();
        String senderId = "largeSender";
        
        receiverCommunicator.addSubscriber(senderId, message -> {
            assertEquals(expectedMessage, message);
            latch.countDown();
        });
        
        communicator.sendMessage("127.0.0.1", receiverCommunicator.getListenPort(), senderId, expectedMessage);
        
        assertTrue(latch.await(3, TimeUnit.SECONDS), "Large message should be received");
    }

    @Test
    public void testMessageWithColons() throws InterruptedException {
        receiverCommunicator = new UdpCommunicator();
        CountDownLatch latch = new CountDownLatch(1);
        String messageWithColons = "This:message:has:many:colons:in:it";
        String senderId = "colonSender";
        
        receiverCommunicator.addSubscriber(senderId, message -> {
            assertEquals(messageWithColons, message);
            latch.countDown();
        });
        
        communicator.sendMessage("127.0.0.1", receiverCommunicator.getListenPort(), senderId, messageWithColons);
        
        assertTrue(latch.await(2, TimeUnit.SECONDS), "Message with colons should be received correctly");
    }

    @Test
    public void testEmptyMessage() throws InterruptedException {
        receiverCommunicator = new UdpCommunicator();
        CountDownLatch latch = new CountDownLatch(1);
        String emptyMessage = "";
        String senderId = "emptySender";
        
        receiverCommunicator.addSubscriber(senderId, message -> {
            assertEquals(emptyMessage, message);
            latch.countDown();
        });
        
        communicator.sendMessage("127.0.0.1", receiverCommunicator.getListenPort(), senderId, emptyMessage);
        
        assertTrue(latch.await(2, TimeUnit.SECONDS), "Empty message should be received");
    }

    @Test
    public void testReplaceSubscriber() throws InterruptedException {
        receiverCommunicator = new UdpCommunicator();
        CountDownLatch latch = new CountDownLatch(1);
        String senderId = "replaceSender";
        
        IMessageListener oldListener = mock(IMessageListener.class);
        IMessageListener newListener = message -> {
            assertEquals("test message", message);
            latch.countDown();
        };
        
        receiverCommunicator.addSubscriber(senderId, oldListener);
        receiverCommunicator.addSubscriber(senderId, newListener); // Replace the old listener
        
        communicator.sendMessage("127.0.0.1", receiverCommunicator.getListenPort(), senderId, "test message");
        
        assertTrue(latch.await(2, TimeUnit.SECONDS), "Message should be received by new listener");
        verify(oldListener, never()).onMessageReceived(anyString());
    }
}
