package it.uniroma3.agiw3.http_connector;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.ArrayList;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

public class HttpConnector {

	/**GET METHOD
	 * @throws IOException **/
	public static Document getPageRetrieve(String url, int timeout, int size, String agent) throws IOException {
		Document d;
		String userAgent;
		if(agent!=null)
			userAgent = RandomUserAgent.getRandomUserAgent(agent);
		else
			userAgent = RandomUserAgent.getRandomUserAgent();
		d = Jsoup.connect(url)
				.timeout(timeout)
				.maxBodySize(size)
				.userAgent(userAgent)
				.get();
		return d;
	}

	/**POST METHOD**/
	public static Document postPageRetrieve(String url, ArrayList<String> attributes, ArrayList<String> values, int timeout, int size, String agent) throws MalformedPostAttributesException{
		Document doc=null;
		String userAgent;
		if(agent!=null)
			userAgent = RandomUserAgent.getRandomUserAgent(agent);
		else
			userAgent = RandomUserAgent.getRandomUserAgent();
		try {
			int countAttr = attributes.size();
			if(countAttr != values.size())
				throw new MalformedPostAttributesException();
			else{
				Connection con = Jsoup.connect(url)
						.timeout(timeout)
						.maxBodySize(size)
						.userAgent(userAgent);
				for(int i=0; i<attributes.size(); i++)
					con.data(attributes.get(i),values.get(i));
				doc = con.post();
			}
		}
		catch (IOException e) {
			doc=null;
			System.out.println("[WebConnector]: "+e.getMessage());
		}
		return doc;
	}

	public static boolean getUrlResponseCodeOk(String url){
		try{
			HttpURLConnection huc =  (HttpURLConnection) new URL(url).openConnection(); 
			huc.setInstanceFollowRedirects(false);
			huc.setRequestMethod("HEAD");
			huc.connect(); 
			int responseCode = huc.getResponseCode();
			if(responseCode%100 <3)
				return true;
		}
		catch(ProtocolException e){
			System.out.println(e.getMessage());
		} 
		catch (MalformedURLException e) {
			System.out.println(e.getMessage());
		} 
		catch (IOException e) {
			System.out.println(e.getMessage());
		}
		catch(Exception e){
			System.out.println(e.getMessage());
		}
		return false;
	}
}
