package it.uniroma3.agiw3.mongo;

import org.json.JSONArray;
import org.json.JSONObject;

import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.WriteResult;
import com.mongodb.util.JSON;

public class MongoConnector {
	private DB db;
	private String host;
	private int port;
	private String client;
	private MongoClient mongoClient;

	public MongoConnector(String host, int port, String client) {
		this.host=host;
		this.port=port;
		this.client=client;
		this.mongoClient = new MongoClient(this.host, this.port);
		this.db =  mongoClient.getDB(this.client);
	}

	/*return current db*/
	public DB getDB(){
		return this.db;
	}
	
	public void closeClient(){
		this.mongoClient.close();
	}

	/**Iterate over a JSONArray and add all objects**/
	public int bulkAdd(JSONArray ja, String collectionName){
		int count=0;
		System.out.println("[Mongo]: persisting to "+collectionName+" "+ja.length()+" elements...");
		for(int i=0; i<ja.length(); i++){
			JSONObject jo = ja.getJSONObject(i);
			count+=addObjects(jo, collectionName);
		}
		System.out.println("[Mongo]: persisted "+count+" elements in "+collectionName);
		return count;
	}

	/**Add JSONObject to the collection, return 1 if write was aknowledged 0 otherwise**/
	public int addObjects(JSONObject jo, String collectionName){
		collectionExistanceCheck(collectionName);
		DBCollection collection = this.db.getCollection(collectionName);
		// convert JSON to DBObject directly
		DBObject dbObject = (DBObject) JSON.parse(jo.toString());
		WriteResult s = collection.insert(dbObject);
		return (s.wasAcknowledged())? 1: 0;
	}

	/**Check if the collection doesn't exist, otherwise it create it**/ 
	public void collectionExistanceCheck(String collectionName){
		// To directly connect to a single MongoDB server (note that this will not auto-discover the primary even
		// if it's a member of a replica set:
		if(!this.db.collectionExists(collectionName)){
			this.db.createCollection(collectionName,null);
			System.out.println("[Mongo]: created collection: "+collectionName);
		}
	}

	/**Return the collection members as JSONArray**/
	public JSONArray getCollection(String collectionName){
		JSONArray array = new JSONArray();
		if(!this.db.collectionExists(collectionName)){
			this.db.createCollection(collectionName,null);
			System.out.println("[Mongo]: created collection"+collectionName);
		}
		DBCollection collection = this.db.getCollection(collectionName);
		DBCursor cursor = collection.find();
		System.out.println("[Mongo]: collection "+collectionName+" items");
		JSONObject jo=null;
		while(cursor.hasNext()) {
			DBObject dbo = cursor.next();
			jo = new JSONObject(dbo.toString());
			array.put(jo);
		}
		return array;
	}

	/**Info about collection - actually binded to the JSON fields, not generalized**/
	public void showCollection(String collectionName){
		if(!this.db.collectionExists(collectionName)){
			this.db.createCollection(collectionName,null);
			System.out.println("[Mongo]: created collection"+collectionName);
		}
		DBCollection collection = this.db.getCollection(collectionName);
		DBCursor cursor = collection.find();
		System.out.println("[Mongo]: collection "+collectionName+" items");
		while(cursor.hasNext()) {
			DBObject dbo = cursor.next();
			JSONObject jo = new JSONObject(dbo.toString());
			//System.out.println(jo.get("ID")+" "+jo.getString("Url")+" "+jo.getString("Title"));
			System.out.println(jo.toString());
		}
		System.out.println("[MONGO]: items in the collection "+collection.count());
	}
	

	/**drop mongo collection taken by name**/
	public void dropCollection(String collectionName){
		boolean hasCollection = this.getDB().collectionExists(collectionName);
		if(hasCollection){
			this.getDB().getCollection(collectionName).drop();
			boolean hasDropped = !this.getDB().collectionExists(collectionName);
			if(hasCollection && hasDropped)
				System.out.println("Collection "+collectionName+" has been dropped");
			else
				System.out.println("Collection "+collectionName+" wasn't dropped");

		}
	}
}