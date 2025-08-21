# javamail4ews
This is a bridge between [Exchange web services](https://github.com/OfficeDev/ews-java-api) java API and JavaMail API
This is a fork of org.sourceforge.net.javamail4ews
You will need the EWS url to connect, something like *https://owa.example.com/ews/exchange.asmx* and maybe a username and password

![CI Pipeline](https://github.com/gartcimore/javamail4ews/workflows/CI%20Pipeline/badge.svg) ![Release](https://github.com/gartcimore/javamail4ews/workflows/Release/badge.svg)

## Requirements

- **Java 21 or higher** (LTS recommended)
- Maven 3.6.0 or higher
- Access to Microsoft Exchange Web Services

## Recent Updates (2025)

This project has been upgraded with recent dependencies and Java 21 compatibility:
- Migrated from javax.mail to Jakarta Mail API 2.1.1
- Updated to Java 21 (from Java 8)
- Upgraded all Maven plugins to latest versions
- Resolved security vulnerabilities in dependencies
- Improved performance and maintainability

See [MIGRATION_GUIDE.md](docs/MIGRATION_GUIDE.md) for detailed upgrade information.

## CI/CD Pipeline

This project uses GitHub Actions for continuous integration and deployment:

- **CI Pipeline**: Runs on every push and pull request, testing against Java 17 and 21
- **Release Pipeline**: Automatically deploys tagged releases to GitHub Packages
- **Workflow Validation**: Weekly validation of CI/CD setup

See [GITHUB_ACTIONS_MIGRATION_GUIDE.md](docs/GITHUB_ACTIONS_MIGRATION_GUIDE.md) for detailed information about the CI/CD setup.


# Reading emails
```java
//Initalize a session
Session session = Session.getInstance(new Properties());


//Get the EWS store implementation
Store store = session.getStore("ewsstore");

//Connect to the Exchange server - No port required.
//Also connect() might be used if the session is initalized with the known mail.* properties
store.connect("https://example.com/ews/exchange.asmx",
                                "test@example.com",
                                "password");

Folder folder = store.getDefaultFolder();
folder.open(Folder.READ_ONLY);
Message[] messages = folder.getMessages();
```

# Sending emails
```java
//Initalize a session
Session session = Session.getInstance(new Properties());

//Get the EWS transport implementation
Transport lTransport = session.getTransport("ewstransport");

//Connect to the Exchange server - No port required.
//Also connect() might be used if the session is initalized with the known mail.* properties
lTransport.connect("https://example.com/ews/exchange.asmx",
                    "test@example.com",
                    "password");

//Create a message as before
Message lMessage = new MimeMessage(session);
lMessage.setRecipient(RecipientType.TO, new InternetAddress("user@example.com"));
lMessage.setSubject("Hello World!");
lMessage.setText("Hello World!");

//Send the mail via EWS
lTransport.sendMessage(lMessage, lMessage.getRecipients(RecipientType.TO));
```

## Sample Application
The `mailsample` module provides a working example:

```bash
# Build the sample
cd mailsample
mvn clean compile

# Run the sample (requires EWS configuration)
mvn exec:java -Dexec.mainClass="org.gartcimore.java.mailsample.MailSample"
```

## Configuration

### EWS Connection Properties
Create a properties file or set system properties:

```properties
# EWS server configuration
mail.ewsstore.server=https://owa.example.com/ews/exchange.asmx
mail.ewsstore.username=user@example.com
mail.ewsstore.password=your-password

# Optional: Trust all certificates (for testing only)
mail.ewsstore.trustall=true
```

### JavaMail Session Configuration
```java
Properties props = new Properties();
props.setProperty("mail.store.protocol", "ewsstore");
props.setProperty("mail.transport.protocol", "ewstransport");
props.setProperty("mail.ewsstore.server", "https://owa.example.com/ews/exchange.asmx");

Session session = Session.getInstance(props);
```

## Building and Testing

### Build Requirements
Ensure you have Java 21+ and Maven 3.6+ installed:
```bash
java -version  # Should show Java 21 or higher
mvn -version   # Should show Maven 3.6.0 or higher
```

### Building the Project
```bash
# Clean build with all tests
mvn clean compile test

# Build without tests
mvn clean compile -DskipTests

# Package the JAR
mvn clean package
```

### Running Tests

#### All Tests
```bash
mvn clean test
```

#### Baseline Tests (Core Functionality)
```bash
mvn clean test -Pbaseline-tests
```

#### Performance Tests
```bash
mvn clean test -Pperformance-tests
```

### Verifying the Upgrade
After upgrading from an older version, run these commands to verify everything works:

```bash
# 1. Verify compilation with Java 21
mvn clean compile

# 2. Run baseline tests to ensure core functionality
mvn clean test -Pbaseline-tests

# 3. Check for dependency issues
mvn dependency:analyze

# 4. Run security scan (optional)
mvn org.owasp:dependency-check-maven:check
```

