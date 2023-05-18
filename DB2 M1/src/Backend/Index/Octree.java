package Backend.Index;

import java.util.Date;
import java.util.Iterator;
import java.util.Vector;

import Exceptions.DBAppException;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;

public class Octree implements Serializable{

	public Node root;
	public Dimension x;
	public Dimension y;
	public Dimension z;

	public Octree(Dimension x, Dimension y, Dimension z) {

		this.x=x;
		this.y=y;
		this.z=z;

		root = new Node(x.columnMin,x.columnMax,y.columnMin,y.columnMax,z.columnMin,z.columnMax);
	}
	
	
	public Vector<Node> getNodes(){
		
		Vector<Node> nodes = new Vector<Node>();
		
		 getNodesRec(this.root, nodes);
		 
		 return nodes;
	}
	
	public void getNodesRec(Node n, Vector<Node> nodes) {
		
		if(!n.splitted) {
			nodes.add(n);
		}else {
			for(Node child : n.children) {
				getNodesRec(child, nodes);
			}
		}
	}
	

	public void delete(Reference r) throws DBAppException {

		deleteRec(this.root, r);

	}


	public void deleteRec(Node n, Reference r) throws DBAppException {

		if(!n.splitted) {

			if(r.xValue.compareTo(n.xMin) >=0 && r.xValue.compareTo(n.xMax)<=0 && r.yValue.compareTo(n.yMin) >=0 && r.yValue.compareTo(n.yMax)<=0
					&& r.zValue.compareTo(n.zMin) >=0 && r.zValue.compareTo(n.zMax)<=0) {

				if(n.references.contains(r)) {
					n.references.remove(r);

					return;
				}else {
					throw new DBAppException("reference was not found or may have been already deleted");
				}

			}


		}else {
			for(Node child: n.children) {
				deleteRec(child, r);
			}

		}


	}


	public Reference searchForReference(String pk) throws DBAppException {

		return searchRec(this.root, pk);
	}
	public Reference searchRec(Node n, String pkValue) throws DBAppException {

		if (!n.splitted) {
			boolean referenceFound = false;
			Iterator<Reference> iterator = n.references.iterator();
			while (iterator.hasNext()) {
				Reference ref = iterator.next();
				if (ref.pkValue.equals(pkValue)) {
					referenceFound = true;
					return ref; 
				}
			}

			if (!referenceFound) {
				throw new DBAppException("Reference to tuple was not found in index");
			}


		}
		else {
			for (Node node : n.children) {
				Reference result = searchRec(node, pkValue);
				if (result != null) {
					return result;
				}
			}
		}


		return null;
	}



	public Vector<Reference> searchWithCondition(String col, String condition, Comparable val){
		Vector<Reference> refs = new Vector<Reference>();
		searchConRec(this.root, col, condition, val, refs);
		return refs;

	}

