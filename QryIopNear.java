/**
 *  Copyright (c) 2015, Carnegie Mellon University.  All Rights Reserved.
 */
import java.io.*;
import java.util.*;

/**
 *  The Near operator for all retrieval models.
 */
public class QryIopNear extends QryIop {

	/**
	 *  Evaluate the query operator; the result is an internal inverted
	 *  list that may be accessed via the internal iterators.
	 *  @throws IOException Error accessing the Lucene index.
	 *  
	 */
	private int n;
	QryIopNear(int n){
		this.n = n;
	}
	private Vector<Integer> findNear(Vector<Integer>locations_p,Vector<Integer>locations_c,int n){
		Vector<Integer> locations = new Vector<Integer>();
		int i =0,j=0;
		while(i < locations_p.size() && j < locations_c.size()){
			if(-locations_p.get(i) + locations_c.get(j) <= n && -locations_p.get(i) + locations_c.get(j) > 0){
				if(!locations.contains(locations_c.get(j))){
				locations.add(locations_c.get(j));
				}
				i++;
				j++;
			}
			else if(-locations_p.get(i) + locations_c.get(j) < 0){
				j++;
			}
			else{
				i++;
				j=0;
			}	
		}

		return locations;
		
	}
	private int advanceAllArgs(int curDocId){
		for(Qry q_i: this.args){
			while(true){
			if (q_i.docIteratorHasMatch (null)) {
				int q_iDocid = q_i.docIteratorGetMatch ();
				if(q_iDocid == curDocId){
					break;
					//continue;
				}
				else if(q_iDocid > curDocId){
					return -1;
				}
				else if(q_iDocid==Qry.INVALID_DOCID){
					return -1;
				}
				else if(q_iDocid < curDocId){
					q_i.docIteratorAdvancePast(q_iDocid);
				}
			}
			else{
				return -1;
			}
			}
		}
		return 0;
	}
	protected void evaluate () throws IOException {

		//  Create an empty inverted list.  If there are no query arguments,
		//  that's the final result.

		this.invertedList = new InvList (this.getField());

		if (args.size () == 0) {
			return;
		}

		//  Each pass of the loop adds 1 document to result inverted list
		//  until all of the argument inverted lists are depleted.
		

		int curDocId = this.getArg(0).docIteratorGetMatch() ;
		if(curDocId == Qry.INVALID_DOCID){
			return ;
		}
		// initialize the argument indexes

		while (true) {

			//  Find the minimum next document id.  If there is none, we're done.

			

			//  Create a new posting that is the union of the posting lists
			//  that match the minDocid.  Save it.
			//  Note:  This implementation assumes that a location will not appear
			//  in two or more arguments.  #SYN (apple apple) would break it.

			List<Integer> positions = new ArrayList<Integer>();
			
			if(advanceAllArgs(curDocId) == -1)
			{
				this.getArg(0).docIteratorAdvancePast(curDocId);
				
				
			}
			else
			{
				Vector<Integer> locations = new Vector<Integer>();
				Qry q_p=this.getArg(0),q_c;
				Vector<Integer> locations_p =
						((QryIop) q_p).docIteratorGetMatchPosting().positions;
				for(int i=1; i<this.args.size(); i++){
					q_c = this.getArg(i);
					
					Vector<Integer> locations_c =
							((QryIop) q_c).docIteratorGetMatchPosting().positions;
					locations =findNear(locations_p,locations_c,n);
					q_p = q_c;
					locations_p = locations;
				}
				if(locations.size()>0){
					positions.addAll(locations);
					Collections.sort (positions);
					this.invertedList.appendPosting (curDocId, positions);
				}
			}
			
			if(curDocId == Qry.INVALID_DOCID){
				break;
			}
			this.getArg(0).docIteratorAdvancePast(curDocId);
			if(this.getArg(0).docIteratorHasMatch (null)){
				curDocId = this.getArg(0).docIteratorGetMatch();
			}
			else{
				break;
			}
		}
	}

}
