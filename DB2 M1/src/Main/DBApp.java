package Main;


import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.RandomAccessFile;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Vector;
import Backend.Page;
import Backend.SQLTerm;
import Backend.Table;
import Backend.Tuple;
import Backend.Index.Dimension;
import Backend.Index.Node;
import Backend.Index.Octree;
import Backend.Index.Reference;
import Exceptions.DBAppException;

public class DBApp{



	//test m1 + m2 methods


	//Milestone 1 methods
	public void init(){

		String fileName = "VectorOfTables" + ".ramsees";		
		FileOutputStream f;
		try {
			f = new FileOutputStream(fileName);
			ObjectOutputStream d = new ObjectOutputStream(f);
			d.close();
			String filePath = System.getProperty("user.dir") + "/src/metadata.csv";
			FileWriter writer = new FileWriter(filePath);			
			writer.write("Table Name, Column Name, Column Type, ClusteringKey, IndexName,IndexType, min, max" + "\n");
			writer.close();
		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}


	}



	public static Vector<Reference> andVectorsRef(Vector<Reference> r1, Vector<Reference> r2){

		Vector<Reference> result = new Vector<Reference>();

		for(Reference r: r1) {
			if(r2.contains(r)) {
				result.add(r);
			}
		}


		return result;
	}
	public static Vector<Reference> orVectorsRef(Vector<Reference> r1, Vector<Reference> r2){

		Vector<Reference> result = new Vector<Reference>();

		for(Reference r: r1) {
			result.add(r);
		}
		for(Reference r: r2) {
			result.add(r);
		}

		return result;
	}
	public static Vector<Reference> xorVectorsRef(Vector<Reference> r1, Vector<Reference> r2) throws DBAppException{

		Vector<Reference> result = new Vector<Reference>();

		if(r1.size() != r2.size()) {
			throw new DBAppException("result sets need to have the same size to XOR");
		}

		for(int i=0; i< r1.size();i++) {
			if(i%2 == 0) {
				result.add(r1.get(i));
			}else {
				result.add(r2.get(i));

			}
		}

		return result;
	}


	//Milestone 2 methods
	public Iterator selectFromTable(SQLTerm[] arrSQLTerms, String[] strarrOperators)throws DBAppException{

		Iterator<Tuple> iterator = null;


		if(strarrOperators.length != arrSQLTerms.length-1) {
			throw new DBAppException("cannot search with inappropriate conditions");
		}


		String tableName = arrSQLTerms[0].strTableName;
		String[] columns = new String[arrSQLTerms.length];

		for( SQLTerm t : arrSQLTerms) {
			if(!t.strTableName.equals(tableName)) {
				throw new DBAppException("cannot search in different tables");
			}
			columns[Arrays.asList(arrSQLTerms).indexOf(t)] = t.strColumnName;
		}

		try {
			
			Vector<Table> tables = deserializeVectorOfTables();
			Table table = getTable(tableName);
			tables.remove(table);
			table.pageFileNames = table.deserializePageFileNameVector();
			table.serializePageFileNameVector();
			Octree octree = null;





			if(table != null) {

				Set<String> keys = table.getFields().keySet();
				for(int i = 0; i<columns.length;i++) {
					if(!keys.contains(columns[i])) {
						throw new DBAppException("columns passed are not part of this table");
					}
				}


				String x = "";
				String y = "";
				String z = "";

				boolean indexFound = false;
				for(int i =0 ; i<columns.length-2;i++) {

					String cols = columns[i]+columns[i+1]+columns[i+2];
					String indexName = tableName+cols+"index";
					Octree oct = deserializeIndex(tableName, indexName);
					serializeIndex(tableName, indexName, octree);

					if(oct != null) {
						indexFound = true;
						x= columns[i];
						y = columns[i+1];
						z = columns[i+2];
						octree = oct;
						break;
					}


				}





				if(indexFound) {
					//search using index

					Vector<Vector<Reference>> resultSets = new Vector<Vector<Reference>>();

					for(SQLTerm term : arrSQLTerms) {

						Vector<Reference> r =octree.searchWithCondition(term.strColumnName, term.strOperator,(Comparable) term.objValue);
						resultSets.add(r);
					}

					String op = strarrOperators[0];
					Vector<Reference> references = new Vector<Reference>();

					if(op.equals("AND")) {
						references = andVectorsRef(resultSets.get(0), resultSets.get(1));
					}
					if(op.equals("OR")) {
						references = orVectorsRef(resultSets.get(0), resultSets.get(1));

					}
					if(op.equals("XOR")) {
						references = xorVectorsRef(resultSets.get(0), resultSets.get(1));

					}



					for(int i=2 ; i< strarrOperators.length;i++) {
						op = strarrOperators[i];

						if(op.equals("AND")) {
							references = andVectorsRef(references, resultSets.get(i));
						}
						if(op.equals("OR")) {
							references = orVectorsRef(references, resultSets.get(i));

						}
						if(op.equals("XOR")) {
							references = xorVectorsRef(references, resultSets.get(i));

						}


					}

					table.pageFileNames = table.deserializePageFileNameVector();
					Vector<Tuple> results = new Vector<Tuple>();


					for(Reference r : references) {
						String page = r.fileName;
						Vector<Tuple> tuples = getPageVectorOfTuples(page);
						table.pageFileNames.remove(page);
						for(Tuple tuple: tuples) {
							Comparable xVal = (Comparable) tuple.column_Value.get(x);
							Comparable yVal = (Comparable) tuple.column_Value.get(y);
							Comparable zVal = (Comparable) tuple.column_Value.get(z);

							if(xVal.compareTo(r.xValue) == 0 && yVal.compareTo(r.yValue) ==0 && zVal.compareTo(r.zValue)==0) {
								results.add(tuple);
							}
						}
						table.pageFileNames.add(page);
					}

					table.serializePageFileNameVector();
					tables.add(table);
					serializeVectorOfTables(tables);
					return results.iterator();



				}else {
					//search using table

					String pk = table.getClusteringKey();
					boolean pkFound = false;

					for(String column : columns) {
						if(pk.equals(column)) {
							pkFound = true;
						}
					}

					if(pkFound) {
						//use binary search to search the table


						ArrayList<Vector<Tuple>> resultSets = new ArrayList<Vector<Tuple>>();
						//check serialization here
						for(SQLTerm term : arrSQLTerms) {
							Vector<Tuple> result = searchQueryBinary(table, term.strColumnName, term.strOperator, term.objValue);
							resultSets.add(result);
						}


						Vector<Tuple> results = new Vector<Tuple>();

						String operatorr =  strarrOperators[0];
						SQLTerm firstTerm = arrSQLTerms[1];

						if(operatorr == "AND") {
							results = andVectorOperator(resultSets.get(0),firstTerm.strColumnName, firstTerm.strOperator, firstTerm.objValue );
						}
						if(operatorr == "OR") {

							results = orVectors(resultSets.get(0), resultSets.get(1));

						}
						if(operatorr == "XOR") {
							results = xorVectors(resultSets.get(0), resultSets.get(1));
						}



						for(int i=2; i<arrSQLTerms.length;i++) {

							String operator =  strarrOperators[i-1];

							if(operator == "AND") {
								SQLTerm term = arrSQLTerms[i];
								results = andVectorOperator(results, term.strColumnName, term.strOperator, term.objValue );

							}
							if(operator == "OR") {

								results = orVectors(results, resultSets.get(i));


							}
							if(operator == "XOR") {
								results = orVectors(results, resultSets.get(i));

							}
						}



						iterator = results.iterator();



					}else {
						//search the table linearly 

						ArrayList<Vector<Tuple>> resultSets = new ArrayList<Vector<Tuple>>();

						for(SQLTerm term : arrSQLTerms) {
							Vector<Tuple> result = searchQueryLinear(table, term.strColumnName, term.strOperator, term.objValue);
							resultSets.add(result);
						}


						Vector<Tuple> results = new Vector<Tuple>();

						String operatorr =  strarrOperators[0];
						SQLTerm firstTerm = arrSQLTerms[1];

						if(operatorr == "AND") {
							results = andVectorOperator(resultSets.get(0),firstTerm.strColumnName, firstTerm.strOperator, firstTerm.objValue );
						}
						if(operatorr == "OR") {

							results = orVectors(resultSets.get(0), resultSets.get(1));

						}
						if(operatorr == "XOR") {
							results = xorVectors(resultSets.get(0), resultSets.get(1));
						}



						for(int i=2; i<arrSQLTerms.length;i++) {

							String operator =  strarrOperators[i-1];

							if(operator == "AND") {
								SQLTerm term = arrSQLTerms[i];
								results = andVectorOperator(results, term.strColumnName, term.strOperator, term.objValue );

							}
							if(operator == "OR") {

								results = orVectors(results, resultSets.get(i));


							}
							if(operator == "XOR") {
								results = orVectors(results, resultSets.get(i));

							}
						}



						iterator = results.iterator();


					}
					
					table.serializePageFileNameVector();
					tables.add(table);
					serializeVectorOfTables(tables);
					
				}




			}else {
				throw new DBAppException("table with name "+ tableName + " was not found");

			}





		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}



		return iterator;
	}

