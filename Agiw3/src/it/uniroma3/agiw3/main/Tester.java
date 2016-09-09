package it.uniroma3.agiw3.main;

import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Tester {
	public static void main(String[] args) {
		match_mail();
		//match_phones();
		//match_address();
		//accents();
	}

	private static void accents() {
		LinkedList<Integer> chars = new LinkedList<Integer>();
		chars.add(160);
		chars.add(134);
		chars.add(138);
		chars.add(143);
		chars.add(147);
		chars.add(152);
		chars.add(157);
//		for(Integer c : chars)
//			System.out.println("|"+(char)c.intValue()+"|");
//		System.out.println();
		int i=0;
		while(i<1000){
			System.out.println(i+" "+(char)i);
			i++;
		}
	}

	public static void match_address(){
		String document ="London, E5 XXX London, SE28 XXX London, SW16 XXX London, SW19 XXX London, SW19 XXX London, N16 XXX London, E5 XXX London, NW9 XXX London, SW4 XXX London, E4 XXX London, NW2 XXX London, NW9 XXX London, E10 XXX London, N11 XXX London, SE23 XXX London, W3 XXX London, SE16 XXX London, SE28 XXX London, SE7 XXX London, E4 XXX London, SW2 XXX London, NW1 XXX London, SE7 XXX London, SE15 XXX London, E17 XXX London, SE16 XXX London, W5 XXX London, SW11 XXX";
	}
	
	public static void match_phones(){
		String document ="754-3010 Local  (541) 754-3010  Domestic +1-541-754-3010 International 1-541-754-3010 Dialed in the US 001-541-754-3010 Dialed from Germany 191 541 754 3010 Dialed from France Now consider a German phone number. Although a German phone number consists of an area code and an extension like a US number, the format is different. Here is the same German phone number in a variety of formats: 636-48018 Local (089) / 636-48018 Domestic +49-89-636-48018 International 19-49-89-636-48018 Dialed from France";
		Pattern p = Pattern.compile("("
				+ "(\\+?[0-9]{1,3}[/\\. -]?[0-9]{3}[/\\. -]?[0-9]{3}[/\\. -]?[0-9]{4})|"
				+ "(\\([0-9]{3}\\) ?[0-9]{3}-?[0-9]{4})|"
				+ "(\\+?[0-9]{1,2}[/\\. -]?[0-9]{2}[/\\. -]?[0-9]{3}[/\\. -]?[0-9]{5})"
				+ ")");	
		Matcher m = p.matcher(document);
		System.out.println("PHONES");
		while (m.find()) {
			System.out.println(m.group().trim());
		}
	}

	public static void match_mail(){
		Pattern p = Pattern.compile("("
				+ "(^[a-zA-Z0-9:!#$%&'-/=^_`{|}~\\*\\+\\?\\.\\[\\]\\(\\)]+@\\[?[a-zA-Z0-9\\.:]+\\]?$)|"
				+ "(^[a-zA-Z0-9:!#$%&'-/=^_`{|}~\\*\\+\\?\\.\\[\\]\\(\\)]+@([a-zA-Z0-9]\\.)+[a-zA-Z0-9]$)"
				+ ")");
		String cases="";
		String good_cases = "abbey.seeds@mossadams.com user@[IPv6:2001:db8::1] user@localserver user@com example@s.solutions example@localhost #!$%&'*+-/=?^_`{}|~@example.org admin@mailserver1 example-indeed@strange-example.com prettyandsimple@example.com very.common@example.com disposable.style.email.with+symbol@example.com other.email-with-dash@example.com x@example.com"; cases+=good_cases+" ";
		//String bad_cases = "Abc.example.com A@b@c@example.com a\"b(c)d,e:f;g<h>i[j\\k]l@example.com"; cases+=bad_cases;
		String[] document = cases.split(" ",-1);
		for(int i=0; i<document.length; i++){
			Matcher m = p.matcher(document[i]);
			while (m.find()) {
				System.out.println(m.group());
			}
		}
	}
}
