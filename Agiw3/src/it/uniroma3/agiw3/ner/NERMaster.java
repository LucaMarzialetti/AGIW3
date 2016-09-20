package it.uniroma3.agiw3.ner;

import java.util.ArrayList;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.Semaphore;

import org.json.JSONObject;

import com.mongodb.BasicDBObject;
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
	private ArrayList<BlockingDeque<JSONObject>> deqs;

	public NERMaster(String inputCollection, String outputFolder, MongoConnector mongoConnector) {
		this.inputCollection = inputCollection;
		this.outputFolder = outputFolder;
		this.mongoConnector = mongoConnector;
		this.deqs = new ArrayList<BlockingDeque<JSONObject>>();
	}

	public void extract(){
		slaveSubmissions();
	}

	private void slaveSubmissions(){
		ExecutorService es = Executors.newCachedThreadPool();
		AlchemyKeyLoader akl = new AlchemyKeyLoader();
		Semaphore s = new Semaphore(0);
		SyncObj so = new SyncObj();
		int threads = 0;
		System.out.println("[NER_Master]: making slaves thread");
		while(akl.hasKey()){
			/*setup degli slaves 
			 * ognuno ha una chiave ed una coda di query 
			 * ogni query viene sottomessa serialmente ai server alchemy*/
			String key = akl.nextKey();
			LinkedBlockingDeque<JSONObject> que = new LinkedBlockingDeque<JSONObject>();
			this.deqs.add(que);
			es.submit(new NERSlave(this.outputFolder, key, threads, que, s, so));
			threads++;
		}	
		int dispatch = this.deqs.size();
		System.out.println("[NER_Master]: "+dispatch+" slaves thread made");
		DB db = this.mongoConnector.getDB();
		if(db.collectionExists(this.inputCollection)){
			DBCollection collection = db.getCollection(this.inputCollection);
			DBCursor cursor = collection.find().sort(new BasicDBObject("Query", 1));
			System.out.println("[NER_Master]: submitting jobs...");
			int slaves = 0;
			int offset = 161+176+152+167+153+135+672;
			int todo = 751;
			String currentName=null;
			int currentDispatch=-1;
			while(cursor.hasNext() && slaves < (offset+todo)){
				DBObject obj = cursor.next();
				if(slaves<offset){
					//skip
				}
				else{
					String url = (String) obj.get("Url");
					String name = (String) obj.get("Query");
					String doc = (String) obj.get("HTML");
					JSONObject o = new JSONObject();
					o.put("Url", url);
					o.put("Query", name);
					o.put("HTML", doc);
					try {
						if(name == null || !name.equals(currentName)){
							currentName=name;
							currentDispatch++;
							currentDispatch = currentDispatch%dispatch;
						}
						this.deqs.get(currentDispatch).putFirst(o);
					} 
					catch (InterruptedException e) {
						System.out.println(e.getMessage());
					}
				}
				slaves++;
			}
			/*poison pill*/
			for(BlockingDeque<JSONObject> lbq : this.deqs){
				try {
					lbq.putFirst(new JSONObject().put("POISON", "PILL"));
				} 
				catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			/**advice slaves**/
			so.submit();
			try {
				System.out.println("[NER_Master]: waiting for "+threads+" slaves threads");
				s.acquire(threads);
			} 
			catch (InterruptedException e) {
				System.out.println(e.getMessage());
			}
			System.out.println("[NER_Master]: done");
			es.shutdown();
		}
	}
}