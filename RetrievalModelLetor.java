import java.io.*;
import java.util.*;
import java.util.Map.Entry;

import org.apache.lucene.analysis.Analyzer.TokenStreamComponents;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.*;
import org.apache.lucene.search.*;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

/**
 *  Copyright (c) 2015, Carnegie Mellon University.  All Rights Reserved.
 */

/**
 *  An object that stores parameters for the ranked Boolean
 *  retrieval model (there are none) and indicates to the query
 *  operators how the query should be evaluated.
 */
public class RetrievalModelLetor extends RetrievalModel {
	
	private List<List<String> > trainQueries;
	private List<List<String> > relDocs;
	private Map<String, Double> pageRanks;
	private Map<String, String> parameters;
	private List<String> scores = new ArrayList<String>() ;
	
	private FeatureList fl_test = new FeatureList();
	
	
	
	private void readInputs(Map<String, String> parameters) throws Exception {
		//store everything in memory
		
		//store train queries
		this.parameters = parameters;
		String str = parameters.get("letor:trainingQueryFile");
		trainQueries = new ArrayList<List<String> >();
		BufferedReader input = null;
		String qLine = null;
		input = new BufferedReader(new FileReader(str));
		while ((qLine = input.readLine()) != null) {
			int d = qLine.indexOf(':');
			String qid = qLine.substring(0, d);
			String qry = qLine.substring(d+1);
			List<String> tmp = new ArrayList<String>();
			tmp.add(qid);
			tmp.add(qry);
			trainQueries.add(tmp);
		}
		input.close();
		//store relevance docs
		str = parameters.get("letor:trainingQrelsFile");
		relDocs = new ArrayList<List<String> >();
		input = null;
		qLine = null;
		input = new BufferedReader(new FileReader(str));
		while ((qLine = input.readLine()) != null) {
			StringTokenizer tokens = new StringTokenizer(qLine, " \t", true);
			String token = null;
			List<String> tmp = new ArrayList<String>();
			while (tokens.hasMoreTokens()) {
				token = tokens.nextToken();
				if (token.matches(" ")) {
					continue;
				}
				tmp.add(token);
			}
			relDocs.add(tmp);
		}
		input.close();
		//store pageRanks
		str = parameters.get("letor:pageRankFile");
		pageRanks = new HashMap<String, Double>();
		input = null;
		qLine = null;
		input = new BufferedReader(new FileReader(str));
		while ((qLine = input.readLine()) != null) {
			int d = qLine.indexOf('\t');
			String doc = qLine.substring(0, d);
			Double pgRank = Double.parseDouble(qLine.substring(d + 1));
			pageRanks.put(doc, pgRank);
		}
		input.close();
		
		//create train data	
	}
	
	private void trainLetor(Map<String, String> parameters) throws Exception {
		FeatureList fl = null;
		fl = new FeatureList();
		double[] fvMin = new double[16];
		double[] fvMax = new double[16];
		
		int start = 0;
		int end = start;
		for(int i=0;i<trainQueries.size();i++){	
			for(int f=0;f<16;f++){
				fvMin[f] = Double.MAX_VALUE;
				fvMax[f] = Double.MIN_VALUE;
			}
			String qid = trainQueries.get(i).get(0);
			String qry = trainQueries.get(i).get(1);
			String t[] = QryEval.tokenizeQuery(qry);
			for(int j=0;j<relDocs.size();j++){
				if(!relDocs.get(j).get(0).equals(qid)){
					continue;
				}
				String extDocId = relDocs.get(j).get(2);
				double relScore = Double.parseDouble(relDocs.get(j).get(3));
				double[] fv;
				fv = getFeatureVector(t,extDocId,fvMin,fvMax);
				fl.add(relScore, qid, fv, extDocId);
				end++;
				
			}
			
			//normalizing
			for(int n = start;n<end;n++){
				for(int k=0;k<16;k++){
					if((fl.featLists.get(n).featureVector[k])==Double.MIN_VALUE){
						fl.featLists.get(n).featureVector[k] = 0.0;
					}
					else{
						double unNormed = fl.featLists.get(n).featureVector[k];
						double denom = fvMax[k]-fvMin[k];
						double normed;
						if(denom ==0.0){
							normed = 0.0;
						}
						else{
							normed = (unNormed - fvMin[k])/denom;
						}
						fl.featLists.get(n).featureVector[k] = normed;	
					}
				}
			}
			start = end;
			end = start;
		}
		fl.sort();
		String path = parameters.get("letor:trainingFeatureVectorsFile");
		writeFeaturesTrain(fl,path);
	}
	
