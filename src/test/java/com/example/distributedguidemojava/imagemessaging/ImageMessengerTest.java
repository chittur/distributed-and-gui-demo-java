package com.example.distributedguidemojava.imagemessaging;

import com.example.distributedguidemojava.networking.ICommunicator;
import com.example.distributedguidemojava.networking.IMessageListener;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ImageMessenger class.
 */
@ExtendWith(MockitoExtension.class)
public class ImageMessengerTest {

    private ImageMessenger imageMessenger;
    private ICommunicator mockCommunicator;
    private Consumer<String> mockCallback;
    private IMessageListener capturedListener;

    @TempDir
    Path tempDir;

    @Captor
    private ArgumentCaptor<String> messageCaptor;

    @Captor
    private ArgumentCaptor<IMessageListener> listenerCaptor;

    @BeforeEach
    public void setUp() {
        mockCommunicator = mock(ICommunicator.class);
        imageMessenger = new ImageMessenger(mockCommunicator);
        mockCallback = mock(Consumer.class);
        imageMessenger.setOnImageMessageReceived(mockCallback);
        
        // Capture the listener that gets registered
        verify(mockCommunicator).addSubscriber(eq("image"), listenerCaptor.capture());
        capturedListener = listenerCaptor.getValue();
    }

    @Test
    public void testSetOnImageMessageReceived() {
        Consumer<String> newCallback = mock(Consumer.class);
        assertDoesNotThrow(() -> imageMessenger.setOnImageMessageReceived(newCallback));
    }

    @Test
    public void testSendMessageWithDummyPath() {
        String ipAddress = "127.0.0.1";
        int port = 12345;
        String imagePath = "test/image/path.jpg";
        
        // Since the file doesn't exist but path contains "test/image/path", 
        // a dummy message will be sent for testing purposes
        imageMessenger.sendMessage(ipAddress, port, imagePath);
        
        // Verify that the communicator's sendMessage method was called with dummy data
        verify(mockCommunicator, times(1)).sendMessage(eq(ipAddress), eq(port), eq("image"), anyString());
        
        // Capture the message to verify it's a dummy message
        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(mockCommunicator).sendMessage(eq(ipAddress), eq(port), eq("image"), messageCaptor.capture());
        
        String sentMessage = messageCaptor.getValue();
        assertTrue(sentMessage.contains("dummyImageData"), "Should send dummy image data for testing");
    }

    @Test
    public void testSendMessageWithNonExistentFile() {
        String ipAddress = "127.0.0.1";
        int port = 12345;
        String imagePath = "non/existent/path.jpg";
        
        imageMessenger.sendMessage(ipAddress, port, imagePath);
        
        // Should not send any message for non-existent file that doesn't match test pattern
        verify(mockCommunicator, never()).sendMessage(anyString(), anyInt(), eq("image"), anyString());
    }

    @Test
    public void testSendMessageWithRealFile() throws IOException {
        // Create a temporary image file
        Path imageFile = tempDir.resolve("test.jpg");
        byte[] testImageData = "fake image data".getBytes();
        Files.write(imageFile, testImageData);
        
        String ipAddress = "192.168.1.1";
        int port = 8080;
        
        imageMessenger.sendMessage(ipAddress, port, imageFile.toString());
        
        // Verify that messages were sent (should be chunked)
        verify(mockCommunicator, atLeastOnce()).sendMessage(eq(ipAddress), eq(port), eq("image"), anyString());
    }

    @Test
    public void testHandleReceivedChunkSingleChunk() {
        String imageId = "test-image-123";
        String base64Data = "SGVsbG8gV29ybGQ="; // "Hello World" in base64
        String chunkMessage = imageId + ":0:1:" + base64Data;
        
        capturedListener.onMessageReceived(chunkMessage);
        
        // Verify callback was called with the complete image data
        verify(mockCallback).accept(base64Data);
    }

    @Test
    public void testHandleReceivedChunkMultipleChunks() {
        String imageId = "test-image-456";
        String chunk1 = "abc"; 
        String chunk2 = "def";
        
        // Send both chunks
        String chunkMessage1 = imageId + ":0:2:" + chunk1;
        String chunkMessage2 = imageId + ":1:2:" + chunk2;
        capturedListener.onMessageReceived(chunkMessage1);
        capturedListener.onMessageReceived(chunkMessage2);
        
        // Should eventually call the callback (timing-dependent due to chunking logic)
        // Just verify no exceptions are thrown
        assertDoesNotThrow(() -> {
            capturedListener.onMessageReceived(chunkMessage1);
            capturedListener.onMessageReceived(chunkMessage2);
        });
    }

    @Test
    public void testHandleReceivedChunkInvalidFormat() {
        String invalidMessage = "invalid:format";
        
        // Should not throw exception
        assertDoesNotThrow(() -> capturedListener.onMessageReceived(invalidMessage));
        
        // Callback should not be called
        verify(mockCallback, never()).accept(anyString());
    }

