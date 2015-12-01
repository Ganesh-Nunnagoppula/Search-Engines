import java.io.*;
import java.util.*;



public class FeatureList {
	
	public class FeatureListEntry{
		public double relScore;
		public String qid;
		public double[] featureVector;
		public String extDocId;
	
		
		private FeatureListEntry(double relScore, String qid,double[] featureVector, String extDocId){
			this.relScore = relScore;
			this.qid = qid;
			this.featureVector = featureVector;
			this.extDocId = extDocId;
			
		}
	}
	public List<FeatureListEntry> featLists = new ArrayList<FeatureListEntry>();
	
	public void add(double relScore, String qid,double[] featureVector, String extDocId) {
		featLists.add(new FeatureListEntry(relScore, qid, featureVector, extDocId));
	}
	
	public String getqid(int n){
		return this.featLists.get(n).qid;
	}
	public double getrelScore(int n){
		return this.featLists.get(n).relScore;
	}
	public String getextDocId(int n){
		return this.featLists.get(n).extDocId;
	}
	public double[] getfeatureVector(int n){
		return this.featLists.get(n).featureVector;
	}
	
	
	public class FeatureListComparator implements Comparator<FeatureListEntry> {
		@Override
		public int compare(FeatureListEntry s1, FeatureListEntry s2) {
			if(Double.parseDouble(s1.qid)>Double.parseDouble(s2.qid)){
				return 1;
			}
			else if(Double.parseDouble(s1.qid)<Double.parseDouble(s2.qid)){
				return -1;
			}
			else{
				return 0;
			}
		}
	}
	
	public void sort () {
		Collections.sort(this.featLists, new FeatureListComparator());
	}
		

}
