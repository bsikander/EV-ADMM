package admm;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.Writable;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.map.ObjectMapper;

/*
 * This class is responsible for serializing and deserializing the data that slaves sends to master using the Jackson library.
 */
public class NetworkObjectSlave implements Writable{
	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
	
//	@JsonProperty("x_i")
//	double[] x_i;
//	
//	@JsonProperty("EVId")
//	private int EVId;
	
	@JsonProperty("xSubAverage")
	double[] xSubAverage;
	
//	@JsonProperty("sumAgainstEVNo")
//	Map<Integer,Integer> sumAgainstEVNo;
	
	
	
//	@JsonProperty("x_i_difference")
//	private double[] x_i_difference;
//	
//	@JsonProperty("cost")
//	private double cost;
	
	/*
	 * Parameterized constructor
	 */
	//public NetworkObjectSlave(double[] x_i, int EVId, double[] x_i_difference, double cost)
	//public NetworkObjectSlave(double[] x_i, int EVId)
	public NetworkObjectSlave(double[] xSubAverage)//, Map<Integer,Integer> sumAgainstEVNo)
	{
		this.xSubAverage = xSubAverage;
		
//		this.x_i_difference = x_i_difference;
//		this.cost = cost;
	}

	public void setNetworkObjectSlave(NetworkObjectSlave n)
	{
		this.xSubAverage = n.xSubAverage;
		//this.sumAgainstEVNo = n.sumAgainstEVNo;
//		this.x_i_difference = n.x_i_difference;
//		this.cost = n.cost;
	}
	
	public NetworkObjectSlave()
	{}
	
	@Override
	public void readFields(DataInput in) throws IOException {
		Text contextJson = new Text();
		contextJson.readFields(in);
		setNetworkObjectSlave(OBJECT_MAPPER.readValue(contextJson.toString(), NetworkObjectSlave.class));
	}

	@Override
	public void write(DataOutput out) throws IOException {
		Text contextJson = new Text(OBJECT_MAPPER.writeValueAsString(this));
		contextJson.write(out);
	}
	
	@JsonProperty("xSubAverage")
	public double[] getSubAverage()
	{
		return this.xSubAverage;
	}
	
//	@JsonProperty("sumAgainstEVNo")
//	public Map<Integer,Integer> getSumAgainstEVNo()
//	{
//		return this.sumAgainstEVNo;
//	}
	
//	@JsonProperty("x_i")
//	public double[] getXi()
//	{
//		return this.x_i;
//	}
//	
//	@JsonProperty("EVId")
//	public int getEVId()
//	{
//		return this.EVId;
//	}
	
//	@JsonProperty("cost")
//	public double getCost()
//	{
//		return this.cost;
//	}
//	
//	@JsonProperty("x_i_difference")
//	public double[] getXiDifference()
//	{
//		return this.x_i_difference;
//	}
}
