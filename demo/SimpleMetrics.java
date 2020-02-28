package org.apache.lucene.demo;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.Directory;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.Term;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;

public final class SimpleMetrics {
    private double doc_freq;
    private double term_freq;


    public SimpleMetrics(double doc_freq, double term_freq) {
        this.doc_freq = doc_freq;
        this.term_freq = term_freq;
    }

    public SimpleMetrics(Term term) {
        try {
            final Path path = Paths.get("index");
            Directory index = FSDirectory.open(path);
            IndexReader reader = DirectoryReader.open(index);
            this.doc_freq = reader.docFreq(term);
            this.term_freq = reader.totalTermFreq(term);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public double getDoc_freq() {
        return doc_freq;
    }

    public double getTerm_freq() {
        return term_freq;
    }


    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Enter a Term: ");

        String term = scanner.nextLine();  // Read user input
        Term t = new Term("contents", term);
        SimpleMetrics simMet = new SimpleMetrics(t);
//        System.out.println(term);
//        System.out.println(simMet);
        System.out.println("The document frequency for term " + term + " is " + simMet.getDoc_freq());
        System.out.println("The term frequency for term " + term + " is " + simMet.getTerm_freq());
    }

}