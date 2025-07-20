package com.example.distributedguidemojava;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.stage.Stage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for App class.
 */
public class AppTest {

    @BeforeAll
    public static void initializeJavaFX() {
        // Initialize JavaFX toolkit for testing
        new JFXPanel();
    }

    @Test
    @Timeout(10)
    public void testFXMLLoading() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        
        Platform.runLater(() -> {
            try {
                FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource("/fxml/MainWindow.fxml"));
                Parent root = fxmlLoader.load();
                assertNotNull(root, "FXML should load successfully");
                latch.countDown();
            } catch (Exception e) {
                fail("FXML loading failed: " + e.getMessage());
            }
        });
        
        assertTrue(latch.await(5, TimeUnit.SECONDS), "FXML loading should complete within 5 seconds");
    }

    @Test
    @Timeout(10)
    public void testAppStart() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        
        Platform.runLater(() -> {
            try {
                App app = new App();
                Stage testStage = new Stage();
                app.start(testStage);
                
                assertNotNull(testStage.getScene(), "Scene should be set");
                assertEquals("Distributed GUI Demo", testStage.getTitle(), "Window title should be set correctly");
                assertEquals(800.0, testStage.getScene().getWidth(), 0.1, "Scene width should be 800");
                assertEquals(600.0, testStage.getScene().getHeight(), 0.1, "Scene height should be 600");
                
                // Clean up
                testStage.close();
                latch.countDown();
            } catch (Exception e) {
                fail("App start failed: " + e.getMessage());
            }
        });
        
        assertTrue(latch.await(5, TimeUnit.SECONDS), "App start should complete within 5 seconds");
    }

    @Test
    public void testMainMethod() {
        // Test that main method can be called without throwing exceptions
        // Note: This won't actually launch the application in test environment
        assertDoesNotThrow(() -> {
            // We can't easily test the actual launch, but we can test that the method exists
            // and can be called without immediate errors
            String[] args = {};
            // App.main(args); // This would try to launch the actual application
            
            // Instead, just verify the method exists and is accessible
            assertNotNull(App.class.getMethod("main", String[].class));
        });
    }

    @Test
    public void testResourceExists() {
        // Test that the required FXML resource exists
        assertNotNull(App.class.getResource("/fxml/MainWindow.fxml"), 
            "MainWindow.fxml resource should exist");
    }

    @Test
    public void testAppCanBeInstantiated() {
        assertDoesNotThrow(() -> new App(), "App should be instantiable");
    }
}
