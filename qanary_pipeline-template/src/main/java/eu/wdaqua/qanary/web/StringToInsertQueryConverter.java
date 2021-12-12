package eu.wdaqua.qanary.web;

import eu.wdaqua.qanary.web.messages.AdditionalTriples;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class StringToInsertQueryConverter implements Converter<String, AdditionalTriples> {

	@Override
	public AdditionalTriples convert(String triples) {
		return new AdditionalTriples(triples);
	}
}
