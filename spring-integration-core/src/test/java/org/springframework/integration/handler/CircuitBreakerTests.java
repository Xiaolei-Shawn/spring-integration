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
package org.springframework.integration.handler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.integration.Message;
import org.springframework.integration.MessagingException;
import org.springframework.integration.core.MessageHandler;
import org.springframework.integration.message.GenericMessage;


/**
 * @author Gary Russell
 * @since 2.2
 *
 */
public class CircuitBreakerTests {

	@Test
	public void test() throws Exception {
		CircuitBreakerBeanPostProcessor bpp = new CircuitBreakerBeanPostProcessor(2, 100);
		bpp.setBeanNamePatterns(new String[] {"*"});
		final AtomicInteger counter = new AtomicInteger(3);
		MessageHandler handler = new MessageHandler() {
			public void handleMessage(Message<?> message) throws MessagingException {
				if (counter.decrementAndGet() >= 0) {
					throw new MessagingException("In handler");
				}
			}
		};
		handler = (MessageHandler) bpp.postProcessAfterInitialization(handler, "aBean");
		MessagingException e = handle(handler);
		assertEquals("In handler", e.getMessage());
		e = handle(handler);
		assertEquals("In handler", e.getMessage());
		e = handle(handler);
		assertEquals("Circuit Breaker is Open for aBean", e.getMessage());
		Thread.sleep(110);
		e = handle(handler);
		assertEquals("In handler", e.getMessage());
		e = handle(handler);
		assertEquals("Circuit Breaker is Open for aBean", e.getMessage());
		Thread.sleep(110);
		e = handle(handler);
		assertNull(e);
	}

	@Test
	public void testAlreadyProxy() throws Exception {
		CircuitBreakerBeanPostProcessor bpp = new CircuitBreakerBeanPostProcessor(2, 100);
		bpp.setBeanNamePatterns(new String[] {"*"});
		final AtomicInteger counter = new AtomicInteger(3);
		MessageHandler handler = new MessageHandler() {
			public void handleMessage(Message<?> message) throws MessagingException {
				if (counter.decrementAndGet() >= 0) {
					throw new MessagingException("In handler");
				}
			}
		};
		handler = (MessageHandler) new ProxyFactory(handler).getProxy();
		handler = (MessageHandler) bpp.postProcessAfterInitialization(handler, "aBean");
		MessagingException e = handle(handler);
		assertEquals("In handler", e.getMessage());
		e = handle(handler);
		assertEquals("In handler", e.getMessage());
		e = handle(handler);
		assertEquals("Circuit Breaker is Open for aBean", e.getMessage());
		Thread.sleep(110);
		e = handle(handler);
		assertEquals("In handler", e.getMessage());
		e = handle(handler);
		assertEquals("Circuit Breaker is Open for aBean", e.getMessage());
		Thread.sleep(110);
		e = handle(handler);
		assertNull(e);
	}

	/**
	 * @param handler
	 */
	private MessagingException handle(MessageHandler handler) {
		try {
			handler.handleMessage(new GenericMessage<String>("Hello, world!"));
			return null;
		}
		catch (MessagingException e) {
			return e;
		}
	}

}
