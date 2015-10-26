/**
 *  Copyright (c) 2015, Carnegie Mellon University.  All Rights Reserved.
 */
import java.io.*;
import java.util.*;

/**
 *  The Window operator for all retrieval models.
 */


public class QryIopWindow extends QryIop {

	/**
	 *  Evaluate the query operator; the result is an internal inverted
	 *  list that may be accessed via the internal iterators.
	 *  @throws IOException Error accessing the Lucene index.
	 *  
	 */
	private int n;
	QryIopWindow(int n){
		this.n = n;
	}
	
	private Vector<Integer> findDocIntersections(){
		Vector<Integer> docIntersections = new Vector<Integer>();
		while(this.docIteratorHasMatchAll(null))
		{
			int docid = this.getArg(0).docIteratorGetMatch ();
			docIntersections.add(docid);
			for(Qry q_i: this.args)
			{
				q_i.docIteratorAdvancePast(docid);
			}
			
		}
		for(Qry q_i: this.args){
			((QryIop)q_i).docIteratorStart();
		}
		return docIntersections;	
	}
	private Vector<Integer> findLocations(int n){
		Vector<Integer> locations = new Vector<Integer>();
		while(true){
			int minValue = Integer.MAX_VALUE;
			int maxValue = Integer.MIN_VALUE;
			Qry minIndex = null;
			for(Qry q_i: this.args){
				if(!((QryIop)q_i).locIteratorHasMatch()){
					return locations;
				}
				else{
					if(((QryIop)q_i).locIteratorGetMatch()<minValue){
						minValue = ((QryIop)q_i).locIteratorGetMatch();
						minIndex = q_i;
					}
					if(((QryIop)q_i).locIteratorGetMatch()>maxValue){
						maxValue = ((QryIop)q_i).locIteratorGetMatch();
					}
				}	
			}
			if(1+maxValue-minValue>n){
				((QryIop)minIndex).locIteratorAdvance();
			}
			else{
				locations.add(maxValue);
				for(Qry q_j: this.args){
					((QryIop)q_j).locIteratorAdvance();
				}
			}
		}	
	}
	
	private int advanceAllArgs(int curDocId){
		for(Qry q_i: this.args){
		
			q_i.docIteratorAdvanceTo(curDocId);
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
		

		/*int curDocId = this.getArg(0).docIteratorGetMatch() ;
		if(curDocId == Qry.INVALID_DOCID){
			return ;
		}*/
		// initialize the argument indexes
		Vector<Integer> docIntersections = findDocIntersections();
		while (true) {
			
			for(int i=0;i<docIntersections.size();i++)
			{
				if(advanceAllArgs(docIntersections.get(i))==-1){
					return;
				}
				else{
					List<Integer> positions = new ArrayList<Integer>();
					Vector<Integer> locations = new Vector<Integer>();
					locations = findLocations(n);
					if(locations.size()>0){
						positions.addAll(locations);
						Collections.sort (positions);
						this.invertedList.appendPosting (docIntersections.get(i), positions);
					}		
				}	
			}
			return;
		}
	}

	
}
