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

import java.util.concurrent.atomic.AtomicInteger;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.aop.framework.Advised;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.aop.support.NameMatchMethodPointcutAdvisor;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.integration.MessagingException;
import org.springframework.integration.core.MessageHandler;
import org.springframework.util.PatternMatchUtils;



/**
 * @author Gary Russell
 * @since 2.2
 *
 */
public class CircuitBreakerBeanPostProcessor implements BeanPostProcessor {

	private volatile String[] beanNamePatterns = new String[] {"*"};

	private final int threshold;

	private final long halfOpenAfter;

	/**
	 * Constructs a bean post processor that will advise {@link MessageHandler}s with
	 * a circuit breaker, based on its bean name matching one or more patterns.
	 * The default pattern is '*' - matching all message handlers.
	 * @param threshold The number of failures after which the circuit breaker
	 * will trip (go to opened state).
	 * @param halfOpenAfter The time in milliseconds after the last failure
	 * before we let an attempt to call the message handler.
	 */
	public CircuitBreakerBeanPostProcessor(int threshold, long halfOpenAfter) {
		this.threshold = threshold;
		this.halfOpenAfter = halfOpenAfter;
	}

	/**
	 * @param beanNamePatterns an array of patterns used to match
	 * {@link MessageHandler} bean names to which the circuit breaker
	 * will be applied.
	 */
	public void setBeanNamePatterns(String[] beanNamePatterns) {
		this.beanNamePatterns = beanNamePatterns;
	}

	public Object postProcessBeforeInitialization(Object bean, String beanName)
			throws BeansException {
		return bean;
	}

	public Object postProcessAfterInitialization(Object bean, String beanName)
			throws BeansException {
		if (!(bean instanceof MessageHandler)) {
			return bean;
		}
		if (!PatternMatchUtils.simpleMatch(this.beanNamePatterns, beanName)) {
			return bean;
		}
		CircuitBreakerInterceptor interceptor =
				new CircuitBreakerInterceptor(beanName, this.threshold, this.halfOpenAfter);
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

	private class CircuitBreakerInterceptor implements MethodInterceptor {

		private final AtomicInteger failures = new AtomicInteger();
		private final int threshold;
		private final long halfOpenAfter;
		private volatile long lastFailure;
		private final String beanName;

		public CircuitBreakerInterceptor(String beanName, int threshold, long halfOpenAfter) {
			this.beanName = beanName;
			this.threshold = threshold;
			this.halfOpenAfter = halfOpenAfter;
		}

		public Object invoke(MethodInvocation invocation) throws Throwable {

			if (this.failures.get() >= this.threshold &&
					System.currentTimeMillis() - this.lastFailure < this.halfOpenAfter) {
				throw new MessagingException("Circuit Breaker is Open for " + this.beanName);
			}
			try {
				Object result = invocation.proceed();
				this.failures.set(0);
				return result;
			}
			catch (MessagingException e) {
				failures.incrementAndGet();
				this.lastFailure = System.currentTimeMillis();
				throw e;
			}
		}
	}
}
