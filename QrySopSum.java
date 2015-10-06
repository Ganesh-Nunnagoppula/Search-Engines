/**
 *  Copyright (c) 2015, Carnegie Mellon University.  All Rights Reserved.
 */

import java.io.*;

/**
 *  The OR operator for all retrieval models.
 */
public class QrySopSum extends QrySop {

	/**
	 *  Indicates whether the query has a match.
	 *  @param r The retrieval model that determines what is a match
	 *  @return True if the query matches, otherwise false.
	 */
	public boolean docIteratorHasMatch (RetrievalModel r) {
		return this.docIteratorHasMatchMin (r);
	}
	
	public double getScore (RetrievalModel r) throws IOException {
		if (r instanceof RetrievalModelBM25) {
			return this.getScoreBM25 (r);
		}
		else{
			throw new IllegalArgumentException
			(r.getClass().getName() + " doesn't support the SUM operator.");
		}
	
	}
	private double getScoreBM25 (RetrievalModel r) throws IOException {
		if(!this.docIteratorHasMatch(r)) {
			return 0.0;
		}else{
			int doc_id_current = this.docIteratorGetMatch();
			double score = 0;
			double temp = 0;
			for(int i=0;i<this.args.size();i++){
				if(this.args.get(i).docIteratorHasMatch(r) && (doc_id_current==this.args.get(i).docIteratorGetMatch())){
					temp = this.args.get(i).getScore(r);
				}
				else{
					temp = 0;
				}
				score = score + temp;
			}
			return score;
		}
		
	}
	public double getDefaultScoreIndri (RetrievalModel r, int doc_id_current)
			throws IOException{
		return 0;	
	}
}