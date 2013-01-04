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
package org.springframework.integration.ip.zmq;

import org.springframework.integration.Message;
import org.springframework.integration.ip.IpHeaders;
import org.springframework.integration.ip.tcp.connection.TcpConnectionSupport;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.util.Assert;
import org.zeromq.ZMQ.Socket;

/**
 * @author Gary Russell
 * @since 3.0
 *
 */
public class ZmqReqConnection extends TcpConnectionSupport {

	private Socket socket;

	public ZmqReqConnection(String host, int port, Socket socket) {
		this.socket = socket;
		this.establishConnectionId(host, port);
	}

	public boolean isOpen() {
		return true;
	}

	public void send(Message<?> message) throws Exception {
		Assert.isInstanceOf(String.class, message.getPayload());
		this.socket.send((String) message.getPayload());
		String reply = this.socket.recvStr();
		this.getListener().onMessage(MessageBuilder.withPayload(reply)
				.setHeader(IpHeaders.CONNECTION_ID, this.getConnectionId())
				.build());
	}

	public Object getPayload() throws Exception {
		return null;
	}

	public int getPort() {
		return 0;
	}

	public void run() {
	}

	@Override
	public void close() {
		super.close();
		this.socket.close();
	}


}
