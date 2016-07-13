package eu.wdaqua.qanary.web;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.query.ResultSetFormatter;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.sparql.resultset.ResultsFormat;

import eu.wdaqua.qanary.business.QanaryConfigurator;
import eu.wdaqua.qanary.sparql.SparqlConnector;

/**
 * represents the general Sparql endpoint wrapping the Qanary triplestore
 *
 * @author AnBo
 */
@Controller
public class QanarySparqlController {

    private static final Logger logger = LoggerFactory.getLogger(QanarySparqlController.class);

    private final QanaryConfigurator qanaryConfigurator;

    private final SparqlConnector sparqlConnector;

    @Autowired
    public QanarySparqlController(final QanaryConfigurator qanaryConfigurator, final SparqlConnector sparqlConnector) {
        this.qanaryConfigurator = qanaryConfigurator;
        this.sparqlConnector = sparqlConnector;
    }

    /**
     * wrapper for SPARQL endpoint
     */
    @RequestMapping(value = "/sparql")
    public void executeSparqlQuery(@RequestParam(value = "query", required = true) final String query,
                                   final HttpServletResponse response) {
        try {
            if (StringUtils.containsIgnoreCase(query, "select")) {
                final ResultSet result = sparqlConnector.select(query);
                response.setContentType(ResultsFormat.FMT_RS_JSON.getSymbol());
                ResultSetFormatter.output(response.getOutputStream(), result, ResultsFormat.FMT_RS_JSON);

            } else if (StringUtils.containsIgnoreCase(query, "construct")) {
                final Model graph = sparqlConnector.construct(query);
                response.setContentType("text/turtle");
                graph.write(response.getOutputStream(), "TURTLE");
            } else if (StringUtils.containsIgnoreCase(query, "INSERT")
                    || StringUtils.containsIgnoreCase(query, "DELETE")) {
                sparqlConnector.update(query);
            }
            response.setStatus(200);
        } catch (final Exception e) {
            try {
                response.sendError(500, e.toString());
            } catch (final IOException e1) {
                e1.printStackTrace();
            }
        }
    }

}
