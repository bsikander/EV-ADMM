package admm;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

//import org.apache.commons.math3.linear.RealMatrix;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.Writable;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.map.ObjectMapper;

public class NetworkObjectSlave implements Writable{
	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
	
	@JsonProperty("x_i")
	double[] x_i;
	
	@JsonProperty("EVId")
	private int EVId;
	
	public NetworkObjectSlave(double[] x_i, int EVId)
	{
		this.x_i = x_i;
		this.EVId = EVId;
	}

	public void setNetworkObjectSlave(NetworkObjectSlave n)
	{
		this.x_i = n.x_i;
		this.EVId = n.EVId;
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
}
