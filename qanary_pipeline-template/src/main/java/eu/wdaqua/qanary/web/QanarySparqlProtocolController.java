package eu.wdaqua.qanary.web;

import eu.wdaqua.qanary.commons.triplestoreconnectors.QanaryTripleStoreConnector;
import eu.wdaqua.qanary.commons.triplestoreconnectors.QanaryTripleStoreProxy;
import eu.wdaqua.qanary.exceptions.SparqlQueryFailed;
import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFormatter;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * a proxy for the triplestore configured for being used for storing the Qanary
 * data implementing the SPARQL protocol (a subset)
 * <p>
 * it hides the actual SPARQL endpoint from the components, s.t., no credentials
 * etc. need to be shared
 *
 * @author AnBo
 */
@CrossOrigin
@Controller
public class QanarySparqlProtocolController {
    public final static String SPARQL_ENDPOINT = "sparql";
    public final static String SPARQL_ENDPOINT_TESTER = "checktriplestoreconnection";
    private static final Logger logger = LoggerFactory.getLogger(QanarySparqlProtocolController.class);
    private QanaryTripleStoreConnector myQanaryTripleStoreConnector;

    public QanarySparqlProtocolController(QanaryTripleStoreProxy myQanaryTripleStoreConnector) throws URISyntaxException {
        this.myQanaryTripleStoreConnector = myQanaryTripleStoreConnector;
        checkTriplestoreConnection();
    }

    private QanaryTripleStoreConnector getQanaryTripleStoreConnector() {
        return this.myQanaryTripleStoreConnector;
    }

    @GetMapping(value = "/" + SPARQL_ENDPOINT_TESTER, consumes = {"*/*"})
    @ResponseBody
    public ResponseEntity<String> checkTriplestoreConnection() throws URISyntaxException {
        String sparqlQuery = "SELECT * { GRAPH ?g { ?s ?p ?o  } } LIMIT 1";
        logger.info("checkTriplestoreConnection: test with {}", sparqlQuery);
        ResultSet result = null;
        try {
            result = this.getQanaryTripleStoreConnector().select(sparqlQuery);
        } catch (SparqlQueryFailed e) {
            e.printStackTrace();
            String message = "SPARQL query for connection check failed";
            logger.error("{}: {}", message, e.toString());
            return ResponseEntity.internalServerError().body(message + ": " + e.toString());
        }

        if (result != null && result.hasNext()) {
            String message = "Triplestore is accessible and returns triples.";
            logger.info(message);
            return ResponseEntity.ok().body(message);
        } else {
            String message = "Triplestore is accessible, but NO triples are returned (correctly initialized?).";
            logger.warn(message);
            return ResponseEntity.ok().body(message);
        }
    }

    /**
     * returns a HTML page
     *
     * @return
     */
    @RequestMapping(value = "/" + SPARQL_ENDPOINT, produces = {MediaType.TEXT_HTML_VALUE}, consumes = {MediaType.ALL_VALUE})
    public String getSparqlAsHTML() {
        return "sparql";
    }


