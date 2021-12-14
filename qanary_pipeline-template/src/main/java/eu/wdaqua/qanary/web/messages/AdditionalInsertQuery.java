package eu.wdaqua.qanary.web.messages;

import org.apache.jena.update.UpdateFactory;
import org.apache.jena.query.QueryParseException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.ArrayList;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class AdditionalInsertQuery {
	private String insertQuery;

	private static final Logger logger = LoggerFactory.getLogger(AdditionalTriples.class);

	public AdditionalInsertQuery(String insertQuery) {
		if (insertQuery != null) {
			try {
				this.insertQuery = parseInsertQuery(addBindsToInsertQuery(insertQuery));
				logger.info("initializing AdditionalTriples with query \n{}", this.insertQuery);
			} catch (Exception e) {
				logger.warn("initializing no additonal query: \n{}", e.getMessage());
				this.insertQuery = null;
			}
		} else {
			logger.info("initializing no additonal query");
			this.insertQuery = null;
		}
	}

	public String getInsertQuery() {
		return this.insertQuery;
	}

	@Deprecated
	public String getInsertQueryForGraph(String graph) {
		return this.insertQuery.replace("<GRAPH>", "<"+graph+">");
	}

	private String addBindsToInsertQuery(String insertQuery) {
		String sparqlBinds = getSparqlBindsForVariables(insertQuery);
		if (sparqlBinds.length()>0) {
			return insertQuery+""// 
				+ " WHERE { " //
				+ sparqlBinds
				+"} "; //
		} else return insertQuery;
	}

	private String getSparqlBindsForVariables(String insertQuery) {
		String sparqlBinds = "";

		List<String> unnamed = findAdditionalVaraibles(insertQuery);

		for (String i : unnamed) {
			logger.info("adding URI bind for var {}", i);
			sparqlBinds += "BIND (IRI(str(RAND())) AS "+i+") . ";
		}
		return sparqlBinds;
	}


	private String parseInsertQuery(String expectedQuery) {
		logger.info("parsing query: \n{}", expectedQuery);
		Pattern pattern = Pattern.compile(".*(delete|clear|load)+.*");
		Matcher matcher = pattern.matcher(expectedQuery.toLowerCase());
		if (expectedQuery.length() == 0) {
			logger.info("no additional SPARQL INSERT query was supplied");
			return null;
		} else if (matcher.find()) {
			throw new QueryParseException(
					"additional SPARQL queries must not contain either of DELETE | CLEAR | LOAD",
					0, matcher.start() // column is not computed
					);
		} else if (!expectedQuery.toLowerCase().contains("insert")) {
			throw new QueryParseException(
					"additional SPARQL queries are expected to contain an insert statement", 0, 0);
		} else {
			try {
				UpdateFactory.create(expectedQuery);
				logger.info("successfully parsed additional SPARQL INSERT query");
				return expectedQuery;
			} catch (Exception e) {
				logger.info("parsing additional SPARQL query failed with exception: \n{}", e.getLocalizedMessage());
				return null;
			}
		}
	}

	/**
	 * return a list of all variables used in a SPARQL query
	 *
	 * @param additionalInsertQuery
	 * @return variables
	 */
	private List<String> findAdditionalVaraibles(String insertQuery) {
		int lastIdx = 0;
		String varString;
		List<String> variables = new ArrayList<String>();

		while (lastIdx != -1) {
			lastIdx = insertQuery.indexOf("?", lastIdx);
			int end = insertQuery.indexOf(" ", lastIdx);

			if (lastIdx != -1) {
				varString = insertQuery.substring(lastIdx, end);
				if (!variables.contains(varString)) {
					logger.info("found new var {} at ({},{})", varString, lastIdx, end);
					variables.add(varString);
				}
				lastIdx += varString.length();
			}
		}
		return variables;
	}

}
