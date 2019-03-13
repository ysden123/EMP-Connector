/*
 * Copyright (c) 2019. WebPals
 */

package com.salesforce.emp.connector.example;

import org.cometd.bayeux.Message;
import org.cometd.bayeux.client.ClientSessionChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Yuriy Stul
 */
public class YSLoggingListener implements ClientSessionChannel.MessageListener {
    private static Logger logger = LoggerFactory.getLogger(YSLoggingListener.class);

    private boolean logSuccess;
    private boolean logFailure;

    public YSLoggingListener(boolean logSuccess, boolean logFailure) {
        this.logSuccess = logSuccess;
        this.logFailure = logFailure;
    }

    @Override
    public void onMessage(ClientSessionChannel clientSessionChannel, Message message) {

        if (logSuccess && message.isSuccessful()){
            logger.info("Success: id={}, message: {} ", clientSessionChannel.getId(), message);
        }else if(logFailure && !message.isSuccessful()){
            logger.error("Failure: id={}, message: {} ", clientSessionChannel.getId(), message);
        }
    }
}
