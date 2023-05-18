package Backend;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Hashtable;
import java.util.Vector;

public class Table implements Serializable{
	
	//attributes
	private int currentNumOfRows;
	private String tableName;
	private Hashtable<String, String> fields= new Hashtable<String, String>();
	private String clusteringKey;
	private Hashtable<String, String> minValues = new Hashtable<String, String>();
	private Hashtable<String, String> maxValues = new Hashtable<String, String>();
	public Vector<String> pageFileNames = new Vector<String>();
	


	//constructor
	public Table(String tableName,String clusteringKey, Hashtable<String, String>  fields, Hashtable<String, String> minValues, Hashtable<String, String> maxValues) {
		this.tableName = tableName;
		this.fields = fields;
		this.clusteringKey = clusteringKey;
		this.minValues = minValues;
		this.maxValues = maxValues;
		this.currentNumOfRows = 0;
		
		
		String folderName = this.getTableName(); // specify the name of the new folder
		File directory = new File(System.getProperty("user.dir") + "/src/" + folderName);

		if (!directory.exists()) {
			directory.mkdir(); // create the new folder if it doesn't exist
			System.out.println("Directory created successfully");
		} else {
			System.out.println("Directory already exists");
		}
		
		try {
			this.serializePageFileNameVector();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	//serialize pagefilename array

	public void serializePageFileNameVector() throws IOException {
		
		String fileName = "src/"+ this.tableName +"/"+this.tableName + "pages" + ".ramsees";
		
		FileOutputStream f = new FileOutputStream(fileName);
		ObjectOutputStream d = new ObjectOutputStream(f);
	
		for(int i=0; i<pageFileNames.size(); i++) {
			d.writeObject(pageFileNames.get(i));
		}
		
		d.close();
	}
	
	public Vector<String> deserializePageFileNameVector() throws IOException, ClassNotFoundException{
		
		
		Vector<String> pages = new Vector<String>();	
		String file = "src/"+ this.tableName +"/"+this.tableName + "pages" + ".ramsees";
		FileInputStream f = new FileInputStream(file);
		ObjectInputStream d = new ObjectInputStream(f);
		
		while(true) {
			try {
				Object o = d.readObject();
				
				if(o instanceof String) {
					String tuple = (String)o;
					pages.add(tuple);
				}
			}catch(Exception e) {
				break;
			}
			
		}
		d.close();
		
		return pages;
	}
	
	
	
	
	public int getCurrentNumOfRows() {
		return currentNumOfRows;
	}


	public void setCurrentNumOfRows(int currentNumOfRows) {
		this.currentNumOfRows = currentNumOfRows;
	}


	public String getTableName() {
		return tableName;
	}


	public String getClusteringKey() {
		return clusteringKey;
	}



	public Hashtable<String, String> getMinValues() {
		return minValues;
	}


	public Hashtable<String, String> getMaxValues() {
		return maxValues;
	}


	public void setFields(Hashtable<String, String> fields) {
		this.fields = fields;
	}


	public Hashtable<String, String> getFields() {
		return this.fields;
	}
	
	
	public Vector<String> getPageFileNames() {
		return pageFileNames;
	}
	
	

}
