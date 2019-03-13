/*
 * Copyright (c) 2016, salesforce.com, inc. All rights reserved. Licensed under the BSD 3-Clause license. For full
 * license text, see LICENSE.TXT file in the repo root or https://opensource.org/licenses/BSD-3-Clause
 */
package com.salesforce.emp.connector.example;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import com.salesforce.emp.connector.BayeuxParameters;
import com.salesforce.emp.connector.EmpConnector;
import com.salesforce.emp.connector.TopicSubscription;
import org.cometd.bayeux.Channel;
import org.eclipse.jetty.util.ajax.JSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An example of using the EMP connector using bearer tokens
 *
 * @author hal.hildebrand
 * @since API v37.0
 */
public class BearerTokenExample {
    private static Logger logger = LoggerFactory.getLogger(BearerTokenExample.class);

    public static void main(String[] argv) throws Exception {
        logger.info("==>main");
        if (argv.length < 2 || argv.length > 4) {
            System.err.println("Usage: BearerTokenExample url token topic [replayFrom]");
            System.exit(1);
        }
        long replayFrom = EmpConnector.REPLAY_FROM_EARLIEST;
        if (argv.length == 4) {
            replayFrom = Long.parseLong(argv[3]);
        }

        BayeuxParameters params = new BayeuxParameters() {

            @Override
            public String bearerToken() {
                logger.info("==>bearerToken");
                return argv[1];
            }

            @Override
            public URL host() {
                logger.info("==>host");
                try {
                    return new URL(argv[0]);
                } catch (MalformedURLException e) {
                    throw new IllegalArgumentException(String.format("Unable to create url: %s", argv[0]), e);
                }
            }
        };

        Consumer<Map<String, Object>> consumer = event -> logger.info("Received: {}", JSON.toString(event));
        EmpConnector connector = new EmpConnector(params);

/*
        connector.addListener(Channel.META_CONNECT, new LoggingListener(true, true))
                .addListener(Channel.META_DISCONNECT, new LoggingListener(true, true))
                .addListener(Channel.META_HANDSHAKE, new LoggingListener(true, true));
*/

        try {
            connector.start().get(5, TimeUnit.SECONDS);

            if (connector.isConnected()) {

                TopicSubscription subscription = connector.subscribe(argv[2], replayFrom, consumer).get(5, TimeUnit.SECONDS);

                logger.info("Subscribed: {}", subscription);
            }

            Thread.sleep(3000);
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
            System.exit(1);
        } finally {
            if (connector.isConnected())
                connector.stop();
        }
        logger.info("<==main");
    }
}
