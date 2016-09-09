package it.uniroma3.agiw3.bing;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;

import it.uniroma3.agiw3.main.SyncObj;

public class BingParserMaster {

	public BingParserMaster() {
	}

	/*Get the HTML target of URL and embed it in the JSONObject in TextContent field
	 * If fail try once more*/
	public JSONObject parse(JSONArray bingResponse, String query){
		Semaphore semaphore = new Semaphore(0);
		SyncObj so = new SyncObj();
		ExecutorService es = Executors.newCachedThreadPool();
		LinkedBlockingQueue<JSONObject> que_ans = new LinkedBlockingQueue<JSONObject>();
		LinkedBlockingQueue<JSONObject> failed = new LinkedBlockingQueue<JSONObject>();
		JSONObject return_collections = new JSONObject();
		JSONArray doc_collection = new JSONArray();
		JSONArray html_collection = new JSONArray();
		int slaves = 0;
		int skipped = 0;
		int faulty=0;
		int length = bingResponse.length();
		/*submit tasks*/
		/**first try**/
		System.out.println("[BingParserMaster] "+query+": parsing Bing JSON...("+length+")");
		for(int i=0; i<length; i++){
			JSONObject bingJSON = bingResponse.getJSONObject(i);
			if(bingJSON!=null && bingJSON.has("Url") && !bingJSON.getString("Url").endsWith(".pdf")){
				es.submit(new BingSlave(bingJSON, que_ans, failed, semaphore, so));
				slaves++;
			}
			else 
				skipped++;
		}
		/**signal on slaves that they can release the semaphore**/
		so.submit();
		try {
			/**wait for slaves**/
			System.out.println("[BingParserMaster] "+query+": waiting for "+(slaves)+" slaves");
			semaphore.acquire(slaves);
		} 
		catch (InterruptedException e) {
			System.out.println(e.getMessage());
		}
		/**retry faulty url**/
		int first_fail = failed.size();
		int good = que_ans.size();
		if(!failed.isEmpty() && first_fail>0){
			/**reinizialize the lists**/
			System.out.println("[BingParserMaster] "+query+": retrying "+first_fail+" faulty Url");
			LinkedList<JSONObject> toCheck = new LinkedList<JSONObject>();
			toCheck.addAll(failed);
			failed = new LinkedBlockingQueue<JSONObject>();
			semaphore = new Semaphore(0);
			so = new SyncObj();
			slaves=0;
			for(int i=0; i<toCheck.size(); i++){
				JSONObject bingJSON = toCheck.get(i);
				es.submit(new BingSlave(bingJSON, que_ans, failed, semaphore, so));
				slaves++;
			}
			so.submit();
			try {
				/**wait for slaves**/
				semaphore.acquire(slaves);
			} 
			catch (InterruptedException e) {
				System.out.println(e.getMessage());
			}
			System.out.println("[BingParserMaster] "+query+": recovered "+(que_ans.size()-good)+" Urls");
		}
		/**killing service**/
		es.shutdown();
		/**compose result**/
		faulty=failed.size();
		Iterator<JSONObject> iter = que_ans.iterator();
		while(iter.hasNext()){
			JSONObject ans = iter.next();
			formatObjects(ans, doc_collection, html_collection, query);
		}
		System.out.println("[BingParserMaster] "+query+": skipped(pdf): "+skipped+"\\"+length+", faulty: "+faulty+"\\"+length+", good: "+return_collections.length()+"\\"+length);
		return_collections.put("html_collection", html_collection);
		return_collections.put("doc_collection", doc_collection);
		return return_collections;
	}

	private void formatObjects(JSONObject ans, JSONArray doc_collection, JSONArray html_collection, String query) {
		doc_collection.put(formatText_DB(ans.getJSONObject("bingJSON"), (Document)ans.get("doc"), query));
		html_collection.put(formatHTML_DB(ans.getJSONObject("bingJSON"), (Document)ans.get("doc"), query));
	}
	
	/**format for collection of HTML documents.
	 * JSON content: ID, HTML**/
	private JSONObject formatHTML_DB(JSONObject bingJSON, Document d, String query) {
		JSONObject newJSON = new JSONObject();
		newJSON.put("ID", bingJSON.getString("ID"));
		newJSON.put("HTML", d.toString());
		newJSON.put("Title", bingJSON.getString("Title"));
		newJSON.put("Url", bingJSON.getString("Url"));
		newJSON.put("Query", query);
		return newJSON;
	}

	/**format for collection of queries.
	 * JSON content: old bing fields + textContent=HTML.text**/
	private JSONObject formatText_DB(JSONObject bingJSON, Document d, String query){
		JSONObject newJSON = new JSONObject(bingJSON.toString());
		if(newJSON.has("__metadata"))
			newJSON.remove("__metadata");
		newJSON.put("TextContent", d.text());
		newJSON.put("Query", query);
		return newJSON;
	}
}