import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.jena.query.Dataset;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFormatter;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.tdb.TDBFactory;
import org.apache.jena.util.FileManager;

public class QueryOWNPT {

	static String OWNPT_FILE = "/Users/besugo/OntoPT_ficheiros/ontologias/openwn-pt/own-pt.nt";
	static String WNEN_FILE = "/Users/besugo/OntoPT_ficheiros/ontologias/openwn-pt/wordnet-en.nt";

	static String PRED_RDF_TYPE = "<http://www.w3.org/1999/02/22-rdf-syntax-ns#type>";
	static String PRED_OWL_SAMEAS = "<http://www.w3.org/2002/07/owl#sameAs>";
	static String PRED_LEXICAL_FORM = "<https://w3id.org/own-pt/wn30/schema/lexicalForm>";
	static String PRED_WORD = "<https://w3id.org/own-pt/wn30/schema/word>";
	static String PRED_WORD_SENSE = "<https://w3id.org/own-pt/wn30/schema/containsWordSense>";
	static String PRED_WORD_NUMBER = "<https://w3id.org/own-pt/wn30/schema/wordNumber>";

	static String CLASS_SYNSET = "<https://w3id.org/own-pt/wn30/schema/Synset>";
	static String CLASS_NOUN_SYNSET = "<https://w3id.org/own-pt/wn30/schema/NounSynset>";
	static String CLASS_VERB_SYNSET = "<https://w3id.org/own-pt/wn30/schema/VerbSynset>";
	static String CLASS_ADJ_SYNSET = "<https://w3id.org/own-pt/wn30/schema/AdjectiveSynset>";
	static String CLASS_ADV_SYNSET = "<https://w3id.org/own-pt/wn30/schema/AdverbSynset>";

	static String PRED_HYPERNYM = "<https://w3id.org/own-pt/wn30/schema/hypernymOf>";
	static String PRED_HYPONYM = "<https://w3id.org/own-pt/wn30/schema/hyponymOf>";

	static String PRED_PART_HOLONYM = "<https://w3id.org/own-pt/wn30/schema/partHolonymOf>";
	static String PRED_PART_MERONYM = "<https://w3id.org/own-pt/wn30/schema/partMeronymOf>";
	static String PRED_MEMBER_HOLONYM = "<https://w3id.org/own-pt/wn30/schema/memberHolonymOf>";
	static String PRED_MEMBER_MERONYM = "<https://w3id.org/own-pt/wn30/schema/memberMeronymOf>";
	static String PRED_SUBSTANCE_HOLONYM = "<https://w3id.org/own-pt/wn30/schema/substanceHolonymOf>";
	static String PRED_SUBSTANCE_MERONYM = "<https://w3id.org/own-pt/wn30/schema/substanceMeronymOf>";

	static String PRED_CAUSES = "<https://w3id.org/own-pt/wn30/schema/causes>";
	static String PRED_ANTONYM = "<https://w3id.org/own-pt/wn30/schema/antonymOf>";
	static String PRED_ENTAILS = "<https://w3id.org/own-pt/wn30/schema/entails>";

	static String TEMPLATE_PRED_RELATION = "<https://w3id.org/own-pt/wn30/schema/?>";
	static String KEY_SYNONYM = "SYNONYM";
	static String KEY_ANTONYM = "ANTONYM";
	static String KEY_PART = "PART";
	static String KEY_WHOLE = "WHOLE";
	static String KEY_CAUSES_X = "CAUSES_X";
	static String KEY_CAUSES_Y = "CAUSES_Y";

	private Model model;
	private Dataset dataset;