	public void searchConRec(Node n,String col, String condition, Comparable val, Vector<Reference> refs){

		if(col.equals(this.x.columnName)) {

			if(condition.equals("=")) {

				if(!n.splitted) {

					for(Reference r : n.references) {
						if(r.xValue.compareTo(val) == 0) {
							refs.add(r);
						}
					}


				}else {
					for(Node node: n.children) {
						searchConRec(node, col, condition, val, refs);
					}
				}


			}
			if(condition.equals("!=")) {


				if(!n.splitted) {

					for(Reference r : n.references) {
						if(!(r.xValue.compareTo(val) == 0)) {
							refs.add(r);
						}
					}


				}else {
					for(Node node: n.children) {
						searchConRec(node, col, condition, val, refs);
					}
				}

			}if(condition.equals(">")) {

				if(!n.splitted) {

					for(Reference r : n.references) {
						if(val.compareTo(r.xValue) > 0) {
							refs.add(r);
						}
					}


				}else {
					for(Node node: n.children) {
						searchConRec(node, col, condition, val, refs);
					}
				}

			}if(condition.equals("<")) {

				if(!n.splitted) {

					for(Reference r : n.references) {
						if(val.compareTo(r.xValue) < 0) {
							refs.add(r);
						}
					}


				}else {
					for(Node node: n.children) {
						searchConRec(node, col, condition, val, refs);
					}
				}

			}if(condition.equals(">=")) {

				if(!n.splitted) {

					for(Reference r : n.references) {
						if(val.compareTo(r.xValue) >= 0) {
							refs.add(r);
						}
					}


				}else {
					for(Node node: n.children) {
						searchConRec(node, col, condition, val, refs);
					}
				}

			}if(condition.equals("<=")) {

				if(!n.splitted) {

					for(Reference r : n.references) {
						if(val.compareTo(r.xValue) <= 0) {
							refs.add(r);
						}
					}


				}else {
					for(Node node: n.children) {
						searchConRec(node, col, condition, val, refs);
					}
				}
			}


		}
		if(col.equals(this.y.columnName)) {

			if(condition.equals("=")) {

				if(!n.splitted) {

					for(Reference r : n.references) {
						if(r.yValue.compareTo(val) == 0) {
							refs.add(r);
						}
					}


				}else {
					for(Node node: n.children) {
						searchConRec(node, col, condition, val, refs);
					}
				}


			}
			if(condition.equals("!=")) {


				if(!n.splitted) {

					for(Reference r : n.references) {
						if(!(r.yValue.compareTo(val) == 0)) {
							refs.add(r);
						}
					}


				}else {
					for(Node node: n.children) {
						searchConRec(node, col, condition, val, refs);
					}
				}

			}if(condition.equals(">")) {

				if(!n.splitted) {

					for(Reference r : n.references) {
						if(val.compareTo(r.yValue) > 0) {
							refs.add(r);
						}
					}


				}else {
					for(Node node: n.children) {
						searchConRec(node, col, condition, val, refs);
					}
				}

			}if(condition.equals("<")) {

				if(!n.splitted) {

					for(Reference r : n.references) {
						if(val.compareTo(r.yValue) < 0) {
							refs.add(r);
						}
					}


				}else {
					for(Node node: n.children) {
						searchConRec(node, col, condition, val, refs);
					}
				}

			}if(condition.equals(">=")) {

				if(!n.splitted) {

					for(Reference r : n.references) {
						if(val.compareTo(r.yValue) >= 0) {
							refs.add(r);
						}
					}


				}else {
					for(Node node: n.children) {
						searchConRec(node, col, condition, val, refs);
					}
				}

			}if(condition.equals("<=")) {

				if(!n.splitted) {

					for(Reference r : n.references) {
						if(val.compareTo(r.yValue) <= 0) {
							refs.add(r);
						}
					}


				}else {
					for(Node node: n.children) {
						searchConRec(node, col, condition, val, refs);
					}
				}
			}



		}
		if(col.equals(this.z.columnName)) {





			if(condition.equals("=")) {

				if(!n.splitted) {

					for(Reference r : n.references) {
						if(r.zValue.compareTo(val) == 0) {
							refs.add(r);
						}
					}


				}else {
					for(Node node: n.children) {
						searchConRec(node, col, condition, val, refs);
					}
				}


			}
			if(condition.equals("!=")) {


				if(!n.splitted) {

					for(Reference r : n.references) {
						if(!(r.zValue.compareTo(val) == 0)) {
							refs.add(r);
						}
					}


				}else {
					for(Node node: n.children) {
						searchConRec(node, col, condition, val, refs);
					}
				}

			}if(condition.equals(">")) {

				if(!n.splitted) {

					for(Reference r : n.references) {
						if(val.compareTo(r.zValue) > 0) {
							refs.add(r);
						}
					}


				}else {
					for(Node node: n.children) {
						searchConRec(node, col, condition, val, refs);
					}
				}

			}if(condition.equals("<")) {

				if(!n.splitted) {

					for(Reference r : n.references) {
						if(val.compareTo(r.zValue) < 0) {
							refs.add(r);
						}
					}


				}else {
					for(Node node: n.children) {
						searchConRec(node, col, condition, val, refs);
					}
				}

			}if(condition.equals(">=")) {

				if(!n.splitted) {

					for(Reference r : n.references) {
						if(val.compareTo(r.zValue) >= 0) {
							refs.add(r);
						}
					}


				}else {
					for(Node node: n.children) {
						searchConRec(node, col, condition, val, refs);
					}
				}

			}if(condition.equals("<=")) {

				if(!n.splitted) {

					for(Reference r : n.references) {
						if(val.compareTo(r.zValue) <= 0) {
							refs.add(r);
						}
					}


				}else {
					for(Node node: n.children) {
						searchConRec(node, col, condition, val, refs);
					}
				}
			}




		}


	}

