package Backend;

public class SQLTerm {

	public String strTableName;
	public String strColumnName;
	public String strOperator;
	public Object objValue;
	
	
	public SQLTerm(String strTableName,String strColumnName,String strOperator,Object objValue) {
		this.strTableName = strTableName;
		this.strColumnName = strColumnName;
		this.strOperator = strOperator;
		this.objValue = objValue;
	}
}