	public static void main (String args[]) {

		String[] sources = {WNEN_FILE, OWNPT_FILE};
		QueryOWNPT ownpt = new QueryOWNPT(sources);
		boolean firstSense = false;

		ownpt.fixLexicalFormsWithExtraSpaces();
		
		System.out.println(ownpt.hyponymsOfWord("lugar", firstSense));

		/*String queryString = "SELECT ?s WHERE {"
				+ "?s <https://w3id.org/own-pt/wn30/schema/containsWordSense> ?ws"
				+ " . ?ws <https://w3id.org/own-pt/wn30/schema/word> ?w"
				+ " . ?w <https://w3id.org/own-pt/wn30/schema/lexicalForm> 'gato'@pt }";*/

		/*		String queryString = "SELECT ?s WHERE {"
				+ "?s <https://w3id.org/own-pt/wn30/schema/containsWordSense> ?ws"
				+ " . ?ws <https://w3id.org/own-pt/wn30/schema/word> ?w"
				+ " . ?w <https://w3id.org/own-pt/wn30/schema/lexicalForm> 'cão'@pt }";
		ownpt.executeQuery(queryString);*/

		//ownpt.wordPairsRelatedBy(PRED_HYPONYM);

		//Set<String> lexicalForms = ownpt.allLexicalForms();
		//ownpt.saveMapLemmasSenses("lemas_sentidos.csv");
		
		/*String query = "SELECT ?lf WHERE {"
				+ "?s1 <https://w3id.org/own-pt/wn30/schema/containsWordSense> ?ws1 . "
				+ "?ws1 <https://w3id.org/own-pt/wn30/schema/word> ?w1 . "
				+ "?w1 <https://w3id.org/own-pt/wn30/schema/lexicalForm> \"correr\"@pt . "
				+ "?sen1 <http://www.w3.org/2002/07/owl#sameAs> ?s1 . "
				+ "?sen2 <https://w3id.org/own-pt/wn30/schema/hyponymOf> ?sen1 . "
				+ "?sen2 <http://www.w3.org/2002/07/owl#sameAs> ?s2 . "
				+ "?s2 <https://w3id.org/own-pt/wn30/schema/containsWordSense> ?ws2 . "
				+ "?ws2 <https://w3id.org/own-pt/wn30/schema/word> ?w2 . "
				+ "?w2 <https://w3id.org/own-pt/wn30/schema/lexicalForm> ?lf}";
		ownpt.executeQuery(query);*/
		
		/*System.out.println(lexicalForms.size());
		System.out.println(lexicalForms.subList(0, 100));
		System.out.println(lexicalForms.contains("de ponta"));
		System.out.println(lexicalForms.contains("de acordo"));
		System.out.println(lexicalForms.contains("(se)"));

		//System.out.println(ownpt.partsOfWord("ser  sutil", false));
		//System.out.println(ownpt.partsOfWord("ser sutil", false));

		System.out.println(ownpt.hyponymsOfWord("aproximação", false));
		System.out.println(ownpt.hypernymsOfWord("aproximação", false));
		System.out.println(ownpt.hyponymsOfWord("estimativa", false));
		System.out.println(ownpt.hypernymsOfWord("estimativa", false));
		System.out.println(ownpt.hyponymsOfWord("frigideira", false));
		System.out.println(ownpt.hypernymsOfWord("frigideira", false));

		String str = "morrer";
		System.out.println("causes of "+str+" = "+ownpt.causesOfWord(str, firstSense));
		System.out.println("related to "+str+" inv = "+ownpt.relatedToWord(str, PRED_CAUSES, firstSense, true));
		System.out.println("related to "+str+" = "+ownpt.relatedToWord(str, PRED_CAUSES, firstSense, false));


		System.out.println("correr = "+ownpt.synonymsOf("correr", firstSense));
		System.out.println("comer = "+ownpt.synonymsOf("comer", firstSense));
		System.out.println("bom = "+ownpt.synonymsOf("bom", firstSense));
		System.out.println("casa = "+ownpt.synonymsOf("casa", firstSense));
		System.out.println("intensamente = "+ownpt.synonymsOf("intensamente", firstSense));

		System.out.println("hypernyms of ... = "+ownpt.hypernymsOfWord("profissão", firstSense));
		System.out.println("hyponyms of ... = "+ownpt.hyponymsOfWord("profissão", firstSense));

		System.out.println("causes of ... = "+ownpt.causesOfWord("comer", firstSense));
		System.out.println("causes of ... = "+ownpt.causesOfWord("correr", firstSense));
		System.out.println("causes of ... = "+ownpt.causesOfWord("afiar", firstSense));
		System.out.println("causes of ... = "+ownpt.causesOfWord("abrir", firstSense));

		System.out.println("entails of ... = "+ownpt.entailsOfWord("comer", firstSense));
		System.out.println("entails of ... = "+ownpt.entailsOfWord("correr", firstSense));
		System.out.println("entails of ... = "+ownpt.entailsOfWord("afiar", firstSense));
		System.out.println("entails of ... = "+ownpt.entailsOfWord("abrir", firstSense));

		System.out.println("parts of ... = "+ownpt.partsOfWord("computador", firstSense));
		System.out.println("parts of ... = "+ownpt.partsOfWord("mesa", firstSense));
		System.out.println("parts of ... = "+ownpt.partsOfWord("carro", firstSense));
		System.out.println("parts of ... = "+ownpt.partsOfWord("perna", firstSense));
		System.out.println("parts of ... = "+ownpt.partsOfWord("Rússia", firstSense));
		List<String[]> partPairs = ownpt.wordPairsRelatedBy(PRED_PART_MERONYM);
		partPairs.addAll(ownpt.wordPairsRelatedBy(PRED_MEMBER_MERONYM));
		partPairs.addAll(ownpt.wordPairsRelatedBy(PRED_SUBSTANCE_MERONYM));
		for(String[] p : partPairs)
			if(p[0].equals("Rússia") || p[1].equals("Rússia"))
				System.out.println("\t"+p[0]+" has part "+p[1]);


		System.out.println("wholes of ... = "+ownpt.wholesOfWord("computador", firstSense));
		System.out.println("wholes of ... = "+ownpt.wholesOfWord("mesa", firstSense));
		System.out.println("wholes of ... = "+ownpt.wholesOfWord("carro", firstSense));
		System.out.println("wholes of ... = "+ownpt.wholesOfWord("perna", firstSense));
		System.out.println("wholes of ... = "+ownpt.wholesOfWord("Rússia", firstSense));
		List<String[]> wholePairs = ownpt.wordPairsRelatedBy(PRED_PART_HOLONYM);
		wholePairs.addAll(ownpt.wordPairsRelatedBy(PRED_MEMBER_HOLONYM));
		wholePairs.addAll(ownpt.wordPairsRelatedBy(PRED_SUBSTANCE_HOLONYM));
		for(String[] p : wholePairs)
			if(p[0].equals("Rússia") || p[1].equals("Rússia"))
				System.out.println("\t"+p[0]+" in whole "+p[1]);

		System.out.println("antonyms of ... = "+ownpt.antonymsOf("bom"));
		System.out.println("antonyms of ... = "+ownpt.antonymsOf("doce"));
		System.out.println("antonyms of ... = "+ownpt.antonymsOf("aumento"));
		System.out.println("antonyms of ... = "+ownpt.antonymsOf("sagrado"));
		System.out.println("antonyms of ... = "+ownpt.antonymsOf("abrir"));

		//ownpt.allPredicates();

		List<String[]> antonyms = ownpt.antonymPairs();
		System.out.println("Antonyms: ");
		for(String[] a : antonyms)
			System.out.println("\t"+a[0]+" antonym "+a[1]);*/

		/*		List<String[]> synonyms = ownpt.synonymPairs();
		System.out.println("Synonyms: ");
		for(String[] a : antonyms)
			System.out.println("\t"+a[0]+" synonym "+a[1]);

		List<String[]> causes = ownpt.wordPairsRelatedBy(PRED_CAUSES);
		System.out.println("Causes: ");
		for(String[] a : causes)
			System.out.println("\t"+a[0]+" causes "+a[1]);*/
	}

