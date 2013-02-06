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

import org.springframework.context.ApplicationEvent;

/**
 * ApplicationEvent representing certain operations on a {@link TcpConnection}.
 * @author Gary Russell
 * @since 3.0
 *
 */
public class TcpConnectionEvent extends ApplicationEvent {

	private static final long serialVersionUID = 5323436446362192129L;

	public static final String OPEN = "OPEN";

	public static final String CLOSE = "CLOSE";

	public static final String EXCEPTION = "EXCEPTION";

	private final String type;

	private final String connectionFactoryName;

	private final Throwable throwable;

	public TcpConnectionEvent(TcpConnectionSupport connection, String type,
			String connectionFactoryName) {
		super(connection);
		this.type = type;
		this.throwable = null;
		this.connectionFactoryName = connectionFactoryName;
	}

	public TcpConnectionEvent(TcpConnectionSupport connection, Throwable t,
			String connectionFactoryName) {
		super(connection);
		this.type = EXCEPTION;
		this.throwable = t;
		this.connectionFactoryName = connectionFactoryName;
	}

	public String getType() {
		return type;
	}

	public String getConnectionId() {
		return ((TcpConnection) this.getSource()).getConnectionId();
	}

	public String getConnectionFactoryName() {
		return connectionFactoryName;
	}

	@Override
	public String toString() {
		return "TcpConnectionEvent [type=" + this.getType() +
				", factory=" + this.connectionFactoryName +
				", connectionId=" + this.getConnectionId() +
			   (this.throwable == null ? "" : ", Exception=" + this.throwable) + "]";
	}

}
