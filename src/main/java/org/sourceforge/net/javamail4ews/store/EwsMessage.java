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
package org.sourceforge.net.javamail4ews.store;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Date;

import javax.mail.Folder;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.InternetHeaders;
import javax.mail.internet.MimeMessage;

import microsoft.exchange.webservices.data.BasePropertySet;
import microsoft.exchange.webservices.data.EmailMessage;
import microsoft.exchange.webservices.data.ItemSchema;
import microsoft.exchange.webservices.data.PropertySet;
import microsoft.exchange.webservices.data.ServiceLocalException;

public class EwsMessage extends MimeMessage {

  private final EmailMessage emailMessage;
  private Date receivedDate;

  public EwsMessage(Folder folder,
                    int msgnum, EmailMessage pEmailMessage) throws MessagingException, ServiceLocalException {
    super(folder, new InternetHeaders(), new byte[0], msgnum);
    emailMessage = pEmailMessage;
  }

  public EwsMessage(Session session) {
    super(session);
    emailMessage = null;
  }

  protected EmailMessage getEmailMessage() {
    return emailMessage;
  }

  @Override
  public Date getReceivedDate() throws MessagingException {
    return receivedDate;
  }

  public void setReceivedDate(Date pDate) {
    this.receivedDate = pDate;
  }

  @Override
  protected InputStream getContentStream() throws MessagingException {
    try {
      PropertySet oPropSetForBodyText = new PropertySet(BasePropertySet.FirstClassProperties);
      oPropSetForBodyText.add(ItemSchema.MimeContent);
      emailMessage.load(oPropSetForBodyText);
      byte[] lContent = emailMessage.getMimeContent().getContent();

      return new ByteArrayInputStream(lContent);
    } catch (Exception e) {
      throw new MessagingException(e.getMessage());
    }
  }

}
