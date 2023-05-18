package Backend.Index;

import java.io.Serializable;
import java.util.ArrayList;

public class Node implements Serializable{

	public ArrayList<Reference> references;
	public Node[] children;
	public Comparable xMin;
	public Comparable xMax;
	public Comparable yMin;
	public Comparable yMax;
	public Comparable zMin;
	public Comparable zMax;
	public int capacity;
	public boolean splitted;

	public Node(Comparable xMin, Comparable xMax, Comparable yMin, Comparable yMax,
			Comparable zMin, Comparable zMax) {


		this.capacity = 2;
		this.references = new ArrayList<Reference>(capacity);

		this.xMin = xMin;
		this.xMax = xMax;
		this.yMin = yMin;
		this.yMax = yMax;
		this.zMin = zMin;
		this.zMax = zMax;

		this.children = new Node[8];
		splitted=false;
	}


	public boolean encloses(Reference r) {
		return this.xMin.compareTo(r.xValue) <= 0 &&
				this.xMax.compareTo(r.xValue) >= 0 &&
				this.yMin.compareTo(r.yValue) <= 0 &&
				this.yMax.compareTo(r.yValue) >= 0 &&
				this.zMin.compareTo(r.zValue) <= 0 &&
				this.zMax.compareTo(r.zValue) >= 0;
	}


	//	


}



//x  0-100  0-49, 50-100
//y 0-20	0-4, 5-10
//z 0-50	0-24, 25-50






