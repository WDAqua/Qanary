package eu.wdaqua.qanary.tgm;

import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

// this code is to parse the generated JSON template, and fetch the needed information out of it.
// Remember, there can be multiple resource, multiple classes, multiple properties generated.
public class PropertyRetrival {
	
	public static Property retrival(String s){
		Property p = new Property();
		//String input="";
		
		//String keyWords[] = {"Property","Resource","Literal","Class",""};
		/*List<String> property = new ArrayList<String>();
		List<String> resource = new ArrayList<String>();
		List<String> resourceL = new ArrayList<String>();
		List<String> classRdf = new ArrayList<String>();*/
		List<String> tempList = new ArrayList<String>();
		
		//Json parser to parse the needed information
		JSONParser parser = new JSONParser();
		try{
			
			JSONArray json = (JSONArray) parser.parse(s);
			
			//JSONArray characters = (JSONArray) json.get("slots");
			Iterator i = json.iterator();
			while (i.hasNext()) {
				JSONObject mainObject = (JSONObject) i.next();
				JSONArray slots = (JSONArray) mainObject.get("slots");
				
				Iterator q_itr = slots.iterator();
				
				String prevSub = "";
				while (q_itr.hasNext()) {
					
					
					JSONObject qstn = (JSONObject) q_itr.next();
								
					String sub = (String) qstn.get("s");
					String obj = (String) qstn.get("o");
					
					
					if(obj.contains("rdf:"))
					{	tempList.clear();
						String kWords[]=null;
						if(obj.contains("|"))
						{
							String t[] = obj.split("\\|");
							int cn = 0;
							kWords = new String[t.length];
							for(String tw:t)
							{
								kWords[cn++]= tw.substring(tw.indexOf(":")+1);
							}
						}
						else
						{
							kWords = new String[1];
							kWords [0] = obj.substring(obj.indexOf(":")+1);
						}
						for(String word:kWords)
						{
							tempList.add(word);
						}
						prevSub = sub;
					}
					else
					{
						if(prevSub.equalsIgnoreCase(sub))
						{
							//System.out.println("Inside==============================");
						//	System.out.println("TempList: "+tempList.toString());
							for(String temp: tempList)
							{
								switch(temp)
								{
									case "Property":
										if(!p.property.contains(obj))
										p.property.add(obj);
										break;
									case "Resource":
										if(!p.resource.contains(obj))
										p.resource.add(obj);
										break;
									case "Literal":
										if(!p.resourceL.contains(obj))
										p.resourceL.add(obj);
										break;
									case "Class":
										if(!p.classRdf.contains(obj))
										p.classRdf.add(obj);
										break;
								}
							}
						}
						prevSub = "";
						tempList.clear();
					}
					
				}
				
			}
			
			//System.out.println("List of Subjects: "+ids.toString());
		}catch(Exception e)
		{
			e.printStackTrace();
		}
		return p;
		/*System.out.println("\nThe rdf:Resource List : "+resource.toString());
		System.out.println("\nThe rdf:Property List : "+property.toString());
		System.out.println("\nThe rdf:Literal List : "+resourceL.toString());
		System.out.println("\nThe rdf:Class List : "+classRdf.toString());*/
	}
}

