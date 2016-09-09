package it.uniroma3.agiw3.ner;

import java.util.LinkedList;

public enum AlchemyKeyOwners {
	bboyrake,
	bbrake;
	
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
