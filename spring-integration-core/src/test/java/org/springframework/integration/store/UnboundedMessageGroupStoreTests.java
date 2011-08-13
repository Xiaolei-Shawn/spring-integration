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
package org.springframework.integration.store;

import static org.junit.Assert.*;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Before;
import org.junit.Test;
import org.springframework.integration.Message;
import org.springframework.integration.support.MessageBuilder;

/**
 * @author Gary Russell
 * @since 2.1
 *
 */
public class UnboundedMessageGroupStoreTests {

	private UnboundedMessageGroupStore store;
	private Message<?> message1;
	private Message<?> message2;
	private Message<?> message3;
	private Message<?> message4;
	private Message<?> message5;
	
	@Before
	public void setup() {
		this.store = new UnboundedMessageGroupStore(new SimpleMessageStore());
		message1 = MessageBuilder.withPayload("Hello, world!")
				 .setCorrelationId("1").setSequenceNumber(1).build();
		this.store.addMessageToGroup("1", message1);
		message2 = MessageBuilder.withPayload("Hello, world!")
				 .setCorrelationId("1").setSequenceNumber(2).build();
		this.store.addMessageToGroup("1", message2);
		
		message3 = MessageBuilder.withPayload("Hello, world!")
				 .setCorrelationId("2").setSequenceNumber(1).build();
		this.store.addMessageToGroup("2", message3);
		message4 = MessageBuilder.withPayload("Hello, world!")
				 .setCorrelationId("2").setSequenceNumber(2).build();
		this.store.addMessageToGroup("2", message4);
		message5 = MessageBuilder.withPayload("Hello, world!")
				 .setCorrelationId("2").setSequenceNumber(3).build();
		this.store.addMessageToGroup("2", message5);

	}
	/**
	 * Test method for {@link org.springframework.integration.store.UnboundedMessageGroupStore#getMessageCountForAllMessageGroups()}.
	 */
	@Test
	public void testGetMessageCountForAllMessageGroups() {
		assertEquals(5, this.store.getMessageCountForAllMessageGroups());
	}

	/**
	 * Test method for {@link org.springframework.integration.store.UnboundedMessageGroupStore#getMarkedMessageCountForAllMessageGroups()}.
	 */
	@Test
	public void testGetMarkedMessageCountForAllMessageGroups() {
		this.store.markMessageFromGroup("1", message1);
		this.store.markMessageFromGroup("2", message3);
		assertEquals(2, this.store.getMarkedMessageCountForAllMessageGroups());
	}

	/**
	 * Test method for {@link org.springframework.integration.store.UnboundedMessageGroupStore#getMessageGroupCount()}.
	 */
	@Test
	public void testGetMessageGroupCount() {
		assertEquals(2, this.store.getMessageGroupCount());
	}

	/**
	 * Test method for {@link org.springframework.integration.store.UnboundedMessageGroupStore#getMessageGroup(java.lang.Object)}.
	 */
	@Test
	public void testGetMessageGroup() {
		MessageGroup group = this.store.getMessageGroup("1");
		assertEquals(2, group.size());
		assertEquals("1", group.getGroupId());
	}

	/**
	 * Test method for {@link org.springframework.integration.store.UnboundedMessageGroupStore#addMessageToGroup(java.lang.Object, org.springframework.integration.Message)}.
	 */
	@Test
	public void testAddMessageToGroup() {
		// already covered
	}

	/**
	 * Test method for {@link org.springframework.integration.store.UnboundedMessageGroupStore#markMessageGroup(org.springframework.integration.store.MessageGroup)}.
	 */
	@Test
	public void testMarkMessageGroup() {
		this.store.markMessageFromGroup("2", this.message3);
		this.store.markMessageFromGroup("2", this.message4);
		assertEquals(3, this.store.getMessageGroup("2").size());
		assertEquals(1, this.store.getMessageGroup("2").getMarked().size());
	}

	/**
	 * Test method for {@link org.springframework.integration.store.UnboundedMessageGroupStore#removeMessageFromGroup(java.lang.Object, org.springframework.integration.Message)}.
	 */
	@Test
	public void testRemoveMessageFromGroup() {
		this.store.removeMessageFromGroup("1", message1);
		assertEquals(1, this.store.getMessageGroup("1").size());
	}

	/**
	 * Test method for {@link org.springframework.integration.store.UnboundedMessageGroupStore#markMessageFromGroup(java.lang.Object, org.springframework.integration.Message)}.
	 */
	@Test
	public void testMarkMessageFromGroup() {
		//already covered
	}

	/**
	 * Test method for {@link org.springframework.integration.store.UnboundedMessageGroupStore#removeMessageGroup(java.lang.Object)}.
	 */
	@Test
	public void testRemoveMessageGroup() {
		this.store.removeMessageGroup("1");
		assertEquals(3, this.store.getMessageCountForAllMessageGroups());
	}

	/**
	 * Test method for {@link org.springframework.integration.store.UnboundedMessageGroupStore#registerMessageGroupExpiryCallback(org.springframework.integration.store.MessageGroupCallback)}.
	 */
	@Test
	public void testRegisterMessageGroupExpiryCallback() {
		final AtomicInteger callbackCalled = new AtomicInteger();
		final Set<Object> expired = new HashSet<Object>();
		this.store.registerMessageGroupExpiryCallback(new MessageGroupCallback() {
			public void execute(MessageGroupStore messageGroupStore, MessageGroup group) {
				expired.add(group.getGroupId());
				callbackCalled.incrementAndGet();
			}
		});
		this.store.expireMessageGroups(0);
		assertEquals(2, callbackCalled.get());
		assertTrue(expired.remove("1"));
		assertTrue(expired.remove("2"));
	}

	/**
	 * Test method for {@link org.springframework.integration.store.UnboundedMessageGroupStore#expireMessageGroups(long)}.
	 */
	@Test
	public void testExpireMessageGroups() {
		this.store.registerMessageGroupExpiryCallback(new MessageGroupCallback() {
			public void execute(MessageGroupStore messageGroupStore, MessageGroup group) {
				messageGroupStore.removeMessageGroup(group.getGroupId());
			}
		});
		this.store.expireMessageGroups(0);
		assertEquals(0, this.store.getMessageCountForAllMessageGroups());
		assertEquals(0, this.store.getMessageGroupCount());
	}

}
