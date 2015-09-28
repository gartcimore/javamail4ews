/*
The JavaMail4EWS project.
Copyright (C) 2011  Sebastian Just

This library is free software; you can redistribute it and/or
modify it under the terms of the GNU Lesser General Public
License as published by the Free Software Foundation; either
version 3.0 of the License, or (at your option) any later version.

This library is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
Lesser General Public License for more details.

You should have received a copy of the GNU Lesser General Public
License along with this library; if not, write to the Free Software
Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
package org.sourceforge.net.javamail4ews.transport;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sourceforge.net.javamail4ews.util.Util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Enumeration;

import javax.mail.Address;
import javax.mail.BodyPart;
import javax.mail.Header;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Part;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.URLName;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import microsoft.exchange.webservices.data.BodyType;
import microsoft.exchange.webservices.data.EmailMessage;
import microsoft.exchange.webservices.data.ExchangeService;
import microsoft.exchange.webservices.data.FileAttachment;
import microsoft.exchange.webservices.data.MessageBody;
import microsoft.exchange.webservices.data.ServiceLocalException;
import microsoft.exchange.webservices.data.ServiceVersionException;
import microsoft.exchange.webservices.data.WellKnownFolderName;

public class EwsTransport extends Transport {

  private static final String TEXT_PLAIN = "text/plain";
  private static final String TEXT_HTML = "text/html";

  private static final Logger logger = LoggerFactory.getLogger(EwsTransport.class);

  private ExchangeService service;

  public EwsTransport(Session session, URLName urlname) {
    super(session, urlname);
  }

  @Override
  protected boolean protocolConnect(String host, int port, String user,
                                    String password) throws MessagingException {
    service = Util.getExchangeService(host, port, user, password, session);
    if (service == null) {
      return false;
    } else {
      return true;
    }
  }

  public void sendMessage(Message pMessage, Address[] addresses, Address[] ccaddresses, Address[] bccaddresses) throws MessagingException {
    try {
      EmailMessage msg = new EmailMessage(getService());

      createHeaders(msg, pMessage);

      createAddresses(msg, pMessage, addresses, ccaddresses, bccaddresses);
      createSubject(msg, pMessage);
      createBody(msg, pMessage);

      sendMessage(msg);

    } catch (MessagingException e) {
      throw e;
    } catch (Exception e) {
      throw new MessagingException(e.getMessage(), e);
    }
  }

  private void sendMessage(EmailMessage msg) throws Exception {
    if (getConfiguration().getBoolean("org.sourceforge.net.javamail4ews.transport.EwsTransport.SendAndSaveCopy")) {
      msg.sendAndSaveCopy(WellKnownFolderName.SentItems);
    } else {
      msg.send();
    }
  }

  private void createHeaders(EmailMessage msg, Message message) {
    //TODO create headers
    //TODO Add X-Creator
  }

  public void sendMessage(Message pMessage, Address[] addresses, Address[] ccaddresses) throws MessagingException {
    sendMessage(pMessage, addresses, ccaddresses, new Address[0]);
  }

  @Override
  public void sendMessage(Message pMessage, Address[] addresses) throws MessagingException {
    sendMessage(pMessage, addresses, new Address[0]);
  }

  private byte[] bodyPart2ByteArray(BodyPart pPart) throws IOException, MessagingException {
    ByteArrayOutputStream os = new ByteArrayOutputStream();
    pPart.getDataHandler().writeTo(os);
    return os.toByteArray();
  }

  private void createBody(EmailMessage msg, Message message) throws Exception {
    MessageBody mb;
    Object rawcontent = message.getContent();
    if (rawcontent instanceof MimeMultipart) {
      logger.debug("Multipart message found");
      mb = createBodyMultipart(msg, (MimeMultipart) rawcontent);
    } else if (rawcontent instanceof String) {
      logger.debug("Only text found - handling as plaintext");
      mb = MessageBody.getMessageBodyFromText((String) rawcontent);
    } else {
      throw new IllegalArgumentException(rawcontent.getClass() + " is an unexpected type!");
    }

    msg.setBody(mb);
  }

  @SuppressWarnings("unchecked")
  private String getFirstHeaderValue(BodyPart part, String pKey) throws MessagingException {
    Enumeration<Header> lMatchingHeaders = part.getMatchingHeaders(new String[]{pKey});

    if (lMatchingHeaders.hasMoreElements()) {
      Header lHeader = lMatchingHeaders.nextElement();
      String lValue = lHeader.getValue();

      return lValue;
    }
    return null;
  }

  private MessageBody createBodyMultipart(EmailMessage msg, MimeMultipart multipart)
      throws MessagingException, IOException, ServiceLocalException, ServiceVersionException {
    MessageBody mb = new MessageBody();
    for (int i = 0; i < multipart.getCount(); i++) {
      logger.trace("Working on item {}", i);
      BodyPart lBodyPart = multipart.getBodyPart(i);

      String lDisposition = lBodyPart.getDisposition();
      logger.debug("Disposition: {}", lDisposition);
      if ((lDisposition == null || lDisposition.equalsIgnoreCase(Part.ATTACHMENT)) && (i != 0)) {
        // treat as attachment if not first part
        byte[] lContentBytes = bodyPart2ByteArray(lBodyPart);

        FileAttachment lNewAttachment;

        String lContentId = getFirstHeaderValue(lBodyPart, "Content-ID");
        if (lContentId != null) {
          lNewAttachment = msg.getAttachments().addFileAttachment(lContentId, lContentBytes);
          lNewAttachment.setContentId(lContentId);
          lNewAttachment.setIsInline(true);

          logger.debug("Attached {} bytes as content {}", lContentBytes.length, lContentId);
        } else {
          String fileName = lBodyPart.getFileName();
          lNewAttachment = msg.getAttachments().addFileAttachment(fileName, lContentBytes);
          lNewAttachment.setIsInline(false);

          logger.debug("Attached {} bytes as file {}", lContentBytes.length, fileName);
        }
        lNewAttachment.setIsContactPhoto(false);
      } else {
        String text = lBodyPart.getContent().toString();
        logger.debug("Setting mail text to {}", text);

        String contentType = lBodyPart.getDataHandler().getContentType();
        if (contentType == null) {
          contentType = lBodyPart.getContentType();
        }

        mb.setText(text);
        //Test or HTML - hopefully
        //TODO multipart with HTML and plain text?
        if (TEXT_HTML.equals(contentType)) {
          logger.debug("Handling mail text as HTML");
          mb.setBodyType(BodyType.HTML);
        } else {
          if (!TEXT_PLAIN.equals(contentType)) {
            logger.warn("Handling content type {} as plain text!", lBodyPart.getContentType());
          }
          logger.debug("Handling mail text as plaintext");
          mb.setBodyType(BodyType.Text);
        }
      }
    }
    return mb;
  }

  @SuppressWarnings("unchecked")
  private <T> T[] mergeArrays(T[] a, T[] b) {
    return (T[]) ArrayUtils.addAll(a, b);
  }

  private void createAddresses(EmailMessage pEmailMessage, Message pMessage, Address[] pToAddresses, Address[] pCcAddresses,
                               Address[] pBccAddresses) throws ServiceLocalException, MessagingException {
    if (pMessage instanceof MimeMessage) {
      MimeMessage lMimeMessage = (MimeMessage) pMessage;

      pToAddresses = mergeArrays(pToAddresses, lMimeMessage.getRecipients(javax.mail.Message.RecipientType.TO));
      pCcAddresses = mergeArrays(pCcAddresses, lMimeMessage.getRecipients(javax.mail.Message.RecipientType.CC));
      pBccAddresses = mergeArrays(pCcAddresses, lMimeMessage.getRecipients(javax.mail.Message.RecipientType.BCC));
    }

    for (Address aAddress : pToAddresses) {
      logger.info("Adding adress {} as TO recepient", aAddress.toString());
      pEmailMessage.getToRecipients().add(aAddress.toString());
    }
    for (Address aAddress : pCcAddresses) {
      logger.info("Adding adress {} as CC recepient", aAddress.toString());
      pEmailMessage.getCcRecipients().add(aAddress.toString());
    }
    for (Address aAddress : pBccAddresses) {
      logger.info("Adding adress {} as BCC recepient", aAddress.toString());
      pEmailMessage.getBccRecipients().add(aAddress.toString());
    }
  }

  private void createSubject(EmailMessage msg, Message message) throws MessagingException, Exception {
    msg.setSubject(message.getSubject());
  }

  private ExchangeService getService() {
    return service;
  }

  private Configuration getConfiguration() {
    return Util.getConfiguration(session);
  }
}
