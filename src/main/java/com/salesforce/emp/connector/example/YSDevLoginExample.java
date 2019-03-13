/*
 * Copyright (c) 2019. WebPals
 */
package com.salesforce.emp.connector.example;

import com.salesforce.emp.connector.BayeuxParameters;
import com.salesforce.emp.connector.EmpConnector;
import com.salesforce.emp.connector.LoginHelper;
import com.salesforce.emp.connector.TopicSubscription;
import org.eclipse.jetty.util.ajax.JSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

import static org.cometd.bayeux.Channel.*;

/**
 * An example of using the EMP connector
 *
 * @author hal.hildebrand
 * @author Yuriy Stul
 * @since API v37.0
 */
public class YSDevLoginExample {
    private static Logger logger = LoggerFactory.getLogger(YSDevLoginExample.class);

    public static void main(String[] argv) throws Throwable {
        logger.info("==>main");
        var url = System.getenv("EM_ENDPOINT");
        var username = System.getenv("EM_USER");
        var password = System.getenv("EM_PASS");
        var token = System.getenv("EM_TOKEN");
//        var topic = "/topic/Test";
        var topic = "/data/ChangeEvents";

        Consumer<Map<String, Object>> consumer = event -> logger.info(String.format("Received:\n%s", JSON.toString(event)));

        BearerTokenProvider tokenProvider = new BearerTokenProvider(() -> {
            try {
                return LoginHelper.login(new URL(url), username, password + token);
            } catch (Exception e) {
                logger.error("Failed during login: " + e.getMessage(), e);
                throw new RuntimeException(e);
            }
        });

        try {
            logger.info("Login ...");
            BayeuxParameters params = tokenProvider.login();
            logger.info("Logged in");

            EmpConnector empConnector = new EmpConnector(params);
            var loggingListener = new YSLoggingListener(true, true);

            empConnector.addListener(META_HANDSHAKE, loggingListener)
                    .addListener(META_CONNECT, loggingListener)
                    .addListener(META_DISCONNECT, loggingListener)
                    .addListener(META_SUBSCRIBE, loggingListener)
                    .addListener(META_UNSUBSCRIBE, loggingListener);

            empConnector.setBearerTokenProvider(tokenProvider);

            logger.info("Starting connection...");
            empConnector.start().get(5, TimeUnit.SECONDS);
            logger.info("Started connection");

            long replayFrom = EmpConnector.REPLAY_FROM_EARLIEST;
/*
            if (argv.length == 5) {
                replayFrom = Long.parseLong(argv[4]);
            }
*/
            TopicSubscription subscription;
            try {
                logger.info("Subscribing...");
                subscription = empConnector.subscribe(topic, replayFrom, consumer).get(5, TimeUnit.SECONDS);
            } catch (ExecutionException e) {
                logger.error(e.getMessage(), e);
//                empConnector.stop();
                System.exit(1);
                throw e.getCause();
            } catch (TimeoutException e) {
                logger.error("Timed out subscribing");
//                empConnector.stop();
                System.exit(1);
                throw e.getCause();
            }

            logger.info(String.format("Subscribed: %s", subscription));
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
        }
        logger.info("<==main");
    }
}
