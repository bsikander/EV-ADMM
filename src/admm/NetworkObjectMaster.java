package admm;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

//import org.apache.commons.math3.linear.RealMatrix;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.Writable;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.map.ObjectMapper;

public class NetworkObjectMaster implements Writable {
	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
	
	@JsonProperty("u")
	private double[] u;
	
	@JsonProperty("xMean")
	private double[] xMean;
	
	@JsonProperty("EVId")
	private int EVId;
	
	//@JsonProperty("x")
	//private double[] x;
	
	public NetworkObjectMaster()
	{}
	
	public NetworkObjectMaster(double[] u, double[] xMean, int EVId)
	{
		this.u = u;
		this.xMean = xMean;	
		this.EVId = EVId;
	}
	
	public void setNetworkObjectMaster(NetworkObjectMaster n)
	{
		this.u = n.u;
		this.xMean = n.xMean;
		this.EVId = n.EVId;
	}
	
	@Override
	public void readFields(DataInput in) throws IOException {
		Text contextJson = new Text();
		contextJson.readFields(in);
		setNetworkObjectMaster(OBJECT_MAPPER.readValue(contextJson.toString(), NetworkObjectMaster.class));
	}

	@Override
	public void write(DataOutput out) throws IOException {
		Text contextJson = new Text(OBJECT_MAPPER.writeValueAsString(this));
		contextJson.write(out);
	}
	
	@JsonProperty("u")
	public double[] getU()
	{
		return this.u;
	}
	
	@JsonProperty("xMean")
	public double[] getxMean()
	{
		return this.xMean;
	}
	
	@JsonProperty("EVId")
	public int getEVId()
	{
		return this.EVId;
	}
	
}
