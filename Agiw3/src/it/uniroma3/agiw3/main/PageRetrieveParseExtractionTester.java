package it.uniroma3.agiw3.main;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import it.uniroma3.agiw3.http_connector.HttpConnector;

public class PageRetrieveParseExtractionTester {
	public static void main(String[] args) {
		String link = "http://torlone.dia.uniroma3.it/";
		try {
			Document d = HttpConnector.getPageRetrieve(link, 0, 0, null);
			String text = Jsoup.parse(d.text()).text();
			System.out.println(text);
			Pattern p = Pattern.compile("("
					+ "([a-zA-Z0-9:!#$%&'-/=^_`{|}~\\*\\+\\?\\.\\[\\]\\(\\)]+@\\[?[a-zA-Z0-9\\.:]+\\]?)|"
					+ "([a-zA-Z0-9:!#$%&'-/=^_`{|}~\\*\\+\\?\\.\\[\\]\\(\\)]+@([a-zA-Z0-9]\\.)+[a-zA-Z0-9])|"
					+ "([a-zA-Z0-9:!#$%&'-/=^_`{|}~\\*\\+\\?\\.\\[\\]\\(\\)]+( at |\\[ ?at ?\\]|\\( ?at ?\\))([a-zA-Z0-9]\\.)+[a-zA-Z0-9])"
					+ ")");
			Matcher m = p.matcher(text);
			System.out.println("============================\n============================");
			while(m.find()){
				System.out.println(m.group());
			}

		} 
		catch (IOException e) {
			System.out.println(e.getMessage());
		}
	}
}
