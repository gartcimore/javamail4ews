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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.mail.Flags;
import javax.mail.Flags.Flag;
import javax.mail.FolderNotFoundException;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.event.ConnectionEvent;
import javax.mail.event.FolderEvent;
import javax.mail.event.FolderListener;
import javax.mail.search.FlagTerm;
import javax.mail.search.SearchTerm;

import org.apache.commons.configuration.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sourceforge.net.javamail4ews.util.EwsMailConverter;

import microsoft.exchange.webservices.data.core.ExchangeService;
import microsoft.exchange.webservices.data.core.PropertySet;
import microsoft.exchange.webservices.data.core.enumeration.property.BasePropertySet;
import microsoft.exchange.webservices.data.core.enumeration.property.WellKnownFolderName;
import microsoft.exchange.webservices.data.core.enumeration.search.LogicalOperator;
import microsoft.exchange.webservices.data.core.enumeration.service.ConflictResolutionMode;
import microsoft.exchange.webservices.data.core.enumeration.service.DeleteMode;
import microsoft.exchange.webservices.data.core.exception.service.local.ServiceLocalException;
import microsoft.exchange.webservices.data.core.exception.service.remote.ServiceResponseException;
import microsoft.exchange.webservices.data.core.service.folder.Folder;
import microsoft.exchange.webservices.data.core.service.item.EmailMessage;
import microsoft.exchange.webservices.data.core.service.item.Item;
import microsoft.exchange.webservices.data.core.service.schema.EmailMessageSchema;
import microsoft.exchange.webservices.data.core.service.schema.FolderSchema;
import microsoft.exchange.webservices.data.core.service.schema.ItemSchema;
import microsoft.exchange.webservices.data.property.complex.FolderId;
import microsoft.exchange.webservices.data.property.complex.ItemId;
import microsoft.exchange.webservices.data.search.FindFoldersResults;
import microsoft.exchange.webservices.data.search.FindItemsResults;
import microsoft.exchange.webservices.data.search.FolderView;
import microsoft.exchange.webservices.data.search.ItemView;
import microsoft.exchange.webservices.data.search.filter.SearchFilter;

//TODO Disconnected event for ConnectionListeners
public class EwsFolder extends javax.mail.Folder {
    private final class FolderListenerPublisher implements FolderListener {
        @Override
        public void folderCreated(FolderEvent e) {
            getStore().notifyFolderListeners(e.getType(), EwsFolder.this);
        }

        @Override
        public void folderDeleted(FolderEvent e) {
            getStore().notifyFolderListeners(e.getType(), EwsFolder.this);
        }

        @Override
        public void folderRenamed(FolderEvent e) {
            getStore().notifyFolderListeners(e.getType(), EwsFolder.this);
        }
    }

    private final int ITEM_VIEW_MAX_ITEMS;

    private final ConflictResolutionMode CONFLICT_RESOLUTION_MODE;

    private final DeleteMode DELETE_MODE;

    private static final Logger logger = LoggerFactory.getLogger(EwsFolder.class);

    private final Folder parentFolder;

    private boolean prefetchItems = true;

    private String name;

    private Folder folder;

    private List<EwsMessage> messages;

    private List<EwsMessage> unreadMessages;

    private Date timestamp;

    private Folder INBOX;