	public Reference searchForReference(Comparable xVal, Comparable yVal, Comparable zVal) throws DBAppException {

		return searchRec(this.root,xVal,  yVal,  zVal);
	}
	public Reference searchRec(Node n, Comparable xVal, Comparable yVal, Comparable zVal) throws DBAppException {

		if (!n.splitted) {
			boolean referenceFound = false;
			Iterator<Reference> iterator = n.references.iterator();
			while (iterator.hasNext()) {
				Reference ref = iterator.next();
				if (ref.xValue.equals(xVal) && ref.yValue.equals(yVal) && ref.zValue.equals(zVal)) {
					referenceFound = true;
					return ref; 
				}
			}

			if (!referenceFound) {
				throw new DBAppException("Reference to tuple was not found in index");
			}


		}
		else {
			for (Node node : n.children) {
				Reference result = searchRec(node, xVal, yVal, zVal);
				if (result != null) {
					return result;
				}
			}
		}


		return null;
	}




	public void update(Reference r) throws DBAppException {
		updateRec(this.root, r);
	}
	public void updateRec(Node n, Reference r) throws DBAppException {
	
		
		if(!n.splitted) {
			
			//boolean referenceFound = false;

			//System.out.println(n.references.size());
			for(Reference ref : n.references) {
				
				Comparable refVal = (Comparable) ref.pkValue;
				Comparable rVal = (Comparable) r.pkValue;
				
				if(refVal.compareTo(rVal) == 0) {
					//referenceFound = true;
					ref.fileName = r.fileName;
					ref.xValue = r.xValue;
					ref.yValue = r.yValue;
					ref.zValue = r.zValue;
					return;
				}
				
			}
			
//			if(!referenceFound) {
//				throw new DBAppException("reference was not find to update");
//			}


		}else {
			for(Node child: n.children) {
				updateRec(child, r);
			}

		}



	}



	public Node searchForNodeToInsert(Reference r) {

		return searchNodeRec(this.root, r);
	}
	public Node searchNodeRec(Node n, Reference r) {


		Comparable xValue = r.xValue;
		Comparable yValue = r.yValue;
		Comparable zValue = r.zValue;


		if((xValue.compareTo(n.xMin)>=0 && xValue.compareTo(n.xMax)<0) &&
				(yValue.compareTo(n.yMin)>=0 && yValue.compareTo(n.yMax)<0)&&
				(zValue.compareTo(n.zMin)>=0 && zValue.compareTo(n.zMax)<0)){

			return n;

		}else {
			for (Node node : n.children) {
				Node result = searchNodeRec(node, r);
				if (result != null) {
					return result;
				}
			}
		}
		return null;
	}


	public void insert(Reference r) throws DBAppException {
		if (root == null) {
			root = new Node(x.columnMin, x.columnMax, y.columnMin, y.columnMax, z.columnMin, z.columnMax);
		}
		insertRec(root, r);

	}

	public void insertRec(Node n, Reference r) throws DBAppException {	

		if(!n.splitted) {
			System.out.println("found leaf node");
			
			
			

			if(r.xValue.compareTo(n.xMin) >=0 && r.xValue.compareTo(n.xMax)<=0 && r.yValue.compareTo(n.yMin) >=0 && r.yValue.compareTo(n.yMax)<=0
					&& r.zValue.compareTo(n.zMin) >=0 && r.zValue.compareTo(n.zMax)<=0) {
				
				
					
				System.out.println("found suitable leaf node");

				if(n.references.size()<n.capacity) {
					n.references.add(r);
					System.out.println("inserted reference: " +r+" ("+r.pkValue+")"+" in octree");
					return;
				}else {
					splitNode(n);
					for(Node child: n.children) {
						insertRec(child, r);
					}

				}

			}


		}else {

			for(Node child: n.children) {
				insertRec(child, r);
			}

		}



	}










	public void printOctree() {
		printNode(this.root);
	}

	private void printNode(Node node) {
		System.out.println(Arrays.toString(node.references.toArray()));
		for (Node child : node.children) {
			if (child != null) {
				printNode(child);
			}
		}
	}





