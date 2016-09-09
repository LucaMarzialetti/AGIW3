package it.uniroma3.agiw3.ner;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Iterator;
import java.util.concurrent.Semaphore;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;

import com.likethecolor.alchemy.api.entity.NamedEntityAlchemyEntity;
import com.likethecolor.alchemy.api.entity.Response;

import it.uniroma3.agiw3.main.SyncObj;

public class NERSlave implements Runnable {

	private String outputFolder;
	private AlchemyKeyManager akm;
	private String url;
	private String name;
	private String document;
	private Semaphore s;
	private SyncObj so;

	public NERSlave(String outputFolder, AlchemyKeyManager akm, String url, String name, String document, Semaphore s, SyncObj so) {
		this.outputFolder = outputFolder;
		this.akm = akm;
		this.url = url;
		this.name = name;
		this.document = Jsoup.parse(document).text();
		this.s = s;
		this.so = so;
	}

	@Override
	public void run() {
		try{
			this.exec();
		}
		catch(Exception e){
			System.out.println("[NERSlave]: UNCATCHED EXCEPTION, catched to avoid deadlock");
		}
		finally{
			this.so.check();
			this.s.release();
		}
	}

	private void exec(){
		JSONObject obj = new JSONObject();
		obj.put("url", this.url);
		JSONObject pm = patternMatching();
		obj.put("PATTERN", pm);
		JSONObject ner = namedEntityRecognition();
		obj.put("NER",ner);
		//obj.put("TEXT",this.document);
		persist(obj);
	}

