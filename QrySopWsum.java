/**
 *  Copyright (c) 2015, Carnegie Mellon University.  All Rights Reserved.
 */

import java.io.*;

/**
 *  The OR operator for all retrieval models.
 */
public class QrySopWsum extends QrySop {

	/**
	 *  Indicates whether the query has a match.
	 *  @param r The retrieval model that determines what is a match
	 *  @return True if the query matches, otherwise false.
	 */
	public boolean docIteratorHasMatch (RetrievalModel r) {
		return this.docIteratorHasMatchMin (r);
	}
	
	public double getScore (RetrievalModel r) throws IOException {
		if (r instanceof RetrievalModelIndri) {
			return this.getScoreIndri (r);
		}
		else{
			throw new IllegalArgumentException
			(r.getClass().getName() + " doesn't support the SUM operator.");
		}
	
	}
	private double getScoreIndri (RetrievalModel r) throws IOException {
		if(!this.docIteratorHasMatch(r)) {
			return 0.0;
		}else{
			int doc_id_current = this.docIteratorGetMatch();
			double score = 0;
			double temp = 0;
			for(int i=0;i<this.args.size();i=i+2){
				double weight = ((QrySopScore)this.args.get(i)).getScoreConstant(r);
				if(this.args.get(i+1).docIteratorHasMatch(r) && (doc_id_current==this.args.get(i+1).docIteratorGetMatch())){
					temp = this.args.get(i+1).getScore(r);
				}
				else{
					temp = ((QrySop)this.args.get(i+1)).getDefaultScoreIndri(r, doc_id_current);
				}
				score = score + weight*temp;
			}
			return score;
		}
		
	}
	public double getDefaultScoreIndri (RetrievalModel r, int doc_id_current)
			throws IOException{
		return 0;	
	}
}