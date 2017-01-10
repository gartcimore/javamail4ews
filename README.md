# javamail4ews
This is a bridge between [Exchange web services](https://github.com/OfficeDev/ews-java-api) java API and JavaMail API

You will need the EWS url to connect, something like *https://owa.example.com/ews/exchange.asmx* and maybe a username and password

#Reading emails
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

#Sending emails
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
