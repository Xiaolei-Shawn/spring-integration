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
package org.springframework.integration.handler;

import java.util.HashSet;
import java.util.Set;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.aop.framework.Advised;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.aop.support.NameMatchMethodPointcutAdvisor;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.SimpleAliasRegistry;
import org.springframework.integration.MessagingException;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;
import org.springframework.integration.core.MessageHandler;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.backoff.BackOffPolicy;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.util.Assert;



/**
 * @author Gary Russell
 * @since 2.2
 *
 */
public class RetryBeanPostProcessor implements BeanPostProcessor, InitializingBean, BeanFactoryAware {

	private final static Log logger = LogFactory.getLog(RetryBeanPostProcessor.class);

	private final RetryTemplate retryTemplate;

	private volatile Set<String> handlerNames;

	private volatile BeanFactory beanFactory;

	public RetryBeanPostProcessor() {
		this(new RetryTemplate());
		this.retryTemplate.setBackOffPolicy(new FixedBackOffPolicy());
	}

	public RetryBeanPostProcessor(RetryTemplate retryTemplate) {
		this.retryTemplate = retryTemplate;
	}

	public void setHandlers(String[] handlerNames) {
		Assert.notNull(handlerNames, "Handlers may not be null");
		Set<String> theHandlerNames = new HashSet<String>();
		for (String handlerName : handlerNames) {
			if (!handlerName.endsWith(IntegrationNamespaceUtils.HANDLER_ALIAS_SUFFIX)) {
				theHandlerNames.add(handlerName + IntegrationNamespaceUtils.HANDLER_ALIAS_SUFFIX);
			}
			else {
				theHandlerNames.add(handlerName);
			}
		}
		this.handlerNames = theHandlerNames;
	}

	public void setBackofPolicy(BackOffPolicy backOffPolicy) {
		this.retryTemplate.setBackOffPolicy(backOffPolicy);
	}

	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		this.beanFactory = beanFactory;
	}

	public void afterPropertiesSet() throws Exception {
		Assert.notNull(this.beanFactory, "BeanFactory cannot be null");
		if (!(this.beanFactory instanceof SimpleAliasRegistry)) {
			return;
		}
		Set<String> canonicalHandlerNames = new HashSet<String>();
		SimpleAliasRegistry registry = (SimpleAliasRegistry) this.beanFactory;
		for (String handlerName : this.handlerNames) {
			canonicalHandlerNames.add(registry.canonicalName(handlerName));
		}
		this.handlerNames = canonicalHandlerNames;
	}

	public Object postProcessBeforeInitialization(Object bean, String beanName)
			throws BeansException {
		return bean;
	}

	public Object postProcessAfterInitialization(Object bean, String beanName)
			throws BeansException {
		if (!(bean instanceof MessageHandler) ||
				!handlerNames.contains(beanName)) {
			return bean;
		}
		StatelessRetryInterceptor interceptor = new StatelessRetryInterceptor();
		NameMatchMethodPointcutAdvisor advisor = new NameMatchMethodPointcutAdvisor(interceptor);
		advisor.addMethodName("handleMessage");
		Class<?> targetClass = AopUtils.getTargetClass(bean);
		if (AopUtils.canApply(advisor.getPointcut(), targetClass)) {
			if (bean instanceof Advised) {
				((Advised) bean).addAdvisor(advisor);
				return bean;
			}
			else {
				ProxyFactory proxyFactory = new ProxyFactory(bean);
				proxyFactory.addAdvisor(advisor);
				return proxyFactory.getProxy(bean.getClass().getClassLoader());
			}
		}
		return bean;
	}

	private class StatelessRetryInterceptor implements MethodInterceptor {

		public Object invoke(final MethodInvocation invocation) throws Throwable {

			Object result = retryTemplate.execute(new RetryCallback<Object>() {

				public Object doWithRetry(RetryContext context) throws Exception {
					try {
						return invocation.proceed();
					}
					catch (Throwable e) {
						if (e instanceof MessagingException) {
							throw (MessagingException) e;
						}
						throw new MessagingException("Failed to invoke handler", e);
					}
				}
			});
			return result;
		}
	}

	// TODO: Stateful
	private class StatefulRetryInterceptor implements MethodInterceptor {

		public Object invoke(MethodInvocation invocation) throws Throwable {
			// TODO Auto-generated method stub
			return null;
		}
	}

}
