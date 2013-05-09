/*
 * Copyright 2002-2013 the original author or authors.
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
package org.springframework.integration.file.tail;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;

import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.integration.Message;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.file.tail.FileTailingMessageProducerSupport.FileTailingEvent;
import org.springframework.integration.test.util.TestUtils;

/**
 * @author Gary Russell
 * @since 3.0
 *
 */
public class FileTailingMessageProducerTests {

	private final String tmpDir = System.getProperty("java.io.tmpdir");

	private File testDir;

	@Before
	public void setup() {
		File f = new File(tmpDir, "FileTailingMessageProducerTests");
		f.mkdir();
		this.testDir = f;
	}

	@Test
	public void testOS() throws Exception {
		OSDelegatingFileTailingMessageProducer adapter = new OSDelegatingFileTailingMessageProducer();
		testGuts(adapter);
	}

	@Test
	public void testApache() throws Exception {
		ApacheCommonsFileTailingMessageProducer adapter = new ApacheCommonsFileTailingMessageProducer();
		adapter.setMissingFileDelay(500);
		adapter.setPollingDelay(100);
		testGuts(adapter);
	}

	private void testGuts(FileTailingMessageProducerSupport adapter) throws InterruptedException,
			FileNotFoundException, IOException {
		final List<FileTailingEvent> events = new ArrayList<FileTailingEvent>();
		adapter.setApplicationEventPublisher(new ApplicationEventPublisher() {
			@Override
			public void publishEvent(ApplicationEvent event) {
				FileTailingEvent tailEvent = (FileTailingEvent) event;
				System.err.println(tailEvent);
				events.add(tailEvent);
			}
		});
		adapter.setFile(new File(testDir, "foo"));
		QueueChannel outputChannel = new QueueChannel();
		adapter.setOutputChannel(outputChannel);
		adapter.afterPropertiesSet();
		File file = new File(testDir, "foo");
		File renamed = new File(testDir, "bar");
		file.delete();
		renamed.delete();
		adapter.start();
		assertTrue((TestUtils.getPropertyValue(adapter, "started", CountDownLatch.class)).await(10, TimeUnit.SECONDS));
		FileOutputStream foo = new FileOutputStream(file);
		for (int i = 0; i < 50; i++) {
			foo.write(("hello" + i + "\n").getBytes());
		}
		foo.flush();
		for (int i = 0; i < 50; i++) {
			Message<?> message = outputChannel.receive(1000);
			assertNotNull(message);
			assertEquals("hello" + i, message.getPayload());
		}
		file.renameTo(renamed);
		foo.close();
		foo = new FileOutputStream(file);
		if (adapter instanceof ApacheCommonsFileTailingMessageProducer) {
			Thread.sleep(1000);
		}
		for (int i = 50; i < 100; i++) {
			foo.write(("hello" + i + "\n").getBytes());
		}
		foo.flush();
		for (int i = 50; i < 100; i++) {
			Message<?> message = outputChannel.receive(3000);
			assertNotNull(message);
			assertEquals("hello" + i, message.getPayload());
		}
		foo.close();
		adapter.stop();
	}

}
