# Apache_Lucene_Modify

## Note

	This project is some modification towards Lucene library in Java. Due to the concern about the size of Lucene library, this repository only includes the modification files. The codebase can be forked from https://github.com/apache/lucene-solr.

## Environment

	1. Install Java 8
	2. Clone Lucene from https://github.com/apache/lucene-solr
	3. Replace the original "demo" folder with the "demo" folder in my repository
	4. Install Docker

## Instructions to Run

	1. docker build -t cmpt456-lucene-solr:6.6.7 .
	2. docker run -v ~/IdeaProjects/A2/src/lucene-solr:/lucene-solr/ -it cmpt456-lucene-solr:6.6.7
	3. ant -f lucene/demo/build.xml
	4. ant -f lucene/demo/build.xml -Ddocs=lucene/demo/data/wiki-small/en/articles/ run-indexing-demo
	5. ant -f lucene/demo/build.xml run-search-index-demo
