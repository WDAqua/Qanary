package eu.wdaqua.qanary.web;

import eu.wdaqua.qanary.web.messages.AdditionalTriples;

import org.springframework.core.convert.converter.Converter;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;

@Component
public class StringToTtlConverter implements Converter<String, AdditionalTriples> {
	@Autowired
	private Environment environment;

	@Override
	public AdditionalTriples convert(String triples) {
		return new AdditionalTriples(triples, environment);
	}
}