    private EwsFolder(EwsStore store, FolderId pFolderId, FolderId pParentFolderId) {
        super(store);
        addFolderListener(new FolderListenerPublisher());

        ITEM_VIEW_MAX_ITEMS = getConfiguration().getInt(
                "org.sourceforge.net.javamail4ews.store.EwsFolder.ItemViewMaxItems");
        CONFLICT_RESOLUTION_MODE = ConflictResolutionMode.valueOf(getConfiguration().getString(
                "org.sourceforge.net.javamail4ews.store.EwsFolder.ConflictResolutionMode"));
        DELETE_MODE = DeleteMode.valueOf(getConfiguration().getString(
                "org.sourceforge.net.javamail4ews.store.EwsFolder.DeleteMode"));
        prefetchItems = getConfiguration().getBoolean("org.sourceforge.net.javamail4ews.store.EwsFolder.prefetchItems");
        try {
            INBOX = Folder.bind(getService(), new FolderId(WellKnownFolderName.Inbox));

            if (pFolderId != null) {
                folder = Folder.bind(getService(), pFolderId);
                parentFolder = Folder.bind(getService(), folder.getParentFolderId());
            } else if (pParentFolderId != null) {
                parentFolder = Folder.bind(getService(), pParentFolderId);
            } else {
                throw new IllegalArgumentException("pFolderId and pParentFolderId are null!");
            }
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    protected EwsFolder(EwsStore store, FolderId pFolderId) {
        this(store, pFolderId, null);
    }

    protected EwsFolder(EwsStore store, String pName, FolderId pParentFolderId) {
        this(store, (FolderId) null, pParentFolderId);
        this.name = pName;
    }

    @Override
    public void appendMessages(Message[] msgs) throws MessagingException {
        // TODO Auto-generated method stub

    }

    @Override
    public void close(boolean expunge) throws MessagingException {
        if (mode == javax.mail.Folder.READ_WRITE) {
            try {
                if (expunge) {
                    expunge();
                }
                // Update the messages
                markMessageRead(messages);
                markMessageRead(unreadMessages);
                // and the folder itself
                folder.update();

            } catch (Exception e) {
                // Close anyway
                throw new MessagingException(e.getMessage(), e);
            } finally {
                folder = null;
                getStore().notifyConnectionListeners(ConnectionEvent.CLOSED);
            }
        }
    }

    private void markMessageRead(List<EwsMessage> messagesRead) throws MessagingException, Exception,
      ServiceResponseException {
        if (messagesRead == null || messagesRead.isEmpty())
            return;
        for (EwsMessage aMessage : messagesRead) {
            EmailMessage aRawMessage = aMessage.getEmailMessage();

            if (aMessage.getFlags().contains(Flag.SEEN)) {
                aRawMessage.setIsRead(Boolean.TRUE);
            } else {
                aRawMessage.setIsRead(Boolean.FALSE);
            }

            aRawMessage.update(CONFLICT_RESOLUTION_MODE);
        }
    }

    @Override
    public boolean create(int type) throws MessagingException {
        try {
            folder.save(parentFolder.getId());
            notifyFolderListeners(FolderEvent.CREATED);
            return true;
        } catch (Exception e) {
            throw new MessagingException(e.getMessage(), e);
        }
    }

    @Override
    public boolean delete(boolean recurse) throws MessagingException {
        if (isOpen()) {
            throw new IllegalStateException("Folder not closed!");
        }
        try {
            if (recurse) {
                for (javax.mail.Folder aFolder : list()) {
                    aFolder.delete(recurse);
                }
            } else {
                // Simplest approach
                if (getMessageCount() > 0) {
                    return false;
                }
            }

            folder.delete(DELETE_MODE);
            notifyFolderListeners(FolderEvent.DELETED);
            return true;
        } catch (Exception e) {
            throw new MessagingException(e.getMessage(), e);
        }
    }

    @Override
    public boolean exists() throws MessagingException {
        if (folder != null) {
            return true;
        }
        try {
            FolderView view = new FolderView(1);
            SearchFilter searchFilter = new SearchFilter.IsEqualTo(FolderSchema.DisplayName, name);
            FindFoldersResults lResult = getService().findFolders(WellKnownFolderName.Inbox, searchFilter, view);
            if (lResult.getTotalCount() > 0) {
                folder = lResult.getFolders().get(0);
                INBOX = Folder.bind(getService(), new FolderId(WellKnownFolderName.Inbox));
                return true;
            } else {
                return false;
            }
        } catch (Exception e) {
            throw new MessagingException(e.getMessage(), e);
        }
    }

    @Override
    public Message[] expunge() throws MessagingException {
        deleteMessages(unreadMessages);
        return deleteMessages(messages);
    }

    private Message[] deleteMessages(List<EwsMessage> messagesToDelete) throws MessagingException {
        if (messagesToDelete == null || messagesToDelete.isEmpty())
            return new Message[] {};
        List<EwsMessage> lDeletedMessages = new ArrayList<EwsMessage>();
        for (int i = messagesToDelete.size() - 1; i >= 0; i--) {
            EwsMessage aMessage = messagesToDelete.get(i);
            if (aMessage.getFlags().contains(Flag.DELETED)) {
                try {
                    EmailMessage aRawMessage = aMessage.getEmailMessage();
                    aRawMessage.delete(DELETE_MODE);
                } catch (Exception e) {
                    throw new MessagingException(e.getMessage(), e);
                }
                lDeletedMessages.add(aMessage);
                messagesToDelete.remove(i);
            }
        }
        Message[] retValue = lDeletedMessages.toArray(new Message[0]);
        if (retValue.length > 0) {
            notifyMessageRemovedListeners(true, retValue);
        }
        return retValue;
    }

    @Override
    public EwsFolder getFolder(String name) throws MessagingException {
        return list(name)[0];
    }

    @Override
    public String getFullName() {
        try {
            StringBuilder sb = new StringBuilder();
            if (getParent() != null) {
                sb.append(getParent().getFullName()).append(getSeparator());
            }
            sb.append(folder.getDisplayName());
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @Override
    public EwsMessage getMessage(int msgnum) throws MessagingException {
        // 1 based!
        EwsMessage lMessage = messages.get(msgnum - 1);
        // lMessage.setFlag(Flag.SEEN, true);
        return lMessage;
    }

    @Override
    public int getMessageCount() throws MessagingException {
        if (prefetchItems) {
            return messages.size();
        } else {
            try {
                return folder.getTotalCount();
            } catch (Exception e) {
                throw new MessagingException(e.getMessage(), e);
            }
        }
    }

    @Override
    public synchronized int getNewMessageCount() throws MessagingException {
        return 0;
    }

    @Override
    public synchronized int getUnreadMessageCount() throws MessagingException {
        ItemView unreadView = new ItemView(ITEM_VIEW_MAX_ITEMS);

        PropertySet setIdOnly = new PropertySet(BasePropertySet.IdOnly);
        unreadView.setPropertySet(setIdOnly);

        SearchFilter unreadFilter = new SearchFilter.SearchFilterCollection(LogicalOperator.And,
                new SearchFilter.IsEqualTo(EmailMessageSchema.IsRead, Boolean.FALSE));

        FindItemsResults<Item> findResults;
        try {
            findResults = getService().findItems(folder.getId(), unreadFilter, unreadView);
        } catch (Exception e) {
            throw new MessagingException(e.getMessage());
        }
        return findResults.getTotalCount();
    }

    @Override
    public String getName() {
        try {
            return folder.getDisplayName();
        } catch (ServiceLocalException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @Override
    public EwsFolder getParent() throws MessagingException {
        try {
            if (!exists()) {
                throw new IllegalStateException("Folder does not exist!");
            }
            // Strange equals method in FolderId...
            if (folder.getId().getUniqueId().equals(INBOX.getId().getUniqueId())) {
                return null;
            } else {
                return new EwsFolder(getStore(), folder.getParentFolderId());
            }
        } catch (ServiceLocalException e) {
            throw new MessagingException(e.getMessage(), e);
        }
    }

    @Override
    public Flags getPermanentFlags() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public char getSeparator() throws MessagingException {
        return '/';
    }

    @Override
    public int getType() throws MessagingException {
        return javax.mail.Folder.HOLDS_MESSAGES | javax.mail.Folder.HOLDS_FOLDERS;
    }

    @Override
    public boolean hasNewMessages() throws MessagingException {
        ItemView view = new ItemView(ITEM_VIEW_MAX_ITEMS);
        SearchFilter.SearchFilterCollection search = new SearchFilter.SearchFilterCollection();
        search.add(new SearchFilter.IsGreaterThanOrEqualTo(ItemSchema.DateTimeReceived, timestamp));
        FindItemsResults<Item> lFindResults;
        try {
            lFindResults = getService().findItems(folder.getId(), search, view);

            if (lFindResults.getTotalCount() > 0) {
                return true;
            } else {
                return false;
            }
        } catch (Exception e) {
            throw new MessagingException(e.getMessage(), e);
        }
    }

    @Override
    public boolean isOpen() {
        return folder != null;
    }

    @Override
    public EwsFolder[] list(String pattern) throws MessagingException {
        FolderView lFolderView = new FolderView(ITEM_VIEW_MAX_ITEMS);
        FindFoldersResults lFindFoldersResults;
        try {
            SearchFilter.SearchFilterCollection lSearchFilter = new SearchFilter.SearchFilterCollection();
            if (!pattern.equals("%")) {
                // TODO incomplete implementation - wildcards are not implemented yet
                lSearchFilter.add(new SearchFilter.IsEqualTo(FolderSchema.DisplayName, pattern));
            }
            lFindFoldersResults = folder.findFolders(lSearchFilter, lFolderView);
            List<Folder> lFolders = lFindFoldersResults.getFolders();

            EwsFolder[] retValue = new EwsFolder[lFolders.size()];
            for (int i = 0; i < retValue.length; i++) {
                retValue[i] = new EwsFolder(getStore(), lFolders.get(i).getId());
            }

            return retValue;
        } catch (Exception e) {
            throw new MessagingException(e.getMessage(), e);
        }
    }

    @Override
    public void open(int mode) throws MessagingException {
        this.mode = mode;
        try {
            if (!exists()) {
                throw new FolderNotFoundException();
            }
            ItemView view = new ItemView(ITEM_VIEW_MAX_ITEMS);
            folder = Folder.bind(getService(), folder.getId());

            if (prefetchItems) {
                FindItemsResults<Item> lFindResults = getService().findItems(folder.getId(), view);
                messages = new ArrayList<>(lFindResults.getTotalCount());
                unreadMessages = new ArrayList<>();
                for (Item aItem : lFindResults) {
                    if (aItem instanceof EmailMessage) {
                        logger.info("Fetching content of item {}", aItem.getId());

                        EmailMessage aEmailMessage = (EmailMessage) aItem;

                        EwsMailConverter aConverter = new EwsMailConverter(this, aEmailMessage, messages.size() + 1);

                        messages.add(aConverter.convert());

                    } else {
                        logger.warn("Skipping item {} as it is a {}", aItem.getId(), aItem.getClass());
                    }
                }
            } else {

            }
            timestamp = new Date();
            getStore().notifyConnectionListeners(ConnectionEvent.OPENED);
        } catch (Exception e) {
            throw new MessagingException(e.getMessage(), e);
        }
    }

    @Override
    public boolean renameTo(javax.mail.Folder f) throws MessagingException {
        if (isOpen()) {
            throw new IllegalStateException("Folder must be closed!");
        }
        FolderId targetFolderId;
        if (f instanceof EwsFolder) {
            targetFolderId = ((EwsFolder) f).folder.getId();
        } else {
            targetFolderId = getFolder(f.getFullName()).folder.getId();
        }
        try {
            folder.move(targetFolderId);
            getStore().notifyFolderListeners(FolderEvent.RENAMED, this);
            return true;
        } catch (Exception e) {
            throw new MessagingException(e.getMessage(), e);
        }
    }

    @Override
    public EwsStore getStore() {
        return (EwsStore) super.getStore();
    }

    protected ExchangeService getService() {
        EwsStore lStore = (EwsStore) getStore();
        return lStore.getService();
    }

    private Configuration getConfiguration() {
        return getStore().getConfiguration();
    }

    public Message[] getUnreadMessage(int maxMessages) throws Exception {

        ItemView unreadView = null;
        if(maxMessages == 0) {
                unreadView = new ItemView(ITEM_VIEW_MAX_ITEMS);
        } else {
            unreadView = new ItemView((maxMessages < ITEM_VIEW_MAX_ITEMS ? maxMessages : ITEM_VIEW_MAX_ITEMS ));
        }

        SearchFilter unreadFilter = new SearchFilter.SearchFilterCollection(LogicalOperator.And,
                new SearchFilter.IsEqualTo(EmailMessageSchema.IsRead, Boolean.FALSE));

        FindItemsResults<Item> findResults = getService().findItems(folder.getId(), unreadFilter, unreadView);
        unreadMessages = new ArrayList<>(findResults.getTotalCount());
        Message[] a = new Message[findResults.getTotalCount()];
        for (Item aItem : findResults) {
            if (aItem instanceof EmailMessage) {
                EmailMessage aEmailMessage = (EmailMessage) aItem;
                EwsMailConverter aConverter = new EwsMailConverter(this, aEmailMessage, unreadMessages.size() + 1);
                unreadMessages.add(aConverter.convert());
            } else {
                logger.warn("Skipping item {} as it is a {}", aItem.getId(), aItem.getClass());
            }
        }
        return unreadMessages.toArray(a);
    }

    public synchronized Message[] search(SearchTerm paramSearchTerm) throws MessagingException {
        if (paramSearchTerm instanceof FlagTerm) {
            FlagTerm flagTerm = (FlagTerm) paramSearchTerm;
            Flags flags = flagTerm.getFlags();
            if (flags.equals(new Flags(Flags.Flag.SEEN))) {
                try {
                    return getUnreadMessage(0);
                } catch (Exception e) {
                    throw new MessagingException(e.getMessage(),e);
                }
            }
        }
        throw new MessagingException("Ews folder don't support search with search term " + paramSearchTerm);
    }

    /**
     * Move an item that was not retrieve to another folder, so :
     * <ul>
     *  <li> it can be checked later
     *  <li> it does not prevent other e-mails to be retrieved
     * </ul>
     * @param id id of the item to be moved
     * @param failedItemFolderName name of a folder where id is moved. If it does not exists, it will be created (if possible)
     * @throws Exception
     */
    public void moveFailedItemTo(String id, String failedItemFolderName) throws Exception {
        FolderId failedFolderId = findFolder(failedItemFolderName);
        if(failedFolderId == null ) {
            failedFolderId = createFolder(failedItemFolderName, folder.getParentFolderId());
        }
        ArrayList<ItemId> ids = new ArrayList<ItemId>(1);
        ids.add(new ItemId(id));
        getService().moveItems(ids, failedFolderId);
        
    }
    
    public FolderId findFolder(String folderName) throws ServiceLocalException, Exception {
        FolderView folderView = new FolderView(ITEM_VIEW_MAX_ITEMS);

        SearchFilter folderNameFilter = new SearchFilter.SearchFilterCollection(LogicalOperator.And,
                new SearchFilter.IsEqualTo(FolderSchema.DisplayName, folderName));

        FindFoldersResults folderResults = getService().findFolders(folder.getParentFolderId(),folderNameFilter,folderView);
        FolderId folderId = null;
        if(folderResults.getTotalCount() > 0) {
            logger.debug("folder '"+folderName+"' found");
            folderId = folderResults.getFolders().get(0).getId();
        }
        return folderId;
    }

    public FolderId createFolder(String folderName, FolderId parentFolderId) throws Exception {
        logger.warn("creating folder '" + folderName + "', sub-directory of '" + parentFolderId + "'");
        Folder failedFolder = new Folder(getService());
        failedFolder.setDisplayName(folderName);
        failedFolder.save(parentFolderId);
        return failedFolder.getId();
    }
    

    public boolean testCreateFolderOperation(String folderName) throws Exception {
        FolderId folderId = findFolder(folderName);
        if(folderId == null) {
            logger.debug("folder '"+folderName+"' not found, creating it");
            try {
                Folder failedFolder = new Folder(getService());
                failedFolder.setDisplayName(folderName);
                failedFolder.save(folder.getParentFolderId());
            } catch(Exception e) {
                throw new Exception("could not create folder named '"+folderName+"'",e);
            }
        }
        return true;
    }
}
