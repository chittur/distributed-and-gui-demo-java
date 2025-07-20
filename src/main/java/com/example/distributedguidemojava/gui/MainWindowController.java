package com.example.distributedguidemojava.gui;

import com.example.distributedguidemojava.networking.CommunicatorFactory;
import com.example.distributedguidemojava.viewmodel.MainPageViewModel;
import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import java.io.File;

/**
 * Controller for the main window, handling user interactions and binding to the ViewModel.
 */
public class MainWindowController {

    /** Text field for entering the IP address. */
    @FXML
    private TextField ipAddressField;
    
    /** Text field for entering the port number. */
    @FXML
    private TextField portField;
    
    /** Text field displaying the receive port. */
    @FXML
    private TextField receivePortField;
    
    /** Text field for entering the message to send. */
    @FXML
    private TextField messageField;
    
    /** Text area displaying received messages. */
    @FXML
    private TextArea messagesArea;
    
    /** Image view for displaying received images. */
    @FXML
    private ImageView imageView;
    
    /** ViewModel for managing the main page logic. */
    private MainPageViewModel viewModel;

    /**
     * Initializes the controller, setting up bindings with the ViewModel.
     */
    @FXML
    public void initialize() {
        // Create ViewModel with a new communicator
        viewModel = new MainPageViewModel(CommunicatorFactory.createCommunicator());
        
        // Bind UI elements to ViewModel properties
        receivePortField.textProperty().bind(viewModel.receivePortProperty());
        messagesArea.textProperty().bind(viewModel.receivedMessageProperty());
        imageView.imageProperty().bind(viewModel.receivedImageProperty());
    }

    /**
     * Handles the send message button click.
     */
    @FXML
    private void sendMessage() {
        final String ipAddress = ipAddressField.getText();
        final int port;
        try {
            port = Integer.parseInt(portField.getText());
        } catch (NumberFormatException e) {
            messagesArea.setText("Invalid port number");
            return;
        }
        final String message = messageField.getText();
        viewModel.sendChatMessage(ipAddress, port, message);
        messageField.setText("");
    }

    /**
     * Handles the send image button click.
     */
    @FXML
    private void sendImage() {
        final FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Image to Send");
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif")
        );
        final File selectedFile = fileChooser.showOpenDialog(null);
        if (selectedFile != null) {
            final String ipAddress = ipAddressField.getText();
            final int port;
            try {
                port = Integer.parseInt(portField.getText());
            } catch (NumberFormatException e) {
                messagesArea.setText("Invalid port number");
                return;
            }
            viewModel.sendImageMessage(ipAddress, port, selectedFile.getAbsolutePath());
        }
    }
}
