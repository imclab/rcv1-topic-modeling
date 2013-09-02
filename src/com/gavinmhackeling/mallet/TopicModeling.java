package com.gavinmhackeling.mallet;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.Iterator;
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


public class TopicModeling  {

	public static void main(String[] args) throws Exception {

		// Begin by importing documents from text to feature sequences
		ArrayList<Pipe> pipeList = new ArrayList<Pipe>();

		// Pipes: lowercase, tokenize, remove stopwords, map to features
		pipeList.add( new CharSequenceLowercase() );
		pipeList.add( new CharSequence2TokenSequence(Pattern.compile("\\p{L}[\\p{L}\\p{P}]+\\p{L}")) );
		pipeList.add( new TokenSequenceRemoveStopwords(
				new File("/home/gavin/dev/mallet-topic-modeling-tutorial/stoplists/en.txt"), 
				"UTF-8", false, false, false) );
		pipeList.add( new TokenSequence2FeatureSequence() );

		InstanceList instances = new InstanceList (new SerialPipes(pipeList));

		Reader fileReader = new InputStreamReader(new FileInputStream(new File(args[0])), "UTF-8");
		instances.addThruPipe(new CsvIterator (fileReader, Pattern.compile("^(\\S*)[\\s,]*(\\S*)[\\s,]*(.*)$"),
				3, 2, 1)); // data, label, name fields

		// Create a model with 100 topics, alpha_t = 0.01, beta_w = 0.01
		//  Note that the first parameter is passed as the sum over topics, while
		//  the second is the parameter for a single dimension of the Dirichlet prior.
		int numTopics = 100;
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

//		Formatter out = new Formatter(new StringBuilder(), Locale.US);
//		for (int position = 0; position < tokens.getLength(); position++) {
//			out.format("%s-%d ", dataAlphabet.lookupObject(tokens.getIndexAtPosition(position)), topics.getIndexAtPosition(position));
//		}
//		System.out.println(out);

		// Estimate the topic distribution of the first instance, 
		//  given the current Gibbs state.
		double[] topicDistribution = model.getTopicProbabilities(0);

		// Get an array of sorted sets of word ID/count pairs
		ArrayList<TreeSet<IDSorter>> topicSortedWords = model.getSortedWords();

		// Show top 5 words in topics with proportions for the first document
//		for (int topic = 0; topic < numTopics; topic++) {
//			Iterator<IDSorter> iterator = topicSortedWords.get(topic).iterator();
//
//			out = new Formatter(new StringBuilder(), Locale.US);
//			out.format("%d\t%.3f\t", topic, topicDistribution[topic]);
//			int rank = 0;
//			while (iterator.hasNext() && rank < 5) {
//				IDSorter idCountPair = iterator.next();
//				out.format("%s (%.0f) ", dataAlphabet.lookupObject(idCountPair.getID()), idCountPair.getWeight());
//				rank++;
//			}
//			System.out.println(out);
//		}

		// Create a new instance with high probability of topic 0
		StringBuilder topicZeroText = new StringBuilder();
		Iterator<IDSorter> iterator = topicSortedWords.get(0).iterator();

		int rank = 0;
		while (iterator.hasNext() && rank < 5) {
			IDSorter idCountPair = iterator.next();
			topicZeroText.append(dataAlphabet.lookupObject(idCountPair.getID()) + " ");
			rank++;
		}


		


		/*
		 * predict
		 */
		// Create a new instance named "test instance" with empty target and source fields.
		String text = "The dollar rose in quiet European trading this morning, boosted by some large buy orders in a market thinned by a British bank holiday, foreign exchange dealers said. Gold rose slightly in Zurich. ``The market is really dead because London is absent,'' said one trader in Rome. In Tokyo, where trading ends as Europe's business day begins, the dollar gained sharply, closing up 0.70 yen at 134.20 yen. Later, in Europe, it was quoted at 134.25 yen. Foreign exchange dealers attributed the dollar's strong performance in Tokyo to loss-cutting and said orders concentrated there because other major markets were closed. Hong Kong's financial institutions were also closed today for a national holiday. ``Those who had sold the dollar at around 133.80 yen bought it back above the 134-yen level today,'' said a dealer at a U.S. bank's Tokyo office. ``That caused the dollar to gain sharply.'' Other dollar rates in Europe at midmorning, compared with late Friday's London rates: _1.8663 West German marks, up from 1.8565 _1.5760 Swiss francs, up from 1.5655 _6.3372 French francs, up from 6.3010 _2.1077 Dutch guilders, up from 2.0970 _1,386.25 Italian lire, down from 1,380.50 _1.2375 Canadian dollars, up from 1.23725 In Europe, the dollar made solid gains against the British pound. One pound cost $1.6855, compared with $1.7025 in London late Friday. The London bullion markets were closed for the holiday, but in Zurich the bid price for gold was $432.35, up slightly from $432.00 bid late Friday.";
		InstanceList testing = new InstanceList(instances.getPipe());
		testing.addThruPipe(new Instance(text, null, "test instance", null));
		TopicInferencer inferencer = model.getInferencer();
		double[] testProbabilities = inferencer.getSampledDistribution(testing.get(0), 10, 1, 5);
		
		for (int i=0; i<testProbabilities.length; i++) {
			if (testProbabilities[i] > 0.2) {
				System.out.println(i + "\t" + testProbabilities[i]);
				Iterator<IDSorter> iterator1 = topicSortedWords.get(i).iterator();
				int rank2 = 0;
				while (iterator1.hasNext() && rank2 < 5) {
					IDSorter idCountPair = iterator1.next();
					System.out.println(dataAlphabet.lookupObject(idCountPair.getID()));
					rank2++;
				}
			}
		}


	}
}

