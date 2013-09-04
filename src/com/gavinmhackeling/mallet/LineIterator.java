package com.gavinmhackeling.mallet;

import java.io.IOException;
import java.io.LineNumberReader;
import java.io.Reader;
import java.util.Iterator;

import cc.mallet.types.Instance;

public class LineIterator implements Iterator<Instance> {

	LineNumberReader reader;
	String currentLine;

	public LineIterator(Reader input)
	{
		this.reader = new LineNumberReader(input);
		try {
			this.currentLine = reader.readLine();
		} catch (IOException e) {
			throw new IllegalStateException ();
		}
	}
	// The PipeInputIterator interface

	public Instance next ()
	{	
		Instance carrier = new Instance(currentLine, "target", "uri", null);
		try {
			this.currentLine = reader.readLine();
		} catch (IOException e) {
			throw new IllegalStateException ();
		}
		return carrier;
	}

	public boolean hasNext() {return currentLine != null;}

	@Override
	public void remove() {
		// TODO Auto-generated method stub
	}

}
