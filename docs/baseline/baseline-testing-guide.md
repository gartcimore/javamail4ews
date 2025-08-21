# Baseline Testing and Documentation Guide

## Overview

This document describes the baseline testing and documentation setup created for the JavaMail4EWS library upgrade project. The baseline establishes the current state of functionality, performance, and dependencies before any upgrades are performed.

## Baseline Components

### 1. Test Suite Structure

#### Core Test Classes
- **`BaselineTestSuite.java`**: Main test suite aggregator
- **`EwsStoreTest.java`**: Tests for EWS store functionality
- **`EwsFolderTest.java`**: Tests for folder operations
- **`EwsMessageTest.java`**: Tests for message handling
- **`EwsTransportTest.java`**: Tests for transport operations
- **`UtilTest.java`**: Tests for utility functions
- **`EwsMailConverterTest.java`**: Tests for mail conversion

#### Performance Test Classes
- **`BaselinePerformanceTest.java`**: Performance and memory usage baselines

### 2. Documentation Files

#### Dependency Analysis
- **`docs/baseline/current-dependencies.md`**: Comprehensive dependency analysis
- **`docs/baseline/baseline-testing-guide.md`**: This guide

#### Maven Profiles
- **`baseline-tests`**: Profile for running core baseline tests
- **`performance-tests`**: Profile for running performance benchmarks

## Running Baseline Tests

### Prerequisites
- Java 8 or higher
- Maven 3.6 or higher
- Git (for commit information)

### Execution Steps

1. **Run the complete baseline suite:**
   ```bash
   mvn clean test -Pbaseline-tests
   ```

2. **Run performance tests:**
   ```bash
   mvn test -Pperformance-tests
   ```

3. **Run individual test categories:**
   ```bash
   # Core functionality tests only
   mvn test -Dtest=BaselineTestSuite
   
   # Specific test class
   mvn test -Dtest=EwsStoreTest
   ```

4. **Generate dependency reports:**
   ```bash
   mvn dependency:tree
   mvn dependency:analyze
   ```

### Test Results Location

Test results are available in standard Maven locations:
- `target/surefire-reports/`: JUnit test reports (XML and TXT)
- `target/baseline-results/dependency-analysis.txt`: Dependency analysis (when using baseline-tests profile)
- Console output: Real-time test execution and performance metrics
- `target/dependency-tree.txt`: Dependency tree (when explicitly generated)

## Test Coverage Areas

### Functional Testing
1. **EWS Store Operations**
   - Store creation and initialization
   - Protocol detection
   - Connection state management
   - Folder retrieval

2. **Folder Operations**
   - Folder creation and deletion
   - Message counting
   - Folder navigation
   - Search functionality

3. **Message Handling**
   - Message creation and parsing
   - Flag management
   - Content processing
   - Attachment handling

4. **Transport Operations**
   - Message sending
   - Connection management
   - Protocol validation

5. **Utility Functions**
   - Configuration loading
   - Exception handling
   - Version information
   - EWS service creation

### Performance Testing
1. **Memory Usage**
   - Session creation memory footprint
   - Multiple session scaling
   - Memory leak detection

2. **Timing Benchmarks**
   - Initialization time
   - Configuration loading time
   - Class loading performance
   - Connection establishment (when available)

## Baseline Metrics

### Current Performance Baselines
- **Session Creation**: < 1 second, < 10MB memory
- **Configuration Loading**: < 500ms
- **Class Loading**: < 2 seconds
- **Multiple Sessions (10)**: < 50MB total memory

### Dependency Versions (Current)
- **Java**: 8
- **javax.mail**: 1.6.1 (2018)
- **commons-configuration**: 1.10 (2014)
- **commons-codec**: 1.9 (2013)
- **slf4j-api**: 1.7.25 (2017)
- **ews-java-api**: 2.0 (2016)

## Test Limitations

### Mock-Based Testing
Most tests use mocking due to the requirement for actual EWS server connectivity. This means:
- Tests validate API contracts and basic functionality
- Full integration testing requires manual setup with real EWS servers
- Some performance tests are disabled by default

### Security Testing
- Tests do not include actual security vulnerability testing
- Dependency vulnerability information is documented but not automatically tested
- Manual security scanning is recommended

## Using Baseline Results

### Before Upgrades
1. Run the complete baseline suite
2. Save all results in `target/baseline-results/`
3. Document any test failures or issues
4. Note performance metrics for comparison

### After Upgrades
1. Run the same test suite
2. Compare results with baseline
3. Investigate any regressions
4. Update performance expectations if improvements are found

### Regression Detection
- Compare test pass/fail rates
- Check for new exceptions or errors
- Validate performance metrics are within acceptable ranges
- Ensure all documented functionality still works

## Extending the Baseline

### Adding New Tests
1. Create test classes following the existing pattern
2. Add them to `BaselineTestSuite.java`
3. Update this documentation
4. Consider both positive and negative test cases

### Adding Performance Metrics
1. Add new methods to `BaselinePerformanceTest.java`
2. Follow the existing timing and memory measurement patterns
3. Set reasonable assertion thresholds
4. Document expected ranges

### Integration Testing
For full integration testing with real EWS servers:
1. Create separate test profiles
2. Use environment variables for credentials
3. Add integration test execution to the baseline script
4. Document setup requirements

## Troubleshooting

### Common Issues
1. **Test Dependencies Missing**: Ensure JUnit 5 and Mockito are properly configured
2. **Compilation Errors**: Verify Java 8 compatibility
3. **Performance Test Failures**: Check system load and available memory
4. **Mock Setup Issues**: Verify Mockito annotations are properly initialized

### Debug Information
- Enable Maven debug output: `mvn -X test`
- Check test logs in `target/surefire-reports/`
- Review dependency conflicts: `mvn dependency:tree -Dverbose`

## Maven Commands Summary

```bash
# Run all baseline tests with dependency analysis
mvn clean test -Pbaseline-tests

# Run performance benchmarks
mvn test -Pperformance-tests

# Run both profiles together
mvn clean test -Pbaseline-tests,performance-tests

# Generate comprehensive dependency report
mvn dependency:tree -DoutputFile=target/dependency-tree.txt
mvn dependency:analyze -DoutputFile=target/dependency-analysis.txt
```

## Next Steps

After establishing the baseline:
1. Review all test results and documentation
2. Address any existing test failures
3. Proceed with the planned dependency upgrades
4. Use this baseline for regression testing throughout the upgrade process

## Maintenance

This baseline should be updated:
- When new functionality is added to the library
- When test coverage is expanded
- When performance expectations change
- Before major dependency upgrades

The baseline serves as the foundation for ensuring the library upgrade maintains compatibility and improves the overall quality of the JavaMail4EWS project.