package eu.wdaqua.qanary.tgmm;

import eu.wdaqua.qanary.component.QanaryComponent;
import eu.wdaqua.qanary.component.QanaryMessage;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.ResultSet;
import org.apache.jena.update.UpdateExecutionFactory;
import org.apache.jena.update.UpdateFactory;
import org.apache.jena.update.UpdateProcessor;
import org.apache.jena.update.UpdateRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.util.UUID;

import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
/**
 * represents a wrapper of the template generator of OKBQA
 *
 * @author 
 */

@Component
public class Tgmm extends QanaryComponent {
    private static final Logger logger = LoggerFactory.getLogger(Tgmm.class);

    /**
     * A method to fetch functionality using a CURL command
     */
    public static String runCurlPOSTWithParam(String weburl,String data,String contentType) throws Exception
	{
		
        String urlString = "";
        try {
        	
              	
        	System.out.println("Kuldeep");
        	URL url = new URL(weburl+"data="+URLEncoder.encode(data,"UTF-8"));
        	//URL url = new URL(weburl+"?data="+URLEncoder.encode(data,"UTF-8"));
    		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
    		
    		/*connection.setRequestMethod("GET");
    		
    		connection.setDoOutput(true);
    		//connection.setRequestProperty("data", data);
    		connection.setRequestProperty("Content-Type", contentType);*/
    				
    		/*DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
    		wr.writeBytes(data);
    		wr.flush();
    		wr.close();*/
    		
    		
    
    		BufferedReader in = new BufferedReader(
    		        new InputStreamReader(connection.getInputStream()));
    		String inputLine;
    		StringBuffer response = new StringBuffer();

    		while ((inputLine = in.readLine()) != null) {
    			response.append(inputLine);
    		}
    		in.close();
    		urlString = response.toString();
    		
    		System.out.println("Curl Response: \n"+urlString);
            logger.info("Response  service {}", urlString);
        } catch (Exception e) {
        	e.printStackTrace();
        }
        return (urlString);

	}
    