	public QueryOWNPT(String[] sources) {

		String directory = "stores/own-pt";
		createTS(directory, sources, "N-TRIPLES");

		dataset = TDBFactory.createDataset(directory);
		model = dataset.getDefaultModel();

		/*		//palavras
		loadRDF(model, OWNPT_FILE, "N-TRIPLES");
		//relacoes
		loadRDF(model, WNEN_FILE, "N-TRIPLES");

		//System.out.println("Writing RDF read from file...");
		// write it to standard out
		//model.write(System.out);*/

	}

	private static void createTS(String location, String[] source, String format)
	{
		File floc = new File(location);

		if(!floc.exists()) {
			floc.mkdirs();

			Dataset dataset = TDBFactory.createDataset(location);
			Model tdb = dataset.getDefaultModel();

			System.out.println("Location "+location+" exists now! Creating triple store...");
			// read the input file - only needs to be done once
			for(String s : source)
				FileManager.get().readModel(tdb, s, format );

			tdb.close();
			dataset.close();
		}
		else 
		{
			System.out.println("Location "+location+" already exists!");

			/*			try {
				Dataset dataset = TDBFactory.createDataset(location);
				Model tdb = dataset.getDefaultModel();

				PrintStream out = new PrintStream("teste.nt");
				tdb.write(out, "N-TRIPLES");

				tdb.close();
				dataset.close();
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}*/

		}


	}

