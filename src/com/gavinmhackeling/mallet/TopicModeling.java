package com.gavinmhackeling.mallet;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import com.gavinmhackeling.mallet.pipes.TokenSequence2FeatureSequenceWrapper;

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
import cc.mallet.types.Instance;
import cc.mallet.types.InstanceList;


public class TopicModeling  {

//	private static InstanceList instances;
//	private static InstanceList testing;
	private static final int NUM_THREADS = 4;
	private static final int numTopics = 100;

	private static List<String> getFileNames(String fileName) {
		System.out.println("Getting file names");
		List<String> files = new ArrayList<>();
		try
		{
			FileInputStream in = new FileInputStream(fileName);
			BufferedReader br = new BufferedReader(new InputStreamReader(in));
			String strLine;
			while((strLine = br.readLine())!= null) files.add(strLine);
		} catch (Exception e) {
			System.out.println(e);
		}
		return files;
	}

	private static List<Pipe> getPipeList()
	{
		System.out.println("Getting pipe list");
		ArrayList<Pipe> pipeList = new ArrayList<Pipe>();
		pipeList.add(new CharSequence2TokenSequence(Pattern.compile("\\S+")));
//		pipeList.add(new TokenSequence2FeatureSequenceWrapper());
		pipeList.add(new TokenSequence2FeatureSequence());
		return pipeList;
	}

	/**
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		long startTime = System.currentTimeMillis();

		List<Pipe> pipeList = getPipeList();
		InstanceList instances = new InstanceList (new SerialPipes(pipeList));
		List<String> fileNames = getFileNames(args[0]);
		
		ExecutorService executorServiceThreadPool = Executors.newFixedThreadPool(NUM_THREADS);
		Set<Future<InstanceList>> futures = new HashSet<>();
		for (String fileName: fileNames)
		{
			Callable<InstanceList> callable = new InstanceLoader(fileName);
			Future<InstanceList> future = executorServiceThreadPool.submit(callable);
			futures.add(future);
		}
		// This will make the executor accept no new threads
		// and finish all existing threads in the queue
		executorServiceThreadPool.shutdown();
		// Wait until all threads are finish
		executorServiceThreadPool.awaitTermination(8, TimeUnit.HOURS);
		System.out.println("All threads completed");
		System.out.println("Futures Size: " + futures.size());
		
		Iterator<Future<InstanceList>> it = futures.iterator(); 
		
		while (it.hasNext()) {
//			instances.addAll(it.next().get());
			System.out.println("num instances " + it.next().get().size());
		}
		
//		System.exit(0);
		
		// Create a model with 100 topics, alpha_t = 0.01, beta_w = 0.01
		//  Note that the first parameter is passed as the sum over topics, while
		//  the second is the parameter for a single dimension of the Dirichlet prior.
		System.out.println("Creating topic model");
		
		ParallelTopicModel model = new ParallelTopicModel(numTopics, 1.0, 0.01);
		model.addInstances(instances);

		// Use two parallel samplers, which each look at one half the corpus and combine
		//  statistics after every iteration.
		model.setNumThreads(4);

		// Run the model for 50 iterations and stop (this is for testing only, 
		//  for real applications, use 1000 to 2000 iterations)
		model.setNumIterations(50);
		model.estimate();
//		printUsage();

		// The data alphabet maps word IDs to strings
		Alphabet dataAlphabet = instances.getDataAlphabet();
//		printUsage();


		//		FeatureSequence tokens = (FeatureSequence) model.getData().get(0).instance.getData();
		//		LabelSequence topics = model.getData().get(0).topicSequence
		//		// Estimate the topic distribution of the first instance, 
		//		//  given the current Gibbs state.
		//		double[] topicDistribution = model.getTopicProbabilities(0);

		// Get an array of sorted sets of word ID/count pairs
		ArrayList<TreeSet<IDSorter>> topicSortedWords = model.getSortedWords();

		for (int i=0; i<topicSortedWords.size(); i++)
		{
			Iterator<IDSorter> iterator = topicSortedWords.get(i).iterator();
			int rank = 0;
			System.out.print("Topic: " + i + ": ");
			while (iterator.hasNext() && rank < 5) {
				IDSorter idCountPair = iterator.next();
				System.out.print(dataAlphabet.lookupObject(idCountPair.getID()) + " ");
				rank++;
			}
			System.out.println();
		}
		
		
		
		// Create a new instance with high probability of topic 0
//		StringBuilder topicZeroText = new StringBuilder();
//		Iterator<IDSorter> iterator = topicSortedWords.get(0).iterator();
//
//		int rank = 0;
//		while (iterator.hasNext() && rank < 5) {
//			IDSorter idCountPair = iterator.next();
//			topicZeroText.append(dataAlphabet.lookupObject(idCountPair.getID()) + " ");
//			rank++;
//		}


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

		private static void printUsage() 
		{
			System.out.println("Available processors (cores): " + Runtime.getRuntime().availableProcessors());
			/* Total amount of free memory available to the JVM */
			System.out.println("Free memory (bytes): " + Runtime.getRuntime().freeMemory());
			/* This will return Long.MAX_VALUE if there is no preset limit */
			long maxMemory = Runtime.getRuntime().maxMemory();
			/* Maximum amount of memory the JVM will attempt to use */
			System.out.println("Maximum memory (bytes): " + (maxMemory == Long.MAX_VALUE ? "no limit" : maxMemory));
			/* Total memory currently in use by the JVM */
			System.out.println("Total memory (bytes): " + Runtime.getRuntime().totalMemory() + "\n\n");	
		}
	//
	//	private static InstanceList addTestCases(String fileName) throws UnsupportedEncodingException, FileNotFoundException {
	//		InstanceList testing = new InstanceList(instances.getPipe());
	//		Reader fileReader = new InputStreamReader(new FileInputStream(new File(fileName)), "UTF-8");
	//		testing.addThruPipe(new CsvIterator(fileReader, Pattern.compile("^(.+)$"), 1, 1, 1));
	//		return testing;
	//	}
}

