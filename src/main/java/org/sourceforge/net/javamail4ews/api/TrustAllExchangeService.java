/*******************************************************************************
 *              Copyright (C) Nexti SA PTY LTD 2016. All Rights reserved.
 * @author      John Bester
 * Project:     TODO: Project description 
 * Description: TODO: File / class description
 *
 * Changelog  
 *  $Log: CustomExchangeService.java,v $
 *  Created on Jun 22, 2018
 *******************************************************************************/

package org.sourceforge.net.javamail4ews.api;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.logging.Level;
import java.util.logging.Logger;
import jakarta.mail.MessagingException;
import microsoft.exchange.webservices.data.EWSConstants;
import microsoft.exchange.webservices.data.core.CookieProcessingTargetAuthenticationStrategy;
import microsoft.exchange.webservices.data.core.EwsSSLProtocolSocketFactory;
import microsoft.exchange.webservices.data.core.ExchangeService;
import microsoft.exchange.webservices.data.core.enumeration.misc.ExchangeVersion;
import org.apache.http.client.AuthenticationStrategy;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.ssl.SSLContextBuilder;

/**
 * This class is used to ignore certificates if "trust=*" parameter
 * is specified or when trustAllCertificates() is called
 * @author john
 * @model
 */
public class TrustAllExchangeService extends ExchangeService {
    private static final Logger LOGGER = Logger.getLogger(TrustAllExchangeService.class.getName());

    public TrustAllExchangeService(ExchangeVersion requestedServerVersion) throws MessagingException {
        super(requestedServerVersion);
        try {
            initializeHttpClient();
        } catch (Exception e) {
            throw new MessagingException(e.getMessage(), e);
        }
    }

    public TrustAllExchangeService() throws MessagingException {
        super();
        try {
            initializeHttpClient();
        } catch (Exception e) {
            throw new MessagingException(e.getMessage(), e);
        }
    }

    private void initializeHttpClient() throws Exception {
        @SuppressWarnings({ "unchecked", "rawtypes" })
        Registry<ConnectionSocketFactory> registry = (Registry) RegistryBuilder.create()
                .register(EWSConstants.HTTP_SCHEME, new PlainConnectionSocketFactory())
                .register(EWSConstants.HTTPS_SCHEME, EwsSSLProtocolSocketFactory.build(
                        null, NoopHostnameVerifier.INSTANCE
                ))
                .build();
                
        @SuppressWarnings("resource")
        HttpClientConnectionManager httpConnectionManager = new PoolingHttpClientConnectionManager(registry);
        AuthenticationStrategy authStrategy = new CookieProcessingTargetAuthenticationStrategy();

        httpClient = HttpClients.custom()
                .setConnectionManager(httpConnectionManager)
                .setTargetAuthenticationStrategy(authStrategy)
                .setSslcontext(new SSLContextBuilder().loadTrustMaterial(null, new TrustStrategy() {
                    public boolean isTrusted(X509Certificate[] arg0, String arg1) throws CertificateException
                    {
                        for (X509Certificate certificate : arg0)
                        {
                            LOGGER.log(Level.INFO, "Check isTrusted for {0}.", certificate.toString());
                        }
                        return true;
                    }
                }).build())
                .build();
    }
}