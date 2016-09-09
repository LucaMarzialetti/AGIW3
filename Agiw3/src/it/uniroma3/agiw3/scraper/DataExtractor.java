package it.uniroma3.agiw3.scraper;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

import org.json.JSONArray;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import it.uniroma3.agiw3.http_connector.HttpConnector;
import it.uniroma3.agiw3.main.MainRunner;
import it.uniroma3.agiw3.main.Flags;
import us.codecraft.xsoup.Xsoup;

public class DataExtractor {
	public static final String outputFileName = "names.txt";
	public static final String urlTarget = "http://solicitors.lawsociety.org.uk/search/results?Type=1&IncludeNlsp=false&Pro=True&Page=";

	public DataExtractor(){
	}

	/*String formatter, normalize name and surname*/
	private String formatterAvvocato(Element e){
		String s = e.text().toString().toLowerCase();
		s=specialCharsNormalize(s);
		return s;
	}

	private String specialCharsNormalize(String s){
		s=
				s.replaceAll("\".*\"", "")		//virgolette
				.replaceAll("\\(.*\\)", "")		//punti
				.trim()							//spazi malformati
				.replaceAll((char)160+""," ")	//spazi nbsp
				.replaceAll("a'", (char)136+"")				
				.replaceAll("e'", (char)143+"")
				.replaceAll("i'", (char)147+"")
				.replaceAll("o'", (char)152+"")
				.replaceAll("u'", (char)157+"")
				.replaceAll("[ ]*$","");		//spazi in coda
		return s;
	}

	/*HTML parser heavy DOM based on xpath*/
	public JSONArray extract() {
		JSONArray array = new JSONArray();
		if(MainRunner.flags.contains(Flags.Remote_extract))
			array = remoteExtract();
		else
			if(MainRunner.flags.contains(Flags.Local_extract))
				array =	localExtract();
		return array;
	}

	/**WEB: write a new names file and already store in a json array the result**/
	private JSONArray remoteExtract() {
		JSONArray array = new JSONArray();
		Set<String> lines = new HashSet<String>();
		try {
			Files.delete(Paths.get(outputFileName));
		}
		catch (IOException e) {
			System.out.println("[DataExtractor]: "+e.getMessage());
		}
		System.out.println("[DataExtractor]: downloading HTML source from "+urlTarget);
		boolean ended = false;
		int index = 1;
		int extracted = 0;
		System.out.println("[DataExtractor]: Web Scraping (Q=query|X=processing|r=retry):");
		while(!ended){
			System.out.print("Q");
			String currentTarget = urlTarget+index;
			Document doc = null;
			try {
				doc = HttpConnector.getPageRetrieve(currentTarget,0,0,"Chrome");
			}
			catch(Exception e){
				System.out.print("r");
			}
			if(doc!=null){
				System.out.print("X");
				//check if there are results in the page
				String pageLimit = Xsoup.compile("//div[contains(@class,'search-results-controls')]//*[contains(@class,'row-fluid')]/h1//*").evaluate(doc).getElements().get(1).text();
				if(pageLimit!=null && Integer.parseInt(pageLimit)>0){
					Elements el = Xsoup.compile("//section[contains(@class,'solicitor')]//header//h2/a").evaluate(doc).getElements();
					extracted+=el.size();
					for(Element e: el){
						String formattedData = formatterAvvocato(e);
						lines.add(formattedData);
						array.put(formattedData);
					}
				}
				else
					ended=true;
				//increment
				index++;
			}
		}	
		System.out.println();
		System.out.println("[DataExtractor]: extracted "+extracted+" DOM elements.");
		LinkedList<String> names = new LinkedList<String>();
		names.addAll(lines);
		Collections.sort(names);
		try {
			Files.write(Paths.get(outputFileName), names, Charset.forName(StandardCharsets.ISO_8859_1.name()), StandardOpenOption.CREATE);
		} 
		catch (IOException e) {
			System.out.println(e.getMessage());
		}
		System.out.println("[DataExtractor]: Names file written");
		/*RETURN*/
		return array;
	}

	/**LOCAL-FILE: only read the names file and store it in the json array**/
	private JSONArray localExtract() {
		JSONArray array = new JSONArray();
		BufferedReader br = null;
		try {
			FileInputStream is = new FileInputStream(outputFileName);
			InputStreamReader isr = new InputStreamReader(is, StandardCharsets.ISO_8859_1.name());
			br = new BufferedReader(isr);			
			System.out.println("[DataExtractor]: reading from file "+outputFileName);
			String line;
			while ((line = br.readLine()) != null)
				array.put(specialCharsNormalize(line));
			System.out.println("[DataExtractor]: extracted "+array.length()+" elements");
		}
		catch (FileNotFoundException e) {
			System.out.println("[DataExtractor]: "+e.getMessage());
		}
		catch (IOException e) {
			System.out.println("[DataExtractor]: "+e.getMessage());
		}
		finally {
			try {
				br.close();
			} 
			catch (IOException e) {
				System.out.println(e.getMessage());
			}
		}
		/*RETURN*/
		return array;
	}
}