	private void writeFeaturesTrain(FeatureList fl,String path) throws Exception{ 
		File output = new File(path);
		try{
			if(!output.exists())
			{
				output.createNewFile();
			}

			FileWriter fw = new FileWriter(output.getAbsoluteFile());
			BufferedWriter bw = new BufferedWriter(fw);
			for(int i=0;i<fl.featLists.size();i++){
				String str = "";
				str = str + Double.toString(fl.getrelScore(i)) + " qid:" + fl.getqid(i) + " ";
				//write features
				for(int f=0;f<16;f++){
					String tmp = "";
					tmp = tmp + Integer.toString(f+1) + ":";
					tmp = tmp + Double.toString((fl.getfeatureVector(i))[f]);
					str = str + tmp + " ";
				}
				str = str + "#" + fl.getextDocId(i);
				bw.write(str);
				bw.newLine();
			}
			bw.close();
			//fw.close();
		}
		catch(Exception e){
			e.printStackTrace();
		}
		
	}
	
	
	private double[] getFeatureVector(String[] t, String extDocId, double[] fvMin, double[] fvMax) throws Exception{
		double[] fv = new double[16];
		for(int i=0;i<16;i++){
			fv[i] = Double.MIN_VALUE;
		}
		
		
		try{
			int docid = Idx.getInternalDocid(extDocId);
			//fv:0 spam score
			fv[0] = Integer.parseInt (Idx.getAttribute ("score", docid));
			if(fv[0]<fvMin[0]){
				fvMin[0] = fv[0];
			}
			if(fv[0]>fvMax[0]){
				fvMax[0] = fv[0];
			}
			//url depth
			String rawUrl = Idx.getAttribute ("rawUrl", docid);
			int count = 0;
			for(int i=0;i<rawUrl.length();i++){
				if(rawUrl.charAt(i)=='/'){
					count++;
				}
			}
			fv[1] = count;
			if(fv[1]<fvMin[1]){
				fvMin[1] = fv[1];
			}
			if(fv[1]>fvMax[1]){
				fvMax[1] = fv[1];
			}
			//fv:2 wikipedia
			if(rawUrl.indexOf("wikipedia.org", 0) >=0){
				fv[2] = 1;
			}
			else{
				fv[2] = 0;
			}
			if(fv[2]<fvMin[2]){
				fvMin[2] = fv[2];
			}
			if(fv[2]>fvMax[2]){
				fvMax[2] = fv[2];
			}
			//fv:3 pageRank
			if(pageRanks.containsKey(extDocId)){
				fv[3] = pageRanks.get(extDocId);
				if(fv[3]<fvMin[3]){
					fvMin[3] = fv[3];
				}
				if(fv[3]>fvMax[3]){
					fvMax[3] = fv[3];
				}
			}
			String[] fields = {"body", "title", "url", "inlink"};
			int start;
			for(int i=0;i<fields.length;i++){
				start = 4+i*3;
				double score;
				score = featureBM25 (t, docid, fields[i]); 
				if(score!=-1){
					fv[start] = score;
					if(fv[start]<fvMin[start]){
						fvMin[start] = fv[start];
					}
					if(fv[start]>fvMax[start]){
						fvMax[start] = fv[start];
					}
				}
				score = featureIndri (t, docid, fields[i]);
				if(score!=-1){
					fv[start+1] = score;
					if(fv[start+1]<fvMin[start+1]){
						fvMin[start+1] = fv[start+1];
					}
					if(fv[start+1]>fvMax[start+1]){
						fvMax[start+1] = fv[start+1];
					}
				}
				score = featureOverlap(t,docid,fields[i]);
				if(score!=-1){
					fv[start+2] = score;
					if(fv[start+2]<fvMin[start+2]){
						fvMin[start+2] = fv[start+2];
					}
					if(fv[start+2]>fvMax[start+2]){
						fvMax[start+2] = fv[start+2];
					}
				}	
			}
			
			
		}
		catch(Exception e){
			e.printStackTrace();
		}
		
		
		
		return fv;
	}
	
