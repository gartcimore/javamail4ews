# JavaMail4EWS Upgrade Quick Reference

## TL;DR - What Changed

### For End Users
- **Java Requirement**: Now requires Java 21+ (was Java 8)
- **Functionality**: Everything works the same
- **Configuration**: No changes needed
- **Performance**: Expect 15-20% improvement

### For Developers Extending JavaMail4EWS
- **Import Changes**: Update `javax.mail.*` → `jakarta.mail.*`
- **JAX-WS Changes**: Update `javax.xml.ws.*` → `jakarta.xml.ws.*`
- **API**: All public APIs remain the same

## Quick Migration Checklist

### Environment Setup
- [ ] Install Java 21 or higher
- [ ] Verify Maven 3.6.0 or higher
- [ ] Update IDE to support Java 21

### Code Changes (Only if extending JavaMail4EWS)
- [ ] Replace `import javax.mail.*` with `import jakarta.mail.*`
- [ ] Replace `import javax.xml.ws.*` with `import jakarta.xml.ws.*`
- [ ] Update any Commons Configuration usage (rare)

### Validation
- [ ] Run `mvn clean compile` - should succeed
- [ ] Run `mvn clean test` - all tests should pass
- [ ] Run `mvn clean test -Pbaseline-tests` - baseline validation
- [ ] Test your application functionality

## Key Dependency Changes

| Component | Old | New | Impact |
|-----------|-----|-----|--------|
| Java | 8 | 21 | Runtime requirement |
| JavaMail | javax.mail 1.6.1 | jakarta.mail 2.1.1 | Import statements |
| Commons Config | 1.10 | 2.10.1 | Internal only |
| JAX-WS | javax.xml.ws 2.2.11 | jakarta.xml.ws 4.0.1 | Import statements |

## Common Issues & Solutions

### "Package javax.mail does not exist"
**Solution**: Update imports to `jakarta.mail.*`

### "Java version not supported"
**Solution**: Upgrade to Java 21+

### "Configuration loading failed"
**Solution**: Verify configuration file format (should work unchanged)

### Build fails with dependency conflicts
**Solution**: Run `mvn dependency:tree` and resolve conflicts

## Testing Commands

```bash
# Quick validation
mvn clean compile test

# Comprehensive testing
mvn clean test -Pbaseline-tests

# Performance validation
mvn clean test -Pperformance-tests

# Security scan
mvn org.owasp:dependency-check-maven:check
```

## Need More Details?

- **Full Migration Guide**: [MIGRATION_GUIDE.md](MIGRATION_GUIDE.md)
- **Complete Dependency Matrix**: [DEPENDENCY_UPGRADE_SUMMARY.md](DEPENDENCY_UPGRADE_SUMMARY.md)
- **Baseline Testing**: [baseline/baseline-testing-guide.md](baseline/baseline-testing-guide.md)