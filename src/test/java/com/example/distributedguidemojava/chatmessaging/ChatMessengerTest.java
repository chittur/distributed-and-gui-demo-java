package com.example.distributedguidemojava.chatmessaging;

import com.example.distributedguidemojava.networking.ICommunicator;
import com.example.distributedguidemojava.networking.IMessageListener;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ChatMessenger class.
 */
@ExtendWith(MockitoExtension.class)
public class ChatMessengerTest {

    private ChatMessenger chatMessenger;
    private ICommunicator mockCommunicator;
    private Consumer<String> mockCallback;
    private IMessageListener capturedListener;

    @Captor
    private ArgumentCaptor<String> messageCaptor;

    @Captor
    private ArgumentCaptor<IMessageListener> listenerCaptor;

    @BeforeEach
    public void setUp() {
        mockCommunicator = mock(ICommunicator.class);
        chatMessenger = new ChatMessenger(mockCommunicator);
        mockCallback = mock(Consumer.class);
        chatMessenger.setOnChatMessageReceived(mockCallback);
        
        // Capture the listener that gets registered
        verify(mockCommunicator).addSubscriber(eq("chat"), listenerCaptor.capture());
        capturedListener = listenerCaptor.getValue();
    }

    @Test
    public void testConstructorRegistersListener() {
        // Verify that the constructor registers a listener with the communicator
        verify(mockCommunicator).addSubscriber(eq("chat"), any(IMessageListener.class));
    }

    @Test
    public void testSetOnChatMessageReceived() {
        Consumer<String> newCallback = mock(Consumer.class);
        assertDoesNotThrow(() -> chatMessenger.setOnChatMessageReceived(newCallback));
        
        // Test that the new callback is used
        String testMessage = "Test with new callback";
        capturedListener.onMessageReceived(testMessage);
        
        verify(newCallback).accept(testMessage);
        verify(mockCallback, never()).accept(testMessage); // Old callback should not be called
    }

    @Test
    public void testSendMessage() {
        String ipAddress = "127.0.0.1";
        int port = 12345;
        String message = "Hello, World!";
        
        chatMessenger.sendMessage(ipAddress, port, message);
        
        // Verify that the communicator's sendMessage method was called with the correct parameters
        verify(mockCommunicator).sendMessage(eq(ipAddress), eq(port), eq("chat"), eq(message));
    }

    @Test
    public void testSendMultipleMessages() {
        String ipAddress1 = "192.168.1.1";
        String ipAddress2 = "10.0.0.1";
        int port1 = 8080;
        int port2 = 9090;
        String message1 = "First message";
        String message2 = "Second message";
        
        chatMessenger.sendMessage(ipAddress1, port1, message1);
        chatMessenger.sendMessage(ipAddress2, port2, message2);
        
        verify(mockCommunicator).sendMessage(eq(ipAddress1), eq(port1), eq("chat"), eq(message1));
        verify(mockCommunicator).sendMessage(eq(ipAddress2), eq(port2), eq("chat"), eq(message2));
    }

    @Test
    public void testSendEmptyMessage() {
        String ipAddress = "127.0.0.1";
        int port = 12345;
        String emptyMessage = "";
        
        chatMessenger.sendMessage(ipAddress, port, emptyMessage);
        
        verify(mockCommunicator).sendMessage(eq(ipAddress), eq(port), eq("chat"), eq(emptyMessage));
    }

    @Test
    public void testReceiveMessage() {
        String testMessage = "Received chat message";
        
        capturedListener.onMessageReceived(testMessage);
        
        verify(mockCallback).accept(testMessage);
    }

    @Test
    public void testReceiveMultipleMessages() {
        String message1 = "First received message";
        String message2 = "Second received message";
        String message3 = "Third received message";
        
        capturedListener.onMessageReceived(message1);
        capturedListener.onMessageReceived(message2);
        capturedListener.onMessageReceived(message3);
        
        verify(mockCallback).accept(message1);
        verify(mockCallback).accept(message2);
        verify(mockCallback).accept(message3);
    }

    @Test
    public void testReceiveMessageWithoutCallback() {
        // Create a fresh mock for this test
        ICommunicator freshMockCommunicator = mock(ICommunicator.class);
        
        // Create ChatMessenger without setting callback
        ChatMessenger messengerWithoutCallback = new ChatMessenger(freshMockCommunicator);
        
        // Get the listener
        ArgumentCaptor<IMessageListener> newListenerCaptor = ArgumentCaptor.forClass(IMessageListener.class);
        verify(freshMockCommunicator).addSubscriber(eq("chat"), newListenerCaptor.capture());
        IMessageListener newListener = newListenerCaptor.getValue();
        
        String testMessage = "Message without callback";
        
        // Should not throw exception when callback is null
        assertDoesNotThrow(() -> newListener.onMessageReceived(testMessage));
    }

    @Test
    public void testReceiveLongMessage() {
        StringBuilder longMessage = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            longMessage.append("This is a long message part ").append(i).append(" ");
        }
        String message = longMessage.toString();
        
        capturedListener.onMessageReceived(message);
        
        verify(mockCallback).accept(message);
    }

    @Test
    public void testReceiveMessageWithSpecialCharacters() {
        String specialMessage = "Message with special chars: @#$%^&*()_+{}|:<>?[]\\;',./";
        
        capturedListener.onMessageReceived(specialMessage);
        
        verify(mockCallback).accept(specialMessage);
    }

    @Test
    public void testReceiveUnicodeMessage() {
        String unicodeMessage = "Unicode message: 你好世界 🌍 éñoîù";
        
        capturedListener.onMessageReceived(unicodeMessage);
        
        verify(mockCallback).accept(unicodeMessage);
    }

    @Test
    public void testCallbackReplacement() {
        Consumer<String> firstCallback = mock(Consumer.class);
        Consumer<String> secondCallback = mock(Consumer.class);
        
        chatMessenger.setOnChatMessageReceived(firstCallback);
        String message1 = "Message for first callback";
        capturedListener.onMessageReceived(message1);
        
        chatMessenger.setOnChatMessageReceived(secondCallback);
        String message2 = "Message for second callback";
        capturedListener.onMessageReceived(message2);
        
        verify(firstCallback).accept(message1);
        verify(secondCallback).accept(message2);
        verify(firstCallback, never()).accept(message2); // First callback should not receive second message
    }

    @Test
    public void testNullCallbackHandling() {
        chatMessenger.setOnChatMessageReceived(null);
        String testMessage = "Message with null callback";
        
        // Should not throw exception when callback is null
        assertDoesNotThrow(() -> capturedListener.onMessageReceived(testMessage));
    }
}
