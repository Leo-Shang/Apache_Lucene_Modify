package org.apache.lucene.demo;

import org.apache.lucene.search.similarities.ClassicSimilarity;

import java.lang.*;

public final class CMPT456Similarity extends ClassicSimilarity {

    public CMPT456Similarity() {
        super();
    }

    @Override
    public float idf(long docFreq, long docCount) {
        return 1 + (float)Math.log((docCount + 2) / (docFreq + 2));
    }

    @Override
    public float tf(float freq) {
        float base = 1 + freq;
        return (float) Math.pow(base, 0.5);
    }


}