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

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.integration.Message;
import org.springframework.integration.aggregator.CorrelatingMessageHandler;
import org.springframework.integration.aggregator.UnboundedSequenceReleaseStrategy;

/**
 * A {@link MessageGroupStore} that contains just the last marked message when
 * at least one message has been released. Used by {@link CorrelatingMessageHandler}
 * when using unbounded groups (requires partial release). In particular, used
 * in a general resequencer. Usually used with an {@link UnboundedSequenceReleaseStrategy}
 *
 * @author Gary Russell
 * @since 2.1
 *
 */
public class UnboundedMessageGroupStore implements MessageGroupStore {

	private MessageGroupStore delegate;

	private Map<Object, UnboundedMessageGroup> groups;

	private static final Log logger = LogFactory.getLog(UnboundedMessageGroupStore.class);


	/**
	 * Constructs an {@link UnboundedMessageGroupStore} that wraps the supplied
	 * MessageGroupStore implementation.
	 * @param messageGroupStore
	 */
	public UnboundedMessageGroupStore(MessageGroupStore messageGroupStore) {
		this.delegate = messageGroupStore;
		groups = new ConcurrentHashMap<Object, UnboundedMessageGroup>();
	}

	/**
	 * @return
	 * @see org.springframework.integration.store.MessageGroupStore#getMessageCountForAllMessageGroups()
	 */
	public int getMessageCountForAllMessageGroups() {
		return this.delegate.getMessageCountForAllMessageGroups();
	}

	/**
	 * @return
	 * @see org.springframework.integration.store.MessageGroupStore#getMarkedMessageCountForAllMessageGroups()
	 */
	public int getMarkedMessageCountForAllMessageGroups() {
		return this.delegate.getMarkedMessageCountForAllMessageGroups();
	}

	/**
	 * @return
	 * @see org.springframework.integration.store.MessageGroupStore#getMessageGroupCount()
	 */
	public int getMessageGroupCount() {
		return this.delegate.getMessageGroupCount();
	}

	/**
	 * @param groupId
	 * @return
	 * @see org.springframework.integration.store.MessageGroupStore#getMessageGroup(java.lang.Object)
	 */
	public synchronized MessageGroup getMessageGroup(Object groupId) {
		MessageGroup messageGroup = this.delegate.getMessageGroup(groupId);
		UnboundedMessageGroup unboundedMessageGroup = groups.get(groupId);
		if (unboundedMessageGroup == null) {
			unboundedMessageGroup = new UnboundedMessageGroup(messageGroup);
			this.groups.put(groupId, unboundedMessageGroup);
		} else {
			unboundedMessageGroup.setDelegate(messageGroup);
		}
		return unboundedMessageGroup;
	}

	/**
	 * @param groupId
	 * @param message
	 * @return
	 * @see org.springframework.integration.store.MessageGroupStore#addMessageToGroup(java.lang.Object, org.springframework.integration.Message)
	 */
	public synchronized MessageGroup addMessageToGroup(Object groupId, Message<?> message) {
		MessageGroup group = this.delegate.addMessageToGroup(groupId, message);
		UnboundedMessageGroup unboundedMessageGroup = groups.get(groupId);
		if (unboundedMessageGroup == null) {
			unboundedMessageGroup = new UnboundedMessageGroup(group);
			groups.put(groupId, unboundedMessageGroup);
		} else {
			unboundedMessageGroup.setDelegate(group);
		}
		return unboundedMessageGroup;
	}

	/**
	 * @param group
	 * @return
	 * @see org.springframework.integration.store.MessageGroupStore#markMessageGroup(org.springframework.integration.store.MessageGroup)
	 */
	public MessageGroup markMessageGroup(MessageGroup group) {
		if (logger.isDebugEnabled()) {
			logger.debug("Marking message Group");
		}
		return this.delegate.markMessageGroup(group);
	}

	/**
	 * @param key
	 * @param messageToRemove
	 * @return
	 * @see org.springframework.integration.store.MessageGroupStore#removeMessageFromGroup(java.lang.Object, org.springframework.integration.Message)
	 */
	public MessageGroup removeMessageFromGroup(Object key,
			Message<?> messageToRemove) {
		if (logger.isDebugEnabled()) {
			logger.debug("Removing message from group:" + messageToRemove);
		}
		return this.delegate.removeMessageFromGroup(key, messageToRemove);
	}

