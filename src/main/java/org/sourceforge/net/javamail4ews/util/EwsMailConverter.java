package org.sourceforge.net.javamail4ews.util;

import java.io.ByteArrayInputStream;
import java.io.UnsupportedEncodingException;
import java.util.Iterator;

import javax.mail.MessagingException;
import javax.mail.Flags.Flag;
import javax.mail.Message.RecipientType;
import javax.mail.internet.InternetAddress;

import org.sourceforge.net.javamail4ews.store.EwsFolder;
import org.sourceforge.net.javamail4ews.store.EwsMessage;

import microsoft.exchange.webservices.data.BasePropertySet;
import microsoft.exchange.webservices.data.EmailAddress;
import microsoft.exchange.webservices.data.EmailAddressCollection;
import microsoft.exchange.webservices.data.EmailMessage;
import microsoft.exchange.webservices.data.ItemSchema;
import microsoft.exchange.webservices.data.PropertySet;
import microsoft.exchange.webservices.data.ServiceLocalException;

public class EwsMailConverter {
	private final EmailMessage emailMessage;
	private final EwsFolder session;
	private final int msgnr;
	private EwsMessage message;

	public EwsMailConverter(EwsFolder session, EmailMessage pEmailMessage, int msgnr) {
		this.session = session;
		this.emailMessage = pEmailMessage;
		this.msgnr = msgnr;
	}
	
	@Override
	protected Object clone() throws CloneNotSupportedException {
		throw new CloneNotSupportedException();
	}
	
	public EwsMessage convert() throws MessagingException, ServiceLocalException, UnsupportedEncodingException {
		 message = new EwsMessage(
					session,
					msgnr,
					emailMessage);
		 try {
//			emailMessage.load();
			PropertySet oPropSetForBodyText = new PropertySet(BasePropertySet.FirstClassProperties);
			oPropSetForBodyText.add(ItemSchema.MimeContent);
			emailMessage.load(oPropSetForBodyText);
		} catch (Exception e) {
			throw new MessagingException(e.getMessage(), e);
		}
		 
		 byte[] bContent = emailMessage.getMimeContent().getContent();
		 ByteArrayInputStream stream = new ByteArrayInputStream(bContent);
		 message.createFromStream(stream);
         setAllHeaders();
         
         return message;
	}
	
	protected void setAllHeaders() throws ServiceLocalException, UnsupportedEncodingException, MessagingException {
		//FROM
		setFromHeaders();
		//TO
		setToHeaders();
		//CC
		setCcHeaders();
		//BCC
		setBccHeaders();
		//Dates
		setDates();
		//Subject
		setSubject();
		//Flags
		setFlags();
	}

	protected void setFlags() throws MessagingException, ServiceLocalException {
		message.setFlag(Flag.DRAFT, emailMessage.getIsDraft());
		message.setFlag(Flag.SEEN, emailMessage.getIsRead().booleanValue());
		//TODO exception
//		message.setFlag(Flag.FLAGGED, emailMessage.getIsReminderSet());
	}
	
	protected void setSubject() throws MessagingException, ServiceLocalException {
		String lSubject = emailMessage.getSubject();
		//EWS reports NULL subjects - causes NPEs in javax.mail
		if (lSubject == null) {
			lSubject = "";
		}
		message.setSubject(lSubject);
	}

	protected void setDates() throws MessagingException, ServiceLocalException {
		message.setSentDate(emailMessage.getDateTimeSent());
		message.setReceivedDate(emailMessage.getDateTimeReceived());
	}

	private InternetAddress emailAddress2InternetAddress(EmailAddress pEmailAddress) throws UnsupportedEncodingException {
	    String internetPart = pEmailAddress.getAddress();
	    String personalPart = pEmailAddress.getName();
        return new InternetAddress((internetPart != null ? internetPart.trim() : ""),
                (personalPart != null ? personalPart.trim() : personalPart));
        
	}
	
	private InternetAddress[] emailAddressCollection2InternetAddresses(EmailAddressCollection pCollections) throws UnsupportedEncodingException {
		InternetAddress[] retValue  = new InternetAddress[pCollections.getCount()];
		int i=0;
		Iterator<EmailAddress> emailAddressIterator = pCollections.iterator();
		while(emailAddressIterator.hasNext()) {
			EmailAddress aEmailAddress = emailAddressIterator.next();
			
			retValue[i++] = emailAddress2InternetAddress(aEmailAddress);
		}
		
		return retValue;
	}
	
	protected void setToHeaders() throws ServiceLocalException, UnsupportedEncodingException, MessagingException {
		EmailAddressCollection lTo = emailMessage.getToRecipients();
		message.setRecipients(RecipientType.TO, emailAddressCollection2InternetAddresses(lTo));
	}

	protected void setCcHeaders() throws ServiceLocalException, UnsupportedEncodingException, MessagingException {
		EmailAddressCollection lCc = emailMessage.getCcRecipients();
		message.setRecipients(RecipientType.CC, emailAddressCollection2InternetAddresses(lCc));
	}
	
	protected void setBccHeaders() throws ServiceLocalException, UnsupportedEncodingException, MessagingException {
		EmailAddressCollection lBcc = emailMessage.getBccRecipients();
		message.setRecipients(RecipientType.BCC, emailAddressCollection2InternetAddresses(lBcc));
	}
	
	protected void setFromHeaders() throws ServiceLocalException, UnsupportedEncodingException, MessagingException  {
		EmailAddress lFrom = emailMessage.getFrom();
		message.setFrom(emailAddress2InternetAddress(lFrom));
	}

}