	public static Comparable[] splitRange(Dimension d) {

		Object max = d.columnMax;

		if(max instanceof Integer) {

			//			int min1 = (Integer)d.columnMin;
			//			int max1 = (((Integer)max - (Integer) d.columnMin)/2);
			//			int min2 = (((Integer)max - (Integer) d.columnMin)/2)+1;
			//			int max2 = (Integer)d.columnMax;
			//			return new Comparable[] {min1, max1, min2, max2};
			int min1 = (Integer) d.columnMin;
			int max1 = ((Integer) max - (Integer) d.columnMin) / 2 + (Integer) d.columnMin;
			int min2 = ((Integer) max - (Integer) d.columnMin) / 2 + 1 + (Integer) d.columnMin;
			int max2 = (Integer) d.columnMax;
			return new Comparable[]{min1, max1, min2, max2};


		}
		else if(max instanceof Double) {
			//			Double min1 = (Double)d.columnMin;
			//			Double max1 = (((Double)max - (Double) d.columnMin)/2);
			//			Double min2 = (((Double)max - (Double) d.columnMin)/2)+1;
			//			Double max2 = (Double)d.columnMax;
			//			return new Comparable[] {min1, max1, min2, max2};
			Double min1 = (Double) d.columnMin;
			Double max1 = ((Double) max - (Double) d.columnMin) / 2 + (Double) d.columnMin;
			Double min2 = ((Double) max - (Double) d.columnMin) / 2 + 1 + (Double) d.columnMin;
			Double max2 = (Double) d.columnMax;
			return new Comparable[]{min1, max1, min2, max2};


		}
		else if(max instanceof Date) {
			Date a = (Date)d.columnMin;
			Date b = (Date)d.columnMax;

			long startMillis = a.getTime();
			long endMillis = b.getTime();
			long midpointMillis = (startMillis + endMillis) / 2;


			Calendar calendar = Calendar.getInstance();

			Date mid = new Date(midpointMillis);
			calendar.setTime(mid);
			calendar.add(Calendar.DAY_OF_MONTH, 1);

			Date min1= (Date)d.columnMin;
			Date max1 = (Date) mid;
			Date min2 = calendar.getTime();
			Date max2 = (Date)d.columnMax;
			return new Comparable[] {min1, max1, min2, max2};
		}
		else {
			String min1 = (String)d.columnMin;
			String max1 = getStringRangeMedian((String)d.columnMin,(String)d.columnMax);

			char c = max1.charAt(max1.length()-1);
			c++;

			String min2	= max1.substring(0,max1.length()-1) + c;
			String max2 = (String)d.columnMax;

			return new Comparable[] {min1, max1, min2, max2};

		}
	}




	public void splitNode(Node n) {
		ArrayList<Reference> tmp = new ArrayList<>(n.references);
		n.references.clear();

		Dimension nodeX = new Dimension(this.x.columnName, this.x.columnType, n.xMin, n.xMax);
		Dimension nodeY = new Dimension(this.y.columnName, this.y.columnType, n.yMin, n.yMax);
		Dimension nodeZ = new Dimension(this.z.columnName, this.z.columnType, n.zMin, n.zMax);

		Comparable[] splitX = splitRange(nodeX);
		Comparable[] splitY = splitRange(nodeY);
		Comparable[] splitZ = splitRange(nodeZ);

		Comparable x1min = splitX[0];
		Comparable x1max = splitX[1];
		Comparable x2min = splitX[2];
		Comparable x2max = splitX[3];

		Comparable y1min = splitY[0];
		Comparable y1max = splitY[1];
		Comparable y2min = splitY[2];
		Comparable y2max = splitY[3];

		Comparable z1min = splitZ[0];
		Comparable z1max = splitZ[1];
		Comparable z2min = splitZ[2];
		Comparable z2max = splitZ[3];

		Node n1 = new Node(x1min, x1max, y1min, y1max, z1min, z1max);
		Node n2 = new Node(x1min, x1max, y1min, y1max, z2min, z2max);
		Node n3 = new Node(x1min, x1max, y2min, y2max, z1min, z1max);
		Node n4 = new Node(x1min, x1max, y2min, y2max, z2min, z2max);
		Node n5 = new Node(x2min, x2max, y1min, y1max, z1min, z1max);
		Node n6 = new Node(x2min, x2max, y1min, y1max, z2min, z2max);
		Node n7 = new Node(x2min, x2max, y2min, y2max, z1min, z1max);
		Node n8 = new Node(x2min, x2max, y2min, y2max, z2min, z2max);

		n.children = new Node[]{n1, n2, n3, n4, n5, n6, n7, n8};

		for (Reference ref : tmp) {
			for (Node child : n.children) {
				if (child.encloses(ref)) {
					child.references.add(ref);
					break;
				}
			}
		}

		n.splitted = true;
	}




