/**
 *  Copyright (c) 2015, Carnegie Mellon University.  All Rights Reserved.
 */

import java.io.*;

/**
 *  The OR operator for all retrieval models.
 */
public class QrySopAnd extends QrySop {

	/**
	 *  Indicates whether the query has a match.
	 *  @param r The retrieval model that determines what is a match
	 *  @return True if the query matches, otherwise false.
	 */
	public boolean docIteratorHasMatch (RetrievalModel r) {
		if(r instanceof RetrievalModelIndri){
			return this.docIteratorHasMatchMin(r);
		}
		return this.docIteratorHasMatchAll(r);
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
			return this.getScoreRankedBoolean (r);
		}
		else if(r instanceof RetrievalModelIndri){
			return this.getScoreIndri (r);
		}
		else {
			throw new IllegalArgumentException
			(r.getClass().getName() + " doesn't support the AND operator.");
		}
	}

	/**
	 *  getScore for the UnrankedBoolean retrieval model.
	 *  @param r The retrieval model that determines how scores are calculated.
	 *  @return The document score.
	 *  @throws IOException Error accessing the Lucene index
	 */
	private double getScoreUnrankedBoolean (RetrievalModel r) throws IOException {
		if (! this.docIteratorHasMatchCache()) {
			return 0.0;
		} else {
			return 1.0;
		}
	}

	private double getScoreRankedBoolean (RetrievalModel r) throws IOException {
		if(!this.docIteratorHasMatch(r)) {
			return 0.0;
		} else {
			double min = 100000.0 ;
			int doc_id_current = this.docIteratorGetMatch();
			for(int i=0;i<this.args.size();i++)
			{
				if(this.args.get(i).docIteratorHasMatch(r) && (doc_id_current==this.args.get(i).docIteratorGetMatch())){
					if(this.args.get(i).getScore(r) < min)
					{
						min = this.args.get(i).getScore(r);
					}
				}
			}
			return min;
		}
	}
	private double getScoreIndri (RetrievalModel r) throws IOException {
		/*if(!this.docIteratorHasMatch(r)) {
			return 0.0;
		}else {*/
			int doc_id_current = this.docIteratorGetMatch();
			double score = 1;
			double temp = 0;
			for(int i=0;i<this.args.size();i++){
				if(this.args.get(i).docIteratorHasMatch(r) && (doc_id_current==this.args.get(i).docIteratorGetMatch())){
					temp = this.args.get(i).getScore(r);
				}
				else{
					temp = ((QrySop)this.args.get(i)).getDefaultScoreIndri(r, doc_id_current);
				}
				score = score * Math.pow(temp, 1/(double)args.size());
			}
			return score;
		//}
	}
	public double getDefaultScoreIndri (RetrievalModel r, int doc_id_current)
			throws IOException{
		double score = 1;
		double temp = 0;
		for(int i=0;i<this.args.size();i++){
			temp = ((QrySop)this.args.get(i)).getDefaultScoreIndri(r, doc_id_current);
			score = score * Math.pow(temp, 1/(double)args.size());
		}
		return score;		
	}

}
