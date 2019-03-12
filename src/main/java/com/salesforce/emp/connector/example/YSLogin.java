/*
 * Copyright (c) 2019. WebPals
 */

package com.salesforce.emp.connector.example;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.util.ByteBufferContentProvider;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.ByteArrayInputStream;
import java.io.UnsupportedEncodingException;
import java.net.ConnectException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.Collections;

/**
 * @author Yuriy Stul
 */
public class YSLogin {
    public static final String COMETD_REPLAY = "/cometd/";
    public static final String COMETD_REPLAY_OLD = "/cometd/replay/";

    private static class LoginResponseParser extends DefaultHandler {

        private String buffer;
        private String faultstring;

        private boolean reading = false;
        private String serverUrl;
        private String sessionId;

        @Override
        public void characters(char[] ch, int start, int length) {
            if (reading) buffer = new String(ch, start, length);
        }

        @Override
        public void endElement(String uri, String localName, String qName) {
            reading = false;
            switch (localName) {
                case "sessionId":
                    sessionId = buffer;
                    break;
                case "serverUrl":
                    serverUrl = buffer;
                    break;
                case "faultstring":
                    faultstring = buffer;
                    break;
                default:
            }
            buffer = null;
        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) {
            switch (localName) {
                case "sessionId":
                    reading = true;
                    break;
                case "serverUrl":
                    reading = true;
                    break;
                case "faultstring":
                    reading = true;
                    break;
                default:
            }
        }
    }

    private static Logger logger = LoggerFactory.getLogger(YSLogin.class);
    private static final String ENV_END = "</soapenv:Body></soapenv:Envelope>";
    private static final String ENV_START = "<soapenv:Envelope xmlns:soapenv='http://schemas.xmlsoap.org/soap/envelope/' "
            + "xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' "
            + "xmlns:urn='urn:partner.soap.sforce.com'><soapenv:Body>";

    public static void main(String[] args) {
        logger.info("==>main");
        HttpClient client = new HttpClient(new SslContextFactory());
        try {
            client.getProxyConfiguration().getProxies().addAll(Collections.emptyList());
            client.start();
            URL endpoint = new URL(new URL(System.getenv("EM_ENDPOINT")), "/services/Soap/u/44.0/");
            Request post = client.POST(endpoint.toURI());
            post.content(new ByteBufferContentProvider("text/xml",
                    ByteBuffer.wrap(soapXmlForLogin(System.getenv("EM_USER"),
                            System.getenv("EM_PASS")))));
            post.header("SOAPAction", "''");
            post.header("PrettyPrint", "Yes");
            ContentResponse response = post.send();
            SAXParserFactory spf = SAXParserFactory.newInstance();
            spf.setFeature("http://xml.org/sax/features/external-general-entities", false);
            spf.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            spf.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            spf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            spf.setNamespaceAware(true);
            SAXParser saxParser = spf.newSAXParser();

            var parser = new LoginResponseParser();
            saxParser.parse(new ByteArrayInputStream(response.getContent()), parser);

            String sessionId = parser.sessionId;
            if (sessionId == null || parser.serverUrl == null) {
                throw new ConnectException(
                        String.format("Unable to login: %s", parser.faultstring));
            }

            URL soapEndpoint = new URL(parser.serverUrl);
            String cometdEndpoint = Float.parseFloat("43.0") < 37 ? COMETD_REPLAY_OLD : COMETD_REPLAY;
            URL replayEndpoint = new URL(soapEndpoint.getProtocol(),
                    soapEndpoint.getHost(),
                    soapEndpoint.getPort(),
                    cometdEndpoint + "43.0");

            logger.info("sessionId: {}, soapEndpoint: {}, replayEndpoint: {}",
                    sessionId, soapEndpoint, replayEndpoint);
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
        } finally {
            try {
                client.stop();
                client.destroy();
            } catch (Exception ex) {
                logger.error(ex.getMessage(), ex);
            }
        }
        logger.info("<==main");
    }

    private static byte[] soapXmlForLogin(String username, String password) throws UnsupportedEncodingException {
        return (ENV_START + "  <urn:login>" + "    <urn:username>" + username + "</urn:username>" + "    <urn:password>"
                + password + "</urn:password>" + "  </urn:login>" + ENV_END).getBytes("UTF-8");
    }
}
