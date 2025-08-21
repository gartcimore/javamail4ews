package org.gartcimore.java.mailsample;

import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import java.util.Arrays;
import java.util.Properties;

public class MailSample {


    public static void main(String[] args) throws MessagingException {

        String url = "https://outlook.office365.com/ews/exchange.asmx";
        Properties properties = new Properties();
        properties.setProperty("org.sourceforge.net.javamail4ews.util.Util.EnableServiceTrace", "true");
        Session session = Session.getInstance(properties);

        System.out.println("retrieving the store");
        //Get the EWS store implementation
        Store store = session.getStore("ewsstore");

        System.out.println("done");

        //Connect to the Exchange server - No port required.
        //Also connect() might be used if the session is initialized with the known mail.* properties
        System.out.printf("connecting to %s%n", url);
        store.connect(url, "username@outlook.com", "MyP4ssW0rd!");

        System.out.printf("connected to %s%n", url);
        System.out.println("get default folder");
        Folder folder = store.getDefaultFolder();
        System.out.println("open default folder as read-only");
        folder.open(Folder.READ_ONLY);
        System.out.println("done");
        Message[] messages = folder.getMessages();
        System.out.printf("there is %d messages%n", messages.length);

        if (messages.length > 0) {
            System.out.println(Arrays.toString(messages[0].getFrom()));
        }

        final Folder sentItems = store.getFolder("SentItems");
        sentItems.open(Folder.READ_ONLY);
        int sentItemsMessageCount = sentItems.getMessageCount();
        System.out.printf("there is %d sent messages%n", sentItemsMessageCount);

        //Get the EWS transport implementation
        Transport lTransport = session.getTransport("ewstransport");

        //Connect to the Exchange server - No port required.
        //Also connect() might be used if the session is initialized with the known mail.* properties
        lTransport.connect(url, "username@outlook.com", "MyP4ssW0rd!");

        //Create a message as before
        MimeMessage lMessage = new MimeMessage(session);
        lMessage.setRecipient(Message.RecipientType.TO, new InternetAddress("user@example.org"));
        lMessage.setSubject("Hello World!");
        lMessage.setText("Hello World!");

        lMessage.addHeaderLine("X_MY_Custom_Header=myValue");

        //Send the mail via EWS
        lTransport.sendMessage(lMessage, lMessage.getRecipients(Message.RecipientType.TO));


    }
}
