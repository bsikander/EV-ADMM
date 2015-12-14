package admm;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.Writable;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.map.ObjectMapper;

/*
 * This class is responsible for serializing and deserializing the data that slaves sends to master using the Jackson library.
 */
public class NetworkObjectSlave implements Writable{
	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
	
	@JsonProperty("x_i")
	double[] x_i;
	
	@JsonProperty("EVId")
	private int EVId;
	
//	@JsonProperty("x_i_difference")
//	private double[] x_i_difference;
//	
//	@JsonProperty("cost")
//	private double cost;
	
	/*
	 * Parameterized constructor
	 */
	//public NetworkObjectSlave(double[] x_i, int EVId, double[] x_i_difference, double cost)
	public NetworkObjectSlave(double[] x_i, int EVId)
	{
		this.x_i = x_i;
		this.EVId = EVId;
//		this.x_i_difference = x_i_difference;
//		this.cost = cost;
	}

	public void setNetworkObjectSlave(NetworkObjectSlave n)
	{
		this.x_i = n.x_i;
		this.EVId = n.EVId;
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
	
	@JsonProperty("x_i")
	public double[] getXi()
	{
		return this.x_i;
	}
	
	@JsonProperty("EVId")
	public int getEVId()
	{
		return this.EVId;
	}
	
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
