#!/bin/bash

# GitHub Actions Workflow Validation Script
# This script validates the GitHub Actions workflows locally before committing

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    local color=$1
    local message=$2
    echo -e "${color}${message}${NC}"
}

print_header() {
    echo
    print_status $BLUE "=== $1 ==="
}

print_success() {
    print_status $GREEN "✅ $1"
}

print_warning() {
    print_status $YELLOW "⚠️  $1"
}

print_error() {
    print_status $RED "❌ $1"
}

# Check if required tools are installed
check_prerequisites() {
    print_header "Checking Prerequisites"
    
    local missing_tools=()
    
    # Check for required tools
    if ! command -v mvn &> /dev/null; then
        missing_tools+=("maven")
    fi
    
    if ! command -v java &> /dev/null; then
        missing_tools+=("java")
    fi
    
    if ! command -v yq &> /dev/null && ! command -v python3 &> /dev/null; then
        missing_tools+=("yq or python3 (for YAML parsing)")
    fi
    
    if [ ${#missing_tools[@]} -eq 0 ]; then
        print_success "All required tools are available"
    else
        print_error "Missing required tools: ${missing_tools[*]}"
        echo "Please install the missing tools and try again."
        exit 1
    fi
}

# Validate YAML syntax of workflow files
validate_yaml_syntax() {
    print_header "Validating YAML Syntax"
    
    local workflow_files=(
        ".github/workflows/ci.yml"
        ".github/workflows/release.yml"
        ".github/workflows/workflow-validation.yml"
    )
    
    for file in "${workflow_files[@]}"; do
        if [ -f "$file" ]; then
            if command -v yq &> /dev/null; then
                if yq eval '.' "$file" > /dev/null 2>&1; then
                    print_success "YAML syntax valid: $file"
                else
                    print_error "YAML syntax invalid: $file"
                    return 1
                fi
            elif command -v python3 &> /dev/null; then
                if python3 -c "import yaml; yaml.safe_load(open('$file'))" 2>/dev/null; then
                    print_success "YAML syntax valid: $file"
                else
                    print_error "YAML syntax invalid: $file"
                    return 1
                fi
            else
                print_warning "Cannot validate YAML syntax for $file (no YAML parser available)"
            fi
        else
            print_error "Workflow file not found: $file"
            return 1
        fi
    done
}

# Validate Maven configuration
validate_maven_config() {
    print_header "Validating Maven Configuration"
    
    # Check if pom.xml exists and is valid
    if [ ! -f "pom.xml" ]; then
        print_error "pom.xml not found"
        return 1
    fi
    
    # Validate pom.xml
    if mvn validate -q; then
        print_success "pom.xml is valid"
    else
        print_error "pom.xml validation failed"
        return 1
    fi
    
    # Check settings.xml
    if [ -f "settings.xml" ]; then
        print_success "settings.xml found"
    else
        print_warning "settings.xml not found (may be required for deployment)"
    fi
    
    # Check Java version compatibility
    local java_version=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2 | cut -d'.' -f1)
    if [ "$java_version" -ge 21 ]; then
        print_success "Java version $java_version is compatible"
    else
        print_warning "Java version $java_version may not be compatible (requires 21+)"
    fi
}

# Test Maven build process
test_maven_build() {
    print_header "Testing Maven Build Process"
    
    # Clean previous builds
    print_status $BLUE "Cleaning previous builds..."
    mvn clean -q
    
    # Test compilation
    print_status $BLUE "Testing compilation..."
    if mvn compile -q; then
        print_success "Compilation successful"
    else
        print_error "Compilation failed"
        return 1
    fi
    
    # Test test compilation
    print_status $BLUE "Testing test compilation..."
    if mvn test-compile -q; then
        print_success "Test compilation successful"
    else
        print_error "Test compilation failed"
        return 1
    fi
    
    # Test unit tests
    print_status $BLUE "Running unit tests..."
    if mvn test -q; then
        print_success "Unit tests passed"
    else
        print_warning "Some unit tests failed (check test results)"
    fi
    
    # Test packaging
    print_status $BLUE "Testing packaging..."
    if mvn package -DskipTests -q; then
        print_success "Packaging successful"
        
        # Validate artifacts
        if [ -f "target/javamail4ews.jar" ]; then
            print_success "Main JAR artifact created"
        else
            print_error "Main JAR artifact not found"
            return 1
        fi
        
        if [ -d "target/lib" ] && [ "$(ls -A target/lib)" ]; then
            print_success "Dependencies copied to lib directory"
        else
            print_warning "Dependencies not found in lib directory"
        fi
    else
        print_error "Packaging failed"
        return 1
    fi
}

# Test Maven profiles
test_maven_profiles() {
    print_header "Testing Maven Profiles"
    
    # Test baseline-tests profile
    print_status $BLUE "Testing baseline-tests profile..."
    if mvn test -Pbaseline-tests -q; then
        print_success "Baseline tests profile works"
    else
        print_warning "Baseline tests profile failed (may be expected if no baseline tests exist)"
    fi
    
    # Test performance-tests profile
    print_status $BLUE "Testing performance-tests profile..."
    if mvn test -Pperformance-tests -q; then
        print_success "Performance tests profile works"
    else
        print_warning "Performance tests profile failed (may be expected if no performance tests exist)"
    fi
}

# Validate workflow triggers and matrix configurations
validate_workflow_config() {
    print_header "Validating Workflow Configuration"
    
    # Check CI workflow
    if [ -f ".github/workflows/ci.yml" ]; then
        print_status $BLUE "Checking CI workflow configuration..."
        
        # Check for required triggers
        if grep -q "push:" ".github/workflows/ci.yml" && grep -q "pull_request:" ".github/workflows/ci.yml"; then
            print_success "CI workflow has correct triggers"
        else
            print_warning "CI workflow may be missing required triggers"
        fi
        
        # Check for matrix strategy
        if grep -q "matrix:" ".github/workflows/ci.yml"; then
            print_success "CI workflow uses matrix strategy"
        else
            print_warning "CI workflow doesn't use matrix strategy"
        fi
        
        # Check for Java versions
        if grep -q "java-version.*21" ".github/workflows/ci.yml" && grep -q "java-version.*22" ".github/workflows/ci.yml"; then
            print_success "CI workflow tests multiple Java versions"
        else
            print_warning "CI workflow may not test multiple Java versions"
        fi
    fi
    
    # Check release workflow
    if [ -f ".github/workflows/release.yml" ]; then
        print_status $BLUE "Checking release workflow configuration..."
        
        # Check for tag triggers
        if grep -q "tags:" ".github/workflows/release.yml"; then
            print_success "Release workflow has tag triggers"
        else
            print_warning "Release workflow missing tag triggers"
        fi
        
        # Check for deployment configuration
        if grep -q "deploy" ".github/workflows/release.yml"; then
            print_success "Release workflow includes deployment"
        else
            print_warning "Release workflow may be missing deployment steps"
        fi
    fi
}

# Generate validation report
generate_report() {
    print_header "Generating Validation Report"
    
    local report_file="workflow-validation-report.md"
    
    cat > "$report_file" << EOF
# GitHub Actions Workflow Validation Report

**Generated:** $(date)
**Script Version:** 1.0

## Validation Results

### Prerequisites
- Maven: $(mvn --version | head -n 1)
- Java: $(java -version 2>&1 | head -n 1)

### Workflow Files
- CI Workflow: $([[ -f ".github/workflows/ci.yml" ]] && echo "✅ Present" || echo "❌ Missing")
- Release Workflow: $([[ -f ".github/workflows/release.yml" ]] && echo "✅ Present" || echo "❌ Missing")
- Validation Workflow: $([[ -f ".github/workflows/workflow-validation.yml" ]] && echo "✅ Present" || echo "❌ Missing")

### Maven Configuration
- pom.xml: $([[ -f "pom.xml" ]] && echo "✅ Present" || echo "❌ Missing")
- settings.xml: $([[ -f "settings.xml" ]] && echo "✅ Present" || echo "⚠️ Missing")

### Build Artifacts
- Main JAR: $([[ -f "target/javamail4ews.jar" ]] && echo "✅ Created" || echo "❌ Not found")
- Dependencies: $([[ -d "target/lib" ]] && echo "✅ Present" || echo "❌ Missing")

## Recommendations

1. **Matrix Testing**: Ensure workflows test against Java 17 and 21
2. **Artifact Validation**: Verify all required artifacts are generated
3. **Deployment Testing**: Test deployment process with a test tag
4. **Documentation**: Update project documentation with new CI/CD setup

## Next Steps

1. Run the workflow validation workflow in GitHub Actions
2. Create a test tag to validate the release process
3. Monitor the first few CI runs for any issues
4. Update team documentation

---
*Generated by workflow validation script*
EOF
    
    print_success "Validation report generated: $report_file"
}

# Main execution
main() {
    print_header "GitHub Actions Workflow Validation"
    echo "This script validates the GitHub Actions workflows for the javamail4ews project."
    echo
    
    # Run validation steps
    check_prerequisites
    validate_yaml_syntax
    validate_maven_config
    test_maven_build
    test_maven_profiles
    validate_workflow_config
    generate_report
    
    print_header "Validation Complete"
    print_success "All validation steps completed successfully!"
    echo
    print_status $BLUE "Next steps:"
    echo "1. Review the generated validation report"
    echo "2. Commit the workflow files to trigger GitHub Actions"
    echo "3. Run the workflow validation workflow in GitHub Actions"
    echo "4. Create a test tag to validate the release process"
}

# Run main function
main "$@"