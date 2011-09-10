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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.context.expression.BeanFactoryResolver;
import org.springframework.context.expression.MapAccessor;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.integration.Message;
import org.springframework.integration.MessagingException;
import org.springframework.integration.file.remote.session.Session;
import org.springframework.integration.file.remote.session.SessionFactory;
import org.springframework.integration.file.remote.session.SessionFactoryResolver;
import org.springframework.util.Assert;

/**
 * @author Gary Russell
 * @since 2.2
 *
 */
public abstract class AbstractRemoteFileTemplate<F> implements
		RemoteFileOperations<F>, BeanFactoryAware {

	private final SessionFactoryResolver<F> sessionFactoryResolver;

	private final Expression commandExpression;

	private final Expression remoteDirectoryExpression;

	private final Expression remoteFileExpression;

	private final Expression localDirectoryExpression;

	private final Expression localFileExpression;

	private volatile BeanFactory beanFactory;

	private volatile String temporaryFileSuffix = ".writing";

	private volatile String fileSeparator = "/";
	/**
	 * Must resolve to get, put, ls, rm, mkdir, rmdir
	 * followed by any command-specific options
	 * <p> e.g. 'get -P'
	 * @param expression
	 */
	public AbstractRemoteFileTemplate(SessionFactoryResolver<F> sessionFactoryResolver,
			Expression commandExpression,
			Expression remoteDirectoryExpression,
			Expression remoteFileExpression,
			Expression localDirectoryExpression,
			Expression localFileExpression) {
		Assert.notNull(sessionFactoryResolver, "SessionFactoryResolver may not be null");
		Assert.notNull(remoteDirectoryExpression, "Remote directory expression may not be null");
		Assert.notNull(remoteFileExpression, "Remote file expression may not be null");
		Assert.notNull(localDirectoryExpression, "Local directory expression may not be null");
		Assert.notNull(localFileExpression, "Local file expression may not be null");

		this.sessionFactoryResolver = sessionFactoryResolver;
		this.commandExpression = commandExpression;
		this.remoteDirectoryExpression = remoteDirectoryExpression;
		this.remoteFileExpression = remoteFileExpression;
		this.localDirectoryExpression = localDirectoryExpression;
		this.localFileExpression = localFileExpression;
	}

	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		Assert.notNull(beanFactory, "BeanFactory may not be null");
		this.beanFactory = beanFactory;
	}

	protected BeanFactory getBeanFactory() {
		return beanFactory;
	}

	protected SessionFactoryResolver<F> getSessionFactoryResolver() {
		return sessionFactoryResolver;
	}

	protected String getCommand(Message<?> message, EvaluationContext context) {
		Assert.notNull(this.commandExpression, "Command expression may not be null");
		String value = (String) commandExpression.getValue(context);
		Assert.notNull(value, "Command resolved to null");
		return value;
	}

	protected String getRemoteDirectory(Message<?> message, EvaluationContext context) {
		String value = (String) remoteDirectoryExpression.getValue(context);
		Assert.notNull(value, "Remote directory resolved to null");
		return value;
	}

	protected String getRemoteFile(Message<?> message, EvaluationContext context) {
		String value = (String) remoteFileExpression.getValue(context);
		Assert.notNull(value, "Remote file resolved to null");
		return value;
	}

	protected String getLocalDirectory(Message<?> message, EvaluationContext context) {
		String value = (String) localDirectoryExpression.getValue(context);
		Assert.notNull(value, "Local directory resolved to null");
		return value;
	}
//TODO: private?
	protected String getLocalFile(Message<?> message, EvaluationContext context) {
		String value = (String) localFileExpression.getValue(context);
		Assert.notNull(value, "Local file resolved to null");
		return value;
	}

	private StandardEvaluationContext createEvaluationContext(){
		StandardEvaluationContext evaluationContext = new StandardEvaluationContext();
		evaluationContext.addPropertyAccessor(new MapAccessor());
		BeanFactory beanFactory = this.getBeanFactory();
		if (beanFactory != null) {
			evaluationContext.setBeanResolver(new BeanFactoryResolver(beanFactory));
		}
		return evaluationContext;
	}

	public File get(Message<?> message) throws IOException {
		StandardEvaluationContext context = createEvaluationContext();
		context.setRootObject(message);
		SessionFactory<F> factory = this.sessionFactoryResolver.resolve(message);
		Session<F> session = factory.getSession();
		File localFile = new File(this.getLocalDirectory(message, context), this.getLocalFile(message, context));
		if (!localFile.exists()) {
			String tempFileName = localFile.getAbsolutePath() + this.temporaryFileSuffix;
			File tempFile = new File(tempFileName);
			FileOutputStream fileOutputStream = new FileOutputStream(tempFile);
			StringBuilder remoteFilePath = new StringBuilder();
			String remoteDirectory = this.getRemoteDirectory(message, context);
			remoteFilePath.append(remoteDirectory);
			if (!(remoteDirectory.endsWith(this.fileSeparator))) {
				remoteFilePath.append(this.fileSeparator);
			}
			String remoteFile = this.getRemoteFile(message, context);
			remoteFilePath.append(remoteFile);
			try {
				session.read(remoteFilePath.toString(), fileOutputStream);
			}
			catch (Exception e) {
				if (e instanceof RuntimeException){
					throw (RuntimeException) e;
				}
				else {
					throw new MessagingException("Failure occurred while copying from remote to local directory", e);
				}
			}
			finally {
				try {
					fileOutputStream.close();
				}
				catch (Exception ignored2) {
				}
			}
			if (!tempFile.renameTo(localFile)) {
				throw new MessagingException("Failed to rename local file");
			}
			return localFile;
		}
		else {
			throw new MessagingException("Local file " + localFile + " already exists");
		}
	}

	public void execute(Message<?> message) throws IOException {
		// TODO Auto-generated method stub

	}

	public List<FileInfo<F>> ls(Message<?> message) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}


}
