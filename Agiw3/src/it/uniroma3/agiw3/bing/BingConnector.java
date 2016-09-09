package it.uniroma3.agiw3.bing;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;
import java.util.LinkedList;

import org.json.JSONArray;
import org.json.JSONObject;

import it.uniroma3.agiw1.keymanager.BingKeyManager;
import it.uniroma3.agiw1.keymanager.KeyExpiredException;
import it.uniroma3.agiw1.keymanager.NoMoreKeysException;

public class BingConnector {
	public final String bingSite = "https://api.datamarket.azure.com/Bing/Search/v1/Web?Query=%27";
	private final LinkedList<String> keysOwners = BingKeysOwners.names();
	private Iterator<String> keyIterator;
	private String currentKey;
	
	public BingConnector() {
		this.keyIterator = keysOwners.iterator();
		if(this.keyIterator.hasNext())
			this.currentKey=keyIterator.next();
	}

	/**try to submit query with the current key.
	 * if the current key is expired, try with the next one. 
	 * if there aren't any other keys an exception is raised**/
	public JSONArray bingQueryWithKeyIterator(String query) throws NoMoreKeysException{
		JSONArray ja = null;
		try{
			ja = bingQuery(query);
		}
		catch(KeyExpiredException e){
			if(this.keyIterator.hasNext()){
				String oldkey = this.currentKey;
				this.nextKey();
				System.out.println("[BingConnector]: current key "+oldkey+" expired. Key rolled to "+this.currentKey);
				ja =  bingQueryWithKeyIterator(query);
			}
			else{
				System.out.println("[BingConnector]: no more keys!");
				throw new NoMoreKeysException();
			}
		}
		return ja;
	}

	/**try a query, if key is expired exception is raised
	 * @throws NoMoreKeysException **/
	public JSONArray bingQuery(String query) throws KeyExpiredException, NoMoreKeysException {
		JSONArray results = null;
		String searchText = query;
		searchText = searchText.replaceAll("\\s", "%20");
		/** CREDENZIALI BING **/
		String accountKeyEnc = BingKeyManager.getByName(this.currentKey);
		URL url;
		HttpURLConnection conn = null;
		try {
			url = new URL(bingSite + searchText + "%27&$format=JSON");
			System.out.println("[BingConnector]: connecting to Bing Search API ["+this.currentKey+"]");
			conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("GET");
			conn.setRequestProperty("Authorization", "Basic " + accountKeyEnc);
			System.out.println("[BingConnector]: getting response..");
			BufferedReader br = new BufferedReader(new InputStreamReader(
					(conn.getInputStream())));
			StringBuilder sb = new StringBuilder();
			String output;

			while ((output = br.readLine()) != null) {
				sb.append(output);
			}
			final JSONObject json = new JSONObject(sb.toString());
			final JSONObject d = json.getJSONObject("d");
			results = d.getJSONArray("results");
			System.out.println("[BingConnector]: "+results.length()+" results for "+query);
			conn.disconnect();
		}
		catch (MalformedURLException e) {
			System.out.println("[BingConnector]: "+e.getMessage());;
		} 
		catch (IOException e) {
			try {
				if(conn.getResponseCode() == 503)
					throw new KeyExpiredException();
				else
					System.out.println("[BingConnector]: "+e.getMessage());
			} 
			catch (IOException e1) {
				System.out.println("[BingConnector]: "+e1.getMessage());

			}
		}
		return results;
	}	
	
	/*change the current key used*/
	public void nextKey(){
		this.currentKey=this.keyIterator.next();
	}
}
