/*
 * Copyright (c) 2016, salesforce.com, inc.
 * All rights reserved.
 * Licensed under the BSD 3-Clause license.
 * For full license text, see LICENSE.TXT file in the repo root  or https://opensource.org/licenses/BSD-3-Clause
 */
package com.salesforce.emp.connector.example;

import com.salesforce.emp.connector.BayeuxParameters;
import com.salesforce.emp.connector.EmpConnector;
import com.salesforce.emp.connector.TopicSubscription;
import org.eclipse.jetty.util.ajax.JSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static com.salesforce.emp.connector.LoginHelper.login;

/**
 * An example of using the EMP connector using login credentials
 *
 * @author hal.hildebrand
 * @since API v37.0
 */
public class LoginExample {
    private static Logger logger = LoggerFactory.getLogger(LoginExample.class);

    public static void main(String[] argv) throws Exception {
        long replayFrom = EmpConnector.REPLAY_FROM_EARLIEST;

        BearerTokenProvider tokenProvider = new BearerTokenProvider(() -> {
            try {
                var userName = System.getenv("EM_USER");
                var userPassword = System.getenv("EM_PASS");
                var loginEndpoint = System.getenv("EM_ENDPOINT");

                logger.info("Login with {} as user to {}", userName, loginEndpoint);

                return login(new URL(loginEndpoint), userName, userPassword);

            } catch (Exception e) {
                logger.error(e.getMessage(), e);
                System.exit(1);
                throw new RuntimeException(e);
            }
        });

        BayeuxParameters params = tokenProvider.login();

        Consumer<Map<String, Object>> consumer = event -> System.out.println(String.format("Received:\n%s", JSON.toString(event)));

        EmpConnector connector = new EmpConnector(params);

        connector.setBearerTokenProvider(tokenProvider);

        connector.start().get(5, TimeUnit.SECONDS);

        var topic = "someTestTopic";

        TopicSubscription subscription = connector.subscribe(topic, replayFrom, consumer).get(5, TimeUnit.SECONDS);

        System.out.println(String.format("Subscribed: %s", subscription));
    }
}
