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
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Date;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.LongPoint;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.demo.CMPT456Analyzer;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;

import org.cyberneko.html.parsers.SAXParser;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;




/** Index all text files under a directory.
 * <p>
 * This is a command-line application demonstrating simple Lucene indexing.
 * Run it with no command-line arguments for usage information.
 */
public class HtmlIndexFiles {
  
  private HtmlIndexFiles() {}

  /** Index all text files under a directory. */
  public static void main(String[] args) {
    String usage = "java org.apache.lucene.demo.HtmlIndexFiles"
                 + " [-index INDEX_PATH] [-docs DOCS_PATH] [-update]\n\n"
                 + "This indexes the documents in DOCS_PATH, creating a Lucene index"
                 + "in INDEX_PATH that can be searched with SearchFiles";
    String indexPath = "index";
    String docsPath = null;
    boolean create = true;
    for(int i=0;i<args.length;i++) {
      if ("-index".equals(args[i])) {
        indexPath = args[i+1];
        i++;
      } else if ("-docs".equals(args[i])) {
        docsPath = args[i+1];
        i++;
      } else if ("-update".equals(args[i])) {
        create = false;
      }
    }

    if (docsPath == null) {
      System.err.println("Usage: " + usage);
      System.exit(1);
    }

    final Path docDir = Paths.get(docsPath);
    if (!Files.isReadable(docDir)) {
      System.out.println("Document directory '" +docDir.toAbsolutePath()+ "' does not exist or is not readable, please check the path");
      System.exit(1);
    }
    
    Date start = new Date();
    try {
      System.out.println("Indexing to directory '" + indexPath + "'...");

      Directory dir = FSDirectory.open(Paths.get(indexPath));
      Analyzer analyzer = new CMPT456Analyzer();
      IndexWriterConfig iwc = new IndexWriterConfig(analyzer);

      if (create) {
        // Create a new index in the directory, removing any
        // previously indexed documents:
        iwc.setOpenMode(OpenMode.CREATE);
      } else {
        // Add new documents to an existing index:
        iwc.setOpenMode(OpenMode.CREATE_OR_APPEND);
      }

      // Optional: for better indexing performance, if you
      // are indexing many documents, increase the RAM
      // buffer.  But if you do this, increase the max heap
      // size to the JVM (eg add -Xmx512m or -Xmx1g):
      //
      // iwc.setRAMBufferSizeMB(256.0);

      IndexWriter writer = new IndexWriter(dir, iwc);
      indexDocs(writer, docDir);

      // NOTE: if you want to maximize search performance,
      // you can optionally call forceMerge here.  This can be
      // a terribly costly operation, so generally it's only
      // worth it when your index is relatively static (ie
      // you're done adding documents to it):
      //
      // writer.forceMerge(1);

      writer.close();

      Date end = new Date();
      System.out.println(end.getTime() - start.getTime() + " total milliseconds");

    } catch (IOException e) {
      System.out.println(" caught a " + e.getClass() +
       "\n with message: " + e.getMessage());
    }
  }

