package com.gavinmhackeling.mallet;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.regex.Pattern;

import cc.mallet.pipe.CharSequence2TokenSequence;
import cc.mallet.pipe.Pipe;
import cc.mallet.pipe.SerialPipes;
import cc.mallet.pipe.iterator.SimpleFileLineIterator;
import cc.mallet.types.InstanceList;

public class InstanceLoader implements Callable<InstanceList> 
{
	String fileName;

	public InstanceLoader(String fileName) 
	{
		this.fileName = fileName;
	}

	@Override
	public InstanceList call() throws Exception {
		ArrayList<Pipe> pipeList = new ArrayList<Pipe>();
		pipeList.add(new CharSequence2TokenSequence(Pattern.compile("\\S+")));
		
		System.out.println("Starting on file: " + fileName + " in thread: " + Thread.currentThread().getName());
		SimpleFileLineIterator simpleFileLineIterator = new SimpleFileLineIterator(new File(fileName));
		
		InstanceList instances = new InstanceList(new SerialPipes(pipeList));
		instances.addThruPipe(simpleFileLineIterator);
		return instances;
	}



}