    @GetMapping(value = "/" + SPARQL_ENDPOINT, produces = "application/sparql-results+json", consumes = {"*/*"})
    @ResponseBody
    public ResponseEntity<String> getSparqlAsJSON( //
                                                   @RequestHeader(value = "accept", required = false) String acceptHeader, //
                                                   @RequestParam(required = true, value = "query") String sparqlQuery //
    ) throws SparqlQueryFailed, JSONException, URISyntaxException {
        logger.info("getSparqlAsJSON // accept-header: {}, SELECT query: {}", acceptHeader, sparqlQuery);

        Query query = null;
        try {
            query = QueryFactory.create(sparqlQuery);
        } catch (Exception e) {
            logger.error("SPARQL query could not be processed because of the error: {}\nfailed query:\n{}", e, sparqlQuery);
            e.printStackTrace();
            throw new SparqlQueryFailed(sparqlQuery, this.getQanaryTripleStoreConnector().getFullEndpointDescription(), e);
        }

        logger.info("ask:{}, select:{}, unknown:{}, query:{}", query.isAskType(), query.isSelectType(),
                query.isUnknownType(), sparqlQuery);

        // get result of SPARQL query from connected triplestore
        if (query.isAskType()) {
            Boolean result = this.getQanaryTripleStoreConnector().ask(sparqlQuery);
            // create HTTP response
            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/sparql-results+json");
            JSONObject json = new JSONObject();
            json.put("head", new JSONObject());
            json.put("boolean", result);
            logger.debug("JSON response for ASK query: {}", json.toString(0));
            ResponseEntity<String> responseEntity = ResponseEntity.ok().headers(headers).body(json.toString(2));
            return responseEntity;
        } else if (query.isSelectType()) {
            ResultSet result = this.getQanaryTripleStoreConnector().select(sparqlQuery);
            // transform the JSON string
            ByteArrayOutputStream myOutputStrean = new ByteArrayOutputStream();
            ResultSetFormatter.outputAsJSON(myOutputStrean, result);
            String resultAsJSON = myOutputStrean.toString();
            logger.debug("resultAsJSON:\n{}", resultAsJSON);

            // for safety only: check if the string is a convertible to JSON
            @SuppressWarnings("unused")
            JSONObject jsonObject;
            try {
                jsonObject = new JSONObject(resultAsJSON);
            } catch (JSONException e) {
                logger.error("Error while converting to JSONObject:\n{}", e.toString());
                throw new SparqlQueryFailed(sparqlQuery + "has lead to " + resultAsJSON,
                        myQanaryTripleStoreConnector.getFullEndpointDescription(), e);
            }

            // create HTTP response
            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/sparql-results+json");
            ResponseEntity<String> responseEntity = ResponseEntity.ok().headers(headers).body(resultAsJSON);
            logger.debug("responseEntity: {}", responseEntity.toString());

            if (jsonObject.has("results") && jsonObject.getJSONObject("results").has("bindings")) {
                logger.info("number of found results: {}", jsonObject.getJSONObject("results").getJSONArray("bindings").length());
            } else {
                logger.warn("number of found results: could not be identified: {}", jsonObject);
            }
            return responseEntity;
        } else {
            throw new SparqlQueryFailed(sparqlQuery, "internal Qanary triplestore proxy",
                    new Exception("given SPARQL query was neither ASK nor SELECT query"));
        }

    }

    @GetMapping(value = "/" + SPARQL_ENDPOINT, produces = {"application/sparql-results+xml",
            MediaType.APPLICATION_XML_VALUE}, consumes = {"*/*"})
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getSparqlAsXML( //
                                                               @RequestHeader(value = "accept", required = false) String acceptHeader, //
                                                               @RequestParam(required = false, defaultValue = "someValue", value = "query") String sparqlQuery //
    ) throws SparqlQueryFailed {
        logger.warn("getSparqlAsXML // accept-header: {}, SELECT query: {}", acceptHeader, sparqlQuery);
        throw new NotImplementedException();
    }

