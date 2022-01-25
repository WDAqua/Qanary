package eu.wdaqua.qanary.web;

import eu.wdaqua.qanary.web.messages.AdditionalInsertQuery;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class StringToInsertQueryConverter implements Converter<String, AdditionalInsertQuery> {

	@Override
	public AdditionalInsertQuery convert(String query) {
		return new AdditionalInsertQuery(query);
	}
}
