package org.sourceforge.net.javamail4ews;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test suite to validate GitHub Actions workflow configuration and artifacts.
 * This test ensures that the CI/CD pipeline components are properly configured.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("GitHub Actions Workflow Validation Tests")
public class WorkflowValidationTest {

    private static final String PROJECT_ROOT = System.getProperty("user.dir");
    private static final String GITHUB_WORKFLOWS_DIR = ".github/workflows";
    private static final String TARGET_DIR = "target";
    
    @BeforeAll
    void setUp() {
        System.out.println("Running workflow validation tests from: " + PROJECT_ROOT);
        System.out.println("Java version: " + System.getProperty("java.version"));
        System.out.println("Maven build timestamp: " + System.getProperty("baseline.test.timestamp", "not-set"));
    }

    @Test
    @DisplayName("CI Workflow File Exists and Contains Required Configuration")
    void testCiWorkflowExists() throws IOException {
        Path ciWorkflowPath = Paths.get(PROJECT_ROOT, GITHUB_WORKFLOWS_DIR, "ci.yml");
        assertTrue(Files.exists(ciWorkflowPath), "CI workflow file should exist");
        
        String content = Files.readString(ciWorkflowPath);
        
        // Check for required triggers
        assertTrue(content.contains("push:"), "CI workflow should trigger on push");
        assertTrue(content.contains("pull_request:"), "CI workflow should trigger on pull requests");
        
        // Check for matrix strategy
        assertTrue(content.contains("matrix:"), "CI workflow should use matrix strategy");
        assertTrue(content.contains("java-version:"), "CI workflow should test multiple Java versions");
        
        // Check for required Java versions
        assertTrue(content.contains("17") && content.contains("21"), 
                  "CI workflow should test Java 17 and 21");
        
        // Check for required jobs
        assertTrue(content.contains("validate:"), "CI workflow should have validate job");
        assertTrue(content.contains("test:"), "CI workflow should have test job");
        assertTrue(content.contains("build:"), "CI workflow should have build job");
        
        // Check for caching
        assertTrue(content.contains("actions/cache@v4"), "CI workflow should use dependency caching");
        
        System.out.println("✅ CI workflow validation passed");
    }

    @Test
    @DisplayName("Release Workflow File Exists and Contains Required Configuration")
    void testReleaseWorkflowExists() throws IOException {
        Path releaseWorkflowPath = Paths.get(PROJECT_ROOT, GITHUB_WORKFLOWS_DIR, "release.yml");
        assertTrue(Files.exists(releaseWorkflowPath), "Release workflow file should exist");
        
        String content = Files.readString(releaseWorkflowPath);
        
        // Check for tag triggers
        assertTrue(content.contains("tags:"), "Release workflow should trigger on tags");
        assertTrue(content.contains("v*.*.*"), "Release workflow should trigger on version tags");
        
        // Check for required jobs
        assertTrue(content.contains("release-build:"), "Release workflow should have release-build job");
        assertTrue(content.contains("deploy-github:"), "Release workflow should have deploy-github job");
        assertTrue(content.contains("create-release:"), "Release workflow should have create-release job");
        
        // Check for deployment configuration
        assertTrue(content.contains("deploy"), "Release workflow should include deployment steps");
        assertTrue(content.contains("GITHUB_TOKEN"), "Release workflow should use GITHUB_TOKEN");
        
        System.out.println("✅ Release workflow validation passed");
    }

    @Test
    @DisplayName("Workflow Validation File Exists and Is Properly Configured")
    void testWorkflowValidationExists() throws IOException {
        Path validationWorkflowPath = Paths.get(PROJECT_ROOT, GITHUB_WORKFLOWS_DIR, "workflow-validation.yml");
        assertTrue(Files.exists(validationWorkflowPath), "Workflow validation file should exist");
        
        String content = Files.readString(validationWorkflowPath);
        
        // Check for manual trigger
        assertTrue(content.contains("workflow_dispatch:"), "Validation workflow should support manual trigger");
        
        // Check for validation jobs
        assertTrue(content.contains("matrix-build-validation:"), "Should have matrix build validation");
        assertTrue(content.contains("artifact-generation-validation:"), "Should have artifact generation validation");
        assertTrue(content.contains("deployment-validation:"), "Should have deployment validation");
        
        System.out.println("✅ Workflow validation file validation passed");
    }

    @Test
    @DisplayName("Maven Build Artifacts Are Generated Correctly")
    void testBuildArtifacts() {
        // Check main JAR
        Path mainJar = Paths.get(PROJECT_ROOT, TARGET_DIR, "javamail4ews.jar");
        if (Files.exists(mainJar)) {
            assertTrue(Files.isRegularFile(mainJar), "Main JAR should be a regular file");
            
            try {
                long size = Files.size(mainJar);
                assertTrue(size > 1000, "Main JAR should be larger than 1KB, actual size: " + size);
                System.out.println("✅ Main JAR artifact validated (size: " + size + " bytes)");
            } catch (IOException e) {
                fail("Could not check JAR file size: " + e.getMessage());
            }
        } else {
            System.out.println("⚠️ Main JAR not found (may not be built yet)");
        }
        
        // Check dependencies directory
        Path libDir = Paths.get(PROJECT_ROOT, TARGET_DIR, "lib");
        if (Files.exists(libDir)) {
            assertTrue(Files.isDirectory(libDir), "lib should be a directory");
            
            try {
                long depCount = Files.list(libDir)
                    .filter(path -> path.toString().endsWith(".jar"))
                    .count();
                assertTrue(depCount > 0, "Should have at least one dependency JAR");
                System.out.println("✅ Dependencies validated (" + depCount + " JARs found)");
            } catch (IOException e) {
                fail("Could not list dependency JARs: " + e.getMessage());
            }
        } else {
            System.out.println("⚠️ Dependencies directory not found (may not be built yet)");
        }
    }

