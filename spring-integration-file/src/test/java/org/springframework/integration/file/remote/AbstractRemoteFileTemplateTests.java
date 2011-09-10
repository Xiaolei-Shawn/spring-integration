/*
 * Copyright 2002-2012 the original author or authors.
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
package org.springframework.integration.file.remote;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.integration.Message;
import org.springframework.integration.file.remote.session.Session;
import org.springframework.integration.file.remote.session.SessionFactory;
import org.springframework.integration.file.remote.session.SessionFactoryResolver;
import org.springframework.integration.file.remote.session.SimpleSessionFactoryResolver;
import org.springframework.integration.message.GenericMessage;
import org.springframework.integration.support.MessageBuilder;

/**
 * @author Gary Russell
 * @since 2.2
 *
 */
public class AbstractRemoteFileTemplateTests {

	/**
	 *
	 */
	private static final String TMP_DIR = System.getProperty("java.io.tmpdir");

	@Test
	public void testGetWithMessage() throws Exception {
		@SuppressWarnings("unchecked")
		SessionFactory<TestFile> sf = mock(SessionFactory.class);
		@SuppressWarnings("unchecked")
		Session<TestFile> session = mock(Session.class);
		when(sf.getSession()).thenReturn(session);
		final AtomicReference<Object> source = new AtomicReference<Object>();
		doAnswer(new Answer<Object>(){
			public Object answer(InvocationOnMock invocation) throws Throwable {
				source.set(invocation.getArguments()[0]);
				((OutputStream) invocation.getArguments()[1]).write("foo".getBytes());
				return null;
			}
		}).when(session).read(Mockito.anyString(), Mockito.any(OutputStream.class));
		File out = new File(TMP_DIR, "qux.out");
		out.delete();
		SessionFactoryResolver<TestFile> sessionFactoryResolver = new SimpleSessionFactoryResolver<TestFile>(sf);
		Expression remoteDirectoryExpression = new SpelExpressionParser().parseExpression("headers['foo']");
		Expression remoteFileExpression = new SpelExpressionParser().parseExpression("headers['baz']");
		Expression localDirectoryExpression = new SpelExpressionParser().parseExpression("'" + TMP_DIR + "'");
		Expression localFileExpression = new SpelExpressionParser().parseExpression("'qux.out'");
		TestRemoteFileTemplate template = new TestRemoteFileTemplate(sessionFactoryResolver, null,
				remoteDirectoryExpression, remoteFileExpression, localDirectoryExpression, localFileExpression);
		Message<String> message = MessageBuilder.withPayload("Hello, world!")
				.setHeader("foo", "bar")
				.setHeader("baz", "qux")
				.build();

		template.get(message);
		assertTrue(out.exists());
		out.delete();
		assertEquals("bar/qux", source.get());
	}

	@Test
	public void testGetLiterals() throws Exception {
		@SuppressWarnings("unchecked")
		SessionFactory<TestFile> sf = mock(SessionFactory.class);
		@SuppressWarnings("unchecked")
		Session<TestFile> session = mock(Session.class);
		when(sf.getSession()).thenReturn(session);
		final AtomicReference<Object> source = new AtomicReference<Object>();
		doAnswer(new Answer<Object>(){
			public Object answer(InvocationOnMock invocation) throws Throwable {
				source.set(invocation.getArguments()[0]);
				((OutputStream) invocation.getArguments()[1]).write("foo".getBytes());
				return null;
			}
		}).when(session).read(Mockito.anyString(), Mockito.any(OutputStream.class));
		File out = new File(TMP_DIR, "qux.out");
		out.delete();
		SessionFactoryResolver<TestFile> sessionFactoryResolver = new SimpleSessionFactoryResolver<TestFile>(sf);
		Expression remoteDirectoryExpression = new SpelExpressionParser().parseExpression("'bar'");
		Expression remoteFileExpression = new SpelExpressionParser().parseExpression("'baz'");
		Expression localDirectoryExpression = new SpelExpressionParser().parseExpression("'" + TMP_DIR + "'");
		Expression localFileExpression = new SpelExpressionParser().parseExpression("'qux.out'");
		TestRemoteFileTemplate template = new TestRemoteFileTemplate(sessionFactoryResolver, null,
				remoteDirectoryExpression, remoteFileExpression, localDirectoryExpression, localFileExpression);
		template.get(new GenericMessage<String>("Hello, world!"));
		assertTrue(out.exists());
		out.delete();
		assertEquals("bar/baz", source.get());
	}

	private class TestRemoteFileTemplate extends AbstractRemoteFileTemplate<TestFile> {

		public TestRemoteFileTemplate(SessionFactoryResolver<TestFile> sessionFactoryResolver,
				Expression commandExpression, Expression remoteDirectoryExpression, Expression remoteFileExpression,
				Expression localDirectoryExpression, Expression localFileExpression) {
			super(sessionFactoryResolver, commandExpression, remoteDirectoryExpression, remoteFileExpression,
					localDirectoryExpression, localFileExpression);
		}


	}

	private class TestFile {

	}

	private class TestFileInfo extends AbstractFileInfo<TestFile> {

		public boolean isDirectory() {
			return false;
		}

		public boolean isLink() {
			return false;
		}

		public long getSize() {
			return 0;
		}

		public long getModified() {
			return 0;
		}

		public String getFilename() {
			return null;
		}

		public String getPermissions() {
			return null;
		}

		public TestFile getFileInfo() {
			return null;
		}

	}
}
