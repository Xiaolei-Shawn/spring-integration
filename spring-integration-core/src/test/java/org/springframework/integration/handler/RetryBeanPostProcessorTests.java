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

import static org.junit.Assert.*;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.integration.Message;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.MessagingException;
import org.springframework.integration.core.MessageHandler;
import org.springframework.integration.message.GenericMessage;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.StopWatch;

/**
 * @author Gary Russell
 * @since 2.2
 *
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class RetryBeanPostProcessorTests {

	@Autowired
	private Service service1;

	@Autowired
	private MessageChannel in;
	
	@Test
	public void testStatelessRetry() {
		StopWatch watch = new StopWatch();
		watch.start();
		try {
			in.send(new GenericMessage<String>("Hello, world!"));
			fail("Expected exception");
		}
		catch (MessagingException e) {
			assertTrue("Expected Failure".equals(e.getCause().getMessage()));
			assertEquals(3, (service1.getInvocationCount()));
			watch.stop();
			assertTrue(watch.getTotalTimeSeconds() >= 2); // default policies 3 attempts, 1 sec delay
		}
	}

	public interface Service {

		public abstract int getInvocationCount();
	}

	public static class ServiceImpl implements Service {

		private int invocationCount;
		
		public int getInvocationCount() {
			return invocationCount;
		}

		public void handle(String s) {
			invocationCount++;
			System.out.println(invocationCount);
			new RuntimeException().printStackTrace();
			throw new MessagingException("Expected Failure");
		}
	}
}
