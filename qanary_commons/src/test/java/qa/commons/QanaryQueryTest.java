package qa.commons;

import static java.util.Map.entry;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.jena.graph.impl.LiteralLabel;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.shared.impl.PrefixMappingImpl;
import org.apache.jena.sparql.algebra.Algebra;
import org.apache.jena.sparql.algebra.Op;
import org.apache.jena.sparql.algebra.OpAsQuery;
import org.apache.jena.sparql.core.TriplePath;
import org.apache.jena.sparql.syntax.ElementFilter;
import org.apache.jena.sparql.syntax.ElementPathBlock;
import org.apache.jena.sparql.syntax.ElementVisitorBase;
import org.apache.jena.sparql.syntax.ElementWalker;


public class QanaryQueryTest {
    
    // static default prefixMapping that may be used to instantiate these Tests
    public static Map<String, String> COMMON_PREFIXES = Map.ofEntries(
        entry("oa", "http://www.w3.org/ns/openannotation/core/"),
        entry("wdt", "http://www.wikidata.org/prop/direct/"),
        entry("rdfs", "http://www.w3.org/2000/01/rdf-schema#"),
        entry("bd", "http://www.bigdata.com/rdf#"),
        entry("wikibase", "http://wikiba.se/ontology#")
    );

	private final PrefixMappingImpl prefixMapping; 

    /**
     * @param prefixMapping use QanaryQueryTest.COMMON_PREFIXES or define your own prefix mappings. 
     */
	public QanaryQueryTest(Map<String, String> prefixMapping) {
        this.prefixMapping = new PrefixMappingImpl();
        this.prefixMapping.setNsPrefixes(prefixMapping);
	}

    //
    // TEST METHODS
    //

    /**
     * Assert that two supplied queries have equal string representations.
     *
     * @param queryString
     * @param expectedGraph
     */
	public void isSparqlQueryEqual(String expectedQueryString, String actualQueryString) {
		Query expectedParsedQuery = QueryFactory.create(expectedQueryString);
        Op expectedOp = Algebra.compile(expectedParsedQuery);
        Query expectedQueryAlgebra = OpAsQuery.asQuery(expectedOp);

		Query actualParsedQuery = QueryFactory.create(actualQueryString);
        Op actualOp = Algebra.compile(actualParsedQuery);
        Query actualQueryAlgebra = OpAsQuery.asQuery(actualOp);

		assertEquals(expectedQueryAlgebra.serialize(), actualQueryAlgebra.serialize(), 
                "The queries do not match");
	}

    /**
     * Assert that the supplied query contains a specific graph.
     *
     * @param queryString
     * @param expectedGraph
     */
    public void queryContainsGraph(String queryString, String expectedGraph) {
        ParsedQueryContent parsedQueryContent = this.parseQueryContent(queryString);
        assertTrue(parsedQueryContent.getGraphs().stream().anyMatch(
                    o -> expectedGraph.equals(o.toString())), 
                "The query does not contain the specified graph");
    }

    /**
     * Assert that the supplied query contains a FILTER statement with specific key and value.
     *
     * @param queryString
     * @param expectedKey a variable name
     * @param expectedValue a URI or literal as String
     */
    public void queryContainsFilterKeyValuePair(String queryString, String expectedKey, String expectedValue) {
        ParsedQueryContent parsedQueryContent = this.parseQueryContent(queryString);
        assertTrue(parsedQueryContent.getFilters().stream().anyMatch( 
                    o -> (o.toString().contains(expectedKey) 
                    && o.toString().contains(expectedValue))),
                "The query does not contain a FILTER statement with the specified key-value pair");
    }

    /**
     * Assert that the supplied query contains a specific triple.
     *
     * @param queryString
     * @param subject a URI string or variable name
     * @param predicate a URI string or variable name
     * @param object a URI string or variable name
     */
    public void queryContainsTriple(String queryString, String subject, String predicate, String object) {
        ParsedQueryContent parsedQueryContent = this.parseQueryContent(queryString);
        assertTrue(parsedQueryContent.getTriples().stream().anyMatch(
                    o -> (subject.equals(o.getSubject().toString(this.prefixMapping)))
                    && predicate.equals(o.getPredicate().toString(this.prefixMapping))
                    && object.equals(o.getObject().toString(this.prefixMapping))),
                "The query does not contain the specified triple");
    }

    /**
     * Assert that the supplied query contains a specific triple.
     *
     * @param queryString
     * @param subject a URI string or variable name
     * @param predicate a URI string or variable name 
     * @param object a literal with lexical form, datatype IRI and optionally a language tag
     */
    public void queryContainsTriple(String queryString, String subject, String predicate, LiteralLabel object) {
        ParsedQueryContent parsedQueryContent = this.parseQueryContent(queryString);
        assertTrue(parsedQueryContent.getTriples().stream().anyMatch(
                    o -> (subject.equals(o.getSubject().toString(this.prefixMapping)))
                    && predicate.equals(o.getPredicate().toString(this.prefixMapping))
                    && object.equals(o.getObject().getLiteral())),
                "The query does not contain the specified triple");
    }

    /**
     * Assert that the supplied query contains a specific triple.
     *
     * @param queryString
     * @param subject a literal with lexical form, datatype IRI and optionally a language tag
     * @param predicate a URI string or variable name 
     * @param object a URI string or variable name
     */
    public void queryContainsTriple(String queryString, LiteralLabel subject, String predicate, String object) {
        ParsedQueryContent parsedQueryContent = this.parseQueryContent(queryString);
        assertTrue(parsedQueryContent.getTriples().stream().anyMatch(
                    o -> (subject.equals(o.getSubject().getLiteral()))
                    && predicate.equals(o.getPredicate().toString(this.prefixMapping))
                    && object.equals(o.getObject().toString(this.prefixMapping))),
                "The query does not contain the specified triple");

    }

    //
    // CONTENT PARSING
    // 

    /** Parses the query to extract graphs, filters and triples.
     * This does not work for INSERT queries (all update queries)
     *
     * @param queryString
     */
    private ParsedQueryContent parseQueryContent(String queryString) {
        List<String> graphs = new LinkedList<>();
        List<String> filters = new LinkedList<>();
        List<TriplePath> triples = new LinkedList<>();

        Query query = QueryFactory.create(queryString);
        graphs  = query.getGraphURIs();
        ElementWalker.walk(query.getQueryPattern(), 
            new ElementVisitorBase() {
                // for visiting Filter statements
                public void visit(ElementFilter filter) {
                    filters.add(filter.getExpr().toString());
                }
                // for visiting a block of Triples
                public void visit(ElementPathBlock el) {
                    // iterate of all Triples in the block
                    Iterator<TriplePath> triplePaths = el.patternElts();
                    while (triplePaths.hasNext()) {
                        TriplePath triple = triplePaths.next();
                        triples.add(triple);
                    }
                } 
            }
        );
        return new ParsedQueryContent(graphs, filters, triples);
    }

    // holds parsed query elements 
	private class ParsedQueryContent {
        private List<String> graphs = new LinkedList<>();
        private List<String> filters = new LinkedList<>();
        private List<TriplePath> triples = new LinkedList<>();

        public ParsedQueryContent(List<String> graphs, List<String> filters, List<TriplePath> triples) {
            this.graphs = graphs;
            this.filters = filters;
            this.triples = triples;
        }

        public List<String> getGraphs() {
            return graphs;
        }

        public List<String> getFilters() {
            return filters;
        }

        public List<TriplePath> getTriples() {
            return triples;
        }
    }
}
