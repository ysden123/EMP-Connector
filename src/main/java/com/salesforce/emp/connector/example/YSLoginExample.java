/*
 * Copyright (c) 2019. WebPals
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
 * Example of login with token
 *
 * @author Yuriy Stul
 */
public class YSLoginExample {
    private static Logger logger = LoggerFactory.getLogger(YSLoginExample.class);

    public static void main(String[] args) {
        logger.info("==>main");

        var replayFrom = EmpConnector.REPLAY_FROM_EARLIEST;

        var params = new BayeuxParameters() {
            /**
             * @return the bearer token used to authenticate
             */
            @Override
            public String bearerToken() {
                logger.info("Getting token...");
                return System.getenv("EM_TOKEN");
            }

/*
            @Override
            public URL host() {
                try {
                    return new URL(System.getenv("EM_ENDPOINT"));
                } catch (Exception ex) {
                    logger.error("Failed building URL: {}", ex.getMessage());
                    System.exit(1);
                    return null;
                }
            }
*/
        };

        BearerTokenProvider tokenProvider = new BearerTokenProvider(() -> {
            try {
                var userName = System.getenv("EM_USER");
                var userPassword = System.getenv("EM_PASS");
                var loginEndpoint = System.getenv("EM_ENDPOINT");

                logger.info("Login with {} as user to {}", userName, loginEndpoint);

                return login(new URL(loginEndpoint), userName, userPassword, params);

            } catch (Exception e) {
                logger.error(e.getMessage(), e);
                System.exit(1);
                throw new RuntimeException(e);
            }
        });

        try {
            BayeuxParameters receivedParams = tokenProvider.login();

            Consumer<Map<String, Object>> consumer = event -> System.out.println(String.format("Received:\n%s", JSON.toString(event)));

            EmpConnector connector = new EmpConnector(params);

            connector.setBearerTokenProvider(tokenProvider);

            connector.start().get(5, TimeUnit.SECONDS);

            var topic = "someTestTopic";

            TopicSubscription subscription = connector.subscribe(topic, replayFrom, consumer).get(5, TimeUnit.SECONDS);

            System.out.println(String.format("Subscribed: %s", subscription));
        }catch(Exception ex){
            logger.error("Failed login: {}", ex.getMessage());
        }

        logger.info("<==main");
    }

}
