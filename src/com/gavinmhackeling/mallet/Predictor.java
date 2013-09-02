package com.gavinmhackeling.mallet;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.security.AlgorithmConstraints;
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
import cc.mallet.topics.ParallelTopicModel;
import cc.mallet.topics.TopicInferencer;
import cc.mallet.types.Alphabet;
import cc.mallet.types.IDSorter;
import cc.mallet.types.Instance;
import cc.mallet.types.InstanceList;

public class Predictor {

	/**
	 * @param args
	 * @throws IOException 
	 * @throws FileNotFoundException 
	 * @throws ClassNotFoundException 
	 */
	public static void main(String[] args) throws FileNotFoundException, IOException, ClassNotFoundException {
		System.out.println("loading model");
		File file = new File("/home/gavin/PycharmProjects/rcv1-cleaning/data/models/model1");
		ObjectInputStream ois = new ObjectInputStream (new FileInputStream(file));
		ParallelTopicModel model = (ParallelTopicModel) ois.readObject();
		ois.close();

		System.out.println("preparing pipes");
		List<Pipe> pipeList = new ArrayList<Pipe>();
		pipeList.add( new CharSequenceLowercase() );
		pipeList.add( new CharSequence2TokenSequence(Pattern.compile("\\p{L}[\\p{L}\\p{P}]+\\p{L}")) );
		pipeList.add( new TokenSequenceRemoveStopwords(
				new File("/home/gavin/dev/mallet-topic-modeling-tutorial/stoplists/en.txt"), 
				"UTF-8", false, false, false) );
		pipeList.add( new TokenSequence2FeatureSequence() );

		InstanceList instances = new InstanceList (new SerialPipes(pipeList));
		InstanceList testing = new InstanceList(instances.getPipe());
		testing.addThruPipe(new Instance("paintball sports games", null, "test instance", null));

		Alphabet alphabet = model.getAlphabet();
		ArrayList<TreeSet<IDSorter>> topicSortedWords = model.getSortedWords();
		
		int numTopics = 500;
		Formatter out = new Formatter(new StringBuilder(), Locale.US);
		double[] topicDistribution = model.getTopicProbabilities(0);
		for (int topic = 0; topic < numTopics; topic++) {
			Iterator<IDSorter> iterator = topicSortedWords.get(topic).iterator();

			out = new Formatter(new StringBuilder(), Locale.US);
			out.format("%d\t%.3f\t", topic, topicDistribution[topic]);
			int rank = 0;
			while (iterator.hasNext() && rank < 5) {
				IDSorter idCountPair = iterator.next();
				out.format("%s (%.0f) ", alphabet.lookupObject(idCountPair.getID()), idCountPair.getWeight());
				rank++;
			}
			System.out.println(out);
		}
		System.out.println("\n\n");
		
		System.out.println("Predictions:");
		TopicInferencer inferencer = model.getInferencer();
		double[] testProbabilities = inferencer.getSampledDistribution(testing.get(0), 10, 1, 5);
		for (int i=0; i<testProbabilities.length; i++) {
			if (testProbabilities[i] > 0.2) {
				System.out.println(i + "\t" + testProbabilities[i]);
				Iterator<IDSorter> iterator = topicSortedWords.get(i).iterator();
				int rank = 0;
				while (iterator.hasNext() && rank < 5) {
					IDSorter idCountPair = iterator.next();
					System.out.println(alphabet.lookupObject(idCountPair.getID()));
					rank++;
				}
			}
		}

	}

}
