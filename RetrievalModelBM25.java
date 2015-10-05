/**
 *  Copyright (c) 2015, Carnegie Mellon University.  All Rights Reserved.
 */

/**
 *  An object that stores parameters for the ranked Boolean
 *  retrieval model (there are none) and indicates to the query
 *  operators how the query should be evaluated.
 */
public class RetrievalModelBM25 extends RetrievalModel {
	
	private double b,k_1,k_3;
	RetrievalModelBM25(double b,double k_1,double k_3){
		this.b = b;
		this.k_1 = k_1;
		this.k_3 = k_3;
	}
	public double getb(){
		return this.b;
	}
	public double getk_1(){
		return this.k_1;
	}
	public double getk_3(){
		return this.k_3;
	}
	
	
	public String defaultQrySopName () {
		return new String ("#sum");
	}

}
