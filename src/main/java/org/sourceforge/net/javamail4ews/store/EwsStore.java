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

import org.apache.commons.configuration.Configuration;
import org.sourceforge.net.javamail4ews.util.Util;

import javax.mail.Folder;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.URLName;

import microsoft.exchange.webservices.data.ExchangeService;
import microsoft.exchange.webservices.data.FolderId;
import microsoft.exchange.webservices.data.WellKnownFolderName;

public class EwsStore extends Store {

  private ExchangeService service;

  public EwsStore(Session session, URLName urlname) {
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

  @Override
  public EwsFolder getDefaultFolder() throws MessagingException {
    return new EwsFolder(this, new FolderId(WellKnownFolderName.Inbox));
  }

  @Override
  public EwsFolder getFolder(String name) throws MessagingException {
    try {
      return new EwsFolder(this, name, new FolderId(WellKnownFolderName.Inbox));
    } catch (Exception e) {
      throw new MessagingException(e.getMessage(), e);
    }
  }

  @Override
  public EwsFolder getFolder(URLName url) throws MessagingException {
    // TODO Auto-generated method stub
    return null;
  }

  protected ExchangeService getService() {
    return service;
  }

  //Make visible
  @Override
  protected void notifyFolderListeners(int type, Folder folder) {
    super.notifyFolderListeners(type, folder);
  }

  @Override
  //Make visible
  protected void notifyConnectionListeners(int type) {
    super.notifyConnectionListeners(type);
  }

  protected Configuration getConfiguration() {
    return Util.getConfiguration(session);
  }
}
