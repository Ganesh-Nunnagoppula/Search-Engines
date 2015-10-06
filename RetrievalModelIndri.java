/**
 *  Copyright (c) 2015, Carnegie Mellon University.  All Rights Reserved.
 */

/**
 *  An object that stores parameters for the ranked Boolean
 *  retrieval model (there are none) and indicates to the query
 *  operators how the query should be evaluated.
 */
public class RetrievalModelIndri extends RetrievalModel {
	
	private double mu,lambda;
	RetrievalModelIndri(double mu,double lambda){
		this.mu = mu;
		this.lambda = lambda;
	}
	public double getmu(){
		return this.mu;
	}
	public double getlambda(){
		return this.lambda;
	}

	
	
	public String defaultQrySopName () {
		return new String ("#and");
	}

}
