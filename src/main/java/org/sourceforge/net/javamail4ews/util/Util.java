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
import java.sql.SQLException;
import java.text.ParseException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.mail.AuthenticationFailedException;
import javax.mail.MessagingException;
import javax.mail.Session;
import microsoft.exchange.webservices.data.core.ExchangeService;
import microsoft.exchange.webservices.data.core.enumeration.misc.ExchangeVersion;
import microsoft.exchange.webservices.data.core.enumeration.property.WellKnownFolderName;
import microsoft.exchange.webservices.data.core.service.folder.Folder;
import microsoft.exchange.webservices.data.credential.ExchangeCredentials;
import microsoft.exchange.webservices.data.credential.WebCredentials;
import org.apache.commons.configuration.CompositeConfiguration;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.sourceforge.net.javamail4ews.api.TrustAllExchangeService;

public final class Util {
	private static final Logger logger = Logger.getLogger("org.sourceforge.net.javamail4ews");

	static {
		logger.log(Level.INFO, "JavaMail 4 EWS loaded in version {}\nUses Microsoft(R) software", getVersion());
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

    /**
     * Re-throw an exception as a different exception class
     * @param <T>
     * @param error
     * @param clazz
     */
    public static <T extends Throwable> T cast(Throwable error, Class<T> clazz) {
        return cast(error, clazz, null);
    }

    /**
     * Re-throw an exception as a different exception class
     * @param <T>
     * @param error
     * @param clazz
     * @param message
     */
    public static <T extends Throwable> T cast(Throwable error, Class<T> clazz, String message) {
        if (clazz != null && error != null && message == null && clazz.isInstance(error)) {
            @SuppressWarnings("unchecked")
            T result = (T) error;
            return result;
        }
        if (message != null && message.trim().isEmpty())
            message = null;
        String tmp = (error != null ? error.getMessage() : "");
        if (tmp == null)
            tmp = "";
        if (clazz == null) {
            @SuppressWarnings({ "unchecked" })
            Class<T> tmpClazz = (Class<T>) (error != null ? error.getClass() : Exception.class);
            clazz = tmpClazz;
        } else if (error instanceof SQLException && clazz.equals(SQLException.class)) {
            SQLException err = (SQLException) error;
            if (message != null)
                message = err.getMessage();
            @SuppressWarnings("unchecked")
            T result = (T) new SQLException(message, err.getSQLState(), err.getErrorCode());
            result.setStackTrace(error.getStackTrace());
            return result;
        }
        if (error == null) {
            int state = 0;
            T exception = null;
            while (state < 5 && exception == null) {
                try {
                    switch (state) {
                    case 0:
                        exception = clazz.getConstructor(String.class, Throwable.class).newInstance(message, error);
                        break;
                    case 1:
                        exception = clazz.getConstructor(Throwable.class, String.class).newInstance(error, message);
                        break;
                    case 2:
                        exception = clazz.getConstructor(Throwable.class).newInstance(error);
                        break;
                    case 3:
                        exception = clazz.getConstructor(String.class).newInstance(message);
                        break;
                    default:
                        exception = clazz.getConstructor().newInstance();
                        exception.initCause(error);
                    }
                } catch (Exception e) {
                    // Ignore exception
                } finally {
                    state++;
                }
            }
            if (exception == null) {
                ClassCastException cce = new ClassCastException("Could not instantiate an exception of class " + clazz.getName());
                cce.initCause(error);
                throw cce;
            }
            return exception;
        }
        if (clazz.isInstance(error) && (message == null || message.equals(tmp))) {
            @SuppressWarnings("unchecked")
            T result = (T) error;
            return result;
        }

        Throwable cause = error.getCause();
        if (cause != null && cause != error && clazz.isAssignableFrom(cause.getClass()))
            error = cause;

        if (message == null || message.length() == 0)
            message = error.getMessage();
        if (clazz == ParseException.class) {
            @SuppressWarnings("unchecked")
            T result = (T) new ParseException(message, 0);
            result.setStackTrace(error.getStackTrace());
            return result;
        }
        T exception = null;
        int state = 0;
        while (state < 5 && exception == null) {
            try {
                switch (state) {
                case 0:
                    exception = clazz.getConstructor(String.class, Throwable.class).newInstance(message, error);
                    break;
                case 1:
                    exception = clazz.getConstructor(Throwable.class, String.class).newInstance(error, message);
                    break;
                case 2:
                    exception = clazz.getConstructor(Throwable.class).newInstance(error);
                    break;
                case 3:
                    exception = clazz.getConstructor(String.class).newInstance(message);
                    break;
                default:
                    exception = clazz.getConstructor().newInstance();
                    exception.initCause(error);
                }
            } catch (Exception e) {
                // Ignore exception
            } finally {
                state++;
            }
        }
        if (exception == null) {
            ClassCastException cce = new ClassCastException("Could not instantiate an exception of class " + clazz.getName());
            cce.initCause(error);
            throw cce;
        }
        exception.setStackTrace(error.getStackTrace());
        return exception;
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
		    RuntimeException ex = new RuntimeException(e.getMessage(), e);
		    ex.setStackTrace(e.getStackTrace());
		    throw ex; 
		}
	}

