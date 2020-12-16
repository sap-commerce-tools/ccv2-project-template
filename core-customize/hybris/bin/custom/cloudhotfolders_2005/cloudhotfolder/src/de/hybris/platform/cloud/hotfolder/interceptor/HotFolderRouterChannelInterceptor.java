/*
 * [y] hybris Platform
 *
 * Copyright (c) 2019 SAP SE or an SAP affiliate company. All rights reserved.
 *
 * This software is the confidential and proprietary information of SAP
 * ("Confidential Information"). You shall not disclose such Confidential
 * Information and shall use it only in accordance with the terms of the
 * license agreement you entered into with SAP.
 */
package de.hybris.platform.cloud.hotfolder.interceptor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.integration.core.MessagingTemplate;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.util.Assert;

/**
 * A hot-folder specific implementation of the {@link ChannelInterceptor} providing
 * archive and error functionality.
 *
 * We intercept the hot folder specific message channel, operating on the same thread,
 * to capture the downstream processing of a {@link Message} and
 * direct the message either to the configured {@param successChannel} or {@param failureChannel}
 */
public class HotFolderRouterChannelInterceptor implements ChannelInterceptor {

    private MessageChannel successChannel;
    private MessageChannel failureChannel;

    private static final Logger LOG = LoggerFactory.getLogger(HotFolderRouterChannelInterceptor.class);

    private final MessagingTemplate messagingTemplate = new MessagingTemplate();


    /**
     * Set the channel to which to send the {@link Message} after successful downstream
     * processing.
     *
     * @param successChannel the channel.
     */
    public void setSuccessChannel(final MessageChannel successChannel) {
        Assert.notNull(successChannel, "'successChannel' must not be null");
        this.successChannel = successChannel;
    }

    /**
     * Set the channel to which to send the {@link Message} if an exception occurs during
     * downstream processing.
     *
     * @param failureChannel the channel.
     */
    public void setFailureChannel(final MessageChannel failureChannel) {
        Assert.notNull(failureChannel, "'failureChannel' must not be null");
        this.failureChannel = failureChannel;
    }

    /**
     * Invoked after {@link MessageChannel} send() and
     * {@link org.springframework.messaging.PollableChannel} receive() calls,
     * regardless of any exception that is raised. Allowing
     * us to direct to {@link Message} on to a success or failure channel.
     *
     * @param message                   the message
     * @param interceptedMessageChannel the channel we've intercepted
     * @param sent                      whether the message was successfully sent on channel interceptedMessageChannel
     * @param ex                        any exceptions
     */
    @Override
    public void afterSendCompletion(final Message<?> message, final MessageChannel interceptedMessageChannel, final boolean sent, final Exception ex) {
        if (ex == null)
        {
            this.messagingTemplate.send(this.successChannel, message);
            LOG.debug("Message [{}] sent to successChannel [{}]", message, successChannel);
        }
        else
        {
            LOG.error("An exception occurred during downstream file processing for message [{}]. Exception [{}].", message, ex);
            this.messagingTemplate.send(this.failureChannel, message);
            LOG.debug("Message [{}] sent to failureChannel [{}]", message, failureChannel);
        }
    }
}
