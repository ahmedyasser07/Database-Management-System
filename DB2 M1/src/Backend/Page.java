package Backend;

import java.io.Serializable;
import java.util.Vector;



public class Page implements Serializable{
	
	//attributes
	private transient int currentNumberOfTuples;
	private transient int maxNumberOfTuples;
	private transient static int pageID=0;
	public Vector<Tuple> tuples;
	private transient boolean isFull;
	
	
	//constructors
	public Page(){
		this.pageID++;
		this.maxNumberOfTuples = 200;
		this.tuples = new Vector<Tuple>(this.maxNumberOfTuples,0);
		this.currentNumberOfTuples = tuples.size();
		this.isFull = false;
		
	}
	
	
	
	
	//CurrentNumberOfTuples
	public int getCurrentNumberOfTuples() {
		return currentNumberOfTuples;
	}
	public void increaseCurrentNumberOfTuples() {
		this.currentNumberOfTuples++;
	}

	//MaxNumberOfTuples
	public int getMaxNumberOfTuples() {
		return maxNumberOfTuples;
	}
	
	//pageID
	public int getPageID() {
		return pageID;
	}
	
	//isFull
	public boolean isFull() {
		return isFull;
	}
	public void toggleIsFull() {
		this.isFull = false;
	}
	
	//vector
	public Vector<Tuple> getVector(){
		return this.tuples;
	}
	
	
	
	
	//page manipulation
	public void insertTuple(Tuple t) {
		
		if(this.currentNumberOfTuples<this.maxNumberOfTuples) {
			this.tuples.add(t);
			this.currentNumberOfTuples++;
		}
		
		if(this.currentNumberOfTuples==this.maxNumberOfTuples) {
			this.isFull=true;
		}
	}
	
	


}