  /**
   * Indexes the given file using the given writer, or if a directory is given,
   * recurses over files and directories found under the given directory.
   * 
   * NOTE: This method indexes one document per input file.  This is slow.  For good
   * throughput, put multiple documents into your input file(s).  An example of this is
   * in the benchmark module, which can create "line doc" files, one document per line,
   * using the
   * <a href="../../../../../contrib-benchmark/org/apache/lucene/benchmark/byTask/tasks/WriteLineDocTask.html"
   * >WriteLineDocTask</a>.
   *  
   * @param writer Writer to the index where the given file/dir info will be stored
   * @param path The file to index, or the directory to recurse into to find files to index
   * @throws IOException If there is a low-level I/O error
   */
  static void indexDocs(final IndexWriter writer, Path path) throws IOException {
    if (Files.isDirectory(path)) {
      Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
          try {
            indexDoc(writer, file, attrs.lastModifiedTime().toMillis());
          } catch (IOException ignore) {
            // don't index files that can't be read.
          }
          return FileVisitResult.CONTINUE;
        }
      });
    } else {
      indexDoc(writer, path, Files.getLastModifiedTime(path).toMillis());
    }
  }

  /** Indexes a single document */
  static void indexDoc(IndexWriter writer, Path file, long lastModified) throws IOException {
    try (InputStream stream = Files.newInputStream(file)) {
      // make a new, empty document
      Document doc = new Document();
      
      // Add the path of the file as a field named "path".  Use a
      // field that is indexed (i.e. searchable), but don't tokenize 
      // the field into separate words and don't index term frequency
      // or positional information:
      Field pathField = new StringField("path", file.toString(), Field.Store.YES);
      doc.add(pathField);
      
      // Add the last modified date of the file a field named "modified".
      // Use a LongPoint that is indexed (i.e. efficiently filterable with
      // PointRangeQuery).  This indexes to milli-second resolution, which
      // is often too fine.  You could instead create a number based on
      // year/month/day/hour/minutes/seconds, down the resolution you require.
      // For example the long value 2011021714 would mean
      // February 17, 2011, 2-3 PM.
      doc.add(new LongPoint("modified", lastModified));
      
      // Add the contents of the file to a field named "contents".  Specify a Reader,
      // so that the text of the file is tokenized and indexed, but not stored.
      // Note that FileReader expects the file to be in UTF-8 encoding.
      // If that's not the case searching for special characters will fail.
//      doc.add(new TextField("contents", new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))));


      Reader reader = new InputStreamReader(stream);
      Parser parser = null;
      try {
        parser = new Parser(reader);
        Field title = new StringField("title", parser.title.toString(), Field.Store.YES);
        doc.add(title);
        Field contents = new TextField("contents", parser.body.toString(), Field.Store.YES);
        doc.add(contents);
      } catch(SAXException e) {
        e.printStackTrace();
      }



      if (writer.getConfig().getOpenMode() == OpenMode.CREATE) {
        // New index, so we just add the document (no old document can be there):
        System.out.println("adding " + file);
        writer.addDocument(doc);
      } else {
        // Existing index (an old copy of this document may have been indexed) so 
        // we use updateDocument instead to replace the old one matching the exact 
        // path, if present:
        System.out.println("updating " + file);
        writer.updateDocument(new Term("path", file.toString()), doc);
      }
    }
  }

  /** The actual parser to read HTML documents */
  public static final class Parser {

    public final Properties metaTags = new Properties();
    public final String title, body;

    public Parser(Reader reader) throws IOException, SAXException {
      this(new InputSource(reader));
    }

    public Parser(InputSource source) throws IOException, SAXException {
      final SAXParser parser = new SAXParser();
      parser.setFeature("http://xml.org/sax/features/namespaces", true);
      parser.setFeature("http://cyberneko.org/html/features/balance-tags", true);
      parser.setFeature("http://cyberneko.org/html/features/report-errors", false);
      parser.setProperty("http://cyberneko.org/html/properties/names/elems", "lower");
      parser.setProperty("http://cyberneko.org/html/properties/names/attrs", "lower");

      final StringBuilder title = new StringBuilder(), body = new StringBuilder();
      final DefaultHandler handler = new DefaultHandler() {
        private int inBODY = 0, inHEAD = 0, inTITLE = 0, suppressed = 0;

        @Override
        public void startElement(String namespaceURI, String localName, String qName, Attributes atts) throws SAXException {
          if (inHEAD > 0) {
            if ("title".equals(localName)) {
              inTITLE++;
            } else {
              if ("meta".equals(localName)) {
                String name = atts.getValue("name");
                if (name == null) {
                  name = atts.getValue("http-equiv");
                }
                final String val = atts.getValue("content");
                if (name != null && val != null) {
                  metaTags.setProperty(name.toLowerCase(Locale.ROOT), val);
                }
              }
            }
          } else if (inBODY > 0) {
            if (SUPPRESS_ELEMENTS.contains(localName)) {
              suppressed++;
            } else if ("img".equals(localName)) {
              // the original javacc-based parser preserved <IMG alt="..."/>
              // attribute as body text in [] parenthesis:
              final String alt = atts.getValue("alt");
              if (alt != null) {
                body.append('[').append(alt).append(']');
              }
            }
          } else if ("body".equals(localName)) {
            inBODY++;
          } else if ("head".equals(localName)) {
            inHEAD++;
          } else if ("frameset".equals(localName)) {
            throw new SAXException("This parser does not support HTML framesets.");
          }
        }

        @Override
        public void endElement(String namespaceURI, String localName, String qName) throws SAXException {
          if (inBODY > 0) {
            if ("body".equals(localName)) {
              inBODY--;
            } else if (ENDLINE_ELEMENTS.contains(localName)) {
              body.append('\n');
            } else if (SUPPRESS_ELEMENTS.contains(localName)) {
              suppressed--;
            }
          } else if (inHEAD > 0) {
            if ("head".equals(localName)) {
              inHEAD--;
            } else if (inTITLE > 0 && "title".equals(localName)) {
              inTITLE--;
            }
          }
        }

        @Override
        public void characters(char[] ch, int start, int length) throws SAXException {
          if (inBODY > 0 && suppressed == 0) {
            body.append(ch, start, length);
          } else if (inTITLE > 0) {
            title.append(ch, start, length);
          }
        }

        @Override
        public InputSource resolveEntity(String publicId, String systemId) {
          // disable network access caused by DTDs
          return new InputSource(new StringReader(""));
        }
      };

      parser.setContentHandler(handler);
      parser.setErrorHandler(handler);
      parser.parse(source);

      // the javacc-based parser trimmed title (which should be done for HTML in all cases):
      this.title = title.toString().trim();

      // assign body text
      this.body = body.toString();
    }

    private static final Set<String> createElementNameSet(String... names) {
      return Collections.unmodifiableSet(new HashSet<>(Arrays.asList(names)));
    }

    /** HTML elements that cause a line break (they are block-elements) */
    static final Set<String> ENDLINE_ELEMENTS = createElementNameSet(
            "p", "h1", "h2", "h3", "h4", "h5", "h6", "div", "ul", "ol", "dl",
            "pre", "hr", "blockquote", "address", "fieldset", "table", "form",
            "noscript", "li", "dt", "dd", "noframes", "br", "tr", "select", "option"
    );

    /** HTML elements with contents that are ignored */
    static final Set<String> SUPPRESS_ELEMENTS = createElementNameSet(
            "style", "script"
    );
  }
}
