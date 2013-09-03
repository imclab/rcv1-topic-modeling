package com.gavinmhackeling.mallet;

import cc.mallet.pipe.Pipe;
import cc.mallet.types.Instance;
import cc.mallet.types.TokenSequence;

public class TokenizerPipe extends Pipe {

	public TokenizerPipe() {
		// TODO
	}
	
	public Instance pipe(Instance carrier) {
		carrier.getData();
		TokenSequence tokenSequence = new TokenSequence();
		carrier.setData(tokenSequence);
		return carrier;
	}

}
