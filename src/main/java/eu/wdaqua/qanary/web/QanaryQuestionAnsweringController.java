package eu.wdaqua.qanary.web;

import java.util.UUID;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.hp.hpl.jena.update.UpdateExecutionFactory;
import com.hp.hpl.jena.update.UpdateFactory;
import com.hp.hpl.jena.update.UpdateProcessor;
import com.hp.hpl.jena.update.UpdateRequest;

import eu.wdaqua.qanary.business.QanaryConfigurator;

/**
 * controller for processing
 * 
 * @author AnBo
 *
 */

@Controller
public class QanaryQuestionAnsweringController {
	private QanaryConfigurator qanaryConfigurator;
	/**
	 * start a configured process
	 * 
	 * @return
	 */
	@RequestMapping(value = "/questionanswering", method = RequestMethod.POST)
	@ResponseBody
	public String questionanswering() {

		UUID runID = UUID.randomUUID();
		
		
		//Create a new graph with this UUID
		
		//Create the name of a new named graph
		String namedGraph=runID.toString();
		//TODO add here blablabla/raw
		String uriQuestion="???";
		String q="";
		//TODO: add address of the triplestore
		String triplestore="????";
		
	    //Load the Open Annotation Ontology
		// TODO: store this locally for performance issues
	    q="LOAD <http://www.openannotation.org/spec/core/20130208/oa.owl> INTO GRAPH "+ namedGraph;
	    loadTripleStore(q, triplestore);
		
	    System.out.println("UPDATED");
	    
	    // TODO: load ontology into graph
		//Load the QAontology
		q="LOAD <http://localhost:"+qanaryConfigurator.getPort()+"/QAOntology_raw.ttl> INTO GRAPH "+ namedGraph;
		loadTripleStore(q, triplestore);
		
	    //Prepare the question, answer and dataset objects
	    q="PREFIX qa: <http://www.wdaqua.eu/qa#>"+
	      "INSERT DATA {GRAPH "+namedGraph+"{ <"+uriQuestion+"> a qa:Question}}";
	    loadTripleStore(q, triplestore);
	   
	    q="PREFIX qa: <http://www.wdaqua.eu/qa#>"+
  	      "INSERT DATA {GRAPH "+namedGraph+"{<http://localhost:"+qanaryConfigurator.getPort()+"/Answer> a qa:Answer}}";
	    loadTripleStore(q, triplestore);
  	    
	  	q="PREFIX qa: <http://www.wdaqua.eu/qa#>"+
  		  "INSERT DATA {GRAPH "+namedGraph+"{<http://localhost:"+qanaryConfigurator.getPort()+"/Dataset> a qa:Dataset}}";
	  	loadTripleStore(q, triplestore);
	  	
	  	//Make the first two annotations
	  	q="PREFIX oa: <http://www.w3.org/ns/openannotation/core/> "
	  	 +"PREFIX qa: <http://www.wdaqua.eu/qa#> "
	  	 +"INSERT DATA { "
	  	 +"GRAPH "+namedGraph+"{ "
	  	 +"<anno1> a  oa:AnnotationOfQuestion; "
		 +"   oa:hasTarget <"+uriQuestion+"> ;"
		 +"   oa:hasBody   <URIAnswer>   ."
		 +"<anno2> a  oa:AnnotationOfQuestion;"
		 +"   oa:hasTarget <"+uriQuestion+"> ;"
		 +"   oa:hasBody   <URIDataset> "
		 +"}}";  
		loadTripleStore(q, triplestore);
		
		

		

		// TODO: call all defined components

		return runID.toString();
	}

	/**
	 * executes a SPARQL INSERT into the triplestore
	 * @param query
	 * @return map
	 */
	public static void loadTripleStore(String sparqlQuery, String endpoint){
		UpdateRequest request = UpdateFactory.create(sparqlQuery) ;
		UpdateProcessor proc = UpdateExecutionFactory.createRemote(request, endpoint);	
		proc.execute();
	}
	
}
