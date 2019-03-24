import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Scanner;
import java.util.Set;

import org.deeplearning4j.models.embeddings.loader.WordVectorSerializer;
import org.deeplearning4j.models.paragraphvectors.ParagraphVectors;
import org.deeplearning4j.models.word2vec.VocabWord;
import org.deeplearning4j.models.word2vec.wordstore.inmemory.AbstractCache;
import org.deeplearning4j.text.documentiterator.LabelsSource;
import org.deeplearning4j.text.sentenceiterator.CollectionSentenceIterator;
import org.deeplearning4j.text.sentenceiterator.SentenceIterator;
import org.deeplearning4j.text.tokenization.tokenizer.DefaultTokenizer;
import org.deeplearning4j.text.tokenization.tokenizer.Tokenizer;
import org.deeplearning4j.text.tokenization.tokenizer.preprocessor.CommonPreprocessor;
import org.deeplearning4j.text.tokenization.tokenizerfactory.DefaultTokenizerFactory;
import org.deeplearning4j.text.tokenization.tokenizerfactory.TokenizerFactory;

public class NLI_OWNPT {

/*	private static String[] TEMPLATES_SYNONYM = {
			"sinónimos de <X>?",
			"outras palavras para <X>?",
			"mesmo significado que <X>?",
			"quer dizer o mesmo que <X>?",
			"o mesmo que <X>?",
	};*/

	static final String FILE_CONFIG = "config.properties";
	//opcoes
	static final String OP_OWNPT = "ownpt";
	static final String OP_WNEN = "wnen";
	static final String OP_TESTAR = "testar";
	static final String OP_FIRST_SENSE = "psent";
	static final String OP_PROP_TREINO = "ptreino";
	
	static final boolean DEFAULT_TESTAR = false;
	static final boolean DEFAULT_REGERA_FRASES = false;
	static final boolean DEFAULT_APENAS_FIRST_SENSE = false;
	static final double DEFAULT_PROP_TREINO = 0.1;

	static final String DIR_TEMPLATES = "templates";

	//Avaliação: treinar com parte (x%) dos dados (templates) e avaliar noutra parte (100-x%)
	static final String FILE_VECTORS = "vectors<?>.zip";
	static final String FILE_FRASES = "frases.txt";

	static final int PREVISOES = 3;
	
	private static String[] sources;
	private static boolean testar;
	private static boolean apenasFirstSense;
	private static double propTreino;
	
	public static void main(String args[]) {

		carregaOpcoes();
		
		QueryOWNPT ownpt = new QueryOWNPT(sources);
		Set<String> vocabulario = ownpt.allLexicalForms();

		boolean regeraFrases = DEFAULT_REGERA_FRASES || !(new File(FILE_FRASES).exists());
		List<FraseLabel> frases = regeraFrases ? geraFrases(DIR_TEMPLATES, ownpt) : carregaFrases(FILE_FRASES);
		System.out.println("\tHá "+frases.size()+" frases!");
		
		String fileVectors = FILE_VECTORS.replace("<?>", ("-"+propTreino).replace(".", ""));
		ParagraphVectors pvec = carregaVetores(fileVectors);

		if(pvec == null) {
			pvec = treina(frases.subList(0, (int)(frases.size()*propTreino)));
			guardaVetores(pvec, fileVectors);
		}

		if(testar)
		{
			testar(ownpt, vocabulario, frases, pvec);
		}
		else {
			Scanner keyboard = new Scanner(System.in);
			while(true)
			{
				System.out.println("Pergunta: ");
				String pergunta = keyboard.nextLine().trim();
				if(pergunta.equals("")) //pelo menos duas palavras
				{
					keyboard.close();
					System.exit(0);
				}
				else if(pergunta.length() > 2 && !pergunta.contains(" "))
				{
					System.out.println("Quantidade insuficiente de texto!");
					continue;
				}

				Set<String> respostas = respostas(ownpt, vocabulario, pvec, pergunta);
				if(respostas != null && respostas.isEmpty())
					System.out.println("Não tenho resposta :(");
				else
					System.out.println(respostas);
			}
		}
	}
	