	/**
	 * Marks messages and removes all but the last marked message for this key.
	 * Assumes called in the appropriate sorted sequence.
	 *
	 * @param key
	 * @param messageToMark
	 * @return
	 * @see org.springframework.integration.store.MessageGroupStore#markMessageFromGroup(java.lang.Object, org.springframework.integration.Message)
	 */
	public MessageGroup markMessageFromGroup(Object key,
			Message<?> messageToMark) {
		UnboundedMessageGroup group = groups.get(key);
		synchronized (group) {
			this.delegate.markMessageFromGroup(key, messageToMark);
			Collection<Message<?>> marked = group.getMarked();
			for (Message<?> message : marked) {
				if (!message.equals(messageToMark)) {
					this.removeMessageFromGroup(key, message);
					group.incrementRemoved();
				}
			}
		}
		return group;
	}

	/**
	 * @param groupId
	 * @see org.springframework.integration.store.MessageGroupStore#removeMessageGroup(java.lang.Object)
	 */
	public void removeMessageGroup(Object groupId) {
		this.delegate.removeMessageGroup(groupId);
		this.groups.remove(groupId);
	}

	/**
	 * @param callback
	 * @see org.springframework.integration.store.MessageGroupStore#registerMessageGroupExpiryCallback(org.springframework.integration.store.MessageGroupCallback)
	 */
	public void registerMessageGroupExpiryCallback(MessageGroupCallback callback) {
		this.delegate.registerMessageGroupExpiryCallback(callback);
	}

	/**
	 * @param timeout
	 * @return the number of message groups expired
	 * @see org.springframework.integration.store.MessageGroupStore#expireMessageGroups(long)
	 */
	public int expireMessageGroups(long timeout) {
		int expired = this.delegate.expireMessageGroups(timeout);
		if (expired > 0) {
			synchronized(this) {
				Iterator<Object> iterator = this.groups.keySet().iterator();
				while (iterator.hasNext()) {
					Object key = iterator.next();
					if (this.delegate.getMessageGroup(key).size() == 0) {
						iterator.remove();
					}
				}
			}
		}
		return expired;
	}

	private class UnboundedMessageGroup implements MessageGroup {

		private MessageGroup delegate;

		private volatile int removed;

		public UnboundedMessageGroup(MessageGroup group) {
			this.delegate = group;
		}

		/**
		 * @param delegate the delegate to set
		 */
		public void setDelegate(MessageGroup delegate) {
			this.delegate = delegate;
		}

		/**
		 * Increments the removed count
		 */
		public void incrementRemoved() {
			this.removed++;
		}

		/**
		 * @param message
		 * @return
		 * @see org.springframework.integration.store.MessageGroup#canAdd(org.springframework.integration.Message)
		 */
		public boolean canAdd(Message<?> message) {
			return delegate.canAdd(message);
		}

		/**
		 * @return
		 * @see org.springframework.integration.store.MessageGroup#getUnmarked()
		 */
		public Collection<Message<?>> getUnmarked() {
			return delegate.getUnmarked();
		}

		/**
		 * @return
		 * @see org.springframework.integration.store.MessageGroup#getMarked()
		 */
		public Collection<Message<?>> getMarked() {
			return delegate.getMarked();
		}

		/**
		 * @return
		 * @see org.springframework.integration.store.MessageGroup#getGroupId()
		 */
		public Object getGroupId() {
			return delegate.getGroupId();
		}

		/**
		 * @return
		 * @see org.springframework.integration.store.MessageGroup#isComplete()
		 */
		public boolean isComplete() {
			return false;
		}

		/**
		 * @return Integer.MAX_VALUE - group is "never" complete.
		 * @see org.springframework.integration.store.MessageGroup#getSequenceSize()
		 */
		public int getSequenceSize() {
			return Integer.MAX_VALUE;
		}

		/**
		 * @return delegate.size() + removed
		 * @see org.springframework.integration.store.MessageGroup#size()
		 */
		public int size() {
			return delegate.size() + this.removed;
		}

		/**
		 * @return
		 * @see org.springframework.integration.store.MessageGroup#getOne()
		 */
		public Message<?> getOne() {
			return delegate.getOne();
		}

		/**
		 * @return
		 * @see org.springframework.integration.store.MessageGroup#getTimestamp()
		 */
		public long getTimestamp() {
			return delegate.getTimestamp();
		}

	}
}
