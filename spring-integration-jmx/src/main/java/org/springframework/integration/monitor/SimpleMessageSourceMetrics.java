/*
 * Copyright 2002-2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package org.springframework.integration.monitor;

import java.util.concurrent.atomic.AtomicInteger;

import javax.management.Notification;
import javax.management.ObjectName;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.integration.core.MessageSource;
import org.springframework.jmx.export.notification.NotificationPublisher;

/**
 * @author Dave Syer
 * @author Gary Russell
 * @since 2.0
 */
public class SimpleMessageSourceMetrics implements MethodInterceptor, MessageSourceMetrics {

	private final AtomicInteger messageCount = new AtomicInteger();

	private final AtomicInteger noWorkCount = new AtomicInteger();

	private final AtomicInteger pollFailureCount = new AtomicInteger();

	private final MessageSource<?> messageSource;

	private volatile String source;

	private volatile String name;

	private volatile NotificationPublisher notificatioNPublisher;

	private volatile ObjectName objectName;


	public SimpleMessageSourceMetrics(MessageSource<?> messageSource) {
		this.messageSource = messageSource;
	}


	public void setName(String name) {
		this.name = name;
	}

	public String getName() {
		return this.name;
	}

	public void setSource(String source) {
		this.source = source;
	}

	public String getSource() {
		return this.source;
	}

	public MessageSource<?> getMessageSource() {
		return this.messageSource;
	}

	void setObjectName(ObjectName objectName) {
		this.objectName = objectName;
	}


	public void reset() {
		this.messageCount.set(0);
		this.pollFailureCount.set(0);
		this.noWorkCount.set(0);
	}

	public int getMessageCount() {
		return this.messageCount.get();
	}

	public int getNoWorkCount() {
		return this.noWorkCount.get();
	}

	public int getPollFailureCount() {
		return this.pollFailureCount.get();
	}


	public Object invoke(MethodInvocation invocation) throws Throwable {
		String method = invocation.getMethod().getName();
		Object result = null;
		boolean callingReceive = "receive".equals(method);
		try {
			result = invocation.proceed();
			if (callingReceive) {
				if (result!=null) {
					this.messageCount.incrementAndGet();
				}
				else {
					this.noWorkCount.incrementAndGet();
				}
			}
		}
		catch (Throwable t) {
			if (callingReceive) {
				int sequence = this.pollFailureCount.incrementAndGet();
				if (this.objectName != null) {
					Notification notification = new Notification("PollFailed",
							this.objectName, sequence, t.getMessage());
					this.notificatioNPublisher.sendNotification(notification);
				}
			}
			throw t;
		}
		return result;
	}

	public void setNotificationPublisher(
			NotificationPublisher notificationPublisher) {
		this.notificatioNPublisher = notificationPublisher;
	}

	@Override
	public String toString() {
		return String
				.format("MessageSourceMonitor: [name=%s, source=%s, count=%d, noWork=%d, failures=%d]",
						name, source, messageCount.get(), noWorkCount.get(), pollFailureCount.get());
	}

}