	private static void loadRDF(Model model, String path, String format) {

		// use the FileManager to find the input file
		InputStream in = FileManager.get().open( path );
		if (in == null) {
			throw new IllegalArgumentException(
					"File: " + path + " not found");
		}

		System.out.println("Reading RDF from "+path+"...");
		model.read(in, null, format);

	}

	public void fixLexicalFormsWithExtraSpaces() {

		System.out.println("Searching for lexical forms that need fixing...");
		String queryString = "SELECT ?w ?lf WHERE {"
				+ " ?sen "+PRED_OWL_SAMEAS+" ?spt"
				+ " . ?spt "+PRED_WORD_SENSE+" ?ws"
				+ " . ?ws "+PRED_WORD+" ?w"
				+ " . ?w "+PRED_LEXICAL_FORM+" ?lf"
				+ " }";

		//System.err.println(queryString);
		List<String[]> pairsToFix = new ArrayList<>();
		Query query = QueryFactory.create(queryString);
		try (QueryExecution qexec = QueryExecutionFactory.create(query, model)) {

			ResultSet results = qexec.execSelect();
			while (results.hasNext())
			{
				QuerySolution soln = results.nextSolution();
				if(soln.getLiteral("?lf") != null) {
					String lf = soln.getLiteral("lf").getLexicalForm();
					String fixedLF = lf.trim().replaceAll("\\s\\s+", " ");

					if(!fixedLF.equals(lf)) {
						fixedLF = fixedLF.replace(" ", "_"); // problem when inserting strings with spaces!
						String wuri = soln.get("?w").toString();
						//System.err.println(wuri+" : "+lf+" -> "+fixedLF);
						String[] p = {wuri, fixedLF};
						pairsToFix.add(p);
					}
				}
/*				else {
					System.err.println("==> "+soln.get("?w").toString());
				}*/
			}
		}

		System.out.println("Fixing "+pairsToFix.size()+" lexical forms...");
		StringBuilder sb = new StringBuilder();
		for(String[] p : pairsToFix) {
			sb.append("<"+p[0]+"> "+PRED_LEXICAL_FORM+" '"+p[1]+"'@pt . ");
		}

		String updateString = "INSERT DATA {"+sb.toString()+"}";
//		System.err.println(updateString);
/*		UpdateRequest update = UpdateFactory.create(updateString);
		UpdateProcessor uproc = UpdateExecutionFactory.create(update, dataset);
		uproc.execute();*/
	}

	public Set<String> synonymsOf(String word, boolean firstSense) {

		List<RDFNode> synsets = synsetsWithWord(word, firstSense);
		Set<String> synonyms = new HashSet<>();
		for(RDFNode node : synsets) {
			List<String> words = wordsInSynset(node);
			synonyms.addAll(words);
		}

		return synonyms;
	}

	public List<RDFNode> synsetsWithWord(String word, boolean firstSense){

		String wordpt = "\""+word+"\"@pt"; //@pt não é necessário quando comparação é feita com lower case
		//synsets com a palavra...
		String queryString =
				"SELECT ?s WHERE {"
						+ "?s "+PRED_WORD_SENSE+" ?ws"
						+ (firstSense ? " . ?ws "+PRED_WORD_NUMBER+" \"1\"" : "")
						+ " . ?ws "+PRED_WORD+" ?w"
						+ " . ?w "+PRED_LEXICAL_FORM+" "+wordpt
						//						+ " . ?w "+PRED_LEXICAL_FORM+" ?wpt"
						//						+ " . FILTER (lcase(str(?wpt)) = "+wordpt+")"
						+ " }"; 
		//"SELECT ?s ?t WHERE {?s "+PRED_LEXICAL_FORM+" \"gato\"@pt"+" . ?s "+PRED_RDF_TYPE+" ?t"+"}"; 
		//"SELECT ?s ?p ?o WHERE {?s ?p ?o} LIMIT 50";

		//System.err.println(queryString);
		Query query = QueryFactory.create(queryString);
		List<RDFNode> listSynsets = new ArrayList<>();
		try (QueryExecution qexec = QueryExecutionFactory.create(query, model)) {

			ResultSet results = qexec.execSelect();
			//results = ResultSetFactory.copyResults(results);
			//ResultSetFormatter.out(System.out, results, query);

			while (results.hasNext())
			{
				QuerySolution soln = results.nextSolution() ;
				RDFNode x = soln.get("?s") ;       // Get a result variable by name.
				listSynsets.add(x);
			}
		}

		return listSynsets;
	}

