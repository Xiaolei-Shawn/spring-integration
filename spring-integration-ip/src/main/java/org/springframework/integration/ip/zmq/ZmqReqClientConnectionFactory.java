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

import java.util.ArrayList;
import java.util.List;

import org.springframework.integration.ip.tcp.connection.AbstractClientConnectionFactory;
import org.springframework.integration.ip.tcp.connection.TcpConnectionSupport;
import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Socket;

/**
 * @author Gary Russell
 * @since 3.0
 *
 */
public class ZmqReqClientConnectionFactory extends AbstractClientConnectionFactory {

	private final int type = ZMQ.REQ;

	private final String url;

	private final String host;

	private final int port;

	private final List<ZmqReqConnection> connections = new ArrayList<ZmqReqConnection>();

	public ZmqReqClientConnectionFactory(String host, int port) {
		super(host, port);
		this.host = host;
		this.port = port;
		this.url = "tcp://" + host + ":" + port;
	}

	@Override
	protected TcpConnectionSupport obtainConnection() throws Exception {
		Socket socket = ZmqUtils.getContext().socket(this.type);
		socket.connect(this.url);
		ZmqReqConnection zmqConnection = new ZmqReqConnection(this.host, this.port, socket);
		zmqConnection.registerListener(this.getListener());
		connections.add(zmqConnection);
		return zmqConnection;
	}

	@Override
	public void start() {
		this.setActive(true);
		super.start();
	}

	@Override
	public void close() {
		for (ZmqReqConnection connection : this.connections) {
			connection.close();
		}
	}

}
