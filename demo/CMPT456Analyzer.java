/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.lucene.demo;


import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.LowerCaseFilter;
import org.apache.lucene.analysis.StopFilter;
import org.apache.lucene.analysis.StopwordAnalyzerBase;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.WordlistLoader;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.standard.StandardFilter;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.en.PorterStemFilter;
//import org.tartarus.snowball.ext.PorterStemmer;

/**
 * Filters {@link StandardTokenizer} with {@link StandardFilter}, {@link
 * LowerCaseFilter} and {@link StopFilter}, using a list of
 * English stop words.
 */
public final class CMPT456Analyzer extends StopwordAnalyzerBase {

    /** An unmodifiable set containing some common English words that are not usually useful
     for searching.*/
    public static final CharArraySet ENGLISH_STOP_WORDS_SET;

    static {
        final List<String> stopWords = Collections.emptyList();
        final CharArraySet stopSet = new CharArraySet(stopWords, false);
        ENGLISH_STOP_WORDS_SET = CharArraySet.unmodifiableSet(stopSet);
    }

    /** Default maximum allowed token length */
    public static final int DEFAULT_MAX_TOKEN_LENGTH = 255;

    private int maxTokenLength = DEFAULT_MAX_TOKEN_LENGTH;

    /** An unmodifiable set containing some common English words that are usually not
     useful for searching. */
    public static final CharArraySet STOP_WORDS_SET = ENGLISH_STOP_WORDS_SET;

    /** Builds an analyzer with the given stop words.
     * @param stopWords stop words */
    public CMPT456Analyzer(CharArraySet stopWords) {
        super(stopWords);
    }

    /** Builds an analyzer with the default stop words ({@link #STOP_WORDS_SET}).
     */
    public CMPT456Analyzer() {
        this(STOP_WORDS_SET);
    }

    /** Builds an analyzer with the stop words from the given reader.
     * @see WordlistLoader#getWordSet(Reader)
     * @param stopwords Reader to read stop words from */
    public CMPT456Analyzer(Reader stopwords) throws IOException {
        this(loadStopwordSet(stopwords));
    }

    /**
     * Set the max allowed token length.  Tokens larger than this will be chopped
     * up at this token length and emitted as multiple tokens.  If you need to
     * skip such large tokens, you could increase this max length, and then
     * use {@code LengthFilter} to remove long tokens.  The default is
     * {@link StandardAnalyzer#DEFAULT_MAX_TOKEN_LENGTH}.
     */
    public void setMaxTokenLength(int length) {
        maxTokenLength = length;
    }

    /** Returns the current maximum token length
     *
     *  @see #setMaxTokenLength */
    public int getMaxTokenLength() {
        return maxTokenLength;
    }

    @Override
    protected TokenStreamComponents createComponents(final String fieldName) {
        final StandardTokenizer src = new StandardTokenizer();
        src.setMaxTokenLength(maxTokenLength);
        TokenStream tok = new StandardFilter(src);
        tok = new LowerCaseFilter(tok);

        ArrayList<String> new_stop_words = new ArrayList<>();
//        System.out.println("new file");
//        File file = new File("lucene/core/src/java/org/apache/lucene/analysis/standard/stopwords.txt");
        InputStream inputStream = CMPT456Analyzer.class.getClassLoader().getResourceAsStream(
                "org/apache/lucene/demo/stopwords.txt");
        BufferedReader br = null;
//        System.out.println(inputStream);
        try {
            br = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }


        try {
            assert br != null;
            String st = br.readLine();
            while (st != null) {
                new_stop_words.add(st);
                st = br.readLine();
            }
        } catch (IOException e) {
            System.out.println(e);
        }

        CharArraySet new_stopSet = new CharArraySet(new_stop_words, false);
//        System.out.println(new_stop_words);
        tok = new StopFilter(tok, new_stopSet);

        tok = new PorterStemFilter(tok);

        return new TokenStreamComponents(src, tok) {
            @Override
            protected void setReader(final Reader reader) {
                // So that if maxTokenLength was changed, the change takes
                // effect next time tokenStream is called:
                src.setMaxTokenLength(CMPT456Analyzer.this.maxTokenLength);
                super.setReader(reader);
            }
        };
    }

    @Override
    protected TokenStream normalize(String fieldName, TokenStream in) {
        TokenStream result = new StandardFilter(in);
        result = new LowerCaseFilter(result);
        return result;
    }
}
