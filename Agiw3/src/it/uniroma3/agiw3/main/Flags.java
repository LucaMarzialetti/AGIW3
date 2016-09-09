package it.uniroma3.agiw3.main;

/**enumeration used to set the operations enabled**/
public enum Flags {
	SearchEngine,			//build search engine
	NamedEntityRecognition,	//build ner
	Mongo_build_HTML,		//build mongo html db
	Mongo_build_DOC,		//build mongo query db
	Mongo_drop_HTML,		//drop mongo html db
	Mongo_drop_DOC,			//drop mongo query db
	FS_HTML,				//write on fs html into directory >queries>
	Local_extract,			//load the name files from local
	Remote_extract,			//remote parse websource for name file
	Elastic_build_index,	//build elastic search index
	Elastic_drop_index,		//drop elastic search index
}
