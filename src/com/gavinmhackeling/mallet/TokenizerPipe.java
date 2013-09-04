package com.gavinmhackeling.mallet;

import opennlp.tools.tokenize.Tokenizer;
import opennlp.tools.tokenize.WhitespaceTokenizer;
import cc.mallet.pipe.Pipe;
import cc.mallet.types.Instance;
import cc.mallet.types.TokenSequence;

public class TokenizerPipe extends Pipe {

	private Tokenizer tokenizer;
	
	public TokenizerPipe() {
		
	}
	
	public Instance pipe(Instance carrier) {
		CharSequence charSequence = (CharSequence) carrier.getData();
		String[] tokens = WhitespaceTokenizer.INSTANCE.tokenize((String) charSequence);
		TokenSequence tokenSequence = new TokenSequence(tokens);
		carrier.setData(tokenSequence);
		return carrier;
	}

}
