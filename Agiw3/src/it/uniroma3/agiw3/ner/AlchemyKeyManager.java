package it.uniroma3.agiw3.ner;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;

import org.apache.commons.io.IOUtils;
import org.json.JSONObject;

import com.likethecolor.alchemy.api.Client;
import com.likethecolor.alchemy.api.call.AbstractCall;
import com.likethecolor.alchemy.api.call.RankedNamedEntitiesCall;
import com.likethecolor.alchemy.api.call.type.CallTypeUrl;
import com.likethecolor.alchemy.api.entity.NamedEntityAlchemyEntity;
import com.likethecolor.alchemy.api.entity.Response;
import com.likethecolor.alchemy.api.params.NamedEntityParams;

public class AlchemyKeyManager {
	private String current_key;
	private JSONObject keys;
	private String key_file = "alchemyKeys.txt";
	private LinkedList<String> owners;
	private Iterator<String> owner_iterator;

	public AlchemyKeyManager(){
		this.keys=loadFile();
		this.owners = AlchemyKeyOwners.names();
		this.owner_iterator = this.owners.iterator();
		try {
			this.current_key = this.nextKey();
		} 
		catch (AlchemyKeyException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private JSONObject loadFile() {
		File f = new File(this.key_file);
		JSONObject o = new JSONObject();
		try(FileInputStream inputStream = new FileInputStream(f)) {
			o = new JSONObject(IOUtils.toString(inputStream));
		} 
		catch (FileNotFoundException e) {
			System.out.println(e.getMessage());
		} 
		catch (IOException e) {
			System.out.println(e.getMessage());
		}
		return o;
	}

	public String nextKey() throws AlchemyKeyException{
		if(this.owner_iterator.hasNext()){
			String owner = owner_iterator.next();
			String key = this.keys.getJSONObject(owner).getJSONObject("credentials").getString("apikey");
			this.current_key = key;
			return this.current_key;
		}
		else
			throw new AlchemyKeyException();
	}

	public String getCurrent_key() {
		return current_key;
	}


	public Response<NamedEntityAlchemyEntity> queryToAlchemy(String url){
		Client client = new Client();
		client.setAPIKey(this.current_key);
		NamedEntityParams namedEntityParams = new NamedEntityParams();
//		namedEntityParams.setIsCoreference(true);
//		namedEntityParams.setIsDisambiguate(true);
//		namedEntityParams.setIsLinkedData(true);
//		namedEntityParams.setIsQuotations(true);
//		namedEntityParams.setIsSentiment(true);
//		namedEntityParams.setIsShowSourceText(true);
		AbstractCall<NamedEntityAlchemyEntity> rankedNamedEntitiesCall = new RankedNamedEntitiesCall(new CallTypeUrl(url), namedEntityParams);
		Response<NamedEntityAlchemyEntity> rankedNamedEntitiesResponse = null;
		try {
			rankedNamedEntitiesResponse = client.call(rankedNamedEntitiesCall);
		} catch (IOException e) {
			System.out.println("ALCHEMY QUERY ERROR: "+e.getMessage());
		}
		return rankedNamedEntitiesResponse;
	}
}