	public static String getStringRangeMedian(String S, String T)
	{
		// Stores the base 26 digits after addition
		int N=0;

		if(S.length()>T.length()) {
			N= S.length();
			//			T+= S.substring(T.length()-1);
			String tmp = T;
			while(tmp.length()<S.length()) {
				tmp+=S.charAt(tmp.length());
			}
			T = tmp;

		}
		else {
			N=T.length();
			//S+= T.substring(S.length()-1);
			String tmp = S;
			while(tmp.length()<T.length()) {
				tmp+=T.charAt(tmp.length());
			}
			S=tmp;
		}



		//S=S.toLowerCase();
		//T=T.toLowerCase();

		int[] a1 = new int[N + 1];

		for (int i = 0; i < N; i++) {
			a1[i + 1] = (int)S.charAt(i) - 97
					+ (int)T.charAt(i) - 97;
		}

		// Iterate from right to left
		// and add carry to next position
		for (int i = N; i >= 1; i--) {
			a1[i - 1] += (int)a1[i] / 26;
			a1[i] %= 26;
		}

		// Reduce the number to find the middle
		// string by dividing each position by 2
		for (int i = 0; i <= N; i++) {

			// If current value is odd,
			// carry 26 to the next index value
			if ((a1[i] & 1) != 0) {

				if (i + 1 <= N) {
					a1[i + 1] += 26;
				}
			}

			a1[i] = (int)a1[i] / 2;
		}

		String res = "";
		for (int i = 1; i <= N; i++) {
			//System.out.print((char)(a1[i] + 97));
			res+= (char)(a1[i] + 97);

		}

		return res;
	}



	public static void main(String[] args) {

		Dimension x = new Dimension("name", "Java.Lang.Integer", 0, 20);
		Dimension y = new Dimension("age", "Java.Lang.Integer", 0,100);
		Dimension z = new Dimension("ID", "Java.Lang.Integer", 0,50);

		Octree octree = new Octree(x,y,z);
		//System.out.println("run");


		Reference r = new Reference("file.csv", "aaa", 0, 12, 12);
		Reference e = new Reference("file.csv", "bbb", 17, 49, 10);
		Reference a = new Reference("file.csv", "sas", 10, 30, 34);
		Reference b = new Reference("file.csv", "ss", 11, 20, 11);
		Reference c = new Reference("file.csv", "wer", 12, 56, 2);
		Reference d = new Reference("file.csv", "zxsx", 9, 21, 0);
		Reference rr = new Reference("file.csv", "aaa1", 0, 12, 12);
		Reference ee = new Reference("file.csv", "bbb1", 17, 49, 10);
		Reference aa = new Reference("file.csv", "sas1", 10, 30, 34);
		Reference bb = new Reference("file.csv", "ss1", 11, 20, 11);
		Reference cc = new Reference("file.csv", "wer1", 12, 56, 2);
		Reference dd = new Reference("file.csv", "zxsx1", 9, 21, 0);


		try {


			//			Node n = octree.searchForNodeToInsert(r);
			//			Node w = octree.searchForNodeToInsert(e);
			//			Node v = octree.searchForNodeToInsert(b);

			octree.insert(r);
			octree.insert(a);
			octree.insert(e);
			octree.insert(b);
			octree.insert(c);	
			octree.insert(d);
			//			

			octree.insert(rr);
			octree.insert(aa);		
			octree.insert(ee);
			octree.insert(bb);
			octree.insert(cc);	
			octree.insert(dd);


			r.setFileName("7amo.csv");
			a.setFileName("maro.csv");

			octree.update(r);
			octree.update(a);

			octree.printOctree();

		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}



		//octree.printOctree();



	}




}