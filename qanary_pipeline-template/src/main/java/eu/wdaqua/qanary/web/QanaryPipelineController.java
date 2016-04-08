package eu.wdaqua.qanary.web;

import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.query.ResultSetFormatter;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.sparql.resultset.ResultsFormat;
import eu.wdaqua.qanary.business.QanaryConfigurator;
import eu.wdaqua.qanary.sparql.SparqlConnector;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * represents the general Qanary pipeline service interfaces
 *
 * @author AnBo
 */
@Controller
public class QanaryPipelineController {

    private static final Logger logger = LoggerFactory.getLogger(QanaryPipelineController.class);

    private final QanaryConfigurator qanaryConfigurator;

    private final SparqlConnector sparqlConnector;

    @Autowired
    public QanaryPipelineController(final QanaryConfigurator qanaryConfigurator,
            final SparqlConnector sparqlConnector) {
        this.qanaryConfigurator = qanaryConfigurator;
        this.sparqlConnector = sparqlConnector;
    }

    /**
     * register the URL of a service, optionally by calling via HTTP
     *
     * @param questionstring
     */
    @RequestMapping(value = "/component", headers = "Accept=text/plain", method = RequestMethod.POST, produces = {
            "text/plain;charset=UTF-8" })
    @ResponseBody
    public String registerComponent(@RequestParam(value = "question", required = true) final String questionstring) {
        // TODO: fetch the triples about the question from the triplestore
        
        // start the NO QA process

        // TODO: return the complete RDF object
        return null;
    }

    /**
     * wrapper for SPARQL endpoint
     *
     * @param sparqlquerystring
     * @return
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
            } else if (StringUtils.containsIgnoreCase(query, "INSERT") || StringUtils
                    .containsIgnoreCase(query, "DELETE")) {
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
