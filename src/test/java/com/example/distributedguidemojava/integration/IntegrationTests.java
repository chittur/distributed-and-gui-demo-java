package com.example.distributedguidemojava.integration;

import com.example.distributedguidemojava.networking.ICommunicator;
import com.example.distributedguidemojava.viewmodel.MainPageViewModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Integration tests for the distributed GUI demo application.
 * These tests verify the interaction between different components of the system.
 */
public class IntegrationTests {

    private ICommunicator mockCommunicator;
    private MainPageViewModel viewModel;
    
    @Captor
    private ArgumentCaptor<String> identityCaptor;
    
    @BeforeEach
    public void setUp() {
        mockCommunicator = mock(ICommunicator.class);
        viewModel = new MainPageViewModel(mockCommunicator);
    }
    
    @Test
    public void testSubscribersBasic() {
        // Verify that messengers subscribe with the communicator
        verify(mockCommunicator, times(2)).addSubscriber(anyString(), any());
    }
    
    @Test
    public void testSendMessageBasic() {
        // Arrange
        String ipAddress = "127.0.0.1";
        int port = 500;
        String chatMessage = "Hello World";
        String imagePath = "test/image/path.jpg"; // Use a dummy path for testing
        String anotherChatMessage = "Another Hello World Message";
        
        // Act
        viewModel.sendChatMessage(ipAddress, port, chatMessage);
        viewModel.sendImageMessage(ipAddress, port, imagePath);
        viewModel.sendChatMessage(ipAddress, port, anotherChatMessage);
        
        // Assert
        verify(mockCommunicator, times(2)).sendMessage(eq(ipAddress), eq(port), eq("chat"), anyString());
        verify(mockCommunicator, times(1)).sendMessage(eq(ipAddress), eq(port), eq("image"), anyString());
    }
    
    // Add more integration tests as needed
}
