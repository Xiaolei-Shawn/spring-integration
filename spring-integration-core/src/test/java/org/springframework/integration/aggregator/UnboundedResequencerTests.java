/*
 * Copyright 2002-2009 the original author or authors.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.Message;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.store.MessageGroupStore;
import org.springframework.integration.store.SimpleMessageStore;
import org.springframework.integration.store.UnboundedMessageGroupStore;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Gary Russell
 * @since 2.1
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class UnboundedResequencerTests {

	private CorrelatingMessageHandler resequencer;

	private ResequencingMessageGroupProcessor processor = new ResequencingMessageGroupProcessor();

	private MessageGroupStore store = new UnboundedMessageGroupStore(new SimpleMessageStore());

	@Autowired
	private DirectChannel in;

	@Autowired
	private QueueChannel out;

	@Before
	public void configureResequencer() {
		this.resequencer = new CorrelatingMessageHandler(processor, store, null, null);
	}

	@Test
	public void testResequencingWithIncompleteSequenceRelease() throws InterruptedException {
		this.resequencer.setReleaseStrategy(new UnboundedSequenceReleaseStrategy());
		QueueChannel replyChannel = new QueueChannel();
		Message<?> message1 = createMessage("123", "ABC", 1, replyChannel);
		Message<?> message2 = createMessage("456", "ABC", 2, replyChannel);
		Message<?> message3 = createMessage("789", "ABC", 3, replyChannel);
		Message<?> message4 = createMessage("UVW", "ABC", 4, replyChannel);
		Message<?> message5 = createMessage("XYZ", "ABC", 5, replyChannel);
		this.resequencer.handleMessage(message1);
		Message<?> reply1 = replyChannel.receive(0);
		assertNotNull(reply1);
		assertEquals(new Integer(1), reply1.getHeaders().getSequenceNumber());
		this.resequencer.handleMessage(message3);
		this.resequencer.handleMessage(message2);
		this.resequencer.handleMessage(message5);
		Message<?> reply2 = replyChannel.receive(0);
		Message<?> reply3 = replyChannel.receive(0);
		Message<?> reply4 = replyChannel.receive(0);
		// only messages 2 and 3 should have been received by now
		assertNotNull(reply2);
		assertEquals(new Integer(2), reply2.getHeaders().getSequenceNumber());
		assertNotNull(reply3);
		assertEquals(new Integer(3), reply3.getHeaders().getSequenceNumber());
		assertNull(reply4);
		// when sending the last message, the whole sequence must have been sent
		this.resequencer.handleMessage(message4);
		reply4 = replyChannel.receive(0);
		Message<?> reply5 = replyChannel.receive(0);
		assertNotNull(reply4);
		assertEquals(new Integer(4), reply4.getHeaders().getSequenceNumber());
		assertNotNull(reply5);
		assertEquals(new Integer(5), reply5.getHeaders().getSequenceNumber());
	}

	private static Message<?> createMessage(String payload, Object correlationId, int sequenceNumber,
			MessageChannel replyChannel) {
		return MessageBuilder.withPayload(payload).setCorrelationId(correlationId)
				.setSequenceNumber(sequenceNumber).setReplyChannel(replyChannel).build();
	}

	@Test
	public void testViaConfig() {
		Message<String> message = MessageBuilder.withPayload("Hello, world! 2")
				.setCorrelationId("42")
				.setHeader("my.sequence.header", new Integer(2)).build();
		in.send(message);
		message = MessageBuilder.withPayload("Hello, world! 1")
				.setCorrelationId("42")
				.setHeader("my.sequence.header", new Integer(1)).build();
		in.send(message);
		Message<?> mOut = out.receive(0);
		assertNotNull(mOut);
		assertEquals(1, mOut.getHeaders().get("my.sequence.header"));
		mOut = out.receive(0);
		assertNotNull(mOut);
		assertEquals(2, mOut.getHeaders().get("my.sequence.header"));

		message = MessageBuilder.withPayload("Hello, world! 4")
				.setCorrelationId("42")
				.setHeader("my.sequence.header", new Integer(4)).build();
		in.send(message);
		message = MessageBuilder.withPayload("Hello, world! 3")
				.setCorrelationId("42")
				.setHeader("my.sequence.header", new Integer(3)).build();
		in.send(message);
		mOut = out.receive(0);
		assertNotNull(mOut);
		assertEquals(3, mOut.getHeaders().get("my.sequence.header"));
		mOut = out.receive(0);
		assertNotNull(mOut);
		assertEquals(4, mOut.getHeaders().get("my.sequence.header"));
	}
}
