# GitHub Actions Quick Reference

## Quick Start

### Triggering Workflows

| Action | Workflow Triggered | Purpose |
|--------|-------------------|---------|
| Push to main/master | CI Pipeline | Full build and test |
| Create Pull Request | CI Pipeline | Validate changes |
| Push tag `v1.2.3` | Release Pipeline | Deploy release |
| Manual trigger | Any workflow | On-demand execution |

### Checking Build Status

1. **GitHub UI**: Go to repository → Actions tab
2. **Pull Request**: Check status at bottom of PR
3. **Commit**: Click ✅/❌ icon next to commit
4. **Badge**: Add to README: `![CI](https://github.com/user/repo/workflows/CI/badge.svg)`

## Common Commands

### Creating a Release
```bash
# Tag and push for release
git tag v1.2.3
git push origin v1.2.3

# Or create through GitHub UI
# Releases → Create a new release → Choose tag → Publish
```

### Manual Workflow Trigger
```bash
# Using GitHub CLI
gh workflow run ci.yml

# Or use GitHub UI
# Actions → Select workflow → Run workflow
```

### Viewing Logs
```bash
# Using GitHub CLI
gh run list
gh run view <run-id>

# Or use GitHub UI
# Actions → Click on workflow run → Click on job
```

## Workflow Files

| File | Purpose | When it runs |
|------|---------|--------------|
| `.github/workflows/ci.yml` | Main CI pipeline | Push, PR |
| `.github/workflows/release.yml` | Release deployment | Tag push |
| `.github/workflows/workflow-validation.yml` | Validate setup | Manual, weekly |

## Java Version Matrix

Current matrix tests against:
- Java 17 (LTS)
- Java 21 (LTS)

To add Java version, edit `.github/workflows/ci.yml`:
```yaml
strategy:
  matrix:
    java-version: [17, 21, 22]  # Add version here
```

## Troubleshooting Quick Fixes

### Build Failing?
1. Check Java version compatibility
2. Clear cache: Delete and re-run workflow
3. Check for dependency conflicts: `mvn dependency:tree`

### Tests Failing in CI but not locally?
1. Check environment differences
2. Review test logs in Actions artifacts
3. Run with same Java version locally

### Deployment Failing?
1. Verify `GITHUB_TOKEN` permissions
2. Check repository settings → Actions → General
3. Ensure tag format matches `v*.*.*`

### Slow Builds?
1. Check cache hit rate in logs
2. Verify cache configuration
3. Consider splitting large test suites

## Useful GitHub CLI Commands

```bash
# Install GitHub CLI
# macOS: brew install gh
# Ubuntu: sudo apt install gh
# Windows: winget install GitHub.cli

# Authentication
gh auth login

# Workflow management
gh workflow list
gh workflow run ci.yml
gh workflow view ci.yml

# Run management
gh run list
gh run view --log <run-id>
gh run rerun <run-id>

# Repository actions
gh repo view --web  # Open in browser
```

## Environment Variables

### Available in all workflows:
- `GITHUB_TOKEN`: Automatic authentication
- `GITHUB_REPOSITORY`: Repository name
- `GITHUB_REF`: Branch/tag reference
- `GITHUB_SHA`: Commit SHA

### Custom variables (set in workflow):
- `MAVEN_OPTS`: JVM options for Maven
- `JAVA_TOOL_OPTIONS`: Java runtime options

## Artifacts and Reports

### Automatic artifacts:
- Test reports (Surefire XML)
- Build JARs
- Dependency analysis
- Performance test results

### Accessing artifacts:
1. **GitHub UI**: Actions → Workflow run → Artifacts section
2. **GitHub CLI**: `gh run download <run-id>`
3. **API**: Use GitHub REST API

### Retention:
- Artifacts: 90 days (configurable)
- Logs: 90 days
- Workflow runs: 90 days

## Security Notes

### Secrets Management:
- Never hardcode secrets in workflows
- Use `${{ secrets.SECRET_NAME }}`
- Manage in Settings → Secrets and variables → Actions

### Permissions:
- Workflows run with minimal permissions by default
- Add `permissions:` block if more access needed
- Use `GITHUB_TOKEN` for GitHub API access

### Best Practices:
- Pin action versions: `uses: actions/checkout@v4`
- Review third-party actions before use
- Limit workflow permissions to minimum required

## Performance Tips

### Caching:
```yaml
# Maven dependencies
- uses: actions/cache@v3
  with:
    path: ~/.m2
    key: ${{ runner.os }}-m2-${{ hashFiles('**/pom.xml') }}
```

### Parallel execution:
```yaml
# Matrix strategy for parallel builds
strategy:
  matrix:
    java-version: [17, 21]
```

### Conditional execution:
```yaml
# Skip on draft PRs
if: github.event.pull_request.draft == false
```

## Migration from Travis CI

### Key differences:
- Configuration in `.github/workflows/` instead of `.travis.yml`
- YAML syntax differences (jobs vs script)
- Better caching and artifact management
- Native GitHub integration

### What changed:
- Java versions: 9 → 17, 21
- Build time: ~10min → ~6min
- Caching: Basic → Advanced multi-layer
- Integration: External → Native GitHub

## Getting Help

### Quick debugging:
1. Check workflow status in Actions tab
2. Review job logs for error messages
3. Download artifacts for detailed analysis
4. Compare with successful runs

### Resources:
- [GitHub Actions Docs](https://docs.github.com/en/actions)
- [Marketplace](https://github.com/marketplace?type=actions)
- [Community Forum](https://github.community/)

### Project-specific:
- Check `docs/GITHUB_ACTIONS_MIGRATION_GUIDE.md` for detailed troubleshooting
- Review workflow files in `.github/workflows/`
- Use GitHub Discussions for questions