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

import org.springframework.integration.ip.tcp.connection.AbstractServerConnectionFactory;
import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Socket;

/**
 * @author Gary Russell
 * @since 3.0
 *
 */
public class ZmqRepServerConnectionFactory extends AbstractServerConnectionFactory {

	private volatile ZmqRepConnection connection;

	public ZmqRepServerConnectionFactory(int port) {
		super(port);
	}

	public void run() {
		Socket socket = ZmqUtils.getContext().socket(ZMQ.REP);
		socket.bind("tcp://*:" + this.getPort());
		ZmqRepConnection connection = new ZmqRepConnection(this.getPort(), socket);
		connection.registerListener(this.getListener());
		this.getSender().addNewConnection(connection);
		this.connection = connection;
		connection.run();
	}

	@Override
	public void close() {
		this.connection.close();
	}


}
