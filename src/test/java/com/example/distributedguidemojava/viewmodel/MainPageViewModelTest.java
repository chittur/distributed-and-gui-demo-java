package com.example.distributedguidemojava.viewmodel;

import com.example.distributedguidemojava.networking.ICommunicator;
import com.example.distributedguidemojava.networking.IMessageListener;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.StringProperty;
import javafx.embed.swing.JFXPanel;
import javafx.scene.image.Image;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Base64;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for MainPageViewModel class.
 */
@ExtendWith(MockitoExtension.class)
public class MainPageViewModelTest {

    private MainPageViewModel viewModel;
    private ICommunicator mockCommunicator;
    private IMessageListener chatListener;
    private IMessageListener imageListener;

    @Captor
    private ArgumentCaptor<String> messageCaptor;

    @Captor
    private ArgumentCaptor<IMessageListener> listenerCaptor;

    @BeforeAll
    public static void initializeJavaFX() {
        // Initialize JavaFX toolkit for testing
        new JFXPanel();
    }

    @BeforeEach
    public void setUp() {
        mockCommunicator = mock(ICommunicator.class);
        when(mockCommunicator.getListenPort()).thenReturn(12345);
        viewModel = new MainPageViewModel(mockCommunicator);
        
        // Capture the listeners that get registered
        verify(mockCommunicator, times(2)).addSubscriber(anyString(), listenerCaptor.capture());
        chatListener = listenerCaptor.getAllValues().get(0);
        imageListener = listenerCaptor.getAllValues().get(1);
    }

    @Test
    public void testConstructorWithNullCommunicator() {
        // Test that viewModel can be created with null communicator (uses factory)
        assertDoesNotThrow(() -> new MainPageViewModel(null));
    }

    @Test
    public void testReceivePortProperty() {
        StringProperty receivePortProperty = viewModel.receivePortProperty();
        assertNotNull(receivePortProperty, "Receive port property should not be null");
        assertEquals("12345", receivePortProperty.get(), "Receive port should match the mocked port");
    }

    @Test
    public void testReceivedMessageProperty() {
        StringProperty receivedMessageProperty = viewModel.receivedMessageProperty();
        assertNotNull(receivedMessageProperty, "Received message property should not be null");
        assertNull(receivedMessageProperty.get(), "Initial received message should be null");
    }

    @Test
    public void testReceivedImageProperty() {
        ObjectProperty<Image> receivedImageProperty = viewModel.receivedImageProperty();
        assertNotNull(receivedImageProperty, "Received image property should not be null");
        assertNull(receivedImageProperty.get(), "Initial received image should be null");
    }

    @Test
    public void testSendChatMessage() {
        String ipAddress = "127.0.0.1";
        int port = 54321;
        String message = "Test chat message";
        
        assertDoesNotThrow(() -> viewModel.sendChatMessage(ipAddress, port, message));
        
        // Verify that the communicator was called to send a chat message
        verify(mockCommunicator, atLeastOnce()).sendMessage(eq(ipAddress), eq(port), eq("chat"), eq(message));
    }

    @Test
    public void testSendImageMessage() {
        String ipAddress = "127.0.0.1";
        int port = 54321;
        String imagePath = "test/image/path.jpg";
        
        assertDoesNotThrow(() -> viewModel.sendImageMessage(ipAddress, port, imagePath));
        
        // Verify that the communicator was called to send an image message (dummy data)
        verify(mockCommunicator, atLeastOnce()).sendMessage(eq(ipAddress), eq(port), eq("image"), anyString());
    }

    @Test
    public void testChatMessageReceived() {
        String testMessage = "Hello from chat!";
        
        // Test that the listener doesn't throw exceptions
        assertDoesNotThrow(() -> chatListener.onMessageReceived(testMessage));
    }

    @Test
    public void testImageMessageReceived() {
        // Create a simple base64 encoded image (1x1 pixel PNG)
        String base64Image = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8/5+hHgAHggJ/PchI7wAAAABJRU5ErkJggg==";
        
        // Test that the listener doesn't throw exceptions
        assertDoesNotThrow(() -> imageListener.onMessageReceived(base64Image));
    }

    @Test
    public void testImageMessageReceivedWithDataURI() {
        // Create a base64 image with data URI prefix
        String base64WithPrefix = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8/5+hHgAHggJ/PchI7wAAAABJRU5ErkJggg==";
        
        assertDoesNotThrow(() -> imageListener.onMessageReceived(base64WithPrefix));
    }

    @Test
    public void testImageMessageReceivedInvalidBase64() {
        String invalidBase64 = "invalid-base64-data";
        
        // Should handle invalid base64 gracefully
        assertDoesNotThrow(() -> imageListener.onMessageReceived(invalidBase64));
    }

    @Test
    public void testMultipleChatMessages() {
        String message1 = "First message";
        String message2 = "Second message";
        
        assertDoesNotThrow(() -> {
            chatListener.onMessageReceived(message1);
            chatListener.onMessageReceived(message2);
        });
    }

    @Test
    public void testSendMultipleMessages() {
        String ipAddress1 = "192.168.1.1";
        String ipAddress2 = "192.168.1.2";
        int port1 = 8080;
        int port2 = 9090;
        String chatMessage = "Chat test";
        String imagePath = "test/image/path.jpg";
        
        viewModel.sendChatMessage(ipAddress1, port1, chatMessage);
        viewModel.sendImageMessage(ipAddress2, port2, imagePath);
        
        verify(mockCommunicator).sendMessage(eq(ipAddress1), eq(port1), eq("chat"), eq(chatMessage));
        verify(mockCommunicator, atLeastOnce()).sendMessage(eq(ipAddress2), eq(port2), eq("image"), anyString());
    }

    @Test
    public void testEmptyMessages() {
        String ipAddress = "127.0.0.1";
        int port = 8080;
        
        assertDoesNotThrow(() -> viewModel.sendChatMessage(ipAddress, port, ""));
        assertDoesNotThrow(() -> viewModel.sendImageMessage(ipAddress, port, ""));
        
        verify(mockCommunicator).sendMessage(eq(ipAddress), eq(port), eq("chat"), eq(""));
        // Empty image path should not send a message (file doesn't exist and doesn't match test pattern)
        verify(mockCommunicator, never()).sendMessage(eq(ipAddress), eq(port), eq("image"), anyString());
    }

    @Test
    public void testPropertyBinding() {
        // Test that properties are properly bound and can be observed
        StringProperty receivePort = viewModel.receivePortProperty();
        StringProperty receivedMessage = viewModel.receivedMessageProperty();
        ObjectProperty<Image> receivedImage = viewModel.receivedImageProperty();
        
        assertNotNull(receivePort);
        assertNotNull(receivedMessage);
        assertNotNull(receivedImage);
        
        // Properties should be observable
        assertTrue(receivePort.toString().contains("StringProperty"));
        assertTrue(receivedMessage.toString().contains("StringProperty"));
        assertTrue(receivedImage.toString().contains("ObjectProperty"));
    }
}
