/**
 *  Copyright (c) 2015, Carnegie Mellon University.  All Rights Reserved.
 */

import java.io.*;
import java.util.Vector;

/**
 *  The OR operator for all retrieval models.
 */
public class QrySopWand extends QrySop {

	/**
	 *  Indicates whether the query has a match.
	 *  @param r The retrieval model that determines what is a match
	 *  @return True if the query matches, otherwise false.
	 */
	public Vector<Double> weights = new Vector<Double>();
	
	public boolean docIteratorHasMatch (RetrievalModel r) {
		if(r instanceof RetrievalModelIndri){
			return this.docIteratorHasMatchMin(r);
		}
		else {
			throw new IllegalArgumentException
			(r.getClass().getName() + " doesn't support the WAND operator.");
		}
	}

	/**
	 *  Get a score for the document that docIteratorHasMatch matched.
	 *  @param r The retrieval model that determines how scores are calculated.
	 *  @return The document score.
	 *  @throws IOException Error accessing the Lucene index
	 */
	public double getScore (RetrievalModel r) throws IOException {

		if(r instanceof RetrievalModelIndri){
			return this.getScoreIndri (r);
		}
		else {
			throw new IllegalArgumentException
			(r.getClass().getName() + " doesn't support the AND operator.");
		}
	}




	private double getScoreIndri (RetrievalModel r) throws IOException {
		if(!this.docIteratorHasMatch(r)) {
			return 0.0;
		}else {
			int doc_id_current = this.docIteratorGetMatch();
			double score = 1;
			double temp = 0;
			double weightSum = 0;
			for(int i=0;i<this.weights.size();i++){
				weightSum = weightSum + this.weights.get(i);	
			}
			//weightSum = 1;
			for(int i=0;i<this.args.size();i++){
				double weight = this.weights.get(i);
				if(this.args.get(i).docIteratorHasMatch(r) && (doc_id_current==this.args.get(i).docIteratorGetMatch())){
					temp = this.args.get(i).getScore(r);
				}
				else{
					temp = ((QrySop)this.args.get(i)).getDefaultScoreIndri(r, doc_id_current);
				}
				score = score * Math.pow(temp, weight/weightSum);
			}
			return score;
		}
	}
	public double getDefaultScoreIndri (RetrievalModel r, int doc_id_current)
			throws IOException{
		double score = 1;
		double temp = 0;
		double weightSum = 0;
		for(int i=0;i<this.weights.size();i++){
			weightSum = weightSum + this.weights.get(i);	
		}
		//weightSum = 1;
		for(int i=0;i<this.args.size();i++){
			double weight = this.weights.get(i);
			temp = ((QrySop)this.args.get(i)).getDefaultScoreIndri(r, doc_id_current);
			score = score * Math.pow(temp, weight/weightSum);
		}
		return score;		
	}

}