	private double featureBM25 (String[] t, int docid, String field) throws Exception{
		double score = 0.0;
		try{
			TermVector forwardIndex = new TermVector(docid,field);
			if(forwardIndex.positionsLength()==0){
				return -1;
			}
			//for(int i=1;i<forwardIndex.)
			double b = Double.parseDouble(parameters.get("BM25:b"));
			double k_1 = Double.parseDouble(parameters.get("BM25:k_1"));
			double k_3 = Double.parseDouble(parameters.get("BM25:k_3"));
			for(int i=0;i<t.length;i++){
				int stemIndex = forwardIndex.indexOfStem(t[i]);
				if(stemIndex!=-1){
					double termFreq = forwardIndex.stemFreq(stemIndex);
					double N = Idx.getNumDocs();
					double docFreq = forwardIndex.stemDf(stemIndex);
					double docLength = Idx.getFieldLength(field, docid);
					double avgLength = Idx.getSumOfFieldLengths(field)/(double)Idx.getDocCount(field);
					double rsjWeight = Math.max(0,Math.log((N-docFreq+0.5)/(docFreq+0.5)));
					double tfWeight = termFreq/(termFreq + k_1*((1-b) + (b*docLength)/avgLength));
					score = score + (rsjWeight*tfWeight);
				}
			}	
		}
		catch(Exception e){
			e.printStackTrace();
		}
		return score;
	}
	
	
	private double featureIndri (String[] t, int docid, String field) throws Exception{
		double score = 1.0;
		double temp = 0;
		int count =0;
		try{
			TermVector forwardIndex = new TermVector(docid,field);
			if(forwardIndex.positionsLength()==0){
				return -1;
			}
			double mu = Double.parseDouble(parameters.get("Indri:mu"));
			double lambda = Double.parseDouble(parameters.get("Indri:lambda"));
			for(int i=0;i<t.length;i++){
				int stemIndex = forwardIndex.indexOfStem(t[i]);
				if(stemIndex!=-1){
					count ++;
					double termFreq = forwardIndex.stemFreq(stemIndex);
					double corpusTermFreq = forwardIndex.totalStemFreq(stemIndex);
					double docLength = Idx.getFieldLength(field, docid);
					double corpusLength = Idx.getSumOfFieldLengths(field);
					double p_mle = corpusTermFreq/corpusLength;
					temp =  (1-lambda)*((termFreq + mu*p_mle)/(docLength+mu)) + lambda*p_mle ;
					//score = score * Math.pow(temp, 1/(double)t.length);
					//score = score + temp;
				}
				else{
					double termFreq = 0.0;
					double corpusTermFreq = Idx.INDEXREADER.totalTermFreq(new Term(field,t[i]));
					double docLength = Idx.getFieldLength(field, docid);
					double corpusLength = Idx.getSumOfFieldLengths(field);
					double p_mle = corpusTermFreq/corpusLength;
					temp =  (1-lambda)*((termFreq + mu*p_mle)/(docLength+mu)) + lambda*p_mle ;	
				}
				score = score * Math.pow(temp, 1/(double)t.length);
			}	
		}
		catch(Exception e){
			e.printStackTrace();
		}
		if(count==0){
			return -1;
		}
		
		return score;
		
	}
	
	
	private double featureOverlap (String[] t, int docid, String field) throws Exception{
		double score =0.0;
		try{
			TermVector forwardIndex = new TermVector(docid,field);
			if(forwardIndex.positionsLength()==0){
				return -1;
			}
			for(int i=0;i<t.length;i++){
				int stemIndex = forwardIndex.indexOfStem(t[i]);
				if(stemIndex!=-1){
					score = score +1;
				}	
			}
			score = score/t.length;
		}
		catch(Exception e){
			e.printStackTrace();
		}
		
		return score;
		
	}
	
	public void processLetorTrain(Map<String, String> parameters) throws Exception {
		
		readInputs(parameters);
		trainLetor(parameters);
		String execPath = parameters.get("letor:svmRankLearnPath");
		String qrelsFeatureOutputFile = parameters.get("letor:trainingFeatureVectorsFile");
		String modelOutputFile = parameters.get("letor:svmRankModelFile");
		Process cmdProc = Runtime.getRuntime().exec(
		        new String[] { execPath, "-c", parameters.get("letor:svmRankParamC"), qrelsFeatureOutputFile,
		            modelOutputFile });
		BufferedReader stdoutReader = new BufferedReader(
		        new InputStreamReader(cmdProc.getInputStream()));
		String line;
		while ((line = stdoutReader.readLine()) != null) {
		    System.out.println(line);
		}
		BufferedReader stderrReader = new BufferedReader(
		            new InputStreamReader(cmdProc.getErrorStream()));
		while ((line = stderrReader.readLine()) != null) {
		     System.out.println(line);
		}
		int retValue = cmdProc.waitFor();
	    if (retValue != 0) {
	      throw new Exception("SVM Rank crashed.");
	    }
	}
	
