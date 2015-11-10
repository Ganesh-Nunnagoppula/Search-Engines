import java.io.*;
import java.util.*;



public class expandedQry {
	
	private class expandedQryEntry{
		private String term;
		private double weight;
	
		
		private expandedQryEntry(String term, double weight){
			this.term = term;
			this.weight = weight;
		}
	}
	private List<expandedQryEntry> expQrys = new ArrayList<expandedQryEntry>();
	
	public void add(String term, double weight) {
		expQrys.add(new expandedQryEntry(term, weight));
	}
	
	public String getTerm(int n){
		return this.expQrys.get(n).term;
	}
	public double getWeight(int n){
		return this.expQrys.get(n).weight;
	}
	public class expandedQryComparator implements Comparator<expandedQryEntry> {
		@Override
		public int compare(expandedQryEntry s1, expandedQryEntry s2) {
			if(s1.weight>s2.weight){
				return -1;
			}
			else if(s1.weight<s2.weight){
				return 1;
			}
			else{
				return 0;
			}
		}
	}
	
	public void sort () {
		Collections.sort(this.expQrys, new expandedQryComparator());
	}
		

}
