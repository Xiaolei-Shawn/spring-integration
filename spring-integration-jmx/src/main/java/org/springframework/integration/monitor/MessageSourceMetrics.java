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

import org.springframework.jmx.export.annotation.ManagedMetric;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.notification.NotificationPublisherAware;
import org.springframework.jmx.support.MetricType;

/**
 * @author Dave Syer
 * @author Gary Russell
 * @since 2.0
 */
public interface MessageSourceMetrics extends NotificationPublisherAware {

	@ManagedOperation
	void reset();

	/**
	 * @return the number of successful handler calls
	 */
	@ManagedMetric(metricType = MetricType.COUNTER, displayName = "Message Source Message Count")
	int getMessageCount();

	/**
	 * @return the number of unsuccessful handler calls
	 */
	@ManagedMetric(metricType = MetricType.COUNTER, displayName = "Message Source Nothing to Process Count")
	int getNoWorkCount();

	/**
	 * @return the number of poll failures
	 */
	@ManagedMetric(metricType = MetricType.COUNTER, displayName = "Message Source Polling Failure Count")
	int getPollFailureCount();

	String getName();

	String getSource();

}