	public void processLetorTest(ScoreList r, String qid, String qry) throws Exception{
		double[] fvMin = new double[16];
		double[] fvMax = new double[16];
		
		int start = fl_test.featLists.size();
		int end = start;
		for(int f=0;f<16;f++){
			fvMin[f] = Double.MAX_VALUE;
			fvMax[f] = Double.MIN_VALUE;
		}
		String t[] = QryEval.tokenizeQuery(qry);
		for(int i=0;i<100;i++){
			int docid = r.getDocid(i);
			String extDocId = Idx.getExternalDocid(docid);
			
			double relScore = 0;
			double[] fv;
			fv = getFeatureVector(t,extDocId,fvMin,fvMax);
			fl_test.add(relScore, qid, fv, extDocId);
			end++;
			
		}
		//normalization
		for(int n = start;n<end;n++){
			for(int k=0;k<16;k++){
				if((fl_test.featLists.get(n).featureVector[k])==Double.MIN_VALUE){
					fl_test.featLists.get(n).featureVector[k] = 0.0;
				}
				else{
					double unNormed = fl_test.featLists.get(n).featureVector[k];
					double denom = fvMax[k]-fvMin[k];
					double normed;
					if(denom ==0.0){
						normed = 0.0;
					}
					else{
						normed = (unNormed - fvMin[k])/denom;
					}
					fl_test.featLists.get(n).featureVector[k] = normed;	
				}
			}
		}
	}
	
	public void processLetorClassify() throws Exception{
		fl_test.sort();
		String path = parameters.get("letor:testingFeatureVectorsFile");
		writeFeaturesTrain(fl_test,path);
		//classify
		String execPath = parameters.get("letor:svmRankClassifyPath");
		String qrelsFeatureOutputFile = parameters.get("letor:testingFeatureVectorsFile");
		String modelOutputFile = parameters.get("letor:svmRankModelFile");
		String predictions = parameters.get("letor:testingDocumentScores");
		Process cmdProc = Runtime.getRuntime().exec(
		        new String[] { execPath, qrelsFeatureOutputFile,
		            modelOutputFile, predictions });
		BufferedReader stdoutReader = new BufferedReader(
		        new InputStreamReader(cmdProc.getInputStream()));
		String line;
		while ((line = stdoutReader.readLine()) != null) {
		    System.out.println(line);
		}
		BufferedReader stderrReader = new BufferedReader(
		            new InputStreamReader(cmdProc.getErrorStream()));
		while ((line = stderrReader.readLine()) != null) {
		     System.out.println(line);
		}
		int retValue = cmdProc.waitFor();
	    if (retValue != 0) {
	      throw new Exception("SVM Rank crashed.");
	    }
	    
	    //read scoresfile
	    String str = parameters.get("letor:testingDocumentScores");
	    BufferedReader input = null;
		String qLine = null;
		input = new BufferedReader(new FileReader(str));
		while ((qLine = input.readLine()) != null) {
			scores.add(qLine);
		}
		input.close();
	    
	    //output scorelist
	    path = parameters.get("trecEvalOutputPath");
	    File output = new File(path);
		if(!output.exists())
		{
			output.createNewFile();
		}
		FileWriter fw = new FileWriter(output.getAbsoluteFile());
		BufferedWriter bw = new BufferedWriter(fw);
		int i=0;
	    while(i<fl_test.featLists.size()){
	    	String qid = fl_test.getqid(i);
	    	String curqid = qid;
	    	String prevqid = qid;
	    	ScoreList r = new ScoreList();
	    	while(i<fl_test.featLists.size() && curqid.equals(prevqid)){
	    		
	    		int docid = Idx.getInternalDocid(fl_test.getextDocId(i));
	    		double score = Double.parseDouble(scores.get(i));
	    		r.add(docid, score);
	    		i++;
	    		if(i<fl_test.featLists.size())
	    			curqid = fl_test.getqid(i);
	    	}
	    	r.sort();
	    	QryEval.writeResults(qid, r, bw);
	    	
	    	
	    }
	    bw.close();	
	}
	

	
	
	public String defaultQrySopName () {
		return new String ("#sum");
	}

}
