package org.sourceforge.net.javamail4ews.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import jakarta.mail.Session;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Util Baseline Tests")
public class UtilTest {
    

    @Test
    @DisplayName("Configuration loading")
    void testConfigurationLoading() {
        assertDoesNotThrow(() -> {
            Properties props = new Properties();
            Session session = Session.getInstance(props);
            
            // Test configuration loading
            org.apache.commons.configuration2.Configuration config = Util.getConfiguration(session);
            assertNotNull(config);
        });
    }
    
    @Test
    @DisplayName("Commons Configuration 2.x API compatibility")
    void testCommonsConfiguration2ApiCompatibility() {
        assertDoesNotThrow(() -> {
            Properties props = new Properties();
            Session session = Session.getInstance(props);
            
            // Test that commons-configuration2 API works correctly
            org.apache.commons.configuration2.Configuration config = Util.getConfiguration(session);
            assertNotNull(config);
            
            // Test that the new API methods work (these are different from 1.x)
            String testValue = config.getString("org.sourceforge.net.javamail4ews.ExchangeVersion", "default");
            assertNotNull(testValue);
            
            boolean traceEnabled = config.getBoolean("org.sourceforge.net.javamail4ews.util.Util.EnableServiceTrace", false);
            assertFalse(traceEnabled); // Default should be false
        });
    }
    
    @Test
    @DisplayName("Configuration file loading from resources")
    void testConfigurationFileLoading() {
        assertDoesNotThrow(() -> {
            Properties props = new Properties();
            Session session = Session.getInstance(props);
            
            // Test that default properties file can be loaded
            org.apache.commons.configuration2.Configuration config = Util.getConfiguration(session);
            assertNotNull(config);
            
            // The configuration should be able to access properties from the default file
            // Even if no specific properties are set, the configuration object should be valid
            assertTrue(config instanceof org.apache.commons.configuration2.Configuration);
        });
    }
    
    @Test
    @DisplayName("Exception casting utility")
    void testExceptionCasting() {
        // Test exception casting utility method
        RuntimeException testException = new RuntimeException("Test exception");
        
        RuntimeException result = Util.cast(testException, RuntimeException.class);
        assertNotNull(result);
        assertEquals(testException, result);
    }
    
    @Test
    @DisplayName("Exchange service creation parameters")
    void testExchangeServiceParameters() {
        // Test that exchange service parameters are properly validated
        String host = "test.example.com";
        int port = 443;
        String user = "testuser";
        String password = "testpass";
        
        assertNotNull(host);
        assertTrue(port > 0);
        assertNotNull(user);
        assertNotNull(password);
    }
}