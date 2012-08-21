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
package org.springframework.integration.jms.request_reply;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import javax.jms.Connection;
import javax.jms.Session;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.junit.Ignore;
import org.junit.Test;

import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.gateway.RequestReplyExchanger;
import org.springframework.integration.jms.config.ActiveMqTestUtils;
import org.springframework.integration.message.GenericMessage;
import org.springframework.jms.connection.CachingConnectionFactory;
import org.springframework.jms.connection.ConnectionFactoryUtils;
import org.springframework.jms.support.JmsUtils;
import org.springframework.util.StopWatch;
/**
 * @author Oleg Zhurakousky
 */
public class MiscellaneousTests {

	/**
	 * Asserts that receive-timeout is honored even if
	 * requests (once in process), takes less then receive-timeout value
	 * when requests are queued up (e.g., single consumer receiver)
	 */
	@Test
	public void testTimeoutHonoringWhenRequestsQueuedUp() throws Exception{
		ActiveMqTestUtils.prepare();
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("honor-timeout.xml", this.getClass());
		final RequestReplyExchanger gateway = context.getBean(RequestReplyExchanger.class);
		final CountDownLatch latch = new CountDownLatch(3);
		final AtomicInteger replies = new AtomicInteger();
		StopWatch stopWatch = new StopWatch();
		stopWatch.start();
		for (int i = 0; i < 3; i++) {
			this.exchange(latch, gateway, replies);
		}
		latch.await();
		stopWatch.stop();
		assertTrue(stopWatch.getTotalTimeMillis() <= 11000);
		assertEquals(1, replies.get());
	}

	@Test
	@Ignore
	public void sessionProxyIdentity() throws Exception{
		ActiveMQConnectionFactory cf = new ActiveMQConnectionFactory("vm://localhost");
		final CachingConnectionFactory ccf = new CachingConnectionFactory();
		ccf.setTargetConnectionFactory(cf);
		ccf.setSessionCacheSize(10);
		ccf.setCacheConsumers(true);
		ccf.setCacheProducers(true);
		ccf.afterPropertiesSet();

		final Connection connection = ccf.createConnection();

		final Map<Object, Object> sessionMap = new HashMap<Object, Object>();

		Executor executor = Executors.newCachedThreadPool();

		for (int i = 0; i < 3000; i++) {

			executor.execute(new Runnable() {

				public void run() {
					Session session = null;
					try {
						session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
						long sessionId = 0;
						if (Proxy.isProxyClass(session.getClass())){
							sessionId = System.identityHashCode(Proxy.getInvocationHandler(session));
						}
						else {
							sessionId = System.identityHashCode(session);
						}
						if (sessionMap.containsKey(sessionId)){
							if (!session.toString().equals(sessionMap.get(sessionId).toString())){
								System.out.println("Found potential duplicate:");
								System.out.println(session + " - " + sessionMap.get(sessionId));
							}
						}
						else {
							sessionMap.put(sessionId, session);
						}
						Thread.sleep(new Random().nextInt(2000));
					} catch (Exception e) {
						e.printStackTrace();
					} finally {
						JmsUtils.closeSession(session);
						ConnectionFactoryUtils.releaseConnection(connection, ccf, true);
					}
				}
			});

		}
		System.in.read();
	}


	private void exchange(final CountDownLatch latch, final RequestReplyExchanger gateway, final AtomicInteger replies) {
		new Thread(new Runnable() {
			public void run() {
				try {
					gateway.exchange(new GenericMessage<String>(""));
					replies.incrementAndGet();
				} catch (Exception e) {
					//ignore
				}
				latch.countDown();
			}
		}).start();
	}
}