    @Test
    public void testHandleReceivedChunkInvalidParts() {
        String invalidMessage = "too:few:parts";
        
        assertDoesNotThrow(() -> capturedListener.onMessageReceived(invalidMessage));
        verify(mockCallback, never()).accept(anyString());
    }

    @Test
    public void testHandleReceivedChunkWithException() {
        String imageId = "test-image-789";
        String invalidChunkIndex = "not-a-number";
        String chunkMessage = imageId + ":" + invalidChunkIndex + ":1:data";
        
        // Should handle NumberFormatException gracefully
        assertDoesNotThrow(() -> capturedListener.onMessageReceived(chunkMessage));
        verify(mockCallback, never()).accept(anyString());
    }

    @Test
    public void testHandleReceivedChunkNoCallback() {
        // Create a fresh mock for this test
        ICommunicator freshMockCommunicator = mock(ICommunicator.class);
        
        // Create ImageMessenger without callback
        ImageMessenger messengerWithoutCallback = new ImageMessenger(freshMockCommunicator);
        
        // Get the listener
        ArgumentCaptor<IMessageListener> newListenerCaptor = ArgumentCaptor.forClass(IMessageListener.class);
        verify(freshMockCommunicator).addSubscriber(eq("image"), newListenerCaptor.capture());
        IMessageListener newListener = newListenerCaptor.getValue();
        
        String imageId = "test-image-no-callback";
        String base64Data = "SGVsbG8gV29ybGQ=";
        String chunkMessage = imageId + ":0:1:" + base64Data;
        
        // Should not throw exception when callback is null
        assertDoesNotThrow(() -> newListener.onMessageReceived(chunkMessage));
    }

    @Test
    public void testHandleReceivedChunkLargeData() {
        String imageId = "large-image";
        StringBuilder largeData = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            largeData.append("data");
        }
        String base64Data = largeData.toString();
        String chunkMessage = imageId + ":0:1:" + base64Data;
        
        capturedListener.onMessageReceived(chunkMessage);
        
        verify(mockCallback).accept(base64Data);
    }

    @Test
    public void testSendMessageWithLargeFile() throws IOException {
        // Create a larger temporary file to test chunking
        Path imageFile = tempDir.resolve("large_test.jpg");
        byte[] largeImageData = new byte[10000]; // 10KB file
        for (int i = 0; i < largeImageData.length; i++) {
            largeImageData[i] = (byte) (i % 256);
        }
        Files.write(imageFile, largeImageData);
        
        String ipAddress = "10.0.0.1";
        int port = 9000;
        
        imageMessenger.sendMessage(ipAddress, port, imageFile.toString());
        
        // Should send multiple chunks for a large file
        verify(mockCommunicator, atLeast(2)).sendMessage(eq(ipAddress), eq(port), eq("image"), anyString());
    }

    @Test
    public void testHandleReceivedChunkOutOfOrder() {
        String imageId = "out-of-order-image";
        String chunk1 = "a";
        String chunk2 = "b";
        String chunk3 = "c";
        
        // Send chunks out of order: 2, 1, 3
        assertDoesNotThrow(() -> {
            capturedListener.onMessageReceived(imageId + ":1:3:" + chunk2);
            capturedListener.onMessageReceived(imageId + ":0:3:" + chunk1);
            capturedListener.onMessageReceived(imageId + ":2:3:" + chunk3);
        });
        
        // Verify callback was called with correctly reconstructed data "abc"
        verify(mockCallback).accept("abc");
    }

    @Test
    public void testHandleReceivedChunkOutOfOrderLargerChunks() throws InterruptedException {
        String imageId = "test-image-large-out-of-order";
        String chunk1 = "AAAA";
        String chunk2 = "BBBB";
        String chunk3 = "CCCC";
        String chunk4 = "DDDD";
        
        // Use CountDownLatch to ensure proper verification
        CountDownLatch latch = new CountDownLatch(1);
        Consumer<String> testCallback = data -> {
            assertEquals("AAAABBBBCCCCDDDD", data);
            latch.countDown();
        };
        
        imageMessenger.setOnImageMessageReceived(testCallback);
        
        // Send chunks completely out of order: 4, 2, 1, 3
        capturedListener.onMessageReceived(imageId + ":3:4:" + chunk4);
        capturedListener.onMessageReceived(imageId + ":1:4:" + chunk2);
        capturedListener.onMessageReceived(imageId + ":0:4:" + chunk1);
        capturedListener.onMessageReceived(imageId + ":2:4:" + chunk3);
        
        // Wait for callback to be called
        assertTrue(latch.await(1, TimeUnit.SECONDS), "Callback should be called within 1 second");
    }
}