    public QanaryMessage process(QanaryMessage myQanaryMessage) {
    	System.out.println("Let the pipeline begin");
        long startTime = System.currentTimeMillis();
        org.apache.log4j.Logger.getRootLogger().setLevel(org.apache.log4j.Level.OFF);
        logger.info("Qanary Message: {}", myQanaryMessage);

        // STEP1: Retrieve the named graph and the endpoint
        String endpoint = myQanaryMessage.getEndpoint().toASCIIString();
        String namedGraph = myQanaryMessage.getInGraph().toASCIIString();
        System.out.println("Graph is" +namedGraph);
        logger.info("Endpoint: {}", endpoint);
        logger.info("InGraph: {}", namedGraph);

        // STEP2: Retrieve information that are needed for the computations, in our case its Question and Langauge of the Question
        //Retrive the uri where the question is exposed
        String sparql = "PREFIX qa:<http://www.wdaqua.eu/qa#> "
                + "SELECT ?questionuri "
                + "FROM <" + namedGraph + "> "
                + "WHERE {?questionuri a qa:Question}";
        
        ResultSet result = selectTripleStore(sparql, endpoint);
        String uriQuestion = result.next().getResource("questionuri").toString();
        logger.info("Uri of the question: {}", uriQuestion);
        //Retrive the question itself
        RestTemplate restTemplate = new RestTemplate();
        //TODO: pay attention to "/raw" maybe change that
        ResponseEntity<String> responseEntity = restTemplate.getForEntity(uriQuestion + "/raw", String.class);
        String question = responseEntity.getBody();
        logger.info("Question: {}", question);
        
        //Fetch the related information of the Question from the triplestore.
        
        String allTypes[]={"AnnotationOfSpotInstance","AnnotationOfSpotProperty","AnnotationOfSpotLiteral","AnnotationOfSpotClass"};
        String allSpecific[]={"SpecificResource","SpecificProperty","SpecificLiteral","SpecificClass"};
        
        Map<String,List<String>> retrivedWords = new HashMap<String,List<String>>();
         
        //public Map<String,List<String>>
        //rdf:Resource|rdfs:Literal rdf:Property rdf:Class
        for(int i=0;i<allTypes.length;i++)
        {
        	String word= "";
	        sparql = "PREFIX qa: <http://www.wdaqua.eu/qa#> "
	                + "PREFIX oa: <http://www.w3.org/ns/openannotation/core/> "
	                + "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> "
	                + "SELECT ?start ?end "
	                + "FROM <" + namedGraph + "> "
	                + "WHERE { "
	                + "?a a qa:"+allTypes[i]+" . "
	                + "?a oa:hasTarget [ "
	                + "		a    oa:"+allSpecific[i]+"; "
	                + "		oa:hasSource    ?q; "
	                + "		oa:hasSelector  [ "
	                + "			a oa:TextPositionSelector ; "
	                + "			oa:start ?start ; "
	                + "			oa:end  ?end "
	                + "		] "
	                + "] ; "
	                + "oa:annotatedBy ?annotator "
	                + "} "
	                + "ORDER BY ?start ";
	        ResultSet r = selectTripleStore(sparql, endpoint);
	        //ArrayList<Spot> spots = new ArrayList<Spot>();
	        System.out.println("The list of "+allTypes[i]);
	        while (r.hasNext()) {
	            QuerySolution s = r.next();
	            word = question.substring(s.getLiteral("start").getInt(),s.getLiteral("end").getInt());	            
	            System.out.println(word);
	            //wordsRetrived.add(question.substring(s.getLiteral("start").getInt(),s.getLiteral("end").getInt()));
	            //logger.info("Spot: {}-{}", spot.begin, spot.end);
	            //spots.add(spot);
	            List<String> wordsRetrived;//new List<String>();
		        if(retrivedWords.size()<1 || !retrivedWords.containsKey(word) )
		        {
		        	wordsRetrived = new ArrayList<String>();
		        }
		        else if(retrivedWords.containsKey(word))
		        {
		        	wordsRetrived = retrivedWords.remove(word);
		        }
		        else
		        {
		        	wordsRetrived = new ArrayList<String>();
		        }
		        
	        	if(allTypes[i].equals("AnnotationOfSpotInstance"))
	        	{
	        		wordsRetrived.add("rdf:Resource");
	        	}
	        	else if(allTypes[i].equals("AnnotationOfSpotProperty"))
	        	{
	        		wordsRetrived.add("rdf:Property");
	        	}
	        	else if(allTypes[i].equals("AnnotationOfSpotLiteral"))
	        	{
	        		wordsRetrived.add("rdfs:Literal");
	        	}
	        	else if(allTypes[i].equals("AnnotationOfSpotClass"))
	        	{
	        		wordsRetrived.add("rdf:Class");
	        	}
	        	
		        System.out.println("=============================================================================");
		        
		        
		        retrivedWords.put(word,wordsRetrived);
		        
		        System.out.println(wordsRetrived.toString());	        
	        }
	        
	        
        }
        
        String inputData="{  "+
   "\"query\":\"SELECT ?v4 WHERE { ?v4 ?v2 ?v6 ; ?v7 ?v3 . } \","+
   "\"question\":\""+question+"\","+
   "\"score\":\"1.0\","+
   "\"slots\":["+  
      "{"+  
         "\"o\":\"<http://lodqa.org/vocabulary/sort_of>\","+
         "\"p\":\"is\","+
         "\"s\":\"v7\""+
      "},";
        int varCount = 3;
        for(String tempWord:retrivedWords.keySet())
        {
        	String classWord = "";
        	List<String> tempListOfClass = retrivedWords.get(tempWord);
        	for(String word: tempListOfClass)
        	{
        		classWord += word+"|";
        	}
        	classWord = classWord.substring(0,classWord.length()-1);
        	
        	inputData +="{"+  
                "\"o\":\""+classWord+"\","+
                "\"p\":\"is\","+
                "\"s\":\"v"+varCount+"\""+
             "},"+
        	"{"+  
                "\"o\":\""+tempWord+"\","+
                "\"p\":\"verbalization\","+
                "\"s\":\"v"+varCount+"\""+
             "},";
        	varCount++;
        }
        inputData = inputData.substring(0,inputData.length()-1)+"] }";
        
        
        
        System.out.println(inputData);
        
       
        
        String url = "http://121.254.173.77:2357/agdistis/run?";
		String data = inputData;
		String contentType = "application/json";
		 
		//http://repository.okbqa.org/components/7
		//Sample input	
		/* 
		 * {
		  	"string": "Which river flows through Seoul?",
		  	"language": "en"
		   } http://ws.okbqa.org:2360/ko/tgm/stub/service
		*/
		
		//data = "{  \"string\":\""+question+"\",\"language\":\""+language1+"\"}";//"{  \"string\": \"Which river flows through Seoul?\",  \"language\": \"en\"}";
		System.out.println("\ndata :" +data);
		System.out.println("\nComponent : 7");
		String output1="";
		try
		{
		output1= Tgmm.runCurlPOSTWithParam(url, data, contentType);
		}catch(Exception e){}
		System.out.println("The output template is:" +output1);
		
		JSONParser parser = new JSONParser();
//		List<String> entities = new ArrayList<String>();
//		List<String> classes = new ArrayList<String>();
//		List<String> literals = new ArrayList<String>();
//		List<String> properties = new ArrayList<String>();
		Map<String,Map<String,Double>> allUrls = new HashMap<String,Map<String,Double>>();
		
		try {

		
			JSONObject json = (JSONObject) parser.parse(output1);

			JSONArray characters = (JSONArray) json.get("ned");
			Iterator i = characters.iterator();

			while (i.hasNext()) {
				JSONObject mainObject = (JSONObject) i.next();
				//JSONArray nested = (JSONArray) mainObject.get("question");
				String keyWords[] = {"entities","classes","literals","properties"};
				
				for(int j=0;j<keyWords.length;j++)
				{
					System.out.println("The List of URLs of "+keyWords[j]);
					JSONArray types = (JSONArray) mainObject.get(keyWords[j]);
					Iterator iTypes = types.iterator();
					Map<String,Double> urlsAndScore = new HashMap<String,Double>();
					
					while (iTypes.hasNext()) {
						
						JSONObject tempObject = (JSONObject) iTypes.next();
						String urls = (String) tempObject.get("value");
						double score = (double) tempObject.get("score");
						urlsAndScore.put(urls, score);	
						System.out.println("url : "+urls+" , Score : "+score);
						
					}
					
					allUrls.put(keyWords[j],urlsAndScore );
					
				}

				
			}
			
			
		} catch (Exception e) {
			e.printStackTrace();
		}
        
		
		
		for (String link : allUrls.keySet()) {
			
			
			String linkWord = "";
			String linkData = "";
			if(link.equals("entities"))
        	{
				linkWord = "AnnotationOfSpotInstance";
			    linkData = "SpecificResource";
        	}
        	else if(link.equals("properties"))
        	{
        		linkWord = "AnnotationOfSpotProperty";
        		 linkData = "SpecificProperty";
        	}
        	else if(link.equals("literals"))
        	{
        		linkWord = "AnnotationOfSpotLiteral";
        	 linkData = "SpecificLiteral";
        	}
        	else if(link.equals("classes"))
        	{
        		linkWord = "AnnotationOfSpotClass";
        		 linkData = "SpecificClass";
        	}
			for(String urls:allUrls.get(link).keySet())
			{
				
				if(linkWord.equals("AnnotationOfSpotLiteral"))
				{
					System.out.println("Inside : Literal: "+allUrls.get(link).get(urls));
					sparql = "prefix qa: <http://www.wdaqua.eu/qa#> "
		                    + "prefix oa: <http://www.w3.org/ns/openannotation/core/> "
		                    + "prefix xsd: <http://www.w3.org/2001/XMLSchema#> "
		                    + "INSERT { "
		                    + "GRAPH <" + namedGraph + "> { "
		                    + "  ?a a qa:"+linkWord+" . "
		                    + "  ?a oa:hasTarget [ "
		                    + "           a    oa:"+linkData+"; "
		                    + "           oa:hasSource    <" + uriQuestion + ">; "
		                    + "           oa:hasSelector  [ "
			                + "                     a oa:TextPositionSelector ; "
			                + "                    oa:start \"" + question.indexOf(urls) + "\"^^xsd:nonNegativeInteger ; "
			                + "                    oa:end  \"" + question.indexOf(urls)+urls.length() + "\"^^xsd:nonNegativeInteger  "
			                + "           ] "
		                    + "  ] . "
		                    + "  ?a oa:hasBody <" + urls + "> ;"
		                    + "     oa:annotatedBy <http://agdistis.aksw.org> ; " 
		                    + "     oa:score \""+allUrls.get(link).get(urls)+"\"ˆˆxsd:decimal ."
		                    + "	    oa:AnnotatedAt ?time  "
		                    + "}} "
		                    + "WHERE { "
		                    + "BIND (IRI(str(RAND())) AS ?a) ."
		                    + "BIND (now() as ?time) "
		                    + "}";
				}
				else{
					System.out.println("Inside : Literal: "+allUrls.get(link).get(urls));
			            sparql = "prefix qa: <http://www.wdaqua.eu/qa#> "
			                    + "prefix oa: <http://www.w3.org/ns/openannotation/core/> "
			                    + "prefix xsd: <http://www.w3.org/2001/XMLSchema#> "
			                    + "INSERT { "
			                    + "GRAPH <" + namedGraph + "> { "
			                    + "  ?a a qa:"+linkWord+" . "
			                    + "  ?a oa:hasTarget [ "
			                    + "           a    oa:"+linkData+"; "
			                    + "           oa:hasSource    <" + uriQuestion + ">; "
			                    + "  ] . "
			                    + "  ?a oa:hasBody <" + urls + "> ;"
			                    + "     oa:annotatedBy <http://agdistis.aksw.org> ; "
			                    + "	    oa:AnnotatedAt ?time  "
			                    + "     oa:score \""+allUrls.get(link).get(urls)+"\"ˆˆxsd:decimal ."
			                    + "}} "
			                    + "WHERE { "
			                    + "BIND (IRI(str(RAND())) AS ?a) ."
			                    + "BIND (now() as ?time) "
			                    + "}";
				}
            //logger.info("Sparql query {}", sparql);
            //myQanaryUtils.updateTripleStore(sparql, myQanaryQuestion.getEndpoint().toString());
				logger.info("Sparql query {}", sparql);
                loadTripleStore(sparql, endpoint);
			}
	    }
		
		
		
		
		
        long estimatedTime = System.currentTimeMillis() - startTime;
        logger.info("Time: {}", estimatedTime);

        return myQanaryMessage;
    }

    private void loadTripleStore(String sparqlQuery, String endpoint) {
        UpdateRequest request = UpdateFactory.create(sparqlQuery);
        UpdateProcessor proc = UpdateExecutionFactory.createRemote(request, endpoint);
        proc.execute();
    }

    private ResultSet selectTripleStore(String sparqlQuery, String endpoint) {
        Query query = QueryFactory.create(sparqlQuery);
        QueryExecution qExe = QueryExecutionFactory.sparqlService(endpoint, query);
        return qExe.execSelect();
    }

    private class Selection {
        public int begin;
        public int end;
    }

}


