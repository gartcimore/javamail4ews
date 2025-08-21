package org.sourceforge.net.javamail4ews.store;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;


import jakarta.mail.Session;
import jakarta.mail.URLName;
import jakarta.mail.MessagingException;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("EwsStore Baseline Tests")
public class EwsStoreTest {

    private EwsStore ewsStore;
    private URLName testUrl;
    private Session testSession;
    
    @BeforeEach
    void setUp() {
        Properties props = new Properties();
        testSession = Session.getInstance(props);
        testUrl = new URLName("ewsstore://test.example.com/");
        ewsStore = new EwsStore(testSession, testUrl);
    }
    
    @Test
    @DisplayName("Store creation with valid session and URL")
    void testStoreCreation() {
        assertNotNull(ewsStore);
        // Note: JavaMail Store may modify the URL to include username
        URLName actualUrl = ewsStore.getURLName();
        assertNotNull(actualUrl);
        assertEquals("ewsstore", actualUrl.getProtocol());
        assertEquals("test.example.com", actualUrl.getHost());
    }
    
    @Test
    @DisplayName("Protocol detection from session properties")
    void testProtocolDetection() {
        // Test that the store can handle basic operations
        assertDoesNotThrow(() -> {
            // Basic operations that should work without connection
            assertNotNull(ewsStore.getURLName());
            assertEquals("ewsstore", ewsStore.getURLName().getProtocol());
        });
    }
    
    @Test
    @DisplayName("Default folder retrieval without connection")
    void testGetDefaultFolderWithoutConnection() {
        // Without proper EWS connection, this should throw MessagingException
        assertThrows(Exception.class, () -> {
            ewsStore.getDefaultFolder();
        });
    }
    
    @Test
    @DisplayName("Folder retrieval by name without connection")
    void testGetFolderByNameWithoutConnection() {
        // Without proper EWS connection, this should throw an exception
        assertThrows(Exception.class, () -> {
            ewsStore.getFolder("INBOX");
        });
    }
    
    @Test
    @DisplayName("Store URL name handling")
    void testStoreUrlNameHandling() {
        // Test that the store properly handles URL names
        URLName actualUrl = ewsStore.getURLName();
        assertNotNull(actualUrl);
        assertEquals("ewsstore", actualUrl.getProtocol());
        assertEquals("test.example.com", actualUrl.getHost());
        // URL may include username from system properties
        assertTrue(actualUrl.toString().contains("test.example.com"));
    }
}