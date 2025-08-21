package org.sourceforge.net.javamail4ews.performance;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Disabled;

import jakarta.mail.Session;
import java.util.Properties;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Baseline Performance Tests
 * 
 * These tests establish performance baselines before dependency upgrades.
 * They measure memory usage, initialization time, and basic operation performance.
 * 
 * Note: These tests are disabled by default as they require actual EWS connectivity.
 * Enable them when running baseline measurements.
 */
@DisplayName("Baseline Performance Tests")
public class BaselinePerformanceTest {

    private MemoryMXBean memoryBean;
    private Properties testProperties;
    
    @BeforeEach
    void setUp() {
        memoryBean = ManagementFactory.getMemoryMXBean();
        testProperties = new Properties();
        testProperties.setProperty("org.sourceforge.net.javamail4ews.util.Util.EnableServiceTrace", "false");
    }
    
    @Test
    @DisplayName("Memory usage baseline - Session creation")
    void testSessionCreationMemoryUsage() {
        // Measure memory before
        System.gc(); // Suggest garbage collection
        MemoryUsage beforeHeap = memoryBean.getHeapMemoryUsage();
        
        // Perform operation
        Session session = Session.getInstance(testProperties);
        assertNotNull(session);
        
        // Measure memory after
        System.gc(); // Suggest garbage collection
        MemoryUsage afterHeap = memoryBean.getHeapMemoryUsage();
        
        long memoryUsed = afterHeap.getUsed() - beforeHeap.getUsed();
        
        // Log baseline memory usage
        System.out.printf("Session creation memory usage: %d bytes%n", memoryUsed);
        
        // Basic assertion - memory usage should be reasonable
        assertTrue(memoryUsed < 10_000_000, "Session creation should use less than 10MB");
    }
    
    @Test
    @DisplayName("Initialization time baseline - Session creation")
    void testSessionCreationTime() {
        long startTime = System.nanoTime();
        
        Session session = Session.getInstance(testProperties);
        assertNotNull(session);
        
        long endTime = System.nanoTime();
        long durationMs = (endTime - startTime) / 1_000_000;
        
        // Log baseline timing
        System.out.printf("Session creation time: %d ms%n", durationMs);
        
        // Basic assertion - should be fast
        assertTrue(durationMs < 1000, "Session creation should take less than 1 second");
    }
    
    @Test
    @DisplayName("Configuration loading time baseline")
    void testConfigurationLoadingTime() {
        long startTime = System.nanoTime();
        
        // Test configuration loading performance
        Properties props = new Properties();
        props.setProperty("test.property", "test.value");
        Session session = Session.getInstance(props);
        
        long endTime = System.nanoTime();
        long durationMs = (endTime - startTime) / 1_000_000;
        
        // Log baseline timing
        System.out.printf("Configuration loading time: %d ms%n", durationMs);
        
        // Basic assertion
        assertTrue(durationMs < 500, "Configuration loading should take less than 500ms");
    }
    
    @Test
    @Disabled("Requires actual EWS server connection")
    @DisplayName("EWS Store connection time baseline")
    void testEwsStoreConnectionTime() {
        // This test would measure actual EWS connection time
        // Disabled by default as it requires real server credentials
        
        long startTime = System.nanoTime();
        
        // Simulated connection test
        try {
            Thread.sleep(100); // Simulate connection time
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        long endTime = System.nanoTime();
        long durationMs = (endTime - startTime) / 1_000_000;
        
        System.out.printf("Simulated EWS connection time: %d ms%n", durationMs);
    }
    
    @Test
    @DisplayName("Memory usage baseline - Multiple sessions")
    void testMultipleSessionsMemoryUsage() {
        System.gc();
        MemoryUsage beforeHeap = memoryBean.getHeapMemoryUsage();
        
        // Create multiple sessions to test memory scaling
        Session[] sessions = new Session[10];
        for (int i = 0; i < sessions.length; i++) {
            Properties props = new Properties();
            props.setProperty("session.id", String.valueOf(i));
            sessions[i] = Session.getInstance(props);
        }
        
        System.gc();
        MemoryUsage afterHeap = memoryBean.getHeapMemoryUsage();
        
        long memoryUsed = afterHeap.getUsed() - beforeHeap.getUsed();
        long memoryPerSession = memoryUsed / sessions.length;
        
        System.out.printf("Multiple sessions total memory: %d bytes%n", memoryUsed);
        System.out.printf("Memory per session: %d bytes%n", memoryPerSession);
        
        // Ensure sessions are not null to prevent optimization
        for (Session session : sessions) {
            assertNotNull(session);
        }
        
        assertTrue(memoryUsed < 50_000_000, "10 sessions should use less than 50MB total");
    }
    
    @Test
    @DisplayName("Class loading time baseline")
    void testClassLoadingTime() {
        long startTime = System.nanoTime();
        
        // Force class loading of key classes
        try {
            Class.forName("org.sourceforge.net.javamail4ews.store.EwsStore");
            Class.forName("org.sourceforge.net.javamail4ews.store.EwsFolder");
            Class.forName("org.sourceforge.net.javamail4ews.store.EwsMessage");
            Class.forName("org.sourceforge.net.javamail4ews.transport.EwsTransport");
            Class.forName("org.sourceforge.net.javamail4ews.util.Util");
        } catch (ClassNotFoundException e) {
            fail("Core classes should be loadable: " + e.getMessage());
        }
        
        long endTime = System.nanoTime();
        long durationMs = (endTime - startTime) / 1_000_000;
        
        System.out.printf("Core class loading time: %d ms%n", durationMs);
        
        assertTrue(durationMs < 2000, "Core class loading should take less than 2 seconds");
    }
}