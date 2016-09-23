package it.uniroma3.agiw3.ner;

import java.util.LinkedList;

public enum AlchemyKeyOwners {
	//	luca_tiscali,
	//	luca_hotmail,
	luca_yahoo,
	luca_stud,
	luca_gmail,
	luca_fast,
	ecomarz_tiscali,
	ecomarzi_gmail,
	paola_gmail,
	paola_tiscali;
	
	/*return a list of string containing all the enumeration values*/
	public static LinkedList<String> names() {
		AlchemyKeyOwners[] owners = values();
		LinkedList<String> names = new LinkedList<String>();
		for (int i = 0; i < owners.length; i++) {
			names.add(owners[i].name());
		}
		return names;
	}
}
