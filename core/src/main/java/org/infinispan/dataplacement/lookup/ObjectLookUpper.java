package org.infinispan.dataplacement.lookup;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import javax.swing.text.SimpleAttributeSet;

import com.clearspring.analytics.util.Pair;
import com.sun.corba.se.spi.orb.StringPair;

import org.infinispan.cacheviews.CacheViewsManagerImpl;
import org.infinispan.dataplacement.c50.FileCropper;
import org.infinispan.dataplacement.c50.NameSplitter;
import org.infinispan.dataplacement.c50.TreeElement;
import org.infinispan.dataplacement.c50.TreeParser;
import org.infinispan.util.logging.Log;
import org.infinispan.util.logging.LogFactory;

public class ObjectLookUpper {

	SimpleBloomFilter bf;
	NameSplitter splitter = new NameSplitter();
	String rules;
	TreeParser treeParser = new TreeParser();
	private static final Log log = LogFactory.getLog(ObjectLookUpper.class);
	
	
	public ObjectLookUpper(SimpleBloomFilter bf,
			List<List<TreeElement>> treeList) {
		this.bf = bf;
		treeParser.setTreeList(treeList);
	}

	public ObjectLookUpper() {
		// TODO Auto-generated constructor stub
	}

	public void test(List<String> keyList, int value){
		for(int i =0; i<10000;++i){
			if(query(keyList.get(i)) != value){
				System.out.print(keyList.get(i)+ "  "+ query(keyList.get(i)));
				System.out.println("  False!");
			}
		}
	}

	private List<String> readKeyList(String file)
			throws NumberFormatException, IOException {
		List<String> keys = new ArrayList<String>();

		String outputFolder = file + "splits/";
		File f = new File(outputFolder);
		if (!f.exists()) {
			f.mkdir();
		}
		int flag = 0, counter = 0;

		Reader.getInstance().StartReading(file);
		FileWriter fw = new FileWriter(outputFolder + "_splitted");
		BufferedWriter bw = new BufferedWriter(fw);
		keys = new ArrayList<String>();
		flag = Reader.getInstance().GetElements(keys);
		Reader.getInstance().StopReading();

		return keys;
	}
	
	public List<Pair<String, Integer>>  addValuetoKeyList( List<String> keyList, Integer value){
		List<Pair<String, Integer>> keyPairList = new ArrayList<Pair<String, Integer>>();
		for(String key : keyList){
			keyPairList.add(new Pair<String, Integer>(key, value));
		}
		return keyPairList;
	}
	
	public List<Pair<String, Integer>> mergePairList(List<List<Pair<String, Integer>>> pairLists){
		List<Pair<String, Integer>> finalTestList = new ArrayList<Pair<String, Integer>>();
		for(List<Pair<String, Integer>> pairList : pairLists){
			finalTestList.addAll(pairList);
		}
		return finalTestList;
	}

	public void populateAll(List<Pair<String, Integer>> toMoveObj) {
		log.info("Writing to objects to file");
		writeObjToFiles(toMoveObj);
		try {
			try {
				populateRules();
				populateBloomFilter(toMoveObj);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void writeObjToFiles(List<Pair<String, Integer>> toMoveObj) {
		List<Pair<String, Integer>> splittedKeys = splitter
				.splitPairKeys(toMoveObj);
		FileWriter fw;
		try {
			fw = new FileWriter("../ml/input.data");
			BufferedWriter bw = new BufferedWriter(fw);
			splitter.writePairKeys(bw, splittedKeys);
			bw.close();
			fw.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void populateRules() throws IOException, InterruptedException {
		String curDir = System.getProperty("user.dir");
		Process p = Runtime.getRuntime()
				.exec("../ml/c5.0 -f ../ml/input");// >

		log.info("Reading objects from file");
		// Read result and store in a list
		BufferedReader input = new BufferedReader(new InputStreamReader(
				p.getInputStream()));
		String line;
		List<String> inputs = new ArrayList<String>();
		while ((line = input.readLine()) != null) {
			inputs.add(line);
			//System.out.println(line);
		}
		input.close();

		//Cope not useful information
		FileWriter fw = new FileWriter("../ml/rules");
		BufferedWriter bw = new BufferedWriter(fw);
		FileCropper fp = new FileCropper();
		fp.cropFileAndWrite(inputs, bw);
		bw.close();
		fw.close();

		//Create Tree
		FileReader fr = new FileReader("../ml/rules");
		BufferedReader br = new BufferedReader(fr);
		rules = br.toString();
		log.info("Creating tree");
		treeParser.createTree(br);
	}
    
	public String getRules(){
		return rules;
	}
	
	public void populateBloomFilter(List<Pair<String, Integer>> toMoveObj) {
		bf = new SimpleBloomFilter<String>(toMoveObj.size()*8, toMoveObj.size());
		for(Pair<String, Integer> pair : toMoveObj){
			bf.add(pair.left);
		}
	}

	public Integer query(String key) {
		if(bf.contains(key) == false)
		  return null;
		else{
		  return treeParser.parseTree(splitter.splitSingleKey(key));
		}
	}

	public SimpleBloomFilter getBloomFilter() {
		return bf;
	}

	public void setBloomFilter(SimpleBloomFilter bf2) {
		bf = bf2;
	}
	
	public void setTreeElement(List<List<TreeElement>> treeList) {
		treeParser.setTreeList(treeList);
	}
	
	public List<List<TreeElement>> getTreeList(){
		return treeParser.getTreeList();
	}

}