package com.example.distributedguidemojava.viewmodel;

import com.example.distributedguidemojava.chatmessaging.ChatMessenger;
import com.example.distributedguidemojava.imagemessaging.ImageMessenger;
import com.example.distributedguidemojava.networking.CommunicatorFactory;
import com.example.distributedguidemojava.networking.ICommunicator;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.image.Image;

/**
 * ViewModel for the main page, handling business logic and data binding.
 */
public class MainPageViewModel {

    /** Messenger for handling chat messages. */
    private final ChatMessenger chatMessenger;
    /** Messenger for handling image messages. */
    private final ImageMessenger imageMessenger;
    /** Communicator used for network operations. */
    private final ICommunicator communicator;
    /** Property for the receive port number. */
    private final StringProperty receivePort = new SimpleStringProperty();
    /** Property for received text messages. */
    private final StringProperty receivedMessage = new SimpleStringProperty();
    /** Property for received image data. */
    private final ObjectProperty<Image> receivedImage = new SimpleObjectProperty<>();

    /**
     * Constructs a MainPageViewModel with an optional communicator.
     * @param communicatorInstance Optional ICommunicator instance. If null, a new one is created.
     */
    public MainPageViewModel(final ICommunicator communicatorInstance) {
        this.communicator = getCommunicator(communicatorInstance);
        receivePort.set(String.valueOf(this.communicator.getListenPort()));

        // Initialize messengers and set up callbacks
        chatMessenger = new ChatMessenger(this.communicator);
        chatMessenger.setOnChatMessageReceived(message -> {
            // Update on UI thread
            Platform.runLater(() -> receivedMessage.set(message));
        });

        imageMessenger = new ImageMessenger(this.communicator);
        imageMessenger.setOnImageMessageReceived(base64Image -> {
            // Update on UI thread
            Platform.runLater(() -> {
                final Image decodedImage = decodeBase64ToImage(base64Image);
                receivedImage.set(decodedImage);
            });
        });
    }

    /**
     * Sends a chat message to the specified IP address and port.
     * @param ipAddress IP address of the destination.
     * @param port Port of the destination.
     * @param message Message to send.
     */
    public void sendChatMessage(final String ipAddress, final int port, final String message) {
        chatMessenger.sendMessage(ipAddress, port, message);
    }

    /**
     * Sends an image message to the specified IP address and port.
     * @param ipAddress IP address of the destination.
     * @param port Port of the destination.
     * @param imagePath Path to the image file.
     */
    public void sendImageMessage(final String ipAddress, final int port, final String imagePath) {
        imageMessenger.sendMessage(ipAddress, port, imagePath);
    }

    /**
     * Gets the property for the receive port.
     * @return StringProperty for receive port.
     */
    public StringProperty receivePortProperty() {
        return receivePort;
    }

    /**
     * Gets the property for received messages.
     * @return StringProperty for received messages.
     */
    public StringProperty receivedMessageProperty() {
        return receivedMessage;
    }

    /**
     * Gets the property for received images.
     * @return ObjectProperty for received images.
     */
    public ObjectProperty<Image> receivedImageProperty() {
        return receivedImage;
    }

    /**
     * Decodes a base64 string to an Image.
     * @param base64Image Base64 encoded image string.
     * @return Decoded Image.
     */
    private Image decodeBase64ToImage(final String base64Image) {
        return getDecodedImage(base64Image);
    }
    
    private ICommunicator getCommunicator(final ICommunicator communicatorInstance) {
        if (communicatorInstance != null) {
            return communicatorInstance;
        }
        return CommunicatorFactory.createCommunicator();
    }
    
    private Image getDecodedImage(final String base64Image) {
        try {
            // Remove data URI prefix if present (e.g., "data:image/jpeg;base64,")
            String base64Data = base64Image;
            if (base64Image.contains(",")) {
                final String[] parts = base64Image.split(",");
                base64Data = parts[1];
            }
            // Decode base64 string to byte array
            final byte[] imageBytes = java.util.Base64.getDecoder().decode(base64Data);
            // Convert byte array to Image
            return new Image(new java.io.ByteArrayInputStream(imageBytes));
        } catch (Exception e) {
            e.printStackTrace();
            return null; // Return null if decoding fails
        }
    }
}
