package com.gavinmhackeling.mallet;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.TreeSet;
import java.util.regex.Pattern;

import cc.mallet.pipe.CharSequence2TokenSequence;
import cc.mallet.pipe.CharSequenceLowercase;
import cc.mallet.pipe.Pipe;
import cc.mallet.pipe.SerialPipes;
import cc.mallet.pipe.TokenSequence2FeatureSequence;
import cc.mallet.pipe.TokenSequenceRemoveStopwords;
import cc.mallet.pipe.iterator.CsvIterator;
import cc.mallet.topics.ParallelTopicModel;
import cc.mallet.topics.TopicInferencer;
import cc.mallet.types.Alphabet;
import cc.mallet.types.FeatureSequence;
import cc.mallet.types.IDSorter;
import cc.mallet.types.Instance;
import cc.mallet.types.InstanceList;
import cc.mallet.types.LabelSequence;


/**
 * @author gavin
 * TODO need to stem,
 */
public class IabTopicModeling  {

	private static ArrayList<Pipe> pipeList;
	private static int numTopics = 500;

	public static InstanceList getInstances() throws IOException {
		List<File> folders = new ArrayList<>();
		folders.add(new File("/home/gavin/PycharmProjects/rcv1-cleaning/data/documents"));
		folders.add(new File("/home/gavin/PycharmProjects/rcv1-cleaning/data/documents2"));
		folders.add(new File("/home/gavin/PycharmProjects/rcv1-cleaning/data/documents3"));
		folders.add(new File("/home/gavin/PycharmProjects/rcv1-cleaning/data/documents4"));
		folders.add(new File("/home/gavin/PycharmProjects/rcv1-cleaning/data/documents-test"));
		BufferedReader bufferedReader;
		InstanceList instances = new InstanceList (new SerialPipes(pipeList));
		for (File folder: folders) {
			for (final File fileName : folder.listFiles()) {
				String line;
				FileInputStream fileInputStream = new FileInputStream(fileName);
				bufferedReader = new BufferedReader(new InputStreamReader(fileInputStream, Charset.forName("UTF-8")));
				while ((line = bufferedReader.readLine()) != null)
					instances.addThruPipe(new Instance(line, 3, 2, 1));
				bufferedReader.close();
				fileInputStream.close();
			}
		}
		return instances;
	}

