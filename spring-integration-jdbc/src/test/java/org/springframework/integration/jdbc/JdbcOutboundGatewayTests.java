/*
 * Copyright 2002-2011 the original author or authors.
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
package org.springframework.integration.jdbc;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import junit.framework.Assert;

import org.aopalliance.aop.Advice;
import org.junit.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.integration.Message;
import org.springframework.integration.MessageHandlingException;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.handler.advice.AbstractRequestHandlerAdvice;
import org.springframework.integration.message.ErrorMessage;
import org.springframework.integration.message.GenericMessage;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;

/**
 *
 * @author Gunnar Hillert
 * @since 2.1
 *
 */
public class JdbcOutboundGatewayTests {

	@Test
	public void testSetMaxRowsPerPollWithoutSelectQuery() {


		DataSource dataSource = new EmbeddedDatabaseBuilder().build();

		JdbcOutboundGateway jdbcOutboundGateway = new JdbcOutboundGateway(dataSource, "update something");

		try {
			jdbcOutboundGateway.setMaxRowsPerPoll(10);
			jdbcOutboundGateway.onInit();

		} catch (IllegalArgumentException e) {
			assertEquals("If you want to set 'maxRowsPerPoll', then you must provide a 'selectQuery'.", e.getMessage());
			return;
		}

		fail("Expected an IllegalArgumentException to be thrown.");

	}

	@Test
	public void testConstructorWithNulljdbcOperations() {

		JdbcOperations jdbcOperations = null;

		try {
			new JdbcOutboundGateway(jdbcOperations, "select * from DOES_NOT_EXIST");
		}
		catch (IllegalArgumentException e) {
			Assert.assertEquals("'jdbcOperations' must not be null.", e.getMessage());
			return;
		}

		fail("Expected an IllegalArgumentException to be thrown.");
	}

	@Test
	public void testConstructorWithEmptyAndNullQueries() {

		final DataSource dataSource = new EmbeddedDatabaseBuilder().build();

		final String selectQuery = "   ";
		final String updateQuery = null;

		try {
			new JdbcOutboundGateway(dataSource, updateQuery, selectQuery);
		}
		catch (IllegalArgumentException e) {
			Assert.assertEquals("The 'updateQuery' and the 'selectQuery' must not both be null or empty.", e.getMessage());
			return;
		}

		fail("Expected an IllegalArgumentException to be thrown.");
	}

	/**
	 * Test method for
	 * {@link org.springframework.integration.jdbc.JdbcOutboundGateway#setMaxRowsPerPoll(Integer)}.
	 */
	@Test
	public void testSetMaxRowsPerPoll() {


		DataSource dataSource = new EmbeddedDatabaseBuilder().build();

		JdbcOutboundGateway jdbcOutboundGateway = new JdbcOutboundGateway(dataSource, "select * from DOES_NOT_EXIST");

		try {
			jdbcOutboundGateway.setMaxRowsPerPoll(null);
		} catch (IllegalArgumentException e) {
			assertEquals("MaxRowsPerPoll must not be null.", e.getMessage());
			return;
		}

		fail("Expected an IllegalArgumentException to be thrown.");

	}

	@Test
	public void testRowReturned() {
		ResourceLoader resourceLoader = new ResourceLoader() {

			public Resource getResource(String location) {
				byte[] ddl = "CREATE TABLE FOO (ID VARCHAR(1));".getBytes();
				return new ByteArrayResource(ddl);
			}

			public ClassLoader getClassLoader() {
				return this.getClass().getClassLoader();
			}
		};
		DataSource dataSource = new EmbeddedDatabaseBuilder(resourceLoader)
			.addScript("foo")
			.build();

		JdbcOutboundGateway jdbcOutboundGateway = new JdbcOutboundGateway(dataSource, "insert into FOO VALUES('x')",
				"select * from FOO");

		@SuppressWarnings("unchecked")
		Message<Map<?, ?>> result = (Message<Map<?, ?>>) jdbcOutboundGateway.handleRequestMessage(new GenericMessage<String>(""));
		assertNotNull(result);
		assertEquals("x", result.getPayload().entrySet().iterator().next().getValue());
	}

	@Test
	public void testNoRowsReturned() {
		ResourceLoader resourceLoader = new ResourceLoader() {

			public Resource getResource(String location) {
				byte[] ddl = "CREATE TABLE BAR (ID VARCHAR(1));".getBytes();
				return new ByteArrayResource(ddl);
			}

			public ClassLoader getClassLoader() {
				return this.getClass().getClassLoader();
			}
		};
		DataSource dataSource = new EmbeddedDatabaseBuilder(resourceLoader)
			.addScript("foo")
			.build();

		JdbcOutboundGateway jdbcOutboundGateway = new JdbcOutboundGateway(dataSource, "insert into BAR VALUES('x')",
				"select * from BAR where ID = 'y'");

		List<Advice> adviceChain = new ArrayList<Advice>();
		adviceChain.add(new AbstractRequestHandlerAdvice() {

			@Override
			protected Object doInvoke(ExecutionCallback callback, Object target, Message<?> message) throws Exception {
				Object result = callback.execute();
				if (result == null) {
					result = new ErrorMessage(new MessageHandlingException(message, "No results returned from gateway"));
				}
				return result;
			}
		});

		jdbcOutboundGateway.setAdviceChain(adviceChain);
		QueueChannel outputChannel = new QueueChannel();
		jdbcOutboundGateway.setOutputChannel(outputChannel);
		jdbcOutboundGateway.afterPropertiesSet();

		jdbcOutboundGateway.handleMessage(new GenericMessage<String>(""));

		Message<?> result = outputChannel.receive(100);
		assertNotNull(result);
		assertEquals("No results returned from gateway", ((MessageHandlingException) result.getPayload()).getMessage());
	}
}
