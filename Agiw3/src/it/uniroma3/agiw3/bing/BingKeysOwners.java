package it.uniroma3.agiw3.bing;

import java.util.LinkedList;

public enum BingKeysOwners {
//	matteo,
//	silvia_uniroma3,
//	silvia,
	grayfox,
	edgard,
	luca,
	luca_uniroma3;
//	elena_uniroma3,
//	elena;

	/*return a list of string containing all the enumeration values*/
	public static LinkedList<String> names() {
		BingKeysOwners[] owners = values();
		LinkedList<String> names = new LinkedList<String>();
		for (int i = 0; i < owners.length; i++) {
			names.add(owners[i].name());
		}
		return names;
	}
}