	public void createIndex(String strTableName, String[] strarrColName) throws DBAppException{



		Hashtable<String,String> colMin = new Hashtable<String,String>();
		Hashtable<String,String> colMax = new Hashtable<String,String>();
		

		String indexName=strTableName + strarrColName[0]+ strarrColName[1] + strarrColName[2]+"index";

		try {
			Octree o = deserializeIndex(strTableName, indexName);

			if(o != null) {
				serializeIndex(strTableName,indexName, o);
				throw new DBAppException("Index on these 3 columns was already created in the past, cannot create again");
			}
			
			

		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}


		if(strarrColName.length == 3) {

			boolean tableFound = false;

			String x = strarrColName[0];
			String y = strarrColName[1];
			String z = strarrColName[2];

			boolean xfound = false;
			boolean yfound = false;
			boolean zfound = false;


			String xType = "";
			Comparable xMin = null;
			Comparable xMax = null;

			String yType="";
			Comparable yMin= null;
			Comparable yMax= null;

			String zType="";
			Comparable zMin= null;
			Comparable zMax= null;

			if(x.equals(y) || x.equals(z) || y.equals(z)) {
				throw new DBAppException("you passed duplicate columns, you need 3 columns to create an index");
			}

			String csvFile = System.getProperty("user.dir") + "/src/metadata.csv"; // file path to metadata file
			String line = "";
			String[] metadata;

			try (BufferedReader br = new BufferedReader(new FileReader(csvFile))) {

				while ((line = br.readLine()) != null) {
					metadata = line.split(",");
					if (metadata[0].equalsIgnoreCase(strTableName)) {

						tableFound = true;

						String colName = metadata[1];
						String colType = metadata[2];
						boolean isClusteringKey = Boolean.parseBoolean(metadata[3]);
						String indexNamee = metadata[4];
						String indexType = metadata[5];
						Object min = (metadata[6]);
						Object max = (metadata[7]);

						if(x.equals(colName)) {
							xfound = true;
							xType = colType;
							xMin = (Comparable) min;
							xMax = (Comparable) max;
						}
						if(y.equals(colName)) {
							yfound = true;
							yType = colType;
							yMin = (Comparable) min;
							yMax = (Comparable) max;
						}
						if(z.equals(colName)) {
							zfound = true;
							zType = colType;
							zMin = (Comparable) min;
							zMax = (Comparable) max;
						}
						
						colMin.put(colName, ""+min);
						colMax.put(colName, ""+max);
						
					}
				}

				if(!tableFound) {
					throw new DBAppException("table with name "+ strTableName+ " was not found");

				}

				if(!xfound || !yfound || !zfound) {
					throw new DBAppException("columns passed are not in table "+ strTableName+ " , you need 3 columns to create an index");

				}


			}
			catch(Exception e) {
				System.out.println("buffer reader exception in reading metadata file");
			}

			//create the index


			//			String columnName;
			//			String columnType;
			//			Comparable columnMin;
			//			Comparable columnMax;
			
			Hashtable<String,Object> colMinObj = parseHashtable(colMin);
			Hashtable<String,Object> colMaxObj = parseHashtable(colMax);
			
			
			for(String key: colMin.keySet()) {
				
				if(x.equals(key)) {
					
					xMin = (Comparable) colMinObj.get(key);
					xMax = (Comparable) colMaxObj.get(key);
				}
				if(y.equals(key)) {

					yMin = (Comparable) colMinObj.get(key);
					yMax = (Comparable) colMaxObj.get(key);
				}
				if(z.equals(key)) {

					zMin = (Comparable) colMinObj.get(key);
					zMax = (Comparable) colMaxObj.get(key);
				}
				
			}

			Dimension xColumn = new Dimension(x, xType, xMin, xMax);
//			System.out.println(x+" "+xType+" "+xMin+" "+xMax);
			Dimension yColumn = new Dimension(y, yType, yMin, yMax);
//			System.out.println(y+" "+yType+" "+yMin+" "+yMax);
			Dimension zColumn = new Dimension(z, zType, zMin, zMax);
//			System.out.println(z+" "+zType+" "+zMin+" "+zMax);

			Octree octree = new Octree(xColumn, yColumn, zColumn);
//
//			System.out.println(octree.root);
//			System.out.print(octree.root.splitted);


			try {
				Table table=getTable(strTableName);

				if(table != null) {

					Vector<Table> tables = deserializeVectorOfTables();
					table.pageFileNames = table.deserializePageFileNameVector();
					//table.serializePageFileNameVector();
					tables.remove(table);
					//					table.serializePageFileNameVector();


					for(String pageFile : table.pageFileNames) {
						table.pageFileNames.remove(pageFile);
						Vector<Tuple> tuples = getPageVectorOfTuples(pageFile);

						for(Tuple tuple : tuples) {

							//							String fileName;
							//							Object pkValue;
							//							Comparable xValue;
							//							Comparable yValue;
							//							Comparable zValue;

							Object pk = tuple.column_Value.get(table.getClusteringKey());
							Comparable xValue = (Comparable) tuple.column_Value.get(x);
							Comparable yValue = (Comparable) tuple.column_Value.get(y);
							Comparable zValue = (Comparable) tuple.column_Value.get(z);
//							System.out.println();
//
//							System.out.println(xValue);
//							System.out.println(yValue);
//							System.out.println(zValue);


							Reference reference = new Reference(pageFile, pk ,xValue , yValue, zValue);
							if(octree == null) {
								System.out.println("null octree ");

							}
							octree.insert(reference);
							//octree.printOctree();

							//System.out.println("inserted reference: "+ reference);

							indexName = strTableName+x+y+z+"index";

						}
						table.pageFileNames.add(pageFile);

					}
					serializeIndex(strTableName, indexName, octree);
					table.serializePageFileNameVector();
					tables.add(table);
					serializeVectorOfTables(tables);

					String filePath = System.getProperty("user.dir") + "/src/metadata.csv"; // Path to your metadata file
					//String strTableName = tableName; // Table name to update

					// Read the metadata file and load its content into a list
					List<String[]> metadataLines = new ArrayList<>();

					//					Table tab=getTable(strTableName);


					try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
						//St line;

						while ((line = reader.readLine()) != null) {
							metadata = line.split(",");
							metadataLines.add(metadata);
						}
					} catch (IOException e) {
						e.printStackTrace();
						return;
					}

					tableFound = false;

					// Update the desired fields
					for (String[] metadataa : metadataLines) {
						if (metadataa[0].equalsIgnoreCase(strTableName)) {
							tableFound = true;


							if(metadataa[1].equals(x) || metadataa[1].equals(y) || metadataa[1].equals(z)) {
								metadataa[4] = indexName;
								metadataa[5] = "Octree";
							}

							//							// Update the fields as needed
							//							metadataa[1] = "new_col_name";
							//							metadataa[2] = "new_col_type";
							//							metadataa[3] = "true";
							//							metadataa[4] = "new_index_name";
							//							metadataa[5] = "new_index_type";
							//							metadataa[6] = "new_min_value";
							//							metadataa[7] = "new_max_value";
						}
					}

					if (!tableFound) {
						throw new DBAppException("Table not found in the metadata file.");

					}

					// Write the modified content back to the metadata file
					try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
						for (String[] metadataa : metadataLines) {
							writer.write(String.join(",", metadataa));
							writer.newLine();
						}
					} catch (IOException e) {
						e.printStackTrace();
					}

					//System.out.println("Metadata file updated successfully.");


				}
			} catch (ClassNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}


		}else {
			throw new DBAppException("you passed less than 3 columns, you need 3 columns to create an index");
		}




	}


	//Milestone 2 helper methods
	public static Vector<Tuple> searchQueryLinear(Table table , String column, String operation , Object value){

		Vector<Tuple> result = new Vector<Tuple>();

		if(operation == "=") {

			try {
				Vector<Table> tables = deserializeVectorOfTables();
				tables.remove(table);
				table.pageFileNames = table.deserializePageFileNameVector();
				
				for(String page: table.pageFileNames) {
					table.pageFileNames.remove(page);
					Vector<Tuple> tuples = getPageVectorOfTuples(page);

					for(Tuple tuple: tuples) {

						if(tuple.column_Value.get(column).equals(value)) {
							result.add(tuple);
						}

					}
					table.pageFileNames.add(page);
				}
				table.serializePageFileNameVector();
				tables.add(table);
				serializeVectorOfTables(tables);


			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}





		}
		if(operation == "!=") {


			try {

				Vector<String> pages = table.deserializePageFileNameVector();
				table.serializePageFileNameVector();

				for(String page: pages) {

					Vector<Tuple> tuples = getPageVectorOfTuples(page);

					for(Tuple tuple: tuples) {

						if(!tuple.column_Value.get(column).equals(value)) {
							result.add(tuple);
						}

					}

				}

			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}


		}
		if(operation == "<") {


			try {

				Vector<String> pages = table.deserializePageFileNameVector();
				table.serializePageFileNameVector();

				for(String page: pages) {

					Vector<Tuple> tuples = getPageVectorOfTuples(page);

					for(Tuple tuple: tuples) {


						Comparable tValue = (Comparable) tuple.column_Value.get(column);
						Comparable oValue = (Comparable) value;


						if(tValue.compareTo(oValue) < 0) {
							result.add(tuple);
						}

					}

				}

			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}




		}
		if(operation == ">") {

			try {

				Vector<String> pages = table.deserializePageFileNameVector();
				table.serializePageFileNameVector();

				for(String page: pages) {

					Vector<Tuple> tuples = getPageVectorOfTuples(page);

					for(Tuple tuple: tuples) {


						Comparable tValue = (Comparable) tuple.column_Value.get(column);
						Comparable oValue = (Comparable) value;


						if(tValue.compareTo(oValue) > 0) {
							result.add(tuple);
						}

					}

				}

			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
		if(operation == ">=") {

			try {

				Vector<String> pages = table.deserializePageFileNameVector();
				table.serializePageFileNameVector();

				for(String page: pages) {

					Vector<Tuple> tuples = getPageVectorOfTuples(page);

					for(Tuple tuple: tuples) {


						Comparable tValue = (Comparable) tuple.column_Value.get(column);
						Comparable oValue = (Comparable) value;


						if(tValue.compareTo(oValue) > 0 || tuple.column_Value.get(column).equals(value)) {
							result.add(tuple);
						}

					}

				}

			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
		if(operation == "<="){

			try {

				Vector<String> pages = table.deserializePageFileNameVector();
				table.serializePageFileNameVector();

				for(String page: pages) {

					Vector<Tuple> tuples = getPageVectorOfTuples(page);

					for(Tuple tuple: tuples) {


						Comparable tValue = (Comparable) tuple.column_Value.get(column);
						Comparable oValue = (Comparable) value;


						if(tValue.compareTo(oValue) < 0 || tuple.column_Value.get(column).equals(value)) {
							result.add(tuple);
						}

					}

				}

			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}


		return result;
	}

	public static Vector<Tuple> andVectorOperator(Vector<Tuple> tuples, String column, String operation , Object value){

		Vector<Tuple> result = new Vector<Tuple>();



		if(operation == "=") {

			for(Tuple tuple : tuples) {

				Comparable tValue = (Comparable) tuple.column_Value.get(column);
				Comparable oValue = (Comparable) value;

				if(tValue.compareTo(oValue) == 0) {
					result.add(tuple);

				}



			}


		}
		if(operation == "!=") {
			for(Tuple tuple : tuples) {

				Comparable tValue = (Comparable) tuple.column_Value.get(column);
				Comparable oValue = (Comparable) value;

				if(tValue.compareTo(oValue) != 0) {
					result.add(tuple);

				}



			}

		}
		if(operation == "<") {

			for(Tuple tuple : tuples) {

				Comparable tValue = (Comparable) tuple.column_Value.get(column);
				Comparable oValue = (Comparable) value;

				if(tValue.compareTo(oValue) < 0) {
					result.add(tuple);

				}



			}

		}
		if(operation == ">") {
			for(Tuple tuple : tuples) {

				Comparable tValue = (Comparable) tuple.column_Value.get(column);
				Comparable oValue = (Comparable) value;

				if(tValue.compareTo(oValue) > 0) {
					result.add(tuple);

				}



			}

		}
		if(operation == ">=") {
			for(Tuple tuple : tuples) {

				Comparable tValue = (Comparable) tuple.column_Value.get(column);
				Comparable oValue = (Comparable) value;

				if(tValue.compareTo(oValue) == 0  ||  tValue.compareTo(oValue) > 0) {
					result.add(tuple);

				}



			}


		}
		if(operation == "<="){

			for(Tuple tuple : tuples) {

				Comparable tValue = (Comparable) tuple.column_Value.get(column);
				Comparable oValue = (Comparable) value;

				if(tValue.compareTo(oValue) == 0  ||  tValue.compareTo(oValue) < 0) {
					result.add(tuple);

				}



			}

		}



		return result;
	}

	public static Vector<Tuple> orVectors(Vector<Tuple> a, Vector<Tuple> b){

		Vector<Tuple> result = new Vector<Tuple>();

		for(Tuple t : a) {
			result.add(t);
		}

		for(Tuple t : b) {
			result.add(t);
		}

		return result;

	}

	public static Vector<Tuple> xorVectors(Vector<Tuple> a, Vector<Tuple> b) throws DBAppException{
		Vector<Tuple> result = new Vector<Tuple>();

		if(a.size() != b.size()) {
			throw new DBAppException("to XOR, both result sets must have equal sizes or number of tuples");
		}

		for(int i=0; i<a.size();i++) {

			if(i%2==0) {
				result.add(a.get(i));
			}else {
				result.add(b.get(i));
			}

		}


		return result;
	}

	public static Vector<Tuple> searchQueryBinary(Table table, String column, String operation, Object value) {
		Vector<Tuple> result = new Vector<>();

		try {
			Vector<Table> tables = deserializeVectorOfTables();
			table.pageFileNames = table.deserializePageFileNameVector();
			tables.remove(table);

			for (String page : table.pageFileNames) {
				table.pageFileNames.remove(page);
				Vector<Tuple> tuples = getPageVectorOfTuples(page);

				// Perform binary search on the tuples using the table's primary key
				int low = 0;
				int high = tuples.size() - 1;

				while (low <= high) {
					int mid = (low + high) / 2;
					Tuple tuple = tuples.get(mid);

					Comparable keyValue = (Comparable) tuple.column_Value.get(column);
					Comparable searchValue = (Comparable) value;

					//					System.out.println(keyValue);
					//					System.out.println(searchValue);

					//					if(!(keyValue.getClass().equals(searchValue.getClass()))) {
					//						
					//						throw new DBAppException("types mismatch");
					//					}
					int comparison = (keyValue).compareTo(searchValue);

					// Apply the specified operation based on the comparison result
					if (operation.equals("=")) {
						if (comparison == 0) {
							result.add(tuple);
						}
						if (comparison <= 0) {
							low = mid + 1;
						} else {
							high = mid - 1;
						}
					} else if (operation.equals("!=")) {
						if (comparison != 0) {
							result.add(tuple);
						}
						if (comparison <= 0) {
							low = mid + 1;
						} else {
							high = mid - 1;
						}
					} else if (operation.equals("<")) {
						if (comparison < 0) {
							result.add(tuple);
							high = mid - 1;
						} else {
							high = mid - 1;
						}
					} else if (operation.equals(">")) {
						if (comparison > 0) {
							result.add(tuple);
							low = mid + 1;
						} else {
							low = mid + 1;
						}
					} else if (operation.equals(">=")) {
						if (comparison >= 0) {
							result.add(tuple);
							low = mid + 1;
						} else {
							high = mid - 1;
						}
					} else if (operation.equals("<=")) {
						if (comparison <= 0) {
							result.add(tuple);
							high = mid - 1;
						} else {
							low = mid + 1;
						}
					}
				}
				table.pageFileNames.add(page);
			}
			
			table.serializePageFileNameVector();
			tables.add(table);
			serializeVectorOfTables(tables);

		} catch (Exception e) {
			e.printStackTrace();
		}

		return result;
	}

	public static void serializeIndex(String tableName ,String indexName, Octree octree) throws IOException {

		String fileName = "src/"+ tableName +"/"+ indexName + ".ramsees";

		FileOutputStream f = new FileOutputStream(fileName);
		ObjectOutputStream d = new ObjectOutputStream(f);

		//		Vector<Node> nodes = octree.getNodes();
		//		
		//		for(Node n: nodes) {
		//			d.writeObject(n);
		//		}
		d.writeObject(octree);


		d.close();

	}

	public static Octree deserializeIndex(String tableName, String indexName) throws DBAppException {

		Octree octree=null;

		String fileName = "src/"+ tableName +"/"+ indexName + ".ramsees";

		try {
			FileInputStream f = new FileInputStream(fileName);
			ObjectInputStream d = new ObjectInputStream(f);

			while(true) {
				try {
					Object o = d.readObject();

					if(o instanceof Octree) {
						octree = (Octree) o;
						System.out.println("got octree");
					}
				}catch(Exception e) {
					break;
				}

			}
			d.close();


		}catch(Exception e) {
		}

		//		if(octree == null) {
		//			throw new DBAppException("octree is null, no index found");
		//		}
		return octree;


	}



	//Milestone 1 methods

	public void createTable (String strTableName,
			String strClusteringKeyColumn, Hashtable<String,String> htblColNameType, 
			Hashtable<String,String> htblColNameMin, Hashtable<String,String> htblColNameMax)
					throws DBAppException
	{	

		//deserialize tables vector and add this new table to the vector
		Vector<Table> tables;
		try {
			tables = deserializeVectorOfTables();
			Table table = new Table(strTableName, strClusteringKeyColumn , htblColNameType, htblColNameMin, htblColNameMax );

			if(tables.contains(table)) {
				return;
			}

			writeToDBAppConfig(table);
			tables.add(table);

			addTableToMetaData(table);




			//serialize the new tables vector
			serializeVectorOfTables(tables);
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}



	public static void binarySearchAndInsert(String pk,Vector<Tuple> tuples, Tuple tuple) throws DBAppException {
		int low = 0;
		int high = tuples.size() - 1;

		while (low <= high) {
			int mid = low + (high - low) / 2;
			Tuple currentTuple = tuples.get(mid);

			Comparable tVal = (Comparable) tuple.column_Value.get(pk);
			Comparable cVal = (Comparable) currentTuple.column_Value.get(pk);

			if(tVal.compareTo(cVal) == 0) {
				throw new DBAppException("tuple with primary key value: "+ tuple.column_Value.get(pk)+" already exists");
			}

			if (currentTuple.compareTo(tuple,pk ) < 0) {
				low = mid + 1;
			} else if (currentTuple.compareTo(tuple,pk) > 0) {
				high = mid - 1;
			} else {
				// Handle the case where the tuple already exists at position mid
				throw new DBAppException("tuple with primary key value: "+ tuple.column_Value.get(pk)+" already exists");
				// If needed, update or overwrite the existing tuple
			}
		}

		// At this point, the low index represents the correct position for insertion
		tuples.insertElementAt(tuple, low);
		System.out.println("inserted tuple with primary key value = " + tuple.column_Value.get(pk));
	}

	public static boolean checkInMetadata(String tableName, Hashtable<String,Object> colNameValue ) {


		String csvFile = System.getProperty("user.dir") + "/src/metadata.csv"; // file path to metadata file
		String line = "";
		String[] metadata;

		ArrayList<String> columns = new ArrayList<String>();
		Hashtable<String, Comparable> name_min = new Hashtable<String, Comparable>();
		Hashtable<String, Comparable> name_max = new Hashtable<String, Comparable>();


		try (BufferedReader br = new BufferedReader(new FileReader(csvFile))) {


			while ((line = br.readLine()) != null) {
				metadata = line.split(",");
				if (metadata[0].equalsIgnoreCase(tableName)) {
					String colName = metadata[1];
					String colType = metadata[2];
					boolean isClusteringKey = Boolean.parseBoolean(metadata[3]);
					String indexName = metadata[4];
					String indexType = metadata[5];
					Comparable min = (Comparable)(metadata[6]);
					Comparable max = (Comparable) (metadata[7]);



					if(isClusteringKey) {
						Set<String> set = colNameValue.keySet();

						if(!set.contains(colName)) {
							return false;
						}
					}

					columns.add(colName);
					name_min.put(colName, min);
					name_max.put(colName, max);


				}

			}

			for(String col : colNameValue.keySet()) {

				if(!columns.contains(col)) {
					return false;
				}

				Comparable value = (Comparable) colNameValue.get(col);
				Comparable min = name_min.get(col);
				Comparable max = name_max.get(col);


				if(!(value.compareTo(min) >=0)) {
					return false;
				}

				if(!(value.compareTo(max) <=0)) {
					return false;
				}

			}

		}catch(Exception e) {

		}


		return true;

	}




	public void insertIntoTable(String strTableName, Hashtable<String,Object> htblColNameValue) throws DBAppException{

		try {


			Table table = getTable(strTableName);





			if(table == null) {
				throw new DBAppException("cannot find table with name: "+ strTableName);

			}

			Vector<Table> tables = deserializeVectorOfTables();
			table.pageFileNames = table.deserializePageFileNameVector();
			table.serializePageFileNameVector();	
			tables.remove(table);



			//adjust metadata check

			//			if(!checkInMetadata(strTableName, htblColNameValue)) {
			//				throw new DBAppException("columns or values passed do not match this table's data");
			//			}




			String pK = table.getClusteringKey();
			Set<String> set = htblColNameValue.keySet();
			ArrayList<String> columns = new ArrayList<>(set);

			String indexName = "";
			boolean indexFound = false;

			String xCol="";
			String yCol="";
			String zCol = "";

			for(int i=0; i<columns.size()-2;i++) {
				indexName = strTableName + columns.get(i) + columns.get(i+1) + columns.get(i+2)+"index";
				Octree octree = deserializeIndex(strTableName, indexName);
				if(octree != null) {
					serializeIndex(strTableName, indexName, octree);

					xCol = columns.get(i);
					yCol = columns.get(i+1);
					zCol = columns.get(i+2);

					indexFound=true;
					break;
				}
			}






			Vector<String> pageFiles = table.pageFileNames;
			Tuple tuple = new Tuple(htblColNameValue);
			String pk = table.getClusteringKey();
			String refPage = "";


			if(pageFiles.size() == 0) {
				//new table with no pages
				Page page = new Page();
				page.tuples.add(tuple);
				writeNewPage(table,page);

			}else {

				//create new empty page if last page is full

				String lastPage = table.pageFileNames.lastElement();

				Vector<Tuple> lastTuples = getPageVectorOfTuples(lastPage);

				boolean allPagesFull = false;

				if(lastTuples.size() == 200) {
					Page page = new Page();
					writeNewPage(table,page);
					allPagesFull = true;

				}



				boolean inserted = false;

				for(int i=0; i< pageFiles.size() ; i++) {

					String page = pageFiles.get(i);

					table.pageFileNames.remove(page);

					Vector<Tuple> tuples = getPageVectorOfTuples(page);

					Tuple lastTuple = tuples.lastElement();

					if(!inserted) {

						if(tuples.size()<200) {
							//binary search then insert directly and serialize and break
							//System.out.println("about to insert tuple with reference: "+tuple);
							binarySearchAndInsert(pk,tuples,tuple);
							//	System.out.println("inserted tuple with reference: "+tuple);
							serializePage(page,tuples);

						}else if(allPagesFull){

							//System.out.println("about to insert tuple with reference: "+tuple);

							//remove last tuple then binary search and insert then shift down tuple by tuple and break
							tuples.remove(lastTuple);
							binarySearchAndInsert(pk,tuples,tuple);
							serializePage(page,tuples);

							for(int j= i+1; j<pageFiles.size()-i ; j++) {
								String shiftPage = pageFiles.get(j);
								Vector<Tuple> tmp = getPageVectorOfTuples(shiftPage);
								Tuple tmpTuple = tmp.lastElement();
								tmp.remove(tmpTuple);
								tmp.insertElementAt(lastTuple, 0);
								lastTuple = tmpTuple;
								serializePage(shiftPage,tmp);
							}


						}

						inserted = true;
						refPage = page;

					}

					table.pageFileNames.add(page);

				}

				table.serializePageFileNameVector();
				//resialize table
				serializeTable(table);
				//tables.add(table);
				serializeVectorOfTables(tables);

			}

			if(indexFound) {
				Octree octree = deserializeIndex(strTableName, indexName);

				String pkValue = (String) htblColNameValue.get(pk);
				Comparable xVal = (Comparable) htblColNameValue.get(xCol);
				Comparable yVal = (Comparable) htblColNameValue.get(yCol);
				Comparable zVal = (Comparable) htblColNameValue.get(zCol);

				Reference ref = new Reference(refPage, pkValue, xVal, yVal, zVal);

				octree.insert(ref);


				serializeIndex(strTableName, indexName, octree);

			}




		}catch(Exception e) {
			e.printStackTrace();
		}


	}

	public void updateTable(String strTableName,String strClusteringKeyValue, Hashtable<String,Object> htblColNameValue )throws DBAppException{

		try {
			Table table = getTable(strTableName);
			table.pageFileNames = table.deserializePageFileNameVector();
			table.serializePageFileNameVector();
			Vector<Table> tables = deserializeVectorOfTables();
			tables.remove(table);

			//			Boolean metadataCorrect = checkInMetadata(strTableName, htblColNameValue);
			//			
			//			if(!metadataCorrect) {
			//				throw new DBAppException("data type or column mismatch with table data");
			//
			//			}


			if(table != null) {
				String pK = table.getClusteringKey();
				Set<String> set = htblColNameValue.keySet();
				ArrayList<String> columns = new ArrayList<>(set);

				String indexName = "";
				boolean indexFound = false;

				String xCol="";
				String yCol="";
				String zCol = "";

				for(int i=0; i<columns.size()-2;i++) {
					indexName = strTableName + columns.get(i) + columns.get(i+1) + columns.get(i+2)+"index";
					Octree octree = deserializeIndex(strTableName, indexName);
					if(octree != null) {
						serializeIndex(strTableName, indexName, octree);

						xCol = columns.get(i);
						yCol = columns.get(i+1);
						zCol = columns.get(i+2);

						indexFound=true;
						break;
					}
				}

				if(indexFound) {

					Octree octree = deserializeIndex(strTableName, indexName);

					Reference reference = octree.searchForReference(strClusteringKeyValue);

					String page = reference.fileName;
					table.pageFileNames.remove(page);

					if(reference == null) {
						throw new DBAppException("there is no reference in index for a tuple with primary key value = "+strClusteringKeyValue );
					}

					//change reference data
					for(String column : columns) {

						if(column.equals(xCol)) {

							Comparable xVal = (Comparable) htblColNameValue.get(column);
							reference.setxValue(xVal);

						}
						if(column.equals(yCol)) {

							Comparable yVal = (Comparable) htblColNameValue.get(column);
							reference.setxValue(yVal);
						}
						if(column.equals(zCol)) {
							Comparable zVal = (Comparable) htblColNameValue.get(column);
							reference.setxValue(zVal);

						}

					}
					octree.update(reference);

					Vector<Tuple> tuples = getPageVectorOfTuples(page);

					int low = 0;
					int high = tuples.size()-1;
					int mid = 0;

					while(low<=high) {
						mid = (low + high)/2;
						Tuple t = tuples.get(mid);
						int res = strClusteringKeyValue.compareTo((String) t.column_Value.get(pK));

						if(res>0) {
							low = mid + 1;
						}else if(res<0) {
							high = mid - 1;
						}else {
							t.column_Value.put(xCol, htblColNameValue.get(xCol));
							t.column_Value.put(yCol, htblColNameValue.get(yCol));
							t.column_Value.put(zCol, htblColNameValue.get(zCol));
							break;
						}

					}

					serializePage(page, tuples);
					table.pageFileNames.add(page);
					table.serializePageFileNameVector();
					tables.add(table);
					serializeVectorOfTables(tables);

					serializeIndex(strTableName, indexName, octree);

				}else {

					String primaryKeyColumnName = table.getClusteringKey();
					Vector<String> pageFileNames = table.deserializePageFileNameVector();

					if(pageFileNames.size()<=0) {
						throw new DBAppException("cannot update when table has no rows");

					}


					if(htblColNameValue.keySet().contains(primaryKeyColumnName)) {
						throw new DBAppException("cannot update primary key value");
					}
					if(strClusteringKeyValue == null) {
						throw new DBAppException("primary key value cannot be null");
					}


					for(String page : pageFileNames) {
						table.pageFileNames.remove(page);
						Vector<Tuple> tuples = getPageVectorOfTuples(page);

						boolean isFound=false;
						int beg=0;
						int end=tuples.size()-1;
						int middle=0;

						while(beg<=end) {

							middle = (beg+end)/2;
							Tuple midTuple = tuples.get(middle);

							String pk =""+ midTuple.column_Value.get(primaryKeyColumnName);

							int r = strClusteringKeyValue.compareTo(pk);

							Hashtable<String,Object> tupleHtbl = midTuple.column_Value;

							if(r==0) {


								for(String key : htblColNameValue.keySet()) {

									Object value = htblColNameValue.get(key);
									tupleHtbl.put(key, value);

								}

								break;

							}else if(r>0) {
								end = middle-1;
							}else {
								beg = middle+1;
							}

						}

						//sortVector(tuples,table.getClusteringKey());
						serializePage(page,tuples);
						table.pageFileNames.add(page);

					}
					table.serializePageFileNameVector();
					tables.add(table);
					serializeVectorOfTables(tables);

				}


			}else {
				throw new DBAppException("Table with name: "+strTableName+" was not found");
			}

		}catch(Exception e) {

		}


	}



	public void deleteFromTable(String strTableName, Hashtable<String,Object> htblColNameValue) throws DBAppException{


		boolean hasPK = false;


		try {
			Table table = getTable(strTableName);
			table.pageFileNames = table.deserializePageFileNameVector();
			table.serializePageFileNameVector();


			String pk = table.getClusteringKey();

			Vector<String> pageFiless = table.deserializePageFileNameVector();
			table.serializePageFileNameVector();

			if(pageFiless.size()==0) {
				throw new DBAppException("cannot delete when table has no rows");

			}
			if(htblColNameValue.keySet().contains(pk)) {
				hasPK=true;
			}




			if(table != null) {


				Set<String> set = htblColNameValue.keySet();
				ArrayList<String> columns = new ArrayList<>(set);

				String indexName = "";
				boolean indexFound = false;

				String xCol="";
				String yCol="";
				String zCol = "";

				for(int i=0; i<columns.size()-2;i++) {
					indexName = strTableName + columns.get(i) + columns.get(i+1) + columns.get(i+2)+"index";
					Octree octree = deserializeIndex(strTableName, indexName);
					if(octree != null) {
						serializeIndex(strTableName, indexName, octree);

						xCol = columns.get(i);
						yCol = columns.get(i+1);
						zCol = columns.get(i+2);

						indexFound=true;
						break;
					}
				}


				if(indexFound) {

					//index search

					ArrayList<Reference> references = new ArrayList<Reference>();

					Octree octree = deserializeIndex(strTableName, indexName);
					boolean noMoreReferences = false;

					if(hasPK) {

						String pkValue = (String) htblColNameValue.get(pk);

						while(!noMoreReferences) {
							Reference r = octree.searchForReference(pkValue);

							if(r == null) {
								noMoreReferences = true;
							}else {
								references.add(r);
								octree.delete(r);

							}

						}



					}else {

						Comparable xVal = (Comparable) htblColNameValue.get(xCol);
						Comparable yVal = (Comparable) htblColNameValue.get(yCol);
						Comparable zVal = (Comparable) htblColNameValue.get(zCol);



						while(!noMoreReferences) {
							Reference r = octree.searchForReference(xVal, yVal, zVal);

							if(r == null) {
								noMoreReferences = true;
							}else {
								references.add(r);
								octree.delete(r);

							}

						}

					}

					//remove tuples from pages

					for(Reference r : references) {

						String page = r.fileName;
						table.pageFileNames.remove(page);

						Vector<Tuple> tuples = getPageVectorOfTuples(page);
						Vector<Tuple> newTuples = new Vector<Tuple>();

						for(Tuple tuple : tuples) {

							if(!(tuple.column_Value.get(pk).equals(r.pkValue))) {
								newTuples.add(tuple);
							}
						}

						serializePage(page,newTuples);
						table.pageFileNames.add(page);
						table.serializePageFileNameVector();

					}


					//compact pages






				}else {


					//table search

					//binary search
					if(hasPK) {

						Vector<String> pageFiles = table.deserializePageFileNameVector();
						table.serializePageFileNameVector();
						//					boolean t = isTableMetadataCorrect(strTableName, htblColNameValue);
						//
						//					if(!t) {
						//						return;
						//					}

						String htblPkValue =""+ htblColNameValue.get(pk);

						for(String page: pageFiles) {

							Vector<Tuple> tuples = getPageVectorOfTuples(page);




							int left = 0;
							int right = tuples.size() - 1;

							while (left <= right) {
								int mid = left + (right - left) / 2;
								Tuple tuple = tuples.get(mid);

								// Check if the tuple matches the specified values
								boolean matches = true;
								for (String key : htblColNameValue.keySet()) {
									Object value = htblColNameValue.get(key);
									if (!tuple.column_Value.get(key).equals(value)) {
										matches = false;
										break;
									}
								}

								String tupleValue = "" + tuple.column_Value.get(pk);
								if (matches) {
									tuples.remove(mid);
									right = tuples.size() - 1;
								} else if (htblPkValue.compareTo(tupleValue) > 0) {
									left = mid + 1;
								} else {
									right = mid - 1;
								}
							}

							serializePage(page, tuples);

						}

						//======table.serializePageFileNameVector();


					}


					//linear search
					if(hasPK==false) {
						Vector<String> pageFiles = table.deserializePageFileNameVector();
						boolean flag = false;

						boolean t = isTableMetadataCorrect(strTableName, htblColNameValue);

						if(!t) {
							return;
						}

						for( String page: pageFiles) {

							Vector<Tuple> tuples = getPageVectorOfTuples(page);
							Vector<Tuple> newTuples = new Vector<Tuple>();

							for(Tuple tuple : tuples) {

								Hashtable<String,Object> colValue = tuple.getTupleColumnValues();

								for(String key : htblColNameValue.keySet()) {

									if(colValue.get(key).equals(htblColNameValue.get(key))) {
										flag = true;
									}else {
										flag = false;
										break;
									}

								}

								if(!flag) {
									newTuples.add(tuple);
								}
							}
							serializePage(page, newTuples);

						}
						table.serializePageFileNameVector();

					}


				}

				Vector<String> pageFiles = table.deserializePageFileNameVector();
				Vector<String> pagesToBeDeleted = new Vector<String>();




				for(int i=0 ; i<pageFiles.size(); i++) {

					String page = pageFiles.get(i);
					table.pageFileNames.remove(page);

					Vector<Tuple> tuples = getPageVectorOfTuples(page);

					if(tuples.size()<200) {

						if(i+1<pageFiles.size()) {		
							String nextPage =  pageFiles.get(i+1);
							table.pageFileNames.remove(nextPage);
							Vector<Tuple> nextTuples = getPageVectorOfTuples(nextPage);

							for(int j=0; j<200-tuples.size();j++) {
								tuples.add(nextTuples.get(j));
								if(nextTuples.size()!=0) {
									nextTuples.remove(j);

								}else {

									pagesToBeDeleted.add(page);
								}
							}

							serializePage(nextPage, nextTuples);
							table.pageFileNames.add(nextPage);
							table.serializePageFileNameVector();
						}


					}


					serializePage(page, tuples);
					table.pageFileNames.add(page);
					table.serializePageFileNameVector();


				}


				Vector<String> pages = table.deserializePageFileNameVector();

				for(String p: pagesToBeDeleted) {

					pages.remove(p);
					File file = new File("src/" + table.getTableName() + "/" +p);
					file.delete();
					System.gc();

				}


				Octree octree = deserializeIndex(strTableName, indexName);

				for(String page :pages) {
					Vector<Tuple> tuples = getPageVectorOfTuples(page);

					for(Tuple tuple : tuples) {
						table.pageFileNames.remove(page);
						Object pkValue = tuple.column_Value.get(pk);
						Comparable xVal = (Comparable) tuple.column_Value.get(xCol);
						Comparable yVal = (Comparable) tuple.column_Value.get(yCol);
						Comparable zVal = (Comparable) tuple.column_Value.get(zCol);


						Reference ref = new Reference(page, pkValue, xVal, yVal, zVal);

						octree.update(ref);
						table.pageFileNames.add(page);

					}


				}

				serializeIndex(strTableName, indexName, octree);

				table.serializePageFileNameVector();


				System.out.println("deleted");

			}else {

				throw new DBAppException("Table with name: "+strTableName+" was not found");

			}
		}catch(Exception e) {
			e.printStackTrace();
		}


	}




	// Milestone 1 Helper methods
	public static void serializeVectorOfTables(Vector<Table> tables) throws IOException {
		//		String fileName = "VectorOfTables" + ".ramsees";		
		//		FileOutputStream f = new FileOutputStream(fileName);
		//		ObjectOutputStream d = new ObjectOutputStream(f);
		//
		//		//d.writeObject(tables);
		//
		//		try {
		//			for(int i=0; i<tables.size(); i++) {
		//				d.writeObject(tables.get(i));
		//			}
		//		} finally {
		//			d.close();
		//		}
		String fileName = "VectorOfTables.ramsees";
		RandomAccessFile file = new RandomAccessFile(fileName, "rw");
		file.setLength(0); // truncate the file to 0 bytes
		ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(file.getFD()));

		try {
			for (Table table : tables) {
				out.writeObject(table);
			}
		} finally {
			out.close();
			file.close();
		}
	}

	public static Vector<Table> deserializeVectorOfTables() throws IOException, ClassNotFoundException {
		String file = "VectorOfTables.ramsees";
		FileInputStream f = new FileInputStream(file);
		ObjectInputStream d = new ObjectInputStream(f);
		Vector<Table> tables = new Vector<Table>();

		while (f.available() > 0) {
			Object o = d.readObject();
			if (o instanceof Table) {
				tables.add((Table) o);
			}
		}

		d.close();

		return tables;
	}

	public static Table getTable(String tableName) throws IOException, ClassNotFoundException {

		Vector<Table> tables = deserializeVectorOfTables();


		serializeVectorOfTables(tables);
		//System.out.println(tables.size());

		for(Table table: tables) {
			if(table.getTableName().equals(tableName)) {
				return table;
			}
		}



		return null;



	}

	public static void serializeTable(Table table) throws ClassNotFoundException, IOException {
		Vector<Table> tables = deserializeVectorOfTables();
		tables.add(table);
		serializeVectorOfTables(tables);

	}

	public static Vector<Tuple> readLastPage(Table table) throws IOException, ClassNotFoundException{


		Vector<String> fileNames = table.deserializePageFileNameVector();
		Vector<Tuple> tuples = new Vector<Tuple>();

		if(fileNames.size() > 0) {
			String file = fileNames.get(fileNames.size()-1);
			FileInputStream f = new FileInputStream(file);
			ObjectInputStream d = new ObjectInputStream(f);

			for(int i=0; i<200;i++) {

				Object o = d.readObject();

				if(o instanceof Tuple) {
					Tuple tuple = (Tuple)o;
					tuples.add(tuple);
				}
			}
			d.close();			
		}

		return tuples;
		// if tuples vector's size is 0, then the corresponding table has no pageFiles yet
	}

	public static void writeNewPage(Table table, Page page) throws IOException, ClassNotFoundException {

		String fileName = "src/"+ table.getTableName() +"/" + table.getTableName() + page.getPageID() + ".ramsees";

		Vector<String> pages = table.deserializePageFileNameVector();
		table.pageFileNames = pages;
		table.pageFileNames.add(fileName);

		FileOutputStream f = new FileOutputStream(fileName);
		ObjectOutputStream d = new ObjectOutputStream(f);

		for(int i=0; i<page.tuples.size(); i++) {
			d.writeObject(page.tuples.get(i));
		}


		table.serializePageFileNameVector();
		d.close();
	}

	public static Vector<Tuple> getPageVectorOfTuples(String pageFileName) throws IOException, ClassNotFoundException {

		FileInputStream f = new FileInputStream(pageFileName);
		ObjectInputStream d = new ObjectInputStream(f);

		Vector<Tuple> tuples = new Vector<Tuple>();

		while(true) {

			try {
				Object o = d.readObject();
				if(o instanceof Tuple) {
					Tuple tuple = (Tuple)o;
					tuples.add(tuple);
				}
			}catch(Exception e) {
				break;
			}


		}


		d.close();



		String fileName = "VectorOfTables" + ".ramsees";        
		FileOutputStream ff = new FileOutputStream(fileName);
		ObjectOutputStream dd = new ObjectOutputStream(ff);

		for(Tuple tuple : tuples) {
			dd.writeObject(tuple);
		}

		d.close();

		return tuples;
	}


	public static void serializePage(String pageFileName, Vector<Tuple> tuples) throws IOException {

		FileOutputStream f = new FileOutputStream(pageFileName);
		ObjectOutputStream d = new ObjectOutputStream(f);

		for(int i=0; i<tuples.size(); i++) {
			d.writeObject(tuples.get(i));
		}

		d.close();
	}

	public static void addTableToMetaData(Table table) {

		String metaData = "";
		String pk = table.getClusteringKey();

		for(String key : table.getFields().keySet()) {

			if(key.equals(pk)) {
				metaData += table.getTableName() + "," + key + "," + table.getFields().get(key) + "," + true + "," + null + "," + null + "," + table.getMinValues().get(key) + "," + table.getMaxValues().get(key) + "\n";
			}else {
				metaData += table.getTableName() + "," + key + "," + table.getFields().get(key) + "," + false + "," + null + "," + null + "," + table.getMinValues().get(key) + "," + table.getMaxValues().get(key) + "\n";
			}
		}

		File metadataFile = new File("metadata.csv");
		String filePath = System.getProperty("user.dir") + "/src/metadata.csv";
		String existingMetadata = "";

		try {
			BufferedReader reader = new BufferedReader(new FileReader(filePath));
			String line;
			while ((line = reader.readLine()) != null) {
				existingMetadata += line + "\n";
			}
			reader.close();
		} catch (IOException e) {
			System.out.println("Error reading metadata file: " + e.getMessage());
			return;
		}

		existingMetadata += metaData;


		try {
			FileWriter writer = new FileWriter(filePath);
			writer.write(existingMetadata);
			writer.close();
		} catch (IOException e) {
			System.out.println("Error writing to metadata file: " + e.getMessage());
			return;
		}
	}

	public static boolean verifyTable(String tableName, Hashtable<String,String> colNameValue) throws ClassNotFoundException, IOException {

		Table table = getTable(tableName);
		Hashtable<String, String> fields = table.getFields();
		boolean flag = false;
		for(String key : fields.keySet()) {

			List<String> testKeys = Arrays.asList((String[])colNameValue.keySet().toArray());

			if(testKeys.contains(key)) {

				if(!fields.get(key).equals(colNameValue.get(key))) {
					return false;
				}

			}else {
				return false;
			}

		}

		return true;
	}

	public static void sortVector(Vector<Tuple> tuples, String strClusteringKeyColumn) {
		for (int i = 0; i < tuples.size()-1; i++) {

			for (int j = 0; j < tuples.size()-1 - i - 1; j++) {


				String value1 = ""+ tuples.get(j).column_Value.get(strClusteringKeyColumn);
				String value2 = ""+ tuples.get(j + 1).column_Value.get(strClusteringKeyColumn);


				if (value1.compareTo(value2) > 0) {
					tuples.set(j, tuples.get(j + 1));
					tuples.set(j + 1, tuples.get(j));
				}
			}
		}
	}

	public static boolean isTableMetadataCorrect(String tableName, Hashtable<String, Object> values) {
		String csvFile = System.getProperty("user.dir") + "/src/metadata.csv"; // file path to metadata file
		String line = "";
		String[] metadata;
		boolean isCorrect = true;

		Table table;

		try (BufferedReader br = new BufferedReader(new FileReader(csvFile))) {
			if(getTable(tableName)!=null) {
				table = getTable(tableName);
			} else {
				throw new DBAppException("Table '" + tableName + "' was not found.");
			}

			Hashtable<String,String> htblColNameType = table.getFields();
			Hashtable<String,String> htblColNameMin = table.getMinValues();
			Hashtable<String,String> htblColNameMax = table.getMaxValues();

			while ((line = br.readLine()) != null) {
				metadata = line.split(",");
				if (metadata[0].equalsIgnoreCase(tableName)) {
					String colName = metadata[1];
					String colType = metadata[2];
					boolean isClusteringKey = Boolean.parseBoolean(metadata[3]);
					String indexName = metadata[4];
					String indexType = metadata[5];
					Object min = (metadata[6]);
					Object max = (metadata[7]);

					if(!htblColNameType.containsKey(colName)) {
						isCorrect = false;
						break;
					}

					if(!htblColNameType.get(colName).equals(colType)) {
						isCorrect = false;
						break;
					}

					if(isClusteringKey != colName.equals(table.getClusteringKey())) {
						isCorrect = false;
						break;
					}

					if(indexName != null && indexType != null) {
						isCorrect = false;
						break;
					}

					Comparable minValue = (Comparable) min;
					Comparable maxValue = (Comparable) max;

					if (minValue != null && maxValue != null) {
						String valueStr = (String) values.get(colName);
						Comparable value = null;
						if (valueStr != null && !valueStr.equals("null")) {
							value = (Comparable) values.get(colName);
						}
						if(value != null && (value.compareTo(minValue) < 0 || value.compareTo(maxValue) > 0)) {
							isCorrect = false;
							break;
						}
					}
				}
			}

			Hashtable<String, String> tmp = new Hashtable<String, String>();
			for(String key : htblColNameType.keySet()) {
				tmp.put(key.toLowerCase(), htblColNameType.get(key));
			}

			for(String key : values.keySet()) {
				if(!tmp.containsKey(key.toLowerCase())) {
					return false;
				}

				Object value = values.get(key);
				Object ogVal = tmp.get(key.toLowerCase());

				if((value == null || ogVal == null)) {
					return false;
				}

				if(!(value.getClass().getName().equals(ogVal))) {
					return false;
				}

				Comparable minVal = (Comparable) htblColNameMin.get(key);
				Comparable maxVal = (Comparable) htblColNameMax.get(key);

				Comparable val = (Comparable) values.get(key);

				if(!(val.compareTo(minVal) >= 0 && val.compareTo(maxVal) <= 0) && !val.equals("null")) {
					return false;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return isCorrect;
	}

	public static void writeToDBAppConfig(Table table) {


		String filePath = System.getProperty("user.dir") + "/src/DBApp.config";
		try {
			FileWriter writer = new FileWriter(filePath);
			String data = "";

			data+="MaxPageRowCount = 200;" +"\n";
			data+="TableName = "+table.getTableName() +"\n";

			writer.write(data);
			writer.close();

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}


	}

	public static int binarySearch(Vector<Tuple> vec, Tuple t, String clusteringKey) {
		int low = 0;
		int high = vec.size() - 1;
		int mid;

		while (low <= high) {
			mid = (low + high) / 2;
			Tuple midTuple = vec.get(mid);
			Object midKey = midTuple.column_Value.get(clusteringKey);

			int cmp = ((Comparable)midKey).compareTo(t.column_Value.get(clusteringKey));
			if (cmp < 0)
				low = mid + 1;
			else if (cmp > 0)
				high = mid - 1;
			else
				return mid + 1; 

		}
		return low; 
	}

	public static void printTable(Table t) throws IOException, ClassNotFoundException {

		Vector<String> pages = t.deserializePageFileNameVector();
		t.serializePageFileNameVector();

		Vector<Table> tables = deserializeVectorOfTables();
		tables.remove(t);

		for(String page: pages) {
			t.pageFileNames.remove(page);
			Vector<Tuple> tuples = getPageVectorOfTuples(page);
			serializePage(page, tuples);
			for(Tuple tuple : tuples) {
				Object value = tuple.column_Value.get(t.getClusteringKey());
				System.out.println(value);
			}
			t.pageFileNames.add(page);
		}
		tables.add(t);
		serializeVectorOfTables(tables);

	}

	
    public static Hashtable<String, Object> parseHashtable(Hashtable<String, String> hashtable) {
        Hashtable<String, Object> parsedHashtable = new Hashtable<>();

        for (String key : hashtable.keySet()) {
            String value = hashtable.get(key);
            Object parsedValue = parseValue(value);
            parsedHashtable.put(key, parsedValue);
        }

        return parsedHashtable;
    }

    private static Object parseValue(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e1) {
            try {
                return Double.parseDouble(value);
            } catch (NumberFormatException e2) {
                try {
                    SimpleDateFormat dateFormat = new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy");
                    return dateFormat.parse(value);
                } catch (ParseException e3) {
                    // If parsing as a date fails, return the value as a string
                    return value;
                }
            }
        }
    }
	
	
	
	
	


	public static void main(String[] args) throws DBAppException {
		// TODO Auto-generated method stub

		DBApp app = new DBApp();
		//System.gc();
		//app.init();

		Integer ii = 0;
		Double d =  1.2;
		String s ="sss";
		Date c = new Date();
		
		
		 long milliseconds = 0L; // Number of milliseconds since January 1, 1970, 00:00:00 GMT
	       Date mindate = new Date(milliseconds);
	       
	       
	       int year = 2025;
	        int month = 0; // January is represented by 0 in Java's Date class
	        int day = 1;
	        int hour = 0;
	        int minute = 0;
	        int second = 0;

	        // Create a Calendar object and set the desired date components
	        java.util.Calendar calendar = java.util.Calendar.getInstance();
	        calendar.set(year, month, day, hour, minute, second);

	        // Get the corresponding Date object
	        Date maxdate = calendar.getTime();
		
		

		String tableName = "Students";
		String pkName = "ID";
		Hashtable<String,String> nameType = new Hashtable<String,String>();
		Hashtable<String,String> nameMin = new Hashtable<String,String>();
		Hashtable<String,String> nameMax = new Hashtable<String,String>();

		nameType.put("ID", ""+ii.getClass());
		nameType.put("Name", ""+s.getClass());
		nameType.put("birthday", ""+c.getClass());
		nameType.put("GPA", ""+d.getClass());


		nameMin.put("ID","0");
		nameMin.put("Name", "a");
		nameMin.put("birthday",""+ mindate);
		nameMin.put("GPA", "0.0");

		nameMax.put("ID", "10000");
		nameMax.put("Name", "ZZZZ");
		nameMax.put("birthday", ""+maxdate);
		nameMax.put("GPA",""+ "4.0");

		Hashtable<String,Object> tuple1 = new Hashtable<String,Object>();
		tuple1.put(pkName, 10);
		tuple1.put("Name", "ahm");
		tuple1.put("birthday", c);
		tuple1.put("GPA", 3.5);
		Hashtable<String,Object> tuple2 = new Hashtable<String,Object>();
		tuple2.put(pkName, 20);
		tuple2.put("Name", "ahm");
		tuple2.put("birthday", c);
		tuple2.put("GPA", 3.5);
		Hashtable<String,Object> tuple3 = new Hashtable<String,Object>();
		tuple3.put(pkName, 30);
		tuple3.put("Name", "ahm");
		tuple3.put("birthday", c);
		tuple3.put("GPA", 3.5);
		Hashtable<String,Object> tuple4 = new Hashtable<String,Object>();
		tuple4.put(pkName, 40);
		tuple4.put("Name", "ahm");
		tuple4.put("birthday", c);
		tuple4.put("GPA", 3.5);
		Hashtable<String,Object> tuple6=  new Hashtable<String,Object>();
		tuple6.put(pkName, 42);
		tuple6.put("Name", "ahm");
		tuple6.put("birthday", c);
		tuple6.put("GPA", 3.6);

		//		
		//		System.out.println((""+1202).compareTo(""+10000));
		//		
		//Vector<Table> tables = new Vector<Table>();
		//		


		try {
			//app.createTable(tableName,pkName, nameType, nameMin, nameMax);
//						app.insertIntoTable(tableName, tuple1);
//						app.insertIntoTable(tableName, tuple2);
//						app.insertIntoTable(tableName, tuple3);
//						app.insertIntoTable(tableName, tuple4);
//						app.insertIntoTable(tableName, tuple6);

			//app.updateTable(tableName, "6", tuple);
			//app.deleteFromTable(tableName, tuple);




//									SQLTerm t1 = new SQLTerm(tableName, pkName ,">",10);
//									SQLTerm t2 = new SQLTerm(tableName, "GPA" ,">",3.5);
//						
//									SQLTerm[] terms = {t1,t2};
//									String[] arr= {"AND"};
//						
//						
//									Iterator x =  app.selectFromTable(terms, arr);
//						
//									while (x.hasNext()) {
//										Tuple element = (Tuple) x.next();
//										System.out.println(element.column_Value.get(pkName));
//									}

			
		       
		       
//		        
//			Dimension xColumn = new Dimension(pkName, ""+ pkName.getClass(), 0, 10000);
////			System.out.println(y+" "+yType+" "+yMin+" "+yMax);
//			Dimension yColumn = new Dimension("GPA", ""+ d.getClass(), 0.0, 4.0);
//			Dimension zColumn = new Dimension("birthday",  ""+c.getClass(), mindate, maxdate);
//
////			System.out.println(z+" "+zType+" "+zMin+" "+zMax);
//
//			Octree octree = new Octree(xColumn, yColumn, zColumn);
//
//			
////			public String fileName;
////			public Object pkValue;
////			public Comparable xValue;
////			public Comparable yValue;
////			public Comparable zValue;
//				
//			octree.insert(new Reference("7amo.csv", 42,42,3.6,c));


			String[] cols = {"Name","birthday", "GPA" };
			app.createIndex(tableName, cols);
			//	

			//serializeIndex(tableName, "S  tudentsNamebirthdayGPAindex", oct);

			//			Octree oct = deserializeIndex(tableName,"StudentsNamebirthdayGPAindex" );
			//			
			//			System.out.print(oct); 
			//			
			//			if(oct != null) {
			//				//oct.printOctree();
			//				System.out.println("root = " + oct.root);
			//				System.out.println("root refs = " + oct.root.references.size()); 
			//
			//
			//				for(Node n: oct.root.children) {
			//					System.out.println(n); 
			//
			//				}
			//				
			//				serializeIndex(tableName, "StudentsNamebirthdayGPAindex", oct);
			//			}


		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}




}

