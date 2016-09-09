
package it.uniroma3.agiw3.bing;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.Semaphore;

import org.json.JSONArray;
import org.json.JSONObject;

import it.uniroma3.agiw3.elastic.ElasticConnector;
import it.uniroma3.agiw3.main.MainRunner;
import it.uniroma3.agiw3.main.Flags;
import it.uniroma3.agiw3.main.SyncObj;
import it.uniroma3.agiw3.mongo.MongoConnector;

public class QueryExtractorSlave implements Runnable {
	private BingParserMaster bingParserMaster;
	private MongoConnector mongoConnector; 		//persist documents
	private ElasticConnector elasticConnector;	//index documents
	private String query;
	private JSONArray bingResponse;
	private Semaphore semaphore;
	private SyncObj so;


	public QueryExtractorSlave(String query, JSONArray bingResponse, BingParserMaster bingParserMaster, MongoConnector mongoConnector, ElasticConnector elasticConnector, Semaphore semaphore, SyncObj so) {
		this.query=query;
		this.bingResponse=bingResponse;
		/*services*/
		this.bingParserMaster = bingParserMaster;
		this.mongoConnector = mongoConnector;
		this.elasticConnector = elasticConnector;
		this.semaphore=semaphore;
		this.so=so;
	}

	/**This runnable is used to process a single source(set of links) and procude an output validating the xpath on them**/
	@Override
	public void run() {
		//using try catch finally to avoid deadlock if jsoup parser crashes
		try{
			this.extract();
		}
		catch (Exception e) {
			System.out.println("[QUERY-EXTRACTOR-SLAVE]: UNCATCHED EXCEPTION, captched to avoid deadlock!");
		}
		finally{		
			this.so.check();
			this.semaphore.release();
		}
	}

	private void extract() {
		JSONObject customJSON = this.bingParserMaster.parse(this.bingResponse, query);
		if(MainRunner.flags.contains(Flags.Mongo_build_DOC))
			this.mongoConnector.bulkAdd(customJSON.getJSONArray("doc_collection"),MainRunner.mongoQueryCollection);
		if(MainRunner.flags.contains(Flags.Mongo_build_HTML))
			this.mongoConnector.bulkAdd(customJSON.getJSONArray("html_collection"), MainRunner.mongoHTMLCollection);
		if(MainRunner.flags.contains(Flags.FS_HTML))
			bulkSaveHTML_FILES(query,customJSON.getJSONArray("html_collection"));
		if(MainRunner.flags.contains(Flags.Elastic_build_index))
			this.elasticConnector.bulkPut(customJSON.getJSONArray("doc_collection"), MainRunner.elasticIndexName, MainRunner.elasticTypeName);
	}


	private void bulkSaveHTML_FILES(String folder_name, JSONArray docs){
		System.out.println("[FileSystem]: persisting "+docs.length()+" files in "+query);
		for(int i=0;i<docs.length(); i++)
			saveHTML_FILE(docs.getJSONObject(i));
		System.out.println("[FileSystem]: "+query+" done");
	}

	/**write the html file in the hierarchical folder order**/
	private void saveHTML_FILE(JSONObject customJSON) {
		String name;
		String folder_name = customJSON.getString("Query");
		if(customJSON.has("Title")){
			name=customJSON.getString("Title");
		}
		else {
			name=String.valueOf((folder_name+new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new Date())).hashCode());
		}
		String path = MainRunner.htmlFolder+"/"+folder_name;
		File dir = new File(path);
		if(!dir.exists() || !dir.isDirectory())
			dir.mkdirs();
		String file_name = path+"/"+this.sanitizeFilename(name)+".html";
		Writer writer=null;
		try {
			writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file_name), "utf-8"));
			writer.write(customJSON.getString("HTML"));
		}
		catch (IOException e) {
			System.out.println(e.getMessage());	
		}
		finally {
			try {
				writer.close();
			} 
			catch (Exception e) {
				System.out.println(e.getMessage());	
			}
		}
	}

	/**cast the string as NTFS valid filename**/
	private String sanitizeFilename(String inputName) {
		String safe_name = inputName.replaceAll("[^a-zA-Z0-9-_\\.~#\\[\\]@!$%&'\\(\\)\\+,;=]", "_");
		int padding = 0;
		if(safe_name.endsWith(".html"))
			padding = 5;
		return safe_name.substring(0, Math.min(safe_name.length(), 254-5)-padding);
	}
}