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
package org.springframework.integration.ip.tcp.connection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

import java.io.OutputStream;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.serializer.Serializer;
import org.springframework.integration.message.GenericMessage;

/**
 * @author Gary Russell
 * @since 3.0
 *
 */
public class ConnectionEventTests {

	@Test
	public void test() throws Exception {
		Socket socket = mock(Socket.class);
		final AtomicReference<ApplicationEvent> theEvent = new AtomicReference<ApplicationEvent>();
		TcpNetConnection conn = new TcpNetConnection(socket, false, false, new ApplicationEventPublisher() {

			public void publishEvent(ApplicationEvent event) {
				theEvent.set(event);
			}
		}, "foo");
		assertNotNull(theEvent.get());
		assertEquals("TcpConnectionEvent [type=OPEN, factory=foo, connectionId=" + conn.getConnectionId() + "]", theEvent.get().toString());
		@SuppressWarnings("unchecked")
		Serializer<Object> serializer = mock(Serializer.class);
		doThrow(new RuntimeException("foo")).when(serializer).serialize(Mockito.any(Object.class), Mockito.any(OutputStream.class));
		conn.setMapper(new TcpMessageMapper());
		conn.setSerializer(serializer);
		try {
			conn.send(new GenericMessage<String>("bar"));
			fail("Expected exception");
		}
		catch (Exception e) {}
		assertNotNull(theEvent.get());
		assertEquals("TcpConnectionEvent [type=EXCEPTION, factory=foo, connectionId=" + conn.getConnectionId() +
				", Exception=java.lang.RuntimeException: foo]", theEvent.get().toString());
		conn.close();
		assertNotNull(theEvent.get());
		assertEquals("TcpConnectionEvent [type=CLOSE, factory=foo, connectionId=" + conn.getConnectionId() + "]", theEvent.get().toString());
	}

}
