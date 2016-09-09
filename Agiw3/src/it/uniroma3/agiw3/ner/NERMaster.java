package it.uniroma3.agiw3.ner;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;

import it.uniroma3.agiw3.main.SyncObj;
import it.uniroma3.agiw3.mongo.MongoConnector;

public class NERMaster {
	private String inputCollection;
	private String outputFolder;
	private MongoConnector mongoConnector;

	public NERMaster(String inputCollection, String outputFolder, MongoConnector mongoConnector) {
		this.inputCollection = inputCollection;
		this.outputFolder = outputFolder;
		this.mongoConnector = mongoConnector;
	}

	public void extract(){
		slaveSubmissions();
	}

	private void slaveSubmissions(){
		ExecutorService es = Executors.newCachedThreadPool();
		AlchemyKeyManager akm = new AlchemyKeyManager();
		DB db = this.mongoConnector.getDB();
		if(db.collectionExists(inputCollection)){
			Semaphore s = new Semaphore(0);
			SyncObj so = new SyncObj();
			int slaves = 0;
			DBCollection collection = db.getCollection(inputCollection);
			DBCursor cursor = collection.find();
			System.out.println("[NER_Master]: submitting slaves...");
			while(cursor.hasNext() && slaves<11){
				DBObject obj = cursor.next();
				String url = (String) obj.get("Url");
				String name = (String) obj.get("Query");
				String doc = (String) obj.get("HTML");
				es.submit(new NERSlave(this.outputFolder, akm, url, name, doc, s, so));
				slaves++;
			}
			/**advice slaves**/
			so.submit();
			try {
				System.out.println("[NER_Master]: waiting for "+slaves+" slaves");
				s.acquire(slaves);
			} 
			catch (InterruptedException e) {
				System.out.println(e.getMessage());
			}
			System.out.println("[NER_Master]: done");
			es.shutdown();
		}
	}
}