	public List<String> wordsInSynset(RDFNode synset){

		List<String> words = new ArrayList<String>();
		String queryString = "SELECT ?lf WHERE {"
				+ "<"+synset+">" + " "+PRED_WORD_SENSE+" ?ws"
				+ " . ?ws "+PRED_WORD+" ?w"
				+ " . ?w "+PRED_LEXICAL_FORM+" ?lf"
				+ "}";

		//System.err.println(queryString);
		Query query = QueryFactory.create(queryString);
		try (QueryExecution qexec = QueryExecutionFactory.create(query, model)) {

			ResultSet results = qexec.execSelect();
			//results = ResultSetFactory.copyResults(results);
			//ResultSetFormatter.out(System.out, results, query);

			while (results.hasNext())
			{
				QuerySolution soln = results.nextSolution() ;
				Literal x = soln.getLiteral("?lf");
				words.add(x.getLexicalForm());
			}
		}

		return words;
	}

	public Set<String> hypernymsOfWord(String word, boolean firstSense) {
		return relatedToWord(word, PRED_HYPERNYM, firstSense, false);
	}

	public Set<String> hyponymsOfWord(String word, boolean firstSense) {
		return relatedToWord(word, PRED_HYPONYM, firstSense, false);
	}

	public Set<String> causesOfWord(String word, boolean firstSense) {
		return relatedToWord(word, PRED_CAUSES, firstSense, false);
	}

	public Set<String> entailsOfWord(String word, boolean firstSense) {
		return relatedToWord(word, PRED_ENTAILS, firstSense, false);
	}

	public Set<String> partsOfWord(String word, boolean firstSense) {
		Set<String> parts = new HashSet<>();

		parts.addAll(relatedToWord(word, PRED_PART_HOLONYM, firstSense, false));
		parts.addAll(relatedToWord(word, PRED_MEMBER_HOLONYM, firstSense, false));
		parts.addAll(relatedToWord(word, PRED_SUBSTANCE_HOLONYM, firstSense, false));

		return parts;
	}

	public Set<String> wholesOfWord(String word, boolean firstSense) {
		Set<String> wholes = new HashSet<>();

		wholes.addAll(relatedToWord(word, PRED_PART_MERONYM, firstSense, false));
		wholes.addAll(relatedToWord(word, PRED_MEMBER_MERONYM, firstSense, false));
		wholes.addAll(relatedToWord(word, PRED_SUBSTANCE_MERONYM, firstSense, false));

		return wholes;
	}

	public Set<String> relatedToWord(String word, String relation, boolean firstSense, boolean inverse) {

		Set<String> words = new HashSet<>();
		List<RDFNode> synsets = synsetsWithWord(word, firstSense);
		for(RDFNode node : synsets) {
			Set<RDFNode> hypernyms = relatedToSynset(node, relation, inverse);
			for(RDFNode hyp : hypernyms) {
				words.addAll(wordsInSynset(hyp));
			}
		}

		return words;
	}

