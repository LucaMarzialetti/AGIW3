package it.uniroma3.agiw3.elastic;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Iterator;
import java.util.LinkedList;

import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.cluster.metadata.IndexMetaData;
import org.elasticsearch.common.collect.ImmutableOpenMap;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.json.JSONArray;
import org.json.JSONObject;

import com.carrotsearch.hppc.cursors.ObjectObjectCursor;

public class ElasticConnector {
	private String host;
	private int port;
	private Client client;

	public ElasticConnector(String host, int port) {
		this.host=host;
		this.port=port;
		this.client=prepareClient();
	}

	public Client getClient(){
		return this.client;
	}
	
	public void closeClient(){
		this.client.close();
	}
	
	/**Setting up Elastic Client**/
	private Client prepareClient(){
		Client client = null;
		try {
			client = TransportClient.builder().build()
					.addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName(this.host), this.port));
		}
		catch (UnknownHostException e) {
			System.out.println("[ELASTIC]: "+e.getMessage());
		}
		return client;
	}

	/**Bulk request - actually binded to the JSON fields, not generalized**/
	public void bulkPut(JSONArray ja, String indexName, String typeName){
		BulkRequestBuilder bulkRequest = this.client.prepareBulk();
		/****/
		for(int i = 0; i < ja.length(); i++) {
			
			XContentBuilder builder;
			JSONObject currentJo = ja.getJSONObject(i);
			try {
				builder = XContentFactory
						.jsonBuilder()
						.startObject()
						.field("id", currentJo.getString("ID"))
						.field("document", currentJo.getString("TextContent"))
						.field("url", currentJo.getString("Url"))
						.field("description", currentJo.getString("Description"))
						.field("title", currentJo.getString("Title"))
						.endObject();
			} 
			catch (IOException e) {
				continue;
			}
			IndexRequestBuilder request = this.client.prepareIndex(indexName, typeName)
					.setIndex(indexName)
					.setType(typeName)
					.setSource(builder);
			bulkRequest.add(request);
		}
		BulkResponse bulkResponse = bulkRequest.execute().actionGet();
		int items = bulkResponse.getItems().length;
		System.err.print("[ELASTIC]:indexed [" + items + "] items, with failures? [" + bulkResponse.hasFailures()  + "]");
	}

	/**Retrieve all indeces aliases**/
	public LinkedList<String> getIndicesNames() {
		LinkedList<String> indexes = new LinkedList<String>();
		ImmutableOpenMap<String, IndexMetaData> cont = this.client.admin().cluster()
				.prepareState().get().getState()
				.getMetaData().getIndices();
		Iterator<ObjectObjectCursor<String, IndexMetaData>> iterator = cont.iterator();
		while(iterator.hasNext())
			indexes.add(iterator.next().value.getIndex());
		return indexes;
	}
}