	private static void carregaOpcoes(){
		
		testar = DEFAULT_TESTAR;
		apenasFirstSense = DEFAULT_APENAS_FIRST_SENSE;
		propTreino = DEFAULT_PROP_TREINO;
		String pathOWNPT = null;
		String pathWNEN = null;
		
		Reader reader = null;
		
		try {
			Properties props = new Properties();
 
			//InputStream inputStream = getClass().getClassLoader().getResourceAsStream(FILE_CONFIG);
			reader = new FileReader(FILE_CONFIG);
			props.load(reader);
			
			// get the property value and print it out
			File f1 = new File(props.getProperty(OP_OWNPT));
			if(f1.exists())
				pathOWNPT = f1.getAbsolutePath();
			else
				System.err.println("Ficheiro não encontrado: "+f1);
			File f2 = new File(props.getProperty(OP_WNEN));
			if(f2.exists())
				pathWNEN = f2.getAbsolutePath();
			else
				System.err.println("Ficheiro não encontrado: "+f2);
			
			testar = props.getProperty(OP_TESTAR).equals("1");
			apenasFirstSense = props.getProperty(OP_FIRST_SENSE).equals("1");
			propTreino = Double.parseDouble(props.getProperty(OP_PROP_TREINO));
			
			reader.close();
 
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		System.out.println("Testar ? "+testar);
		System.out.println("First sense only ? "+apenasFirstSense);
		System.out.println("Propoção de treino = "+propTreino);
		System.out.println("Ficheiros RDF = "+pathOWNPT+", "+pathWNEN);
		
		if(pathOWNPT == null || pathWNEN == null) {
			System.err.println("Sem localização de OpenWordNet-PT ou WordNet-EN! Verificar "+FILE_CONFIG);
			System.exit(0);
		}
		else {
			String[] sourcesTmp = {pathOWNPT, pathWNEN};
			sources = sourcesTmp;
		}
	}
	
	/*private static void carregaOpcoes(){
		
		testar = DEFAULT_TESTAR;
		apenasFirstSense = DEFAULT_APENAS_FIRST_SENSE;
		propTreino = DEFAULT_PROP_TREINO;
		String pathOWNPT = null;
		String pathWNEN = null;
		
		File config = new File(FILE_CONFIG);
		if(config.exists()) {
			
			try {
				BufferedReader reader = new BufferedReader(new FileReader(config));
				String line = null;
				
				while((line = reader.readLine()) != null) {
					
					if(line.startsWith("#"))
						continue;
					
					int i = line.indexOf("=")+1;
					if(line.startsWith(OP_OWNPT)) {
						File f = new File(line.substring(i));
						if(f.exists())
							pathOWNPT = f.getAbsolutePath();
						else
							System.err.println("Ficheiro não existente: "+f);
					}
					else if(line.startsWith(OP_WNEN)) {
						File f = new File(line.substring(i));
						if(f.exists())
							pathWNEN = f.getAbsolutePath();
						else
							System.err.println("Ficheiro não existente: "+f);
					}
					else if(line.startsWith(OP_TESTAR)) {
						testar = line.substring(i, i+1).equals("1");
					}
					else if(line.startsWith(OP_PROP_TREINO)) {
						propTreino = Double.parseDouble(line.substring(i));
					}
					else if(line.startsWith(OP_FIRST_SENSE)) {
						apenasFirstSense = line.substring(i, i+1).equals("1");
					}
				}
				
				reader.close();
				
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else {
			System.err.println("Ficheiro "+FILE_CONFIG+" não encontrado: usar opções default.");
		}
		
		System.out.println("Testar ? "+testar);
		System.out.println("First sense only ? "+apenasFirstSense);
		System.out.println("Propoção de treino = "+propTreino);
		System.out.println("Ficheiros RDF = "+pathOWNPT+", "+pathWNEN);
		
		if(pathOWNPT == null || pathWNEN == null) {
			System.err.println("Sem localização de OpenWordNet-PT ou WordNet-EN! Verificar config.txt");
			System.exit(0);
		}
		else {
			String[] sourcesTmp = {pathOWNPT, pathWNEN};
			sources = sourcesTmp;
		}
	}*/
	
	private static Collection<String> preveTipoRelacao(ParagraphVectors pvec, String pergunta){
		
		//verificar se modelo conhece pelo menos uma token
		Tokenizer tokenizer = pvec.getTokenizerFactory().create(pergunta);
		boolean inclui = false;
		while(tokenizer.hasMoreTokens())
			if(pvec.hasWord(tokenizer.nextToken())) {
				inclui = true;
				break;
			}
		
		if(inclui)
			return new ArrayList<String>(pvec.predictSeveral(pergunta, PREVISOES));
		
		return null;
	}

	private static String extraiAlvo(Collection<String> vocabulario, String pergunta) {

		//String[] tokens = pergunta.replaceAll("\\p{Punct}", "").trim().split(" ");
		String[] tokens = pergunta.replaceAll("[\\?!;,]+", "").trim().split("\\s+");
		//System.err.println(pergunta+" --> "+Arrays.asList(tokens));

		String alvo = null;
		int x = 1;
		while(!vocabulario.contains(alvo) && x < tokens.length) { 
			alvo = String.join(" ", Arrays.copyOfRange(tokens, x++, tokens.length));
		}
		return alvo;
	}

	private static Set<String> respostas(QueryOWNPT ownpt, Collection<String> vocabulario, ParagraphVectors pvec, String pergunta){
		
		//enviar pergunta em lower case?
		Collection<String> tipoRel = preveTipoRelacao(pvec, pergunta.toLowerCase());
		if(tipoRel == null) {
			System.out.println("Quantidade insuficiente de texto conhecido!");
			return null;
		}
		
		return respostas(ownpt, vocabulario, pergunta, tipoRel); 
	}
	
	private static Set<String> respostas(QueryOWNPT ownpt, Collection<String> vocabulario, String pergunta, Collection<String> tipoRel){
		
		String alvo = extraiAlvo(vocabulario, pergunta);
		if(alvo == null)
			return null;
		
		for(String tr : tipoRel) {
			Set<String> respostas = respostas(ownpt, tr, alvo);
			if(!respostas.isEmpty())
				return respostas;
		}
		
		return null;
	}

	private static Set<String> respostas(QueryOWNPT ownpt, String tipoRel, String alvo){

		//System.err.println(alvo);
		Set<String> respostas = new HashSet<>();
		if(tipoRel.equals(QueryOWNPT.KEY_ANTONYM)) {
			respostas.addAll(ownpt.antonymsOf(alvo));
		}

		/*			else if(tipoRel.equals(QueryOWNPT.KEY_PART)) {
			respostas.addAll(ownpt.partsOfWord(alvo, FIRST_SENSE));
		}
		else if(tipoRel.equals(QueryOWNPT.KEY_WHOLE)) {
			respostas.addAll(ownpt.wholesOfWord(alvo, FIRST_SENSE));
		}*/
		else if(tipoRel.equals(QueryOWNPT.KEY_CAUSES_X)) {
			respostas.addAll(ownpt.relatedToWord(alvo, QueryOWNPT.PRED_CAUSES, apenasFirstSense, true));
		}
		else if(tipoRel.equals(QueryOWNPT.KEY_CAUSES_Y)) {
			respostas.addAll(ownpt.relatedToWord(alvo, QueryOWNPT.PRED_CAUSES, apenasFirstSense, false));
		}
		else {
			String predTipoRel = uriTipoRelacao(tipoRel);
			respostas.addAll(ownpt.relatedToWord(alvo, predTipoRel, apenasFirstSense, false));
		}

		return respostas;
	}

	private static List<FraseLabel> carregaFrases(String path){

		System.out.println("A carregar frases...");
		List<FraseLabel> frases = new ArrayList<>();
		try {
			BufferedReader reader = new BufferedReader(new FileReader(path));
			String line = null;
			while((line = reader.readLine()) != null) {
				FraseLabel fl = FraseLabel.fromText(line);
				frases.add(fl);
			}
			reader.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return frases;
	}

	private static Map<String, Collection<String>> carregaTemplates(String dir){
		
		System.out.println("A carregar templates...");
		Map<String, Collection<String>> mapaTemplatesNomeRel = new HashMap<>();
		
		File templatesDir = new File(dir);
		
		if(templatesDir.exists()) {
			for(File f : templatesDir.listFiles()) {
				String fname = f.getName().substring(0, f.getName().indexOf("."));
				mapaTemplatesNomeRel.put(fname, new ArrayList<>());
				
				try {
					BufferedReader reader = new BufferedReader(new FileReader(f));
					
					String line = null;
					while((line = reader.readLine()) != null) {
						
						if(!line.isEmpty() && !line.startsWith("#") && (line.contains("<X>") || line.contains("<Y>"))) {
							mapaTemplatesNomeRel.get(fname).add(line.trim());
						}
					}
					
					reader.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		
		System.out.println("\ttemplates para: "+mapaTemplatesNomeRel.keySet());
		return mapaTemplatesNomeRel;
	}
	
	private static List<FraseLabel> geraFrases(String dirTemplates, QueryOWNPT ownpt){

/*		Map<String, Collection<String>> mapaTemplatesNomeRel = new HashMap<>();
		//mapaTemplatesNomeRel.put(QueryOWNPT.KEY_SYNONYM, new ArrayList<>());
		//mapaTemplatesNomeRel.put(QueryOWNPT.KEY_PART, new ArrayList<>());
		//mapaTemplatesNomeRel.put(QueryOWNPT.KEY_WHOLE, new ArrayList<>());

		mapaTemplatesNomeRel.put(QueryOWNPT.KEY_ANTONYM, new ArrayList<>());
		mapaTemplatesNomeRel.put(QueryOWNPT.PRED_HYPONYM, new ArrayList<>());
		mapaTemplatesNomeRel.put(QueryOWNPT.PRED_HYPERNYM, new ArrayList<>());
		mapaTemplatesNomeRel.put(QueryOWNPT.KEY_CAUSES_X, new ArrayList<>());
		mapaTemplatesNomeRel.put(QueryOWNPT.KEY_CAUSES_Y, new ArrayList<>());
		mapaTemplatesNomeRel.put(QueryOWNPT.PRED_ENTAILS, new ArrayList<>());
		mapaTemplatesNomeRel.put(QueryOWNPT.PRED_PART_HOLONYM, new ArrayList<>());
		mapaTemplatesNomeRel.put(QueryOWNPT.PRED_PART_MERONYM, new ArrayList<>());
		mapaTemplatesNomeRel.put(QueryOWNPT.PRED_MEMBER_HOLONYM, new ArrayList<>());
		mapaTemplatesNomeRel.put(QueryOWNPT.PRED_MEMBER_MERONYM, new ArrayList<>());
		mapaTemplatesNomeRel.put(QueryOWNPT.PRED_SUBSTANCE_HOLONYM, new ArrayList<>());
		mapaTemplatesNomeRel.put(QueryOWNPT.PRED_SUBSTANCE_MERONYM, new ArrayList<>());

		mapaTemplatesNomeRel.get(QueryOWNPT.KEY_ANTONYM).addAll(Arrays.asList(TEMPLATES_ANTONYM));
		mapaTemplatesNomeRel.get(QueryOWNPT.PRED_HYPONYM).addAll(Arrays.asList(TEMPLATES_HYPONYM));
		mapaTemplatesNomeRel.get(QueryOWNPT.PRED_HYPERNYM).addAll(Arrays.asList(TEMPLATES_HYPERNYM));
		mapaTemplatesNomeRel.get(QueryOWNPT.KEY_CAUSES_X).addAll(Arrays.asList(TEMPLATES_CAUSES_X));
		mapaTemplatesNomeRel.get(QueryOWNPT.KEY_CAUSES_Y).addAll(Arrays.asList(TEMPLATES_CAUSES_Y));
		mapaTemplatesNomeRel.get(QueryOWNPT.PRED_ENTAILS).addAll(Arrays.asList(TEMPLATES_ENTAILS_Y));

		mapaTemplatesNomeRel.get(QueryOWNPT.PRED_PART_HOLONYM).addAll(Arrays.asList(TEMPLATES_PART_HOLO));
		mapaTemplatesNomeRel.get(QueryOWNPT.PRED_PART_MERONYM).addAll(Arrays.asList(TEMPLATES_PART_MERO));
		mapaTemplatesNomeRel.get(QueryOWNPT.PRED_MEMBER_HOLONYM).addAll(Arrays.asList(TEMPLATES_MEMBER_HOLO));
		mapaTemplatesNomeRel.get(QueryOWNPT.PRED_MEMBER_MERONYM).addAll(Arrays.asList(TEMPLATES_MEMBER_MERO));
		mapaTemplatesNomeRel.get(QueryOWNPT.PRED_SUBSTANCE_HOLONYM).addAll(Arrays.asList(TEMPLATES_SUBSTANCE_HOLO));
		mapaTemplatesNomeRel.get(QueryOWNPT.PRED_SUBSTANCE_MERONYM).addAll(Arrays.asList(TEMPLATES_SUBSTANCE_MERO));*/

		/*		mapaTemplatesNomeRel.get(QueryOWNPT.KEY_PART).addAll(Arrays.asList(TEMPLATES_PART_HOLO));
		mapaTemplatesNomeRel.get(QueryOWNPT.KEY_PART).addAll(Arrays.asList(TEMPLATES_MEMBER_HOLO));
		mapaTemplatesNomeRel.get(QueryOWNPT.KEY_PART).addAll(Arrays.asList(TEMPLATES_SUBSTANCE_HOLO));
		mapaTemplatesNomeRel.get(QueryOWNPT.KEY_WHOLE).addAll(Arrays.asList(TEMPLATES_PART_MERO));
		mapaTemplatesNomeRel.get(QueryOWNPT.KEY_WHOLE).addAll(Arrays.asList(TEMPLATES_MEMBER_MERO));
		mapaTemplatesNomeRel.get(QueryOWNPT.KEY_WHOLE).addAll(Arrays.asList(TEMPLATES_SUBSTANCE_MERO));*/


		Map<String, Collection<String>> mapaTemplatesNomeRel = carregaTemplates(dirTemplates);
		System.out.println("A gerar frases...");
		
		Set<FraseLabel> frases = new HashSet<>(); //sem perguntas duplicadas
		for(String key : mapaTemplatesNomeRel.keySet()) {
			Set<FraseLabel> tmp = geraFrasesTipo(ownpt, key, mapaTemplatesNomeRel);
			System.out.println("\tfrases para "+key+": "+tmp.size());
			frases.addAll(tmp);
		}

		List<FraseLabel> listaFrases = new ArrayList<>(frases);
		Collections.shuffle(listaFrases);
		if(!listaFrases.isEmpty())
			guardaTextos(listaFrases);

		return listaFrases;
	}

	private static Set<FraseLabel> geraFrasesTipo(QueryOWNPT ownpt, String tipo, Map<String, Collection<String>> mapaTemplates)
	{
		Set<FraseLabel> frases = new HashSet<>();
		List<String[]> pares = paresRelacionados(ownpt, tipo);
		Collection<String> templates = mapaTemplates.get(tipo);

		for(String[] par : pares) {

			//String a1 = par[0].trim();
			//String a2 = par[1].trim();
			String a1 = par[0];
			String a2 = par[1];

			if(a1.startsWith(" ") || a2.startsWith(" ") || a1.endsWith(" ") || a2.endsWith(" ") 
					|| a1.contains("  ") || a2.contains("  ")) {
				continue;
			}

			for(String template : templates)
			{
				String per = null;
				String resp = null;
				if(template.contains("<X>")) {
					per = template.replace("<X>", a1);
					resp = a2;
				}
				else {
					per = template.replaceAll("<Y>", a2);
					resp = a1;
				}

				frases.add(new FraseLabel(tipo, per, resp));
				if(tipo.equals(QueryOWNPT.KEY_ANTONYM)) {
					frases.add(new FraseLabel(tipo, template.replace("<X>", a2), a1));
				}
			}
		}
		return frases;
	}
	
	private static String uriTipoRelacao(String tipoRel) {
		
		if(Character.isLowerCase(tipoRel.charAt(0)))
			return QueryOWNPT.TEMPLATE_PRED_RELATION.replace("?", tipoRel);
		
		return tipoRel;
	}

	private static List<String[]> paresRelacionados(QueryOWNPT ownpt, String tipo) {

		if(tipo.equals(QueryOWNPT.KEY_SYNONYM))
			return null;

		else if(tipo.equals(QueryOWNPT.KEY_ANTONYM))
			return ownpt.antonymPairs();

		else if(tipo.equals(QueryOWNPT.KEY_PART)) {
			List<String[]> lista = new ArrayList<>();

			lista.addAll(ownpt.wordPairsRelatedBy(QueryOWNPT.PRED_PART_HOLONYM));
			lista.addAll(ownpt.wordPairsRelatedBy(QueryOWNPT.PRED_MEMBER_HOLONYM));
			lista.addAll(ownpt.wordPairsRelatedBy(QueryOWNPT.PRED_SUBSTANCE_HOLONYM));
			return lista;
		}

		else if(tipo.equals(QueryOWNPT.KEY_WHOLE)) {
			List<String[]> lista = new ArrayList<>();

			lista.addAll(ownpt.wordPairsRelatedBy(QueryOWNPT.PRED_PART_MERONYM));
			lista.addAll(ownpt.wordPairsRelatedBy(QueryOWNPT.PRED_MEMBER_MERONYM));
			lista.addAll(ownpt.wordPairsRelatedBy(QueryOWNPT.PRED_SUBSTANCE_MERONYM));
			return lista;
		}

		else if(tipo.equals(QueryOWNPT.KEY_CAUSES_X)) {
			return ownpt.wordPairsRelatedBy(QueryOWNPT.PRED_CAUSES);
		}

		else if(tipo.equals(QueryOWNPT.KEY_CAUSES_Y)) {
			return new ArrayList<>(); //para não repetir os pares de CAUSES_X
		}
		else {
		
			String uriTipoRel = uriTipoRelacao(tipo);
			//System.err.println(tipo+" -> "+uriTipoRel);
			return ownpt.wordPairsRelatedBy(uriTipoRel);
		}
		
	}

	private static void guardaTextos(List<FraseLabel> frases) {

		try {
			PrintStream out = new PrintStream(FILE_FRASES);

			for(FraseLabel fl : frases)
				out.println(fl);

			out.close();

		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private static ParagraphVectors carregaVetores(String path) {

		System.out.println("A carregar vetores...");

		ParagraphVectors pvec = null;
		File file = new File(path);
		if(file.exists())
			try {
				pvec = WordVectorSerializer.readParagraphVectors(file);
				TokenizerFactory t = new DefaultTokenizerFactory();
				t.setTokenPreProcessor(new CommonPreprocessor());
				pvec.setTokenizerFactory(t);
				return pvec;

			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		return null;
	}

	private static void guardaVetores(ParagraphVectors pvec, String path) {

		System.out.println("Guardar modelo...");
		WordVectorSerializer.writeParagraphVectors(pvec, path);
		System.out.println("Modelo guardado...");
	}

	private static ParagraphVectors treina(List<FraseLabel> frasesLabel) {

		Collection<String> frases = new ArrayList<>();
		List<String> labels = new ArrayList<>();
		for(FraseLabel fl : frasesLabel) {
			frases.add(fl.pergunta);
			labels.add(fl.label);
		}

		SentenceIterator iter = new CollectionSentenceIterator(frases);
		LabelsSource lsource = new LabelsSource(labels);
		AbstractCache<VocabWord> cache = new AbstractCache<>();
		TokenizerFactory t = new DefaultTokenizerFactory();
		t.setTokenPreProcessor(new CommonPreprocessor());

		System.out.println("\tA treinar com "+frases.size()+" frases ...");
		ParagraphVectors pvec = new ParagraphVectors.Builder()
				.minWordFrequency(1)
				.iterations(5)
				.epochs(1)
				.layerSize(100)
				.learningRate(0.025)
				.labelsSource(lsource)
				.windowSize(5)
				.iterate(iter)
				.trainWordVectors(false)
				.vocabCache(cache)
				.tokenizerFactory(t)
				.sampling(0)
				.build();

		pvec.fit();
		System.out.println("Treino terminado!");


		return pvec;
	}
	
	private static void testar(QueryOWNPT ownpt, Collection<String> vocabulario, List<FraseLabel> frases, ParagraphVectors pvec) {
		
		System.out.println("Testar...");
		List<FraseLabel> teste = frases.subList((int)(frases.size()*propTreino), frases.size());
		int respostasCertas = 0;
		int tiposCertos = 0;
		Map<String,List<Integer>> mapaRespostas = new HashMap<>();
		
		for(FraseLabel fl : teste) {
			List<String> tipoRel = new ArrayList<String>(preveTipoRelacao(pvec, fl.pergunta));
			
			if(tipoRel.get(0).equals(fl.label)) {
				tiposCertos++;
			}
			/*else {
				System.err.println("Era="+fl.label+"; resposta="+tipoRel+" Pergunta="+fl.pergunta);
			}*/

			if(!mapaRespostas.containsKey(fl.label))
				mapaRespostas.put(fl.label, new ArrayList<>());
			
			Set<String> respostas = respostas(ownpt, vocabulario, fl.pergunta, tipoRel);
			if(respostas != null && respostas.contains(fl.resposta)) {
				respostasCertas++;
				mapaRespostas.get(fl.label).add(1);
			}
			//else if(tipoRel.contains(fl.label))
			//	System.err.println(fl.pergunta+" -> "+alvo+" -> "+respostas);
			else mapaRespostas.get(fl.label).add(0);

		}
		System.out.println("Tipos certos: "+tiposCertos+" -> "+ (double)tiposCertos/teste.size());
		System.out.println("Respostas certas: "+respostasCertas+" -> "+ (double)respostasCertas/teste.size());
		
		for(String label : mapaRespostas.keySet()) {
			
			float certas = contaCertas(mapaRespostas.get(label));
			float propCertas = certas / mapaRespostas.get(label).size();
			
			System.out.println("\t"+label+":\t"+certas+" ("+propCertas+")");
		}
	}
	
	
	private static int contaCertas(Collection<Integer> numeros) {
		/*int total = 0;
		for(Integer i : numeros)
			if(i > 0) total++;
		return total;*/
		
		return numeros.stream().mapToInt(Integer::intValue).sum();
	}
}

class FraseLabel {
	String label;
	String pergunta;
	String resposta;

	public FraseLabel(String l, String p, String r) {
		this.label = l;
		this.pergunta = p;
		this.resposta = r;
	}

	public String toString() {
		return label+"\t"+pergunta+"\t"+resposta;
	}

	public static FraseLabel fromText(String texto) {
		String[] cols = texto.split("\t");
		return new FraseLabel(cols[0], cols[1], cols[2]);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((label == null) ? 0 : label.hashCode());
		result = prime * result + ((pergunta == null) ? 0 : pergunta.hashCode());
		//result = prime * result + ((resposta == null) ? 0 : resposta.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		FraseLabel other = (FraseLabel) obj;
		if (label == null) {
			if (other.label != null)
				return false;
		} else if (!label.equals(other.label))
			return false;
		if (pergunta == null) {
			if (other.pergunta != null)
				return false;
		} else if (!pergunta.equals(other.pergunta))
			return false;
		/*		if (resposta == null) {
			if (other.resposta != null)
				return false;
		} else if (!resposta.equals(other.resposta))
			return false;*/
		return true;
	}

}
