/*
 * Copyright 2002-2011 the original author or authors.
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
package org.springframework.integration.aggregator;

import java.util.Collection;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.integration.Message;
import org.springframework.integration.MessageHeaders;
import org.springframework.integration.store.MessageGroup;

/**
 * @author Gary Russell
 * @since 2.1
 *
 */
public class UnboundedSequenceReleaseStrategy extends
		SequenceSizeReleaseStrategy {

	private static final Log logger = LogFactory.getLog(UnboundedSequenceReleaseStrategy.class);

	private String sequenceHeader;

	public UnboundedSequenceReleaseStrategy() {
		super(true);
	}

	@Override
	protected boolean canReleasePartial(MessageGroup messages,
			List<Message<?>> sorted) {
		Message<?> releaseCandidate = sorted.get(0);
		int tail = getSequenceNumber(releaseCandidate);
		Collection<Message<?>> marked = messages.getMarked();
		boolean release;
		if (marked.size() == 0) {
			release = tail == 1; // first message - ok to release if seq=1
		} else {
			release = getSequenceNumber(marked.iterator().next()) == tail - 1;
		}
		if (logger.isTraceEnabled() && release) {
			logger.trace("Release imminent because tail [" + tail + "] is next in line.");
		}
		return release;
	}

	protected Integer getSequenceNumber(Message<?> message) {
		if (this.sequenceHeader == null) {
			return message.getHeaders().getSequenceNumber();
		}
		return (Integer) message.getHeaders().get(this.sequenceHeader);
	}

	/**
	 * Use a custom sequence header; defaults to standard {@link MessageHeaders.SEQUENCE_NUMBER}.
	 * Header value must be an Integer.
	 * @return
	 */
	public void setSequenceHeader(String header) {
		this.sequenceHeader = header;
		this.setComparator(new SequenceNumberComparator(header));
	}

	@Override
	public void setReleasePartialSequences(boolean releasePartialSequences) {
		if (!releasePartialSequences) {
			throw new UnsupportedOperationException("Release Partial Sequences Must be True");
		}
	}

}