	public static void main(String[] args) throws Exception {
		// Pipes: lowercase, tokenize, remove stopwords, map to features
		pipeList = new ArrayList<Pipe>();
		pipeList.add( new CharSequenceLowercase() );
		pipeList.add( new CharSequence2TokenSequence(Pattern.compile("\\p{L}[\\p{L}\\p{P}]+\\p{L}")) );
		pipeList.add( new TokenSequenceRemoveStopwords(
				new File("/home/gavin/dev/mallet-topic-modeling-tutorial/stoplists/en.txt"), 
				"UTF-8", false, false, false) );
		pipeList.add( new TokenSequence2FeatureSequence() );

		InstanceList instances = getInstances();
		System.out.println("There are " + instances.size() + " instances");

		// Create a model with 100 topics, alpha_t = 0.01, beta_w = 0.01
		//  Note that the first parameter is passed as the sum over topics, while
		//  the second is the parameter for a single dimension of the Dirichlet prior.
		ParallelTopicModel model = new ParallelTopicModel(numTopics, 1.0, 0.01);
		model.addInstances(instances);
		// Use two parallel samplers, which each look at one half the corpus and combine
		//  statistics after every iteration.
		model.setNumThreads(2);
		// Run the model for 50 iterations and stop (this is for testing only, 
		//  for real applications, use 1000 to 2000 iterations)
		model.setNumIterations(50);
		model.estimate();

		// Show the words and topics in the first instance

		// The data alphabet maps word IDs to strings
		Alphabet dataAlphabet = instances.getDataAlphabet();

		FeatureSequence tokens = (FeatureSequence) model.getData().get(0).instance.getData();
		LabelSequence topics = model.getData().get(0).topicSequence;

		Formatter out = new Formatter(new StringBuilder(), Locale.US);
		for (int position = 0; position < tokens.getLength(); position++) {
			out.format("%s-%d ", dataAlphabet.lookupObject(tokens.getIndexAtPosition(position)), topics.getIndexAtPosition(position));
		}
		System.out.println(out);

		// Estimate the topic distribution of the first instance, 
		//  given the current Gibbs state.
		double[] topicDistribution = model.getTopicProbabilities(0);

		// Get an array of sorted sets of word ID/count pairs
		ArrayList<TreeSet<IDSorter>> topicSortedWords = model.getSortedWords();

		// Show top 5 words in topics with proportions for the first document
		for (int topic = 0; topic < numTopics; topic++) {
			Iterator<IDSorter> iterator = topicSortedWords.get(topic).iterator();

			out = new Formatter(new StringBuilder(), Locale.US);
			out.format("%d\t%.3f\t", topic, topicDistribution[topic]);
			int rank = 0;
			while (iterator.hasNext() && rank < 5) {
				IDSorter idCountPair = iterator.next();
				out.format("%s (%.0f) ", dataAlphabet.lookupObject(idCountPair.getID()), idCountPair.getWeight());
				rank++;
			}
			System.out.println(out);
		}
		//
		//		// Create a new instance with high probability of topic 0
		//		StringBuilder topicZeroText = new StringBuilder();
		//		Iterator<IDSorter> iterator = topicSortedWords.get(0).iterator();
		//
		//		int rank = 0;
		//		while (iterator.hasNext() && rank < 5) {
		//			IDSorter idCountPair = iterator.next();
		//			topicZeroText.append(dataAlphabet.lookupObject(idCountPair.getID()) + " ");
		//			rank++;
		//		}

		//		System.out.println("Writing model to file");
		//		model.write(new File("/home/gavin/PycharmProjects/rcv1-cleaning/data/models/model1"));

		// Create a new instance named "test instance" with empty target and source fields.
		InstanceList testing = new InstanceList(instances.getPipe());
		testing.addThruPipe(new Instance("US officials hope that any military assault on Syria will be surgical and limited. But what does the US do after the missiles or bombs have fallen?  It could go either way. The US may attack - or may not. 'I've not made a decision,' US President Barack Obama said on Wednesday.  Mr Obama has maintained that if the Syrian government uses chemical weapons, the US will act militarily.  And last week, according to US officials, President Bashar al-Assad's forces deployed poison gas against rebels in a Damascus suburb. More than 1,000 people, including women and children, were reported killed.  Syrian government officials say they did not use chemical weapons, but the US is ready to act.  'We are prepared' Jet fighters were lined up for Operation Desert Fox in Iraq in 1998 Operation Desert Fox in Iraq was 'short' - similar to an operation planned in Syria UN inspectors are looking for evidence of a gas attack in the Damascus suburb, and plan to finish their work on Friday.  Meanwhile officials in Washington DC are laying the groundwork for military operations.  'We are prepared,' Defence Secretary Chuck Hagel told the BBC.  If US officials proceed with military operations, they will likely be supported by Turkey and France, at least in some fashion. They will not have the backing of the UK, where Parliament on Thursday night rejected a government motion supporting intervention in Syria.  Nor is the UN Security Council expected to support an attack, because the Russians are opposed.  The US military would most likely use Tomahawk cruise missiles for an attack on the Syrian government forces. These missiles are now stored on destroyers in the eastern Mediterranean.  The missiles would not be fired at places where chemical weapons might be stored, since poisonous gas could spread or chemical agents could fall into the wrong hands.  Instead, military facilities would be targeted - radio centres, command posts and missile launchers, says Douglas Ollivant, who served as an operations officer with the Army's Fifth Cavalry Regiment in Iraq.  The initial military operation would be fast.", null, "test instance", null));

		System.out.println("Predictions:");
		TopicInferencer inferencer = model.getInferencer();
		double[] testProbabilities = inferencer.getSampledDistribution(testing.get(0), 10, 1, 5);
		System.out.println("0\t" + testProbabilities[0]);
	}
}