    /**
     * POST queries (UPDATE SPARQL queries)
     *
     * @param r
     * @param headers
     * @param acceptHeader
     * @param body
     * @return
     * @throws SparqlQueryFailed
     */
    @PostMapping(value = "/" + SPARQL_ENDPOINT, consumes = {MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            "application/sparql-update"}, produces = {"application/sparql-results+xml",
            MediaType.APPLICATION_XML_VALUE, "application/sparql-results+json",})
    public ResponseEntity<?> postSparql( //
                                         HttpServletRequest r, //
                                         @RequestHeader Map<String, String> headers, //
                                         @RequestHeader(value = "accept", required = false) String acceptHeader, //
                                         @RequestParam MultiValueMap<String, String> body) throws SparqlQueryFailed {
        logger.info("postSparql // accept-header: {}, body: {}", acceptHeader, body);
        logger.debug("requestURL: {}, contentLength: {}, encoding: {}, contentType: {}", r.getRequestURL().toString(),
                r.getContentLength(), r.getCharacterEncoding(), r.getContentType());
        headers.forEach((key, value) -> {
            logger.debug(String.format("Header '%s' = %s", key, value));
        });

        // get query from message body
        String updateQuery = this.getUpdateQueryFromBody(r, body);
        if (updateQuery != null) {
            logger.debug("recognized SPARQL UPDATE query: {}", updateQuery);
            logger.info("UPDATE query: {}", updateQuery);

            // execute SPARQL query
            try {
                this.getQanaryTripleStoreConnector().update(updateQuery);
            } catch (SparqlQueryFailed e) {
                logger.error("UPDATE query failed: {}", ExceptionUtils.getStackTrace(e));
                throw e;
            }

            // create response message, no header and no content
            HttpHeaders headersResponse = new HttpHeaders();
            ResponseEntity<?> responseEntity = ResponseEntity.noContent().headers(headersResponse).build();
            logger.warn("responseEntity: {}", responseEntity.toString());

            return responseEntity;
        } else {
            logger.info("redirect processing as it is no UPDATE query: {}", body);
            try {
                return this.getSparqlAsJSON(acceptHeader, body.get("query").get(0));
            } catch (Exception e) {
                try {
                    return this.getSparqlAsJSON(acceptHeader, this.getQueryFromBodyInputStream(r));
                } catch (Exception e2) {
                    throw new SparqlQueryFailed("no query extractable", "internal Qanary triplestore proxy", e);
                }
            }
        }
    }

    /**
     * catch all and log it
     *
     * @param r
     * @param headers
     * @return
     */
    @RequestMapping(value = "/" + SPARQL_ENDPOINT + "*", method = {RequestMethod.GET, RequestMethod.POST})
    public ResponseEntity<Object> getAnythingelse(HttpServletRequest r, @RequestHeader Map<String, String> headers) {
        logger.warn("getAnythingelse: {}: {}, {}", r.getMethod(), r.getRequestURL().toString(), r.getQueryString());
        logger.warn("number of headers: {}", headers.keySet().toArray().length);
        headers.forEach((key, value) -> {
            logger.warn(String.format("Header '%s' = %s", key, value));
        });

        for (Iterator<String> iterator = r.getAttributeNames().asIterator(); iterator.hasNext(); ) {
            String name = (String) iterator.next();
            logger.warn("{}: {}", name, r.getAttribute(name));
        }

        Map<String, Object> values = new HashMap<>();
        return new ResponseEntity<>(values, HttpStatus.NOT_FOUND);
    }

    /**
     * retrieve the body from the inputstream of an HttpServletRequest instance
     *
     * @param r
     * @return
     * @throws SparqlQueryFailed
     */
    private String getQueryFromBodyInputStream(HttpServletRequest r) throws SparqlQueryFailed {
        try {
            String extractedQuery = new String(r.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            logger.debug("extractedQuery: '{}'", extractedQuery);
            if (extractedQuery == null || extractedQuery.isEmpty()) {
                return null;
            } else {
                return extractedQuery;
            }
        } catch (IOException e) {
            String message = "no query could be extracted from the message body";
            logger.error("{}: {}", message, ExceptionUtils.getStackTrace(e));
            throw new SparqlQueryFailed(message, "internal Qanary triplestore proxy", e);
        }
    }

    /**
     * returns the an UPDATE query if present in the form-encoded body or the
     * inputstream of an HttpServletRequest instance
     * <p>
     * remark: returns null if the body contains a SELECT query
     *
     * @param r
     * @param body
     * @return
     * @throws SparqlQueryFailed
     */
    private String getUpdateQueryFromBody(HttpServletRequest r, MultiValueMap<String, String> body)
            throws SparqlQueryFailed {
        try {
            String updateQuery = body.get("update").get(0);
            if (updateQuery != null) {
                return updateQuery;
            } else {
                String selectQuery = body.get("query").get(0);
                if (selectQuery != null) {
                    // an UPDATE query is required, so return null if a SELECT query was found
                    return null;
                } else {
                    return this.getQueryFromBodyInputStream(r);
                }
            }
        } catch (Exception e) {
            return this.getQueryFromBodyInputStream(r);
        }
    }
}
