/*
 * Copyright 2002-2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.channel;

import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.integration.Message;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.core.MessageHandler;
import org.springframework.integration.core.SubscribableChannel;
import org.springframework.integration.dispatcher.AbstractDispatcher;
import org.springframework.integration.dispatcher.MessageDispatcher;
import org.springframework.util.Assert;

/**
 * Base implementation of {@link MessageChannel} that invokes the subscribed
 * {@link MessageHandler handler(s)} by delegating to a {@link MessageDispatcher}.
 * 
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Gary Russell
 */
public abstract class AbstractSubscribableChannel extends AbstractMessageChannel implements SubscribableChannel {
	
	private final AtomicInteger handlerCounter = new AtomicInteger();
	
	public boolean subscribe(MessageHandler handler) {
		MessageDispatcher dispatcher = this.getRequiredDispatcher();
		boolean added = dispatcher.addHandler(handler);
		if (added) {
			int counter = 0;
			if (dispatcher instanceof AbstractDispatcher) {
				counter = ((AbstractDispatcher) dispatcher).getHandlerCount();
			}
			else {
				// some other dispatcher hand-roll the counter
				counter = handlerCounter.incrementAndGet();
			}
			if (logger.isInfoEnabled()) {
				logger.info("Channel '" + this.getComponentName() + "' has " + counter + " subscriber(s).");
			}
		}
		return added;
	}

	public boolean unsubscribe(MessageHandler handle) {
		MessageDispatcher dispatcher = this.getRequiredDispatcher();
		boolean removed = dispatcher.removeHandler(handle);
		if (removed) {
			int counter = 0;
			if (dispatcher instanceof AbstractDispatcher) {
				counter = ((AbstractDispatcher) dispatcher).getHandlerCount();
			}
			else {
				// some other dispatcher hand-roll the counter
				counter = handlerCounter.decrementAndGet();
			}
			if (logger.isInfoEnabled()) {
				logger.info("Channel '" + this.getComponentName() + "' has " + counter + " subscriber(s).");
			}
		}
		return removed;
	}

	@Override
	protected boolean doSend(Message<?> message, long timeout) {
		return this.getRequiredDispatcher().dispatch(message);
	}

	private MessageDispatcher getRequiredDispatcher() {
		MessageDispatcher dispatcher = this.getDispatcher();
		Assert.state(dispatcher != null, "'dispatcher' must not be null");
		return dispatcher;
	}

	protected abstract MessageDispatcher getDispatcher();

}