	public Set<RDFNode> relatedToSynset(RDFNode synset, String relation, boolean inverse){

		Set<RDFNode> related = new HashSet<>();
		String queryString = "SELECT ?s2 WHERE {"
				+ "?sen1 "+PRED_OWL_SAMEAS+" <"+synset+">"
				+ (inverse ? ". ?sen1 "+relation+" ?sen2" : ". ?sen2 "+relation+" ?sen1")
				//				+ ". ?sen1 ?r ?sen2"
				+ ". ?sen2 "+PRED_OWL_SAMEAS+" ?s2"
				+ "}";

		//System.err.println(queryString);
		Query query = QueryFactory.create(queryString);
		try (QueryExecution qexec = QueryExecutionFactory.create(query, model)) {

			ResultSet results = qexec.execSelect();
			//results = ResultSetFactory.copyResults(results);
			//ResultSetFormatter.out(System.out, results, query);

			while (results.hasNext())
			{
				QuerySolution soln = results.nextSolution() ;
				related.add(soln.get("s2"));
			}
		}

		return related;
	}

	public Set<String> antonymsOf(String word){
		Set<String> antonyms = new HashSet<>();

		String wordpt = "\""+word+"\"@pt"; //@pt não é necessário quando comparação é feita com lower case
		String queryString = "SELECT ?a WHERE {"
				+ "?ws1 "+PRED_WORD+" ?w1"
				+ " . ?w1 "+PRED_LEXICAL_FORM+" "+wordpt
				//				+ " . ?w1 "+PRED_LEXICAL_FORM+" ?wpt"
				//				+ " . FILTER (lcase(str(?wpt)) = "+wordpt+")"
				+ " . {{?ws1 "+PRED_ANTONYM+" ?ws2} UNION {?ws2 "+PRED_ANTONYM+" ?ws1}}"
				+ " . ?ws2 "+PRED_WORD+" ?w2"
				+ " . ?w2 "+PRED_LEXICAL_FORM+" ?a"
				+ "}";

		//System.out.println(queryString);
		Query query = QueryFactory.create(queryString);
		try (QueryExecution qexec = QueryExecutionFactory.create(query, model)) {

			ResultSet results = qexec.execSelect();
			//results = ResultSetFactory.copyResults(results);
			//ResultSetFormatter.out(System.out, results, query);

			while (results.hasNext())
			{
				QuerySolution soln = results.nextSolution();
				antonyms.add(soln.getLiteral("a").getLexicalForm());
			}
		}

		return antonyms;
	}

	public List<String[]> wordPairsRelatedBy(String relation){
		List<String[]> triples = new ArrayList<>();

		String queryString = "SELECT DISTINCT ?a ?b WHERE {"
				+ "?ws1 "+PRED_WORD+" ?w1"
				+ " . ?w1 "+PRED_LEXICAL_FORM+" ?a"
				+ " . ?s1 "+PRED_WORD_SENSE+" ?ws1"
				+ " . ?sen1 "+PRED_OWL_SAMEAS+" ?s1"
				+ " . ?sen1 "+relation+" ?sen2"
				+ " . ?ws2 "+PRED_WORD+" ?w2"
				+ " . ?w2 "+PRED_LEXICAL_FORM+" ?b"
				+ " . ?s2 "+PRED_WORD_SENSE+" ?ws2"
				+ " . ?sen2 "+PRED_OWL_SAMEAS+" ?s2"
				+ "}";

		//System.err.println(queryString);
		Query query = QueryFactory.create(queryString);
		try (QueryExecution qexec = QueryExecutionFactory.create(query, model)) {

			ResultSet results = qexec.execSelect();
			//results = ResultSetFactory.copyResults(results);
			//ResultSetFormatter.out(System.out, results, query);

			while (results.hasNext())
			{
				QuerySolution soln = results.nextSolution();
				String a = soln.getLiteral("a").getLexicalForm();
				String b = soln.getLiteral("b").getLexicalForm();

				//pode acontecer ...
				if(!a.equalsIgnoreCase(b)) {
					String[] triple = {a, b};
					triples.add(triple);
				}
			}
		}

		return triples;
	}

