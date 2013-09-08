package com.gavinmhackeling.mallet.pipes;

import cc.mallet.pipe.TokenSequence2FeatureSequence;
import cc.mallet.types.Alphabet;
import cc.mallet.types.FeatureSequence;
import cc.mallet.types.Instance;
import cc.mallet.types.TokenSequence;

public class TokenSequence2FeatureSequenceWrapper extends TokenSequence2FeatureSequence 
{
	@Override
	public Instance pipe(Instance carrier)
	{
		TokenSequence ts = (TokenSequence) carrier.getData();
		FeatureSequence ret = new FeatureSequence((Alphabet) getDataAlphabet(), ts.size());
		for (int i = 0; i < ts.size(); i++) {
			ret.add (ts.get(i).getText());
			System.out.println("adding: " + ts.get(i).getText());
		}
		carrier.setData(ret);
		return carrier;
	}
	
	
}
