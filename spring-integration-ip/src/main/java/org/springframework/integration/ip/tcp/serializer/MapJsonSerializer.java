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
package org.springframework.integration.ip.tcp.serializer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonParser.Feature;
import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.serializer.Deserializer;
import org.springframework.core.serializer.Serializer;
import org.springframework.util.Assert;

/**
 * Serializes a {@link Map} as JSON. Deserializes JSON to
 * a {@link Map}. The default {@link ObjectMapper} can be
 * overridden using {@link #setObjectMapper(ObjectMapper)}.
 * Note that Feature.AUTO_CLOSE_SOURCE and
 * JsonGenerator.Feature.AUTO_CLOSE_TARGET
 * and will be disabled to avoid closing the connection's
 * InputStream and OutputStream.
 * <p/>
 * The jackson deserializer can't delimit multiple JSON
 * objects. Therefore another (de)serializer is used to
 * apply structure to the stream. By default, this is a
 * simple {@link ByteArrayLfSerializer}, which inserts/expects
 * LF (0x0a) between messages.
 *
 * @author Gary Russell
 * @since 3.0
 *
 */
public class MapJsonSerializer implements Serializer<Map<?, ?>>, Deserializer<Map<?, ?>>,
		InitializingBean {

	private volatile ObjectMapper objectMapper = new ObjectMapper();

	private volatile Deserializer<byte[]> packetDeserializer = new ByteArrayLfSerializer();

	private volatile Serializer<byte[]> packetSerializer = new ByteArrayLfSerializer();

	/**
	 * An {@link ObjectMapper} to be used for the conversion to/from
	 * JSON. Use this if you wish to set additional Jackson features.
	 * @param objectMapper the objectMapper.
	 */
	public void setObjectMapper(ObjectMapper objectMapper) {
		Assert.notNull(objectMapper, "'objectMapper' cannot be null");
		this.objectMapper = objectMapper;
	}

	/**
	 * A {@link Deserializer} that will construct the full JSON content from
	 * the stream which is then passed to the ObjectMapper. Default is
	 * {@link ByteArrayLfSerializer}.
	 * @param packetDeserializer the packetDeserializer
	 */
	public void setPacketDeserializer(Deserializer<byte[]> packetDeserializer) {
		Assert.notNull(packetDeserializer, "'packetDeserializer' cannot be null");
		this.packetDeserializer = packetDeserializer;
	}

	/**
	 * A {@link Serializer} that will delimit the full JSON content in
	 * the stream. Default is
	 * {@link ByteArrayLfSerializer}.
	 * @param packetSerializer the packetSerializer
	 */
	public void setPacketSerializer(Serializer<byte[]> packetSerializer) {
		Assert.notNull(packetSerializer, "'packetSerializer' cannot be null");
		this.packetSerializer = packetSerializer;
	}

	public void afterPropertiesSet() throws Exception {
		this.objectMapper.configure(Feature.AUTO_CLOSE_SOURCE, false);
		this.objectMapper.configure(JsonGenerator.Feature.AUTO_CLOSE_TARGET, false);
	}

	public Map<?, ?> deserialize(InputStream inputStream) throws IOException {
		byte[] bytes = this.packetDeserializer.deserialize(inputStream);
		return this.objectMapper.readValue(new ByteArrayInputStream(bytes), Map.class);
	}

	public void serialize(Map<?, ?> object, OutputStream outputStream) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		this.objectMapper.writeValue(baos, object);
		this.packetSerializer.serialize(baos.toByteArray(), outputStream);
		outputStream.flush();
	}

}
