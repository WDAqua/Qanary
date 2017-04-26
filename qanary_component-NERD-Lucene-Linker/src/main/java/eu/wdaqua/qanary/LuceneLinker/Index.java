package eu.wdaqua.qanary.LuceneLinker;

import org.apache.jena.graph.Triple;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.lang.PipedRDFIterator;
import org.apache.jena.riot.lang.PipedRDFStream;
import org.apache.jena.riot.lang.PipedTriplesStream;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.util.CharArraySet;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.FieldInvertState;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.highlight.InvalidTokenOffsetsException;
import org.apache.lucene.search.similarities.TFIDFSimilarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Index {
    private static final Logger logger = LoggerFactory.getLogger(LuceneLinker.class);
    private static final Analyzer analyzer = new EnglishAnalyzer(CharArraySet.EMPTY_SET);
    private static Directory index;
    private static String dump;

    public Index(String d) throws IOException {
        //dump=d;
        dump = "/tmp/dump.nt";
        index = FSDirectory.open(Paths.get("/tmp/lucene"));
        if (!DirectoryReader.indexExists(index)) {
            index();
        }
    }


    private static void index() throws IOException {
        //The index uses the same analyser as the query parser
        IndexWriterConfig config = new IndexWriterConfig(analyzer).setSimilarity(new CustomSimilarity());

        //Create an index for the instances
        IndexWriter w_instances = new IndexWriter(index, config);
        PipedRDFIterator<Triple> iter = parse(dump);
        int count = 0;
        while (iter.hasNext()) {
            Triple next = iter.next();
            if (next.getPredicate().toString().equals("http://www.w3.org/2000/01/rdf-schema#label") || next.getPredicate().toString().equals("http://dbpedia.org/ontology/demonym")) {
                if (next.getObject().getLiteralLanguage().equals("en")) {
                    addDoc(w_instances, next.getSubject().toString(), next.getObject().getLiteralValue().toString());
                }
            }
            count++;
            if (count % 10000 == 0) {
                logger.info("Number {} ", count);
            }
        }
        w_instances.close();

    }

    private static void addDoc(IndexWriter w, String resource, String lexicalization) throws IOException {
        Document doc = new Document();
        doc.add(new TextField("resource", resource, Field.Store.YES));
        doc.add(new TextField("lexicalization", lexicalization, Field.Store.YES));
        w.addDocument(doc);
    }

    public static List<String> query(String querystr) throws IOException, ParseException, InvalidTokenOffsetsException {
        List<String> result = new ArrayList<String>();
        // the "lexicalization" arg specifies the default field to use
        // when no field is explicitly specified in the query.

        Query q = new QueryParser("lexicalization", analyzer).parse(querystr);
        IndexReader reader = DirectoryReader.open(index);
        IndexSearcher searcher = new IndexSearcher(reader);

        searcher.setSimilarity(new CustomSimilarity());
        TopDocs docs_instances = searcher.search(q, 10);
        ScoreDoc[] hits = docs_instances.scoreDocs;

        //Search more if perfect match
        //if (hits.length>0 && hits[0].score==hits[hits.length-1].score){
        if (hits.length > 0 && compareStemmed(querystr, searcher.doc(hits[hits.length - 1].doc).get("lexicalization"))) {
            docs_instances = searcher.search(q, 20);
            hits = docs_instances.scoreDocs;
        }

        if (hits.length > 0 && compareStemmed(querystr, searcher.doc(hits[hits.length - 1].doc).get("lexicalization"))) {
            docs_instances = searcher.search(q, 60);
            hits = docs_instances.scoreDocs;
        }

        for (int i = 0; i < hits.length; ++i) {
            int docId = hits[i].doc;
            Document d = searcher.doc(docId);


            //System.out.println("Search " + querystr + " found " + d.get("resource") + "  Score: " + hits[i].score);

            //Look if the retrieved result matches exactly the searched
            if (compareStemmed(querystr, d.get("lexicalization"))) {
                result.add(d.get("resource"));
            } else {
                result.add("http://dbpedia.org/");
            }
        }
        reader.close();
        return result;
    }

    //Methods to parse rdf dunmps. It takes as input the location of the dump and returns an iterator over it
    private static PipedRDFIterator<Triple> parse(String dump) {
        PipedRDFIterator<Triple> iter = new PipedRDFIterator<Triple>();
        final String d = dump;
        final PipedRDFStream<Triple> inputStream = new PipedTriplesStream(iter);
        // PipedRDFStream and PipedRDFIterator need to be on different threads
        ExecutorService executor = Executors.newSingleThreadExecutor();
        // Create a runnable for our parser thread
        Runnable parser = new Runnable() {
            @Override
            public void run() {
                // Call the parsing process.
                RDFDataMgr.parse(inputStream, d);
            }
        };

        // Start the parser on another thread
        executor.submit(parser);
        return iter;
    }

    private static boolean compareStemmed(String s1, String s2) throws IOException {
        ArrayList<String> token1 = new ArrayList<String>();
        TokenStream tokenStream1 = analyzer.tokenStream(null, new StringReader(s1));
        tokenStream1.reset();
        while (tokenStream1.incrementToken()) {
            token1.add(tokenStream1.getAttribute(CharTermAttribute.class).toString());
        }
        tokenStream1.close();
        tokenStream1.end();

        ArrayList<String> token2 = new ArrayList<String>();
        TokenStream tokenStream2 = analyzer.tokenStream(null, new StringReader(s2));
        tokenStream2.reset();
        while (tokenStream2.incrementToken()) {
            token2.add(tokenStream1.getAttribute(CharTermAttribute.class).toString());
        }
        tokenStream2.close();
        tokenStream2.end();
        return token1.equals(token2);
    }

}

class CustomSimilarity extends TFIDFSimilarity {

    public CustomSimilarity() {
    }

    //Turns off tf (term frequency score). If "Lincon" is search the top ranked should not be "Lincon President Lincon"
    @Override
    public float tf(float freq) {
        //return (float)Math.sqrt(freq);
        return 1;
    }

    //The following is to give higher weight to documents hows length is near to the length of the searched string
    //Since length norms are generally going to leave us with results less than one, multiply
    //by a sufficiently large number to not lose all our precision when casting to long
    private static final float NORM_ADJUSTMENT = Integer.MAX_VALUE;

    @Override
    public final long encodeNormValue(float f) {
        return (long) (f * NORM_ADJUSTMENT);
    }

    @Override
    public final float decodeNormValue(long norm) {
        return ((float) norm) / NORM_ADJUSTMENT;
    }

    //Leave this as standard
    @Override
    public float coord(int overlap, int maxOverlap) {
        return overlap / (float) maxOverlap;
    }

    @Override
    public float queryNorm(float sumOfSquaredWeights) {
        return (float) (1.0 / Math.sqrt(sumOfSquaredWeights));
    }

    @Override
    public float lengthNorm(FieldInvertState state) {
        return state.getBoost() * ((float) (1.0 / Math.sqrt(state.getOffset())));
    }

    @Override
    public float sloppyFreq(int distance) {
        return 1.0f / (distance + 1);
    }

    @Override
    public float scorePayload(int doc, int start, int end, BytesRef payload) {
        return 1;
    }

    @Override
    public float idf(long docFreq, long numDocs) {
        return (float) (Math.log(numDocs / (double) (docFreq + 1)) + 1.0);
    }

    @Override
    public String toString() {
        return "CustumSimilarity";
    }
}
