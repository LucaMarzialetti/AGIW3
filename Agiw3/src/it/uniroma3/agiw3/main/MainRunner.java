package it.uniroma3.agiw3.main;

import java.util.EnumSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexResponse;
import org.json.JSONArray;

import it.uniroma3.agiw1.keymanager.NoMoreKeysException;
import it.uniroma3.agiw3.bing.BingConnector;
import it.uniroma3.agiw3.bing.BingParserMaster;
import it.uniroma3.agiw3.bing.QueryExtractorSlave;
import it.uniroma3.agiw3.elastic.ElasticConnector;
import it.uniroma3.agiw3.mongo.MongoConnector;
import it.uniroma3.agiw3.ner.NERMaster;
import it.uniroma3.agiw3.scraper.DataExtractor;

public class MainRunner {
	/**targets**/
	public static final	String mongoHTMLCollection = "HTML_documents_3";
	public static final	String mongoQueryCollection = "index_documents_3";
	public static final String elasticIndexName = "avvocati_inglesi_index";
	public static final String elasticTypeName = "documents";
	public static final String htmlFolder = "queries";
	/**connectivity**/
	public static final String elasticHost = "localhost";
	public static final int elasticPort = 9300;
	public static final String mongoHost = "localhost";
	public static final String mongoClient = "local";
	public static final int mongoPort = 27017;
	/**ner**/
	public static final String nerMongoInput = mongoHTMLCollection;
	public static final String nerOutputFolder = "ner";
	/**flags**/
	public static EnumSet<Flags> flags;

	public static void main(String[] args){
		/*flags setup*/
		flags = EnumSet.of(
				//Flags.SearchEngine							//build search engine
				Flags.NamedEntityRecognition					//build ner
				//				Flags.Mongo_build_HTML			//build mongo html db
				//				Flags.Mongo_build_DOC			//build mongo query db
				//				Flags.Mongo_drop_HTML			//drop mongo html db
				//				Flags.Mongo_drop_DOC			//drop mongo query db
				//				Flags.FS_HTML					//write on fs html into directory >queries>
				//				Flags.Local_extract				//load the name files from local
				//				Flags.Remote_extract			//remote parse websource for name file
				//				Flags.Elastic_drop_index		//drop elastic search index
				//				Flags.Elastic_build_index		//build elastic search index

				);
		if(flags.contains(Flags.SearchEngine)){
			searchEngineDropCheck();							//drop every DB and index used (according to flags)
			searchEngineBuildCheck();							//build DB and index (according to flags)
		}
		if(flags.contains(Flags.NamedEntityRecognition)){
			namedEntityRecognition();
		}
	}

	private static void namedEntityRecognition() {
		System.out.println("[NamedEntityRecognitionMain]: starting services");
		MongoConnector mongoConnector = new MongoConnector(mongoHost, mongoPort, mongoClient);	//persist documents
		NERMaster ner = new NERMaster(nerMongoInput, nerOutputFolder, mongoConnector);
		ner.extract();
		System.out.println("[NamedEntityRecognitionMain]: stopping services");
		mongoConnector.closeClient();
	}

