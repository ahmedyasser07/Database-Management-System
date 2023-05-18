package Backend.Index;

import java.io.Serializable;

public class Dimension implements Serializable {

	String columnName;
	String columnType;
	Comparable columnMin;
	Comparable columnMax;
	
	public Dimension(String columnName, String columnType, Comparable columnMin, Comparable columnMax) {
		
		this.columnName=columnName;
		this.columnType=columnType;
		this.columnMin=columnMin;
		this.columnMax=columnMax;
	}
	
}