	//Muito lento!
	/*	public List<String[]> synonymPairs(){

		List<String[]> pairs = new ArrayList<>();
		String queryString = "SELECT DISTINCT ?a ?b WHERE {"
				+ "?w1 "+PRED_LEXICAL_FORM+" ?a"
				+ " . ?ws1 "+PRED_WORD+" ?w1"
				+ " . ?s1 "+PRED_WORD_SENSE+" ?ws1"
				+ " . ?sen1 "+PRED_OWL_SAMEAS+" ?s1" //para garantir que synset é PT
				+ " . ?s1 "+PRED_WORD_SENSE+" ?ws2"
				+ " . ?ws2 "+PRED_WORD+" ?w2"
				+ " . ?w2 "+PRED_LEXICAL_FORM+" ?b"
				+ "}";

		Query query = QueryFactory.create(queryString);
		try (QueryExecution qexec = QueryExecutionFactory.create(query, model)) {

			ResultSet results = qexec.execSelect();
			//ResultSetFormatter.out(System.out, results, query);

			while (results.hasNext())
			{
				QuerySolution soln = results.nextSolution();
				String a = soln.getLiteral("a").getLexicalForm();
				String b = soln.getLiteral("b").getLexicalForm();
				if(a.equals(b))
					continue;

				String[] pair = {a, b};
				pairs.add(pair);
			}
		}


		return pairs;
	}*/

	public List<String[]> antonymPairs(){

		List<String[]> pairs = new ArrayList<>();
		String queryString = "SELECT DISTINCT ?a ?b WHERE {"
				+ "?ws1 "+PRED_WORD+" ?w1"
				+ " . ?w1 "+PRED_LEXICAL_FORM+" ?a"
				+ " . {{?ws1 "+PRED_ANTONYM+" ?ws2} UNION {?ws2 "+PRED_ANTONYM+" ?ws1}}"
				+ " . ?ws2 "+PRED_WORD+" ?w2"
				+ " . ?w2 "+PRED_LEXICAL_FORM+" ?b"
				+ " . ?s1 "+PRED_WORD_SENSE+" ?ws2" //para garantir que estão associados a um synset PT
				+ " . ?sen1 "+PRED_OWL_SAMEAS+" ?s1" //idem
				+ "}";

		Query query = QueryFactory.create(queryString);
		try (QueryExecution qexec = QueryExecutionFactory.create(query, model)) {

			ResultSet results = qexec.execSelect();
			//ResultSetFormatter.out(System.out, results, query);

			while (results.hasNext())
			{
				QuerySolution soln = results.nextSolution();
				String a = soln.getLiteral("a").getLexicalForm();
				String b = soln.getLiteral("b").getLexicalForm();
				String[] pair = {a, b};
				pairs.add(pair);
			}
		}


		return pairs;
	}

	public void executeQuery(String queryString) {

		//System.err.println(queryString);
		Query query = QueryFactory.create(queryString);
		try (QueryExecution qexec = QueryExecutionFactory.create(query, model)) {
			ResultSet results = qexec.execSelect();
			ResultSetFormatter.out(System.out, results, query);
		}
	}

	public void allPredicates() {

		String queryString = "SELECT DISTINCT ?r WHERE {"
				+ "?s1 ?r ?s2"
				+ "}";
		Query query = QueryFactory.create(queryString);
		try (QueryExecution qexec = QueryExecutionFactory.create(query, model)) {

			ResultSet results = qexec.execSelect();
			ResultSetFormatter.out(System.out, results, query);

			/*			while (results.hasNext())
			{
				QuerySolution soln = results.nextSolution() ;
				related.add(soln.get("s2"));
			}*/
		}
	}

	public Set<String> allLexicalForms() {

		Set<String> forms = new HashSet<>();
		String queryString = "SELECT DISTINCT ?lf WHERE {"
				+ "?w "+PRED_LEXICAL_FORM+" ?lf"
				+ " . FILTER (lang(?lf) = 'pt')"
				+ "}";
		Query query = QueryFactory.create(queryString);
		try (QueryExecution qexec = QueryExecutionFactory.create(query, model)) {

			ResultSet results = qexec.execSelect();
			//ResultSetFormatter.out(System.out, results, query);

			while (results.hasNext())
			{
				QuerySolution soln = results.nextSolution() ;
				forms.add(soln.getLiteral("lf").getLexicalForm().trim());
			}
		}

		return forms;
	}
	
	public void saveMapLemmasSenses(String path) {
		
		try {
			PrintStream out = new PrintStream(path);
			
			Set<String> lexicalForms = allLexicalForms();
			for(String l : lexicalForms) {
				List<RDFNode> synsets = synsetsWithWord(l, false);
				out.println(l+"\t"+synsets.size());
			}
			
			out.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
}
