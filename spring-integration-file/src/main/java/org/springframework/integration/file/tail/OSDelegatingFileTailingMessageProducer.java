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
package org.springframework.integration.file.tail;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.springframework.integration.MessagingException;
import org.springframework.util.Assert;

/**
 * A file tailing message producer that delegates to the OS tail program.
 * This is likely the most efficient mechanism on platforms that support it.
 * Default options are "-F -n 0" (follow file name, no existing records).
 *
 * @author Gary Russell
 * @since 3.0
 *
 */
public class OSDelegatingFileTailingMessageProducer extends FileTailingMessageProducerSupport
		implements Runnable {

	private volatile Process process;

	private volatile String options = "-F -n 0";

	private volatile String command = "tail";

	private volatile BufferedReader reader;

	public void setOptions(String options) {
		this.options = options;
	}

	@Override
	public String getComponentType() {
		return super.getComponentType() + " (native)";
	}

	@Override
	protected void onInit() {
		Assert.notNull(getFile(), "File cannot be null");
		super.onInit();
		String command = "tail";
		this.command = command +  " " + this.options + " " + this.getFile().getAbsolutePath();
	}

	@Override
	protected void doStart() {
		super.doStart();
		this.runExec();
	}

	@Override
	protected void doStop() {
		super.doStop();
		if (this.process != null) {
			this.process.destroy();
			this.process = null;
		}
	}

	private void runExec() {
		try {
			this.process = Runtime.getRuntime().exec(this.command);
			this.reader = new BufferedReader(new InputStreamReader(this.process.getInputStream()));
			this.getTaskExecutor().execute(this);
			this.startStatusReader();
			this.startProcessMonitor();
		}
		catch (IOException e) {
			throw new MessagingException("Failed to exec tail command: '" + this.options + "'", e);
		}
	}

	private void startProcessMonitor() {
		this.getTaskExecutor().execute(new Runnable() {

			@Override
			public void run() {
				int result = Integer.MIN_VALUE;
				try {
					result = process.waitFor();
					if (logger.isInfoEnabled()) {
						logger.info("tail process terminated with value " + result);
					}
				}
				catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
				finally {
					if (process != null) {
						process.destroy();
						process = null;
					}
				}
				if (isRunning()) {
					logger.info("Restarting tail process");
					runExec();
				}
			}
		});
	}

	private void startStatusReader() {
		final BufferedReader errorReader = new BufferedReader(new InputStreamReader(this.process.getErrorStream()));
		this.getTaskExecutor().execute(new Runnable() {

			@Override
			public void run() {
				String statusMessage;
				try {
					while ((statusMessage = errorReader.readLine()) != null) {
						publish(statusMessage);
						if (logger.isTraceEnabled()) {
							logger.trace(statusMessage);
						}
					}
				}
				catch (IOException e) {
					logger.error("Exception on tail error reader", e);
				}
				finally {
					try {
						errorReader.close();
					}
					catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		});
	}

	@Override
	public void run() {
		String line;
		this.hasStarted();
		try {
			while ((line = this.reader.readLine()) != null) {
				this.send(line);
			}
		}
		catch (IOException e) {
			logger.error("Exception on tail error reader", e);
			try {
				this.reader.close();
			}
			catch (IOException e1) {

			}
			this.process.destroy();
			process = null;
		}
	}


}
