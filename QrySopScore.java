/**
 *  Copyright (c) 2015, Carnegie Mellon University.  All Rights Reserved.
 */

import java.io.*;
import java.lang.IllegalArgumentException;

/**
 *  The SCORE operator for all retrieval models.
 */
public class QrySopScore extends QrySop {

	/**
	 *  Document-independent values that should be determined just once.
	 *  Some retrieval models have these, some don't.
	 */

	/**
	 *  Indicates whether the query has a match.
	 *  @param r The retrieval model that determines what is a match
	 *  @return True if the query matches, otherwise false.
	 */
	public boolean docIteratorHasMatch (RetrievalModel r) {
		return this.docIteratorHasMatchFirst (r);
	}

	/**
	 *  Get a score for the document that docIteratorHasMatch matched.
	 *  @param r The retrieval model that determines how scores are calculated.
	 *  @return The document score.
	 *  @throws IOException Error accessing the Lucene index
	 */
	public double getScore (RetrievalModel r) throws IOException {

		if (r instanceof RetrievalModelUnrankedBoolean) {
			return this.getScoreUnrankedBoolean (r);
		} 
		else if(r instanceof RetrievalModelRankedBoolean){
			return this.getScoreRankedBoolean(r);
		}
		else if(r instanceof RetrievalModelBM25){
			return this.getScoreBM25(r);
		}
		else {
			throw new IllegalArgumentException
			(r.getClass().getName() + " doesn't support the SCORE operator.");
		}
	}

	/**
	 *  getScore for the Unranked retrieval model.
	 *  @param r The retrieval model that determines how scores are calculated.
	 *  @return The document score.
	 *  @throws IOException Error accessing the Lucene index
	 */
	public double getScoreUnrankedBoolean (RetrievalModel r) throws IOException {
		if (! this.docIteratorHasMatchCache()) {
			return 0.0;
		} else {
			return 1.0;
		}
	}

	public double getScoreRankedBoolean (RetrievalModel r) throws IOException {
		if (! this.docIteratorHasMatchCache()) {
			return 0.0;
		} else {
			return(this.args.get(0).getScore(r));
		}
	}
	
	public double getScoreBM25 (RetrievalModel r) throws IOException {
		if (! this.docIteratorHasMatchCache()) {
			return 0.0;
		} else {
			int doc_id_current = this.docIteratorGetMatch();
			double termFreq = this.args.get(0).getScore(r);
			double N = Idx.getNumDocs();
			String temp = this.getArg(0).toString();
			//temp.substring(0, temp.indexOf('.'))
			double docFreq = this.getArg(0).getDf(); 
					//this.getArg(0).getDf();
			double docLength = Idx.getFieldLength(this.getArg(0).invertedList.field, doc_id_current);
			double avgLength = Idx.getSumOfFieldLengths(this.getArg(0).invertedList.field)/Idx.getDocCount(this.getArg(0).invertedList.field);
			double b = ((RetrievalModelBM25) r).getb();
			double k_1 = ((RetrievalModelBM25) r).getk_1();
			double k_3 = ((RetrievalModelBM25) r).getk_3();
			double rsjWeight = Math.log((N-docFreq+0.5)/(docFreq+0.5));
			double tfWeight = termFreq/(termFreq + k_1*((1-b) + (b*docLength)/avgLength));
			return(rsjWeight*tfWeight);
		}
	}
	

	/**
	 *  Initialize the query operator (and its arguments), including any
	 *  internal iterators.  If the query operator is of type QryIop, it
	 *  is fully evaluated, and the results are stored in an internal
	 *  inverted list that may be accessed via the internal iterator.
	 *  @param r A retrieval model that guides initialization
	 *  @throws IOException Error accessing the Lucene index.
	 */
	public void initialize (RetrievalModel r) throws IOException {

		Qry q = this.args.get (0);
		q.initialize (r);
	}

}
