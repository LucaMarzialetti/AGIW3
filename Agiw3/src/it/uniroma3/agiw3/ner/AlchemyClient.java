package it.uniroma3.agiw3.ner;

import java.io.IOException;

import com.likethecolor.alchemy.api.Client;
import com.likethecolor.alchemy.api.call.AbstractCall;
import com.likethecolor.alchemy.api.call.RankedNamedEntitiesCall;
import com.likethecolor.alchemy.api.call.type.CallTypeText;
import com.likethecolor.alchemy.api.entity.NamedEntityAlchemyEntity;
import com.likethecolor.alchemy.api.entity.Response;
import com.likethecolor.alchemy.api.params.Language;
import com.likethecolor.alchemy.api.params.NamedEntityParams;

import it.uniroma3.agiw3.support.SleepTime;

public class AlchemyClient {
	private String current_key;
	private Client alchemy_client;
	private int sleepMin;
	private int sleepMax;
	private int failCount;
	
	public AlchemyClient(String key){
		this.current_key=key;
		this.alchemy_client = setUpAlchemyClient(this.current_key);
		this.sleepMin=3000;
		this.sleepMax=10000;
		this.failCount=1;
	}

	private Client setUpAlchemyClient(String key){
		try{
			return new Client(key);
		}
		catch(Exception e){
			System.out.println("ERR CLIENT "+e.getMessage());
		}
		return setUpAlchemyClient(key);
	}


	public Response<NamedEntityAlchemyEntity> query(String text) throws AlchemyKeyException {
		NamedEntityParams namedEntityParams = new NamedEntityParams();
		namedEntityParams.setLanguage(Language.ENGLISH);
		//		namedEntityParams.setIsCoreference(true);
		//		namedEntityParams.setIsDisambiguate(true);
		//		namedEntityParams.setIsLinkedData(true);
		//		namedEntityParams.setIsQuotations(true);
		//		namedEntityParams.setIsSentiment(true);
		//		namedEntityParams.setIsShowSourceText(true);
		AbstractCall<NamedEntityAlchemyEntity> rankedNamedEntitiesCall = new RankedNamedEntitiesCall(new CallTypeText(text), namedEntityParams);
		Response<NamedEntityAlchemyEntity> rankedNamedEntitiesResponse = null;
		try {
			rankedNamedEntitiesResponse = this.alchemy_client.call(rankedNamedEntitiesCall);
			if(this.failCount>1)
				this.failCount--;
		} 
		catch (IOException e) {
			System.out.println();
			this.failCount++;
			Long sleepTime = SleepTime.calc(this.sleepMin*failCount, this.sleepMax*failCount);
			System.out.println("[AlchemyClient] Failed call. Thread will sleep "+sleepTime+" millis, and then retry");
			try {
				Thread.sleep(sleepTime);
			} 
			catch (InterruptedException e1) {
				System.out.println(e1.getMessage());
			}
			return this.query(text);
		} 
		return rankedNamedEntitiesResponse;
	}
}
