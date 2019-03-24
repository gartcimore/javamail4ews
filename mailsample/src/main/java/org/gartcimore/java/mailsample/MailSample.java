package org.gartcimore.java.mailsample;

import javax.mail.*;
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
        System.out.println(String.format("connecting to %s", url));
        store.connect(url, "username@outlook.com", "MyP4ssW0rd!");

        System.out.println(String.format("connected to %s", url));
        System.out.println("get default folder");
        Folder folder = store.getDefaultFolder();
        System.out.println("open default folder as read-only");
        folder.open(Folder.READ_ONLY);
        System.out.println("done");
        Message[] messages = folder.getMessages();
        System.out.println(String.format("there is %d messages", messages.length));

        if (messages.length > 0) {
            System.out.println(Arrays.toString(messages[0].getFrom()));
        }

        final Folder sentItems = store.getFolder("SentItems");
        sentItems.open(Folder.READ_ONLY);
        int sentItemsMessageCount = sentItems.getMessageCount();
        System.out.println(String.format("there is %d sent messages", sentItemsMessageCount));

    }
}
