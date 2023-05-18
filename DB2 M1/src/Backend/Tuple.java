package Backend;

import java.io.Serializable;
import java.util.Hashtable;

public class Tuple implements Serializable{

//	private transient Table parentTable;
//	private transient Hashtable<String, String> fields= new Hashtable<String, String>();
	public Hashtable<String,Object> column_Value = new Hashtable<String,Object>();
	


	public Tuple(Hashtable<String,Object> column_Value ) {
		
//		this.parentTable = parentTable;
//		this.fields = parentTable.getFields();		
		this.column_Value=column_Value;
		
	}
	
	
	
	public void setColumn_Value(Hashtable<String, Object> column_Value) {
		this.column_Value = column_Value;
	}
	
	public Hashtable<String,Object> getTupleColumnValues(){
		return this.column_Value;
	}



	
	public int compareTo(Tuple t , String pk ) {
				
		Comparable myPKValue = (Comparable)this.column_Value.get(pk);
		Comparable otherPKValue = (Comparable)t.column_Value.get(pk);
		
		int r = myPKValue.compareTo(otherPKValue);
		
		if(r>0) {
			return 1;
		}else if(r<0) {
			return -1;
		}
		return 0;
	}



}
