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
package org.sourceforge.net.javamail4ews.util;

import java.net.ConnectException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import javax.mail.AuthenticationFailedException;
import javax.mail.MessagingException;
import javax.mail.Session;

import org.apache.commons.configuration.CompositeConfiguration;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import microsoft.exchange.webservices.data.core.ExchangeService;
import microsoft.exchange.webservices.data.core.enumeration.misc.ExchangeVersion;
import microsoft.exchange.webservices.data.core.enumeration.property.WellKnownFolderName;
import microsoft.exchange.webservices.data.core.service.folder.Folder;
import microsoft.exchange.webservices.data.credential.ExchangeCredentials;
import microsoft.exchange.webservices.data.credential.WebCredentials;

public final class Util {
	private static final Logger logger = LoggerFactory.getLogger("org.sourceforge.net.javamail4ews"); 
	
	static {
		logger.info("JavaMail 4 EWS loaded in version {}\nUses Microsoft(R) software", getVersion());
	}
	
	private Util() {
	}

	@Override
	protected Object clone() throws CloneNotSupportedException {
		throw new CloneNotSupportedException();
	}
	
	public static String getVersion() {
		Package lPackage = Util.class.getPackage();
		return lPackage.getImplementationVersion();
	}
	
	
	public static Configuration getConfiguration(Session pSession) {
		try {
			PropertiesConfiguration prop = new PropertiesConfiguration();
			for(Object aKey : pSession.getProperties().keySet()) {
				Object aValue = pSession.getProperties().get(aKey);
				
				prop.addProperty(aKey.toString(), aValue);
			}
			
			CompositeConfiguration config = new CompositeConfiguration();
			config.addConfiguration(prop);
			URL lURL = Thread.currentThread().getContextClassLoader().getResource("javamail-ews-bridge.default.properties");
			config.addConfiguration(new PropertiesConfiguration(lURL));
			return config;
		} catch (ConfigurationException e) {
			throw new RuntimeException(e.getMessage(), e);
		}
	}
	
	public static ExchangeService getExchangeService(String host, int port, String user,
			String password, Session pSession) throws MessagingException {
		if (user == null) {
			return null;
		}
		if (password == null) {
			return null;
		}

		String version = getConfiguration(pSession).getString(
				"org.sourceforge.net.javamail4ews.ExchangeVersion", "");
		ExchangeVersion serverVersion = null;
		if (!version.isEmpty()) {
			try {
				serverVersion = Enum.valueOf(ExchangeVersion.class, version);
			} catch (IllegalArgumentException e) {
				logger.info("Unknown version for exchange server: '" + version
						+ "' using default : no version specified");
			}
		}
		boolean enableTrace = getConfiguration(pSession).getBoolean("org.sourceforge.net.javamail4ews.util.Util.EnableServiceTrace");
		ExchangeService service = null;
		if (serverVersion != null) {
			service = new ExchangeService(serverVersion);
		} else {
		      service = new ExchangeService();
		}
		Integer connectionTimeout = getConnectionTimeout(pSession);
        Integer protocolTimeout = getProtocolTimeout(pSession);
        if(connectionTimeout != null) {
//            service.setConnectionTimeout(connectionTimeout.intValue());
					// TODO should be still possible to set connection timeout
          logger.debug("setting connection timeout to {} is ignored", connectionTimeout);
        }
        if(protocolTimeout != null) {
//            service.setProtocolTimeout(protocolTimeout.intValue());
          // TODO should be still possible to set protocol timeout
          logger.debug("setting protocol timeout to {} is ignored", protocolTimeout);
        }
		service.setTraceEnabled(enableTrace);

		ExchangeCredentials credentials = new WebCredentials(user, password);
		service.setCredentials(credentials);

		try {
			service.setUrl(new URI(host));
		} catch (URISyntaxException e) {
			throw new MessagingException(e.getMessage(), e);
		}

		try {
			//Bind to check if connection parameters are valid
			if (getConfiguration(pSession).getBoolean("org.sourceforge.net.javamail4ews.util.Util.VerifyConnectionOnConnect")) {
				logger.debug("Connection settings : trying to verify them");
				Folder.bind(service, WellKnownFolderName.Inbox);
				logger.info("Connection settings verified.");
			} else {
				logger.info("Connection settings not verified yet.");
			}
			return service;
		} catch (Exception e) {
		    Throwable cause = e.getCause();
		    if(cause != null) {
		        if (cause instanceof ConnectException) {
		            Exception nested = (ConnectException) cause;
		            throw new MessagingException(nested.getMessage(), nested);
                }
		    }
		    throw new AuthenticationFailedException(e.getMessage());
		}
	}

    private static Integer getConnectionTimeout(Session pSession) {
        Integer connectionTimeout = null;
        String cnxTimeoutStr = pSession.getProperty("mail.pop3.connectiontimeout");
        if(cnxTimeoutStr != null) {
            connectionTimeout = Integer.valueOf(cnxTimeoutStr);
        }
        return connectionTimeout;
    }
    
    private static Integer getProtocolTimeout(Session pSession) {
        Integer protocolTimeout = null;
        String protTimeoutStr = pSession.getProperty("mail.pop3.timeout");
        if( protTimeoutStr != null) {
            protocolTimeout = Integer.valueOf(protTimeoutStr);
        }
        return protocolTimeout;
    }
}
