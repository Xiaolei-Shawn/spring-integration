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
package org.springframework.integration.ip.tcp.sockjs;

import java.io.IOException;
import java.net.Socket;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.net.SocketFactory;

import org.springframework.integration.ip.tcp.serializer.ByteArrayCrLfSerializer;
import org.springframework.integration.ip.tcp.serializer.SoftEndOfStreamException;
import org.springframework.integration.ip.tcp.serializer.XHRStreamingChunkDeserializer;

/**
 * @author Gary Russell
 * @since 2.2
 *
 */
public class SockJSXHRStreamingClient {

	private XHRStreamingChunkDeserializer deserializer = new XHRStreamingChunkDeserializer();

	private ByteArrayCrLfSerializer crlfDeserializer = new ByteArrayCrLfSerializer();

	Map<String, String> cookies = new ConcurrentHashMap<String, String>(); // TODO: needs to be nicer than this

	public static void main(String[] args) throws Exception {
		new SockJSXHRStreamingClient().start();
	}

	public void start() throws Exception {
		int port = 80;
//		int port = 8081;
//		String host = "localhost";
		String host = "echo-test.cloudfoundry.com";
//		String host = "192.168.222.132";
		String uuid = UUID.randomUUID().toString();
		String init =
			"POST /echo/000/" + uuid + "/xhr_streaming HTTP/1.1\r\n" +
			"Host: " + host + "\r\n" +
			"Connection: keep-alive\r\n" +
			"Accept-Encoding: identity\r\n" +
			"Content-Length: 0\r\n" +
			"\r\n";
		Socket sock = SocketFactory.getDefault().createSocket(host, port);
		ExecutorService executor = Executors.newSingleThreadExecutor();
		executor.execute(new SocksJSXHRStreamingReader(sock, uuid));
		sock.getOutputStream().write(init.getBytes());
		String statusLine = "HTTP/1.1 204 No Content";
		int count = 0;
		do {
			Thread.sleep(1000);
			if (sock.isClosed()) {
				break;
			}
			if (cookies.get(uuid) == null) {
				System.out.println("No cookies yet");
				continue;
			}
			Socket sender = SocketFactory.getDefault().createSocket(host, port);
			statusLine = send(sender, uuid, host);
		}
		while (statusLine.equals("HTTP/1.1 204 No Content") && count++ < 40);
		this.deserializer.removeState(sock.getInputStream());
		sock.close();
		executor.shutdown();
	}

	private String readHeaders(Socket sock) throws IOException {
		String statusLine = new String(this.crlfDeserializer.deserialize(sock.getInputStream()));
		while (true) {
			String s = new String(this.crlfDeserializer.deserialize(sock.getInputStream()));
//			System.out.println(s);
			if (s.length() == 0) {
				break;
			}
		}
//		System.out.println("Read headers");
//		System.out.println(statusLine);
		return statusLine;
	}

	private String send(Socket sock, String uuid, String host) throws IOException {
		String content = "[\"" + new String(new char[128]).replace('\0', 'x') + "\"]";
		String sendData =
			"POST /echo/000/" + uuid + "/xhr_send HTTP/1.1\r\n" +
			"Host: " + host + "\r\n" +
			"Accept-Encoding: identity\r\n" +
			this.cookies.get(uuid) + "\r\n" +
			"Content-Length: " + content.length() + "\r\n" +
			"\r\n" +
			content;
		System.out.println("Sending... " + content);
		sock.getOutputStream().write(sendData.getBytes());
		String statusLine = readHeaders(sock);
		sock.close();
		return statusLine;
	}

	private class SocksJSXHRStreamingReader implements Runnable {

		private final Socket sock;

		private final String uuid;

		private SocksJSXHRStreamingReader(Socket sock, String uuid) {
			this.sock = sock;
			this.uuid = uuid;
		}

		public void run() {
			try {
				while (true) {
					try {
						String frameData = deserializer.deserialize(this.sock.getInputStream());
						// some servers put multiple frames in the same chunk
						String[] frames;
						if (frameData.contains("\r") && frameData.startsWith("HTTP")) {
							System.out.println("Received:Headers\r\n" + frameData);
							String[] headers = frameData.split("\\r\\n");
							String cookies = "Cookie: ";
							for (String header : headers) {
								if (header.startsWith("Set-Cookie")) {
									String[] bits = header.split(": *");
									cookies += bits[1] + "; ";
								}
							}
							System.out.println(cookies);
							SockJSXHRStreamingClient.this.cookies.put(this.uuid, cookies);
						}
						else {
							if (frameData.contains("\n")) {
								frames = frameData.split(",");
							}
							else {
								frames = new String[] {frameData};
							}
							for (String data : frames) {
								if (data.length() == 1 && data.equals("h")) {
									System.out.println("Received:SockJS-Heartbeat");
								}
								else if (data.length() == 0x800 && data.startsWith("hhhhhhhhhhhhh")) {
									System.out.println("Received:SockJS-XHR-Prelude");
								}
								else if (data.length() == 1 && data.equals("o")) {
									System.out.println("Received:SockJS-Open");
								}
								else if (data.length() > 0 && data.startsWith("c")) {
									System.out.println("Received SockJS-Close:" + data.substring(1));
									sock.close();
									return;
								}
								else if (data.length() > 0 && data.startsWith("a")) {
									System.out.println("Received data:" + data.substring(1));
								}
								else {
									System.out.println("Received unexpected:" + new String(data));
								}
							}
						}
					}
					catch (SoftEndOfStreamException seose) {
						System.out.println("Stream closed");
						throw new RuntimeException(seose);
					}
					catch (IOException e) {
						if(!("Socket closed".equals(e.getMessage()))) {
							e.printStackTrace();
							throw new RuntimeException(e);
						}
						return;
					}
				}
			}
			catch (RuntimeException re) {
				if (!(re.getCause() instanceof SoftEndOfStreamException)) {
					re.printStackTrace();
				}
			}
			finally {
				try {
					this.sock.close();
				} catch (IOException e1) {
					e1.printStackTrace();
					throw new RuntimeException(e1);
				}
			}
		}
	}

}