	/*save the json object in the choosen directory, file name is an incremental number*/
	private void persist(JSONObject obj){
		File folder = new File(this.outputFolder);
		if(!folder.exists() || !folder.isDirectory())
			folder.mkdir();
		String path = folder.getPath()+"/"+this.name;
		File name_folder = new File(path);
		if(!name_folder.exists() || !name_folder.isDirectory()){
			name_folder.mkdir();
			path+="/0";
		}
		else 
			path+="/"+name_folder.list().length;
		Writer writer=null;
		try {
			writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(path), "utf-8"));
			writer.write(obj.toString(4));
		}
		catch (IOException e) {
			System.out.println(e.getMessage());	
		}
		finally {
			try {
				writer.close();
			} 
			catch (Exception e) {
				System.out.println(e.getMessage());	
			}
		}
	}

	/*compose the NER OBJECT**/
	private JSONObject namedEntityRecognition() {
		JSONObject jo = new JSONObject();
		JSONArray loc = new JSONArray();
		JSONArray per = new JSONArray();
		JSONArray org = new JSONArray();
		Response<NamedEntityAlchemyEntity> r = this.akm.queryToAlchemy(this.url);
		Iterator<NamedEntityAlchemyEntity> ri = r.iterator();
		while(ri.hasNext()){
			NamedEntityAlchemyEntity entity = ri.next();
			String type = entity.getType();
			String text = entity.getText();
			switch (type) {
				case "City" :
				case "Continent" :
				case "Country" :
				case "GeographicFeature" :
				case "Region" :
				case "StateOrCountry" :
					loc.put(text);
					break;
				case "JobTitle" :
				case "MusicGroup" :
				case "Person" :
				case "ProfessionalDegree" :
					per.put(text);
					break;
				case "Company" :
				case "Facility" :
				case "Organization" :
					org.put(text);
					break;
				default:
					break;
			}
		}
		jo.put("LOC", loc);
		jo.put("ORG", org);
		jo.put("PER", per);
		return jo;
	}

	/*compose the PM OBJECT**/
	private JSONObject patternMatching(){
		JSONObject jo = new JSONObject();
		jo.put("email", match_emails());
		jo.put("tel",match_phones());
		jo.put("addr",match_addresses());
		return jo;
	}

	/*PM - match emails*/
	private JSONArray match_emails(){
		JSONArray ja = new JSONArray();
		Pattern p = Pattern.compile("("
				+ "([a-zA-Z0-9:!#$%&'-/=^_`{|}~\\*\\+\\?\\.\\[\\]\\(\\)]+@\\[?[a-zA-Z0-9\\.:]+\\]?)|"
				+ "([a-zA-Z0-9:!#$%&'-/=^_`{|}~\\*\\+\\?\\.\\[\\]\\(\\)]+@([a-zA-Z0-9]\\.)+[a-zA-Z0-9])|"
				+ "([a-zA-Z0-9:!#$%&'-/=^_`{|}~\\*\\+\\?\\.\\[\\]\\(\\)]+(at|\\[at\\]|\\(at\\))([a-zA-Z0-9]\\.)+[a-zA-Z0-9])"
				+ ")");
		String[] cases = this.document.split(" ",-1);
		for(int i=0; i<cases.length; i++){
			Matcher m = p.matcher(cases[i]);
			while(m.find()){
				String group = m.group().trim();
				if(!group.isEmpty())
					ja.put(group);	
			}
		}
		return ja;
	}

	/*PM - match eng phones*/
	private JSONArray match_phones(){
		JSONArray ja = new JSONArray();
		Pattern p = Pattern.compile("("
				+ "(\\+?[0-9]{1,3}[/\\. -]?[0-9]{3}[/\\. -]?[0-9]{3}[/\\. -]?[0-9]{4})|"
				+ "(\\([0-9]{3}\\) ?[0-9]{3}-?[0-9]{4})|"
				+ "(\\+?[0-9]{1,2}[/\\. -]?[0-9]{2}[/\\. -]?[0-9]{3}[/\\. -]?[0-9]{5})|"
				+ "(\\+?[0-9]{3}[/\\. -]?[0-9]{3}[/\\. -]?[0-9]{3,4})|"
				+ "(\\+?[0-9]{2}(\\([0-9]\\))?[0-9]{2,3}[/\\. -]?[0-9]{3,4}[/\\. -]?[0-9]{3,4})"
				+ ")");
		Matcher m = p.matcher(this.document);
		while (m.find()) {
			String group = m.group().trim();
			if(!group.isEmpty())
				ja.put(group);
		}
		return ja;
	}

	/*PM - match address*/
	private JSONArray match_addresses(){
		JSONArray ja = new JSONArray();
		JSONArray postcodes = postcodes(this.document);
		for(int i=0; i<postcodes.length(); i++){
			ja.put(postcodes.get(i));
		}
		JSONArray streets = streets(this.document);
		for(int i=0; i<streets.length(); i++){
			ja.put(streets.get(i));
		}
		return ja;
	}

	/*PM - match address - postcodes*/
	private JSONArray postcodes(String document){
		JSONArray ja = new JSONArray();
		Pattern p = Pattern.compile("("
				+ "([A-Z]{2}[0-9][A-Z] ?[0-9][A-Z]{2})|"
				+ "([A-Z][0-9][A-Z] ?[0-9][A-Z]{2})|"
				+ "([A-Z][0-9] ?[0-9][A-Z]{2})|"
				+ "([A-Z][0-9]{2} ?[0-9][A-Z]{2})|"
				+ "([A-Z]{2}[0-9] ?[0-9][A-Z]{2})|"
				+ "([A-Z]{2}[0-9]{2} ? [0-9][A-Z]{2})"
				+ ")");
		Matcher m = p.matcher(this.document);
		while (m.find()) {
			String group = m.group().trim();
			if(!group.isEmpty())
				ja.put(group);
		}
		return ja;
	}

	/*PM - match address - streets*/
	private JSONArray streets(String document){
		JSONArray ja = new JSONArray();
		Pattern p = Pattern.compile("("
				+ "[0-9]* ?([A-Za-z\\.]+ ?){1,5} "
				+ "("
					+ "Avenue|avenue|AVENUE|"
					+ "Lane|lane|LANE|"
					+ "Road|road|ROAD|"
					+ "Boulevard|boulevard|BOULEVARD|"
					+ "Drive|drive|DRIVE|"
					+ "Street|street|STREET|"
					+ "Ave\\.|ave\\.|AVE\\.|"
					+ "Dr\\.|dr\\.|DR\\.|"
					+ "Rd\\.|rd\\.|RD\\.|"
					+ "Blvd\\.|blvd\\.|BLVD\\.|"
					+ "Ln\\.|ln\\.|LN\\.|"
					+ "St\\.|st\\.|ST\\."
				+ ")"
				+ ")");
		Matcher m = p.matcher(this.document);
		while (m.find()) {
			String group = m.group().trim();
			if(!group.isEmpty())
				ja.put(group);
		}
		return ja;
	}
}