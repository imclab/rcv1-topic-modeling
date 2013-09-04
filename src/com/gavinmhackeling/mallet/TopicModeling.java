package com.gavinmhackeling.mallet;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.TreeSet;
import java.util.regex.Pattern;

import cc.mallet.pipe.CharSequence2TokenSequence;
import cc.mallet.pipe.CharSequenceLowercase;
import cc.mallet.pipe.Pipe;
import cc.mallet.pipe.SerialPipes;
import cc.mallet.pipe.TokenSequence2FeatureSequence;
import cc.mallet.pipe.TokenSequenceRemoveStopwords;
import cc.mallet.pipe.iterator.CsvIterator;
import cc.mallet.pipe.iterator.SimpleFileLineIterator;
import cc.mallet.topics.ParallelTopicModel;
import cc.mallet.topics.TopicInferencer;
import cc.mallet.types.Alphabet;
import cc.mallet.types.IDSorter;
import cc.mallet.types.InstanceList;


public class TopicModeling  {
	
	private static InstanceList instances;
	private static InstanceList testing;

	/**
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		long startTime = System.currentTimeMillis();
		
		// Begin by importing documents from text to feature sequences
		ArrayList<Pipe> pipeList = new ArrayList<Pipe>();

		// Pipes: lowercase, tokenize, remove stopwords, map to features
		pipeList.add(new CharSequenceLowercase() );
		// letter one or more letter or punctuation letter
//		pipeList.add(new CharSequence2TokenSequence(Pattern.compile("\\S+")) );
		pipeList.add(new TokenizerPipe());
		pipeList.add(new TokenSequenceRemoveStopwords(new File("stoplists/en.txt"), "UTF-8", false, false, false) );
		pipeList.add( new TokenSequence2FeatureSequence() );

		instances = new InstanceList (new SerialPipes(pipeList));

		Reader fileReader = new InputStreamReader(new FileInputStream(new File(args[0])), "UTF-8");
//		instances.addThruPipe(new CsvIterator(fileReader, Pattern.compile("^(\\S*)[\\s,]*(\\S*)[\\s,]*(.*)$"), 3, 2, 1)); // data, label, name fields
		System.out.println("Constructing csvIterator");
//		CsvIterator csvIterator = new CsvIterator(fileReader, Pattern.compile("^(.*)$"), 1, 1, 1);
//		LineIterator lineIterator = new LineIterator(fileReader);
		SimpleFileLineIterator simpleFileLineIterator = new SimpleFileLineIterator(new File(args[0]));
		System.out.println("Preparing to add through pipe");
//		instances.addThruPipe(csvIterator);
//		instances.addThruPipe(lineIterator);
		instances.addThruPipe(simpleFileLineIterator);

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

		// The data alphabet maps word IDs to strings
		Alphabet dataAlphabet = instances.getDataAlphabet();

//		FeatureSequence tokens = (FeatureSequence) model.getData().get(0).instance.getData();
//		LabelSequence topics = model.getData().get(0).topicSequence
//		// Estimate the topic distribution of the first instance, 
//		//  given the current Gibbs state.
//		double[] topicDistribution = model.getTopicProbabilities(0);

		// Get an array of sorted sets of word ID/count pairs
		ArrayList<TreeSet<IDSorter>> topicSortedWords = model.getSortedWords();

		// Create a new instance with high probability of topic 0
		StringBuilder topicZeroText = new StringBuilder();
		Iterator<IDSorter> iterator = topicSortedWords.get(0).iterator();

		int rank = 0;
		while (iterator.hasNext() && rank < 5) {
			IDSorter idCountPair = iterator.next();
			topicZeroText.append(dataAlphabet.lookupObject(idCountPair.getID()) + " ");
			rank++;
		}

		
//		/*
//		 * predict
//		 */
//		// Create a new instance named "test instance" with empty target and source fields.
//		System.out.println("loading testing instances");
//		InstanceList testInstances = addTestCases(args[1]);
//		
//		TopicInferencer inferencer = model.getInferencer();
//		for (int i=0; i<testInstances.size(); i++) {
//			System.out.println("Test case: " + i);
//			double[] testProbabilities = inferencer.getSampledDistribution(testInstances.get(i), 10, 1, 5);
//			for (int j=0; j<testProbabilities.length; j++) {
//				if (testProbabilities[j] > 0.1) {
//					System.out.println(j + "\t" + testProbabilities[j]);
//					Iterator<IDSorter> iterator1 = topicSortedWords.get(j).iterator();
//					int count = 0;
//					while (iterator1.hasNext() && count < 10) {
//						IDSorter idCountPair = iterator1.next();
//						System.out.print(dataAlphabet.lookupObject(idCountPair.getID()) + " ");
//						count++;
//					}
//					System.out.println("\n");
//				}
//			}
//			System.out.println("\n");
//		}
		
		long duration = System.currentTimeMillis()-startTime;
		System.out.println("Completed in " + duration + " milliseconds");

	}

	private static InstanceList addTestCases(String fileName) throws UnsupportedEncodingException, FileNotFoundException {
		InstanceList testing = new InstanceList(instances.getPipe());
		Reader fileReader = new InputStreamReader(new FileInputStream(new File(fileName)), "UTF-8");
		testing.addThruPipe(new CsvIterator(fileReader, Pattern.compile("^(.+)$"), 1, 1, 1));
		return testing;
	}
}

