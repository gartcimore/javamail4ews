package org.gartcimore.java.mailsample;

import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Store;

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
    //Also connect() might be used if the session is initalized with the known mail.* properties
    System.out.println(String.format("connecting to %s", url));
    store.connect(url,
      "myuser@example.org",
      "p4ssw0rd");

    System.out.println(String.format("connected to %s", url));
    System.out.println(String.format("get default folder", url));
    Folder folder = store.getDefaultFolder();
    System.out.println(String.format("open default folder as read-only"));
    folder.open(Folder.READ_ONLY);
    System.out.println(String.format("done"));
    Message[] messages = folder.getMessages();
    System.out.println(String.format("there is %d messages", messages.length));

    System.out.println(messages[0].getFrom());
  }
}