	/********** BUILDING FUNCTION **********/
	/** Make elastic index, persist document in mongo_db or on file system**/
	private static void searchEngineBuildCheck() {
		System.out.println("[SearchEngineBuildMain]: starting services");
		/*connectors setup*/
		BingConnector bingConnector = new BingConnector();										//bing search api
		DataExtractor dataExtractor = new DataExtractor();										//web data extraction
		MongoConnector mongoConnector = new MongoConnector(mongoHost, mongoPort, mongoClient);	//persist documents
		BingParserMaster bingParser = new BingParserMaster();
		ElasticConnector elasticConnector = new ElasticConnector(elasticHost, elasticPort);		//index documents
		/*iterating over names and making DBs and index */
		JSONArray sourceNames = dataExtractor.extract();
		Iterator<Object> iterator = sourceNames.iterator();
		ExecutorService es = Executors.newCachedThreadPool();
		Semaphore s = new Semaphore(0);
		SyncObj so = new SyncObj();
		int slaves = 0;
		while(iterator.hasNext()){
			String query = (String) iterator.next();
			try{
				JSONArray bingResponse = bingConnector.bingQueryWithKeyIterator(query);
				if(bingResponse != null && bingResponse.length()!=0){
					es.submit(new QueryExtractorSlave(query, bingResponse, bingParser, mongoConnector, elasticConnector, s, so));
					slaves++;
				}
			}
			catch(NoMoreKeysException e){
				break;
			}
		}
		/**signal on slaves that they can relese the semaphore**/
		so.submit();
		try {
			/**wait for all other slaves**/
			System.out.println("[SearchEngineBuildMain]: waiting for slaves "+slaves);
			s.acquire(slaves);
		} 
		catch (InterruptedException e) {
			System.out.println(e.getMessage());
		}
		/**shutdown the service**/
		System.out.println("[SearchEngineBuildMain]: stopping services");
		mongoConnector.closeClient();
		elasticConnector.closeClient();
		es.shutdown();
		/**For Testing**/
		/*show DBs contents*/
		//mongoConnector.showCollection(mongoHTMLCollection);
		//mongoConnector.showCollection(mongoQueryCollection);
	}

	/********** DROPPING FUNCTIONS **********/
	/**rollback main drop mongo collections AND/OR elasic indexes**/
	private static void searchEngineDropCheck() {
		if(flags.contains(Flags.Mongo_drop_DOC) || flags.contains(Flags.Mongo_drop_HTML)){
			System.out.println("==== DROPPING MONGO ====");
			dropMongo();
			System.out.println("==== MONGO DONE ====");
		}
		if(flags.contains(Flags.Elastic_drop_index)) {
			System.out.println("==== DROPPING ELASTIC ====");
			dropElastic();
			System.out.println("==== ELASTIC DONE ====");
		}
	}

	/**dropping mongo collections used**/
	private static void dropMongo() {
		/**Mongo Collections**/
		MongoConnector mongo = new MongoConnector(mongoHost,mongoPort,mongoClient);
		Set<String> collections = mongo.getDB().getCollectionNames();
		System.out.println("Existing collections:");
		for(String s: collections)
			System.out.println(s);
		System.out.println("Dropping:");
		for(String s: collections){
			if(flags.contains(Flags.Mongo_drop_HTML) && s.equals(mongoHTMLCollection)){
				mongo.dropCollection(mongoHTMLCollection);
				if(mongo.getDB().getCollection(mongoHTMLCollection)==null)
					System.out.println(mongoHTMLCollection+" has been dropped");
				else
					System.out.println(mongoHTMLCollection+" has not been dropped, something went wrong");

			}
			if(flags.contains(Flags.Mongo_drop_DOC) && s.equals(mongoQueryCollection)) {
				mongo.dropCollection(mongoQueryCollection);
				if(mongo.getDB().getCollection(mongoQueryCollection)==null)
					System.out.println(mongoQueryCollection+" has been dropped");
				else
					System.out.println(mongoQueryCollection+" has not been dropped, something went wrong");
			}
		}
	}

	/**dropping elastic indexes used**/
	private static void dropElastic(){
		/**Elastic Indexes**/
		ElasticConnector elastic = new ElasticConnector(elasticHost, elasticPort);
		LinkedList<String> indexes = elastic.getIndicesNames();
		System.out.println("Existing indexes:");
		for(String s: indexes)
			System.out.println(s);
		if(indexes.contains(elasticIndexName)){
			DeleteIndexResponse delete = elastic.getClient().admin().indices().delete(new DeleteIndexRequest(elasticIndexName)).actionGet();
			if(!delete.isAcknowledged())
				System.out.println("Index "+elasticIndexName+"wasn't dropped");
			else
				System.out.println("Index "+elasticIndexName+"has been dropped");
		}
	}
}