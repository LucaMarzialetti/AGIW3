package it.uniroma3.agiw3.ner;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;

import org.apache.commons.io.IOUtils;
import org.json.JSONObject;

public class AlchemyKeyLoader {
	private JSONObject keys;
	private LinkedList<String> owners;
	private String key_file = "alchemyKeys.txt";
	private Iterator<String> owner_iterator;

	public AlchemyKeyLoader(){
		this.keys=loadFile();
		this.owners = AlchemyKeyOwners.names();
		this.owner_iterator = this.owners.iterator();
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

	public boolean hasKey(){
		return this.owner_iterator.hasNext();
	}
	
	public String nextKey(){
		String owner = this.owner_iterator.next();
		String key = this.keys.getJSONObject(owner).getJSONObject("credentials").getString("apikey");
		return key;
	}
}
