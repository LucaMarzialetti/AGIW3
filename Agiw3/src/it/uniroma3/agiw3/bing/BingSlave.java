package it.uniroma3.agiw3.bing;

import java.io.IOException;
import java.nio.charset.IllegalCharsetNameException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;

import org.json.JSONObject;
import org.jsoup.nodes.Document;

import it.uniroma3.agiw3.http_connector.HttpConnector;
import it.uniroma3.agiw3.main.SyncObj;

public class BingSlave implements Runnable{
	private JSONObject bingJSON;
	private LinkedBlockingQueue<JSONObject> que_ans;
	private LinkedBlockingQueue<JSONObject> failed;
	private Semaphore semaphore;
	private SyncObj so;

	public BingSlave(JSONObject bingJSON, LinkedBlockingQueue<JSONObject> que_ans, LinkedBlockingQueue<JSONObject> failed, Semaphore semaphore, SyncObj so) {
		this.bingJSON = bingJSON;
		this.que_ans=que_ans;
		this.failed=failed;
		this.semaphore=semaphore;
		this.so=so;
	}

	@Override
	public void run() {
		//using try catch finally to avoid deadlock if jsoup parser crashes
		try {
			this.parse();
		}
		catch (Exception e) {
			System.out.println("[BING-SLAVE]: UNCATCHED EXCEPTION, captched to avoid deadlock!");
		}
		finally{
			this.so.check();
			this.semaphore.release();
		}
	}

	private void parse() {
		/**PDF are not included**/
		String url = this.bingJSON.getString("Url");
		try {
			Document d = HttpConnector.getPageRetrieve(url,20000,0,null);
			JSONObject ans = new JSONObject();
			ans.put("bingJSON", this.bingJSON);
			ans.put("doc", d);
			this.que_ans.put(ans);
		}	
		catch (IllegalCharsetNameException | IOException e) {
			try {
				this.failed.put(this.bingJSON);
			} 
			catch (InterruptedException e1) {
				System.out.println(e1.getMessage());
			}
		} 
		catch (InterruptedException e) {
			System.out.println(e.getMessage());
		}
	}
}