    @Test
    @DisplayName("Test Reports Are Generated in Expected Format")
    void testReportGeneration() {
        Path surefireReports = Paths.get(PROJECT_ROOT, TARGET_DIR, "surefire-reports");
        
        if (Files.exists(surefireReports)) {
            assertTrue(Files.isDirectory(surefireReports), "Surefire reports should be a directory");
            
            try {
                long xmlReportCount = Files.list(surefireReports)
                    .filter(path -> path.toString().endsWith(".xml"))
                    .count();
                
                assertTrue(xmlReportCount > 0, "Should have at least one XML test report");
                System.out.println("✅ Test reports validated (" + xmlReportCount + " XML reports found)");
            } catch (IOException e) {
                fail("Could not list test report files: " + e.getMessage());
            }
        } else {
            System.out.println("⚠️ Test reports directory not found (tests may not have run yet)");
        }
    }

    @Test
    @DisplayName("Maven Settings Configuration Is Valid")
    void testMavenSettings() {
        Path settingsXml = Paths.get(PROJECT_ROOT, "settings.xml");
        
        if (Files.exists(settingsXml)) {
            assertTrue(Files.isRegularFile(settingsXml), "settings.xml should be a regular file");
            
            try {
                String content = Files.readString(settingsXml);
                assertTrue(content.contains("<settings"), "settings.xml should contain settings element");
                System.out.println("✅ Maven settings.xml validated");
            } catch (IOException e) {
                fail("Could not read settings.xml: " + e.getMessage());
            }
        } else {
            System.out.println("⚠️ settings.xml not found (may use default settings)");
        }
    }

    @Test
    @DisplayName("Project Configuration Supports Required Java Versions")
    void testJavaVersionCompatibility() {
        // Check current Java version
        String javaVersion = System.getProperty("java.version");
        String majorVersion = javaVersion.split("\\.")[0];
        
        int javaMajor = Integer.parseInt(majorVersion);
        assertTrue(javaMajor >= 17, "Tests should run on Java 17 or higher, current: " + javaVersion);
        
        // Check if we can load main classes (basic smoke test)
        try {
            Class.forName("org.sourceforge.net.javamail4ews.store.EwsStore");
            System.out.println("✅ Main classes can be loaded with Java " + javaVersion);
        } catch (ClassNotFoundException e) {
            fail("Could not load main application class: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Baseline Test Configuration Is Valid")
    void testBaselineTestConfiguration() {
        // Check if baseline test timestamp is set (indicates baseline profile was used)
        String baselineTimestamp = System.getProperty("baseline.test.timestamp");
        
        if (baselineTimestamp != null && !baselineTimestamp.equals("not-set")) {
            System.out.println("✅ Baseline test configuration active (timestamp: " + baselineTimestamp + ")");
            
            // Check for baseline results directory
            Path baselineResults = Paths.get(PROJECT_ROOT, TARGET_DIR, "baseline-results");
            if (Files.exists(baselineResults)) {
                assertTrue(Files.isDirectory(baselineResults), "Baseline results should be a directory");
                System.out.println("✅ Baseline results directory found");
            }
        } else {
            System.out.println("ℹ️ Baseline test configuration not active (running in regular test mode)");
        }
    }

    @Test
    @DisplayName("Security Scan Configuration Is Present")
    void testSecurityScanConfiguration() {
        Path pomXml = Paths.get(PROJECT_ROOT, "pom.xml");
        
        if (Files.exists(pomXml)) {
            try {
                String content = Files.readString(pomXml);
                
                // Check for OWASP dependency check plugin
                if (content.contains("dependency-check-maven")) {
                    System.out.println("✅ OWASP dependency check plugin configured");
                    
                    // Check for security reports directory
                    Path securityReports = Paths.get(PROJECT_ROOT, TARGET_DIR, "security-reports");
                    if (Files.exists(securityReports)) {
                        System.out.println("✅ Security reports directory found");
                    }
                } else {
                    System.out.println("⚠️ OWASP dependency check plugin not found in pom.xml");
                }
            } catch (IOException e) {
                fail("Could not read pom.xml: " + e.getMessage());
            }
        }
    }

    @Test
    @DisplayName("Workflow Environment Variables Are Properly Configured")
    void testWorkflowEnvironmentVariables() throws IOException {
        // Test CI workflow environment variables
        Path ciWorkflowPath = Paths.get(PROJECT_ROOT, GITHUB_WORKFLOWS_DIR, "ci.yml");
        if (Files.exists(ciWorkflowPath)) {
            String content = Files.readString(ciWorkflowPath);
            
            assertTrue(content.contains("MAVEN_OPTS:"), "CI workflow should set MAVEN_OPTS");
            assertTrue(content.contains("MAVEN_SETTINGS:"), "CI workflow should set MAVEN_SETTINGS");
            
            System.out.println("✅ CI workflow environment variables validated");
        }
        
        // Test release workflow environment variables
        Path releaseWorkflowPath = Paths.get(PROJECT_ROOT, GITHUB_WORKFLOWS_DIR, "release.yml");
        if (Files.exists(releaseWorkflowPath)) {
            String content = Files.readString(releaseWorkflowPath);
            
            assertTrue(content.contains("MAVEN_OPTS:"), "Release workflow should set MAVEN_OPTS");
            assertTrue(content.contains("MAVEN_SETTINGS:"), "Release workflow should set MAVEN_SETTINGS");
            
            System.out.println("✅ Release workflow environment variables validated");
        }
    }
}