package org.springframework.integration.util;
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


import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.events.XMLEvent;

import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;

/**
 * Examines the schema with the highest version and tries
 * to load any classes found in &lt;tool:expected-type/>&gt; or
 * &lt;tool:exports.&gt; elements. Returns 0 if all ok, -1 if
 * no schema found or number of classes that can't be loaded. 
 * @author Gary Russell
 * @since 2.2
 *
 */
public class SchemaClassChecker {

	/**
	 * @param args First argument is an ant path to the schema(s).
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		if (args.length < 1) {
			System.err.println("Requires schema search pattern");
			System.exit(1);
		}
		String schemaPattern = args[0];
		int missingClassesCount = 0;
		System.out.println(schemaPattern);
		ResourcePatternResolver patternResolver = new PathMatchingResourcePatternResolver();
		Resource[] resources = patternResolver.getResources(schemaPattern);
		System.out.println(resources.length);
		Pattern pattern = Pattern.compile(".*([0-9]\\.[0-9]).xsd$");
		float highVersion = 0.0f;
		Resource theSchema = null;
		for (Resource resource : resources) {
			String name = resource.getFilename();
			Matcher matcher = pattern.matcher(name);
			if (matcher.find()) {
				float version = Float.parseFloat(matcher.group(1));
				if (version > highVersion) {
					highVersion = version;
					theSchema = resource;
				}
			}
		}
		if (theSchema == null) {
			System.err.println("No Schema found at " + schemaPattern);
			System.exit(-1);
		}
		System.out.println(theSchema);
		XMLInputFactory factory = XMLInputFactory.newInstance();
		XMLStreamReader streamReader = factory.createXMLStreamReader(theSchema.getInputStream());
		boolean inExpectedType = false;
		while (streamReader.hasNext()) {
			int event = streamReader.next();
			switch (event) {
			case XMLEvent.START_ELEMENT:
				QName qName = streamReader.getName();
				inExpectedType = "http://www.springframework.org/schema/tool"
						.equals(qName.getNamespaceURI())
						&& ("expected-type".equals(qName.getLocalPart()) ||
						    "exports".equals(qName.getLocalPart()));
				if (inExpectedType) {
					int attributeCount = streamReader.getAttributeCount();
					for (int i = 0; i < attributeCount; i++) {
						String attributeName = streamReader.getAttributeLocalName(i);
						if ("type".equals(attributeName)) {
							String className = streamReader.getAttributeValue(i);
							try {
								System.out.println("loading:" + className);
								Class.forName(className);
							} catch (Exception e) {
								System.err.println("No class found:" + className + " in schema " + theSchema);
								missingClassesCount++;
							}
						}
					}
				}
				break;
			}
		}
		System.exit(missingClassesCount);
	}

}
