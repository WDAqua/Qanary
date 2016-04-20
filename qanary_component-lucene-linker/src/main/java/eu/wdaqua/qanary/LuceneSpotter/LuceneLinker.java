package eu.wdaqua.qanary.LuceneSpotter;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.ResultSet;
import org.apache.jena.update.UpdateExecutionFactory;
import org.apache.jena.update.UpdateFactory;
import org.apache.jena.update.UpdateProcessor;
import org.apache.jena.update.UpdateRequest;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.util.CharArraySet;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.highlight.InvalidTokenOffsetsException;
import org.apache.lucene.util.AttributeSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import eu.wdaqua.qanary.component.QanaryComponent;
import eu.wdaqua.qanary.component.QanaryMessage;

/**
 * represents a component that tries to map sequences of words in the question to the rdfs:labels given in the ontology
 * 
 * @author Dennis Diefenbach
 *
 */

@Component
public class LuceneLinker extends QanaryComponent {
	private static final Logger logger = LoggerFactory.getLogger(LuceneLinker.class);

	/**
	 * default processor of a QanaryMessage
	 */
	public QanaryMessage process(QanaryMessage myQanaryMessage) {
		long startTime = System.currentTimeMillis();
		org.apache.log4j.Logger.getRootLogger().setLevel(org.apache.log4j.Level.OFF);
		logger.info("Qanary Message: {}", myQanaryMessage);

		try {
			// STEP1: Retrieve the named graph and the endpoint
			String endpoint = myQanaryMessage.getEndpoint().toASCIIString();
			String namedGraph = myQanaryMessage.getInGraph().toASCIIString();
			logger.info("Endpoint: {}", endpoint);
			logger.info("InGraph: {}", namedGraph);
	
			// STEP2: Retrieve information that are needed for the computations
			//Retrive the uri where the question is exposed 
			String sparql = "PREFIX qa:<http://www.wdaqua.eu/qa#> "
							+"SELECT ?questionuri "
							+"FROM <"+namedGraph+"> "
							+"WHERE {?questionuri a qa:Question}";
			ResultSet result=selectTripleStore(sparql,endpoint);
			String uriQuestion=result.next().getResource("questionuri").toString();
			logger.info("Uri of the question: {}", uriQuestion);
			//Retrive the question itself
			RestTemplate restTemplate = new RestTemplate();
			//TODO: pay attention to "/raw" maybe change that
			ResponseEntity<String> responseEntity = restTemplate.getForEntity(uriQuestion+"/raw", String.class);
			String question=responseEntity.getBody();
			logger.info("Question: {}", question);

			// STEP3: Pass the information to the component and execute it
			//Tokenize the question using the lucene tokenizer
			ArrayList<String> stopWords = new ArrayList<String>(Arrays.asList(new String[]{"give", "me", "is", "are", "was", "were", "has", "have", "had", "do", "does", "did", "of", "the", "a", "in", "by", "to", "me", "all", "with", "from", "for", "and", "who"}));
			Analyzer analyzer = new EnglishAnalyzer(CharArraySet.EMPTY_SET);
			TokenStream tokenStream = analyzer.tokenStream(null, new StringReader(question));
			tokenStream.reset();
			List<AttributeSource> tokens = new ArrayList<AttributeSource>();
			while(tokenStream.incrementToken()){
				tokens.add(tokenStream.cloneAttributes());
				System.out.println(tokenStream.getAttribute(CharTermAttribute.class).toString());
			}
			tokenStream.close();
			tokenStream.end();
			analyzer.close();
			
			Index index = new Index("/Users/Dennis/Desktop/dump.ttl");
			//Try to match each sequence of words in a question to an uri searching at rdfs:label
			List<Annotation> annotations = new ArrayList<Annotation>();
			for (int i=0; i<tokens.size(); i++){
				boolean found=true;
				String search=tokens.get(i).getAttribute(CharTermAttribute.class).toString();
				int k=1;
				List<String> candidates = new ArrayList<String>();
				while (found==true){
					System.out.println("SEARCH"+search);
					List<String> tmp=index.query("\""+search+"\"");
					
					for (String s: tmp){
						System.out.println(s);
					}
					
					//If no matches found add the previews ones if they exist 
					if (tmp.size()==0){
						found=false;
						System.out.println("Here1 k:"+k);
						for (String uri:candidates){
							if (uri.equals("http://dbpedia.org/")==false){
								int begin=tokens.get(i).getAttribute(OffsetAttribute.class).startOffset();
								int end=tokens.get(i+k-2).getAttribute(OffsetAttribute.class).endOffset();
								System.out.println(question.substring(begin, end));
        						System.out.println(uri);
								annotations.add(new Annotation(begin,end,uri));
							}
						}
					} else{
						if (candidates.size()>0){
							System.out.println("Here3");
							for (String uri:candidates){
								if (uri.equals("http://dbpedia.org/")==false){
									int begin=tokens.get(i).getAttribute(OffsetAttribute.class).startOffset();
									int end=tokens.get(i+k-2).getAttribute(OffsetAttribute.class).endOffset();
									System.out.println(question.substring(begin, end));
	        						System.out.println(uri);
									annotations.add(new Annotation(begin,end,uri));
								}
							}
						}                    
						//candidates=new ArrayList<String>(Arrays.asList(tmp));
						candidates=tmp;
						if (i+k<tokens.size()){
							search += " "+tokens.get(i+k).getAttribute(CharTermAttribute.class).toString();
							k++;
						} else{
							found = false;
                           	for (String uri:candidates){
                           		if (uri.equals("http://dbpedia.org/")==false){
	                           		int begin=tokens.get(i).getAttribute(OffsetAttribute.class).startOffset();
	        						int end=tokens.get(i+k-1).getAttribute(OffsetAttribute.class).endOffset();
	        						System.out.println(question.substring(begin, end));
	        						System.out.println(uri);
	        						annotations.add(new Annotation(begin,end,uri));
                           		}
       						}
						}
					}
				}
			}

			Iterator<Annotation> it = annotations.iterator();
			while(it.hasNext()){
				Annotation a = it.next();
				System.out.println(question.substring(a.begin, a.end));
				if (stopWords.contains(question.substring(a.begin, a.end).toLowerCase())){
					it.remove();
				}
			}

			for (Annotation a : annotations){
				System.out.println(a.uri);
			}
			
			// STEP4: Push the result of the component to the triplestore
			logger.info("Apply vocabulary alignment on outgraph");
		
			for (Annotation a : annotations) {
				sparql = "prefix qa: <http://www.wdaqua.eu/qa#> "
						+ "prefix oa: <http://www.w3.org/ns/openannotation/core/> "
						+ "prefix xsd: <http://www.w3.org/2001/XMLSchema#> " 
						+ "INSERT { " 
						+ "GRAPH <" + namedGraph + "> { "
						+ "  ?a a qa:AnnotationOfNamedEntity . " 
						+ "  ?a oa:hasTarget [ "
						+ "           a    oa:SpecificResource; " 
						+ "           oa:hasSource    <" + uriQuestion + ">; "
						+ "           oa:hasSelector  [ " 
						+ "                    a oa:TextPositionSelector ; "
						+ "                    oa:start \""+ a.begin + "\"^^xsd:nonNegativeInteger ; "
						+ "                    oa:end  \"" + a.end + "\"^^xsd:nonNegativeInteger  " 
						+ "           ] "
						+ "  ] . " 
						+ "  ?a oa:hasBody <"+a.uri+"> ;"
						+ "     oa:annotatedBy <http://nlp.stanford.edu/software/CRF-NER.shtml> ; "
						+ "	    oa:AnnotatedAt ?time  " 
						+ "}} " 
						+ "WHERE { " 
						+ "BIND (IRI(str(RAND())) AS ?a) ."
						+ "BIND (now() as ?time) " + "}";
				loadTripleStore(sparql, endpoint);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvalidTokenOffsetsException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	
		long estimatedTime = System.currentTimeMillis() - startTime;
		logger.info("Time: {}", estimatedTime);

		return myQanaryMessage;
	}

	public void loadTripleStore(String sparqlQuery, String endpoint) {
		UpdateRequest request = UpdateFactory.create(sparqlQuery);
		UpdateProcessor proc = UpdateExecutionFactory.createRemote(request, endpoint);
		proc.execute();
	}

	public ResultSet selectTripleStore(String sparqlQuery, String endpoint) {
		Query query = QueryFactory.create(sparqlQuery);
		QueryExecution qExe = QueryExecutionFactory.sparqlService(endpoint, query);
		return qExe.execSelect();
	}

	

}
