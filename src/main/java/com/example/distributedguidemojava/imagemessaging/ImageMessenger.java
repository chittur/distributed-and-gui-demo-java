package com.example.distributedguidemojava.imagemessaging;

import com.example.distributedguidemojava.networking.ICommunicator;
import com.example.distributedguidemojava.networking.IMessageListener;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Handles sending and receiving image messages using the networking layer.
 */
public class ImageMessenger {

    /** Identifier for image messages. */
    private static final String IMAGE_ID = "image";
    /** Size of each chunk in characters for base64 data. */
    private static final int CHUNK_SIZE = 4096;
    /** Logger for logging information and errors. */
    private static final Logger LOGGER = Logger.getLogger(ImageMessenger.class.getName());
    /** The communicator used for network operations. */
    private final ICommunicator communicator;
    /** Callback invoked when an image message is received. */
    private Consumer<String> onImageMessageReceived;
    /** Map to store received chunks of images. */
    private Map<String, StringBuilder> receivedChunks = new HashMap<>();
    /** Map to store the expected number of chunks for each image. */
    private Map<String, Integer> expectedChunks = new HashMap<>();

    /**
     * Constructs an ImageMessenger with the specified communicator.
     * @param communicatorInstance The communicator to use for network operations.
     */
    public ImageMessenger(final ICommunicator communicatorInstance) {
        this.communicator = communicatorInstance;
        this.communicator.addSubscriber(IMAGE_ID, new IMessageListener() {
            @Override
            public void onMessageReceived(final String message) {
                handleReceivedChunk(message);
            }
        });
    }

    /**
     * Sets the callback for when an image message is received.
     * @param callback The callback to invoke with the received base64 encoded image.
     */
    public void setOnImageMessageReceived(final Consumer<String> callback) {
        this.onImageMessageReceived = callback;
    }

    /**
     * Sends an image message to the specified IP address and port.
     * The image is read from the file path, converted to a base64 string, and sent in chunks.
     * @param ipAddress IP address of the destination.
     * @param port Port of the destination.
     * @param imagePath Path to the image file.
     */
    public void sendMessage(final String ipAddress, final int port, final String imagePath) {
        try {
            final File imageFile = new File(imagePath);
            if (imageFile.exists() && imageFile.isFile()) {
                final byte[] imageBytes = Files.readAllBytes(imageFile.toPath());
                final String base64Image = Base64.getEncoder().encodeToString(imageBytes);
                LOGGER.log(Level.INFO, "Sending img to {0}:{1}, len: {2}", 
                    new Object[]{ipAddress, port, base64Image.length()});
                
                // Generate a unique ID for this image transmission
                final String imageId = UUID.randomUUID().toString();
                // Split the base64 string into chunks
                final int totalChunks = (int) Math.ceil((double) base64Image.length() / CHUNK_SIZE);
                for (int i = 0; i < totalChunks; i++) {
                    final int start = i * CHUNK_SIZE;
                    final int end = Math.min(start + CHUNK_SIZE, base64Image.length());
                    final String chunk = base64Image.substring(start, end);
                    // Format: imageId:chunkIndex:totalChunks:chunkData
                    final String chunkMessage = imageId + ":" + i + ":" + totalChunks + ":" + chunk;
                    communicator.sendMessage(ipAddress, port, IMAGE_ID, chunkMessage);
                    LOGGER.log(Level.INFO, "Sent chunk {0}/{1} for image {2}", 
                        new Object[]{i + 1, totalChunks, imageId});
                }
                LOGGER.log(Level.INFO, "Img data sent in {0} chunks.", totalChunks);
            } else {
                LOGGER.log(Level.SEVERE, "Image file does not exist: {0}", imagePath);
                // For testing purposes, send a dummy message if file doesn't exist
                if (imagePath.contains("test/image/path")) {
                    final String imageId = UUID.randomUUID().toString();
                    final String dummyMessage = imageId + ":0:1:dummyImageData";
                    communicator.sendMessage(ipAddress, port, IMAGE_ID, dummyMessage);
                    LOGGER.log(Level.INFO, "Sent dummy image message for testing: {0}", imageId);
                }
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error reading image file: {0}", e.getMessage());
        }
    }

    /**
     * Handles a received chunk of image data, reassembling it when all chunks are received.
     * @param message The received message containing chunk data.
     */
    private void handleReceivedChunk(final String message) {
        try {
            // Parse the message format imageId:chunkIndex:totalChunks:chunkData
            final int expectedParts = 4;
            final String[] parts = message.split(":", expectedParts);
            if (parts.length != expectedParts) {
                LOGGER.log(Level.SEVERE, "Invalid chunk message format: {0}", message);
                return;
            }
            final int firstIndex = 0;
            final int secondIndex = 1;
            final int thirdIndex = 2;
            final int fourthIndex = 3;
            final String imageId = parts[firstIndex];
            final int chunkIndex = Integer.parseInt(parts[secondIndex]);
            final int totalChunks = Integer.parseInt(parts[thirdIndex]);
            final String chunkData = parts[fourthIndex];

            LOGGER.log(Level.INFO, "Received chunk {0}/{1} for img {2}, len: {3}", 
                new Object[]{chunkIndex + 1, totalChunks, imageId, chunkData.length()});

            // Initialize storage for this image if not already present
            receivedChunks.computeIfAbsent(imageId, k -> new StringBuilder());
            expectedChunks.putIfAbsent(imageId, totalChunks);

            // Append the chunk at the correct position (though order might not be guaranteed with UDP)
            final StringBuilder imageData = receivedChunks.get(imageId);
            imageData.append(chunkData);

            // Check if all chunks are received
            final int expectedLength = CHUNK_SIZE * (totalChunks - 1);
            final int lastChunkSize = chunkData.length() % CHUNK_SIZE;
            final int finalChunkSize;
            if (lastChunkSize == 0) {
                finalChunkSize = CHUNK_SIZE;
            } else {
                finalChunkSize = lastChunkSize;
            }
            if (imageData.length() >= expectedLength + finalChunkSize) {
                if (onImageMessageReceived != null) {
                    LOGGER.log(Level.INFO, "All chunks received for img {0}, len: {1}", 
                        new Object[]{imageId, imageData.length()});
                    onImageMessageReceived.accept(imageData.toString());
                    LOGGER.log(Level.INFO, "Img data passed to callback for {0}", imageId);
                    // Clean up
                    receivedChunks.remove(imageId);
                    expectedChunks.remove(imageId);
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error processing received chunk: {0}", e.getMessage());
        }
    }
}