    private static String getProperty(String protocol, Session pSession, String... keys) {
        for (String key : keys) {
            if (!key.startsWith("."))
                key = "." + key;
            String result = pSession.getProperty("mail." + protocol + key);
            if (result != null)
                return result;
        }
        return null;
    }
    
	public static String getUsername(String protocol, Session pSession) {
	    return getProperty(protocol, pSession, ".user");
	}
	
    public static String getPassword(String protocol, Session pSession) {
        return getProperty(protocol, pSession, ".user");
    }
    
    public static String getTrust(String protocol, Session pSession) {
        return getProperty(protocol, pSession, ".trust", ".ssl.trust", ".https.trust");
    }
    
	public static ExchangeService getExchangeService(String protocol, String host, int port, String user, String password, Session pSession) throws MessagingException {
		if (user == null) 
			user = getUsername(protocol, pSession);
        if (user == null) 
            return null;
        if (password == null) 
            password = getPassword(protocol, pSession);
		if (password == null) 
			return null;
		String trust = getTrust(protocol, pSession); 
		
        String version = getProperty(protocol, pSession, "exchange", "version", "exchange.version");
        if (version == null || version.trim().isEmpty())
            version = getConfiguration(pSession).getString("org.sourceforge.net.javamail4ews.ExchangeVersion", "");
		ExchangeVersion serverVersion = null;
		if (!version.isEmpty()) {
			try {
				serverVersion = Enum.valueOf(ExchangeVersion.class, version);
			} catch (IllegalArgumentException e) {
				logger.info("Unknown version for exchange server: '" + version
						+ "' using default : no version specified");
			}
		}
		Configuration config = getConfiguration(pSession); 
		boolean enableTrace = config.getBoolean("org.sourceforge.net.javamail4ews.util.Util.EnableServiceTrace", false);
		ExchangeService service = null;
		if (trust != null && trust.trim().equals("*")) {
		    logger.log(Level.FINE, "trusting all certificates");
            if (serverVersion != null) {
                service = new TrustAllExchangeService(serverVersion);
            } else {
                service = new TrustAllExchangeService();
            }
		} else {
    		if (serverVersion != null) {
    			service = new ExchangeService(serverVersion);
    		} else {
    		    service = new ExchangeService();
    		}
		}
		Integer connectionTimeout = getConnectionTimeout(pSession);
        Integer protocolTimeout = getProtocolTimeout(pSession);
        if(connectionTimeout != null) {
            logger.log(Level.FINE, "setting timeout to {} using connection timeout value", connectionTimeout);
            service.setTimeout(connectionTimeout.intValue());
        }
        if(protocolTimeout != null) {
          logger.log(Level.FINE, "setting protocol timeout to {0} is ignored", protocolTimeout);
        }
		service.setTraceEnabled(enableTrace);

		ExchangeCredentials credentials = new WebCredentials(user, password);
		service.setCredentials(credentials);

		try {
		    String url = host;
		    if (!url.contains("://"))
		        url = "https://" + url;
		    if (!url.contains("/ews/"))
		        url += "/ews/Exchange.asmx";
			service.setUrl(new URI(url));
		} catch (URISyntaxException e) {
			throw new MessagingException(e.getMessage(), e);
		}

		try {
			//Bind to check if connection parameters are valid
			if (getConfiguration(pSession).getBoolean("org.sourceforge.net.javamail4ews.util.Util.VerifyConnectionOnConnect", false)) {
				logger.log(Level.FINE, "Connection settings : trying to verify them");
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
		            MessagingException error = new MessagingException(nested.getMessage(), nested);
		            error.setStackTrace(nested.getStackTrace());
		            throw error;
                }
		    }
		    AuthenticationFailedException error = new AuthenticationFailedException(e.getMessage());
            error.setStackTrace(e.getStackTrace());
		    throw error;
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
