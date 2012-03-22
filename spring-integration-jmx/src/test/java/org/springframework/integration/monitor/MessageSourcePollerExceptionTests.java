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
package org.springframework.integration.monitor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import javax.management.Notification;
import javax.management.NotificationListener;
import javax.management.ObjectName;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.integration.Message;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.core.MessageSource;
import org.springframework.integration.endpoint.SourcePollingChannelAdapter;
import org.springframework.jmx.export.MBeanExporter;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.PeriodicTrigger;

/**
 * @author Gary Russell
 * @since 2.2
 *
 */
public class MessageSourcePollerExceptionTests {

	private StaticApplicationContext context = new StaticApplicationContext();

	private TestNotificationListener notificationListener = new TestNotificationListener();

	private final static String DOMAIN = MessageSourcePollerExceptionTests.class.getSimpleName();

	private final static String OBJECT_NAME = DOMAIN + ":type=MessageSource,name=spca,bean=endpoint";

	private ObjectName objectName;

	@Before
	public void setup() throws Exception {
		objectName = ObjectName.getInstance(OBJECT_NAME);

		BeanDefinition exporterBeanDefinition = new RootBeanDefinition(IntegrationMBeanExporter.class);
		exporterBeanDefinition.getPropertyValues().add("defaultDomain", DOMAIN);
		context.registerBeanDefinition("exporter", exporterBeanDefinition);
		context.registerSingleton("taskScheduler", ThreadPoolTaskScheduler.class);
		context.registerSingleton("errorChannel", QueueChannel.class);

		RootBeanDefinition beanDefinition = new RootBeanDefinition(SourcePollingChannelAdapter.class);
		MutablePropertyValues propertyValues = beanDefinition.getPropertyValues();
		propertyValues.add("source", new RootBeanDefinition(AlwaysFailsMessageSource.class));
		propertyValues.add("outputChannel", new RootBeanDefinition(QueueChannel.class));
		RootBeanDefinition trigger = new RootBeanDefinition(PeriodicTrigger.class);
		trigger.getConstructorArgumentValues().addGenericArgumentValue(2000);
		propertyValues.add("trigger", trigger);
		context.registerBeanDefinition("spca", beanDefinition);
		context.refresh();
		MBeanExporter exporter = context.getBean(MBeanExporter.class);
		exporter.getServer()
				.addNotificationListener(
						ObjectName.getInstance(OBJECT_NAME),
					this.notificationListener, null, null);
	}

	@Test
	public void test() throws Exception {
		assertTrue(notificationListener.notificationReceived.tryAcquire(5, TimeUnit.SECONDS));
		Notification notification = notificationListener.notifications.get(0);
		assertEquals("PollFailed", notification.getType());
		assertEquals(AlwaysFailsMessageSource.MESSAGE, notification.getMessage());
		assertEquals(objectName, notification.getSource());
	}

	public static class AlwaysFailsMessageSource implements MessageSource<String> {

		public static final String MESSAGE = "AlwaysFails";

		public Message<String> receive() {

			throw new RuntimeException(MESSAGE);
		}

	}

	public static class TestNotificationListener implements NotificationListener {

		private Semaphore notificationReceived = new Semaphore(0);

		private final List<Notification> notifications = new ArrayList<Notification>();

		public void handleNotification(Notification notification, Object handback) {
			this.notifications.add(notification);
			this.notificationReceived.release();
		}

		void clearNotifications() {
			this.notifications.clear();
		}
	}

}
