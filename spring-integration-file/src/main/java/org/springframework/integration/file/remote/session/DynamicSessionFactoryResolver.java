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
package org.springframework.integration.file.remote.session;

import org.springframework.expression.Expression;
import org.springframework.integration.Message;

/**
 * Uses a {@link SessionFactory} as a template, overriding the
 * connection attributes as necessary. Wraps each instance
 * in a {@link CachingSessionFactory} if requested. Once a
 * session factory has been created, it is cached for further use.
 * <p>
 * Credential expressions needs to be provided appropriately - for
 * example, it is not appropriate to provide a privateKey expression
 * when the template is an Ftp session factory.
 *
 * @author Gary Russell
 * @since 2.2
 *
 */
public abstract class DynamicSessionFactoryResolver<F> implements SessionFactoryResolver<F> {

	private SessionFactory<F> templateSessionFactory;

	public DynamicSessionFactoryResolver(SessionFactory<F> templateSessionFactory,
			Expression hostExpression,
			Expression portExpression,
			Expression credentialsExpression,
			Expression passwordExpression) {
		this.templateSessionFactory = templateSessionFactory;
	}

	public SessionFactory<F> resolve(Message<?> message) {
		// TODO Auto-generated method stub
		return null;
	}
}
