package admm;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


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
	
	@JsonProperty("EVs")
	private List<Integer> EVs;
	
	public NetworkObjectMaster()
	{
		EVs = new ArrayList<Integer>();
	}
	
	public NetworkObjectMaster(double[] u, double[] xMean, List<Integer> EVs)
	{
		this.u = u;
		this.xMean = xMean;	
		this.EVs = EVs;
	}
	
	public void setNetworkObjectMaster(NetworkObjectMaster n)
	{
		this.u = n.u;
		this.xMean = n.xMean;
		this.EVs = n.EVs;
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
	
	@JsonProperty("EVs")
	public List<Integer> getEV()
	{
		return this.EVs;
	}
	
	public void setU(double[] value)
	{
		this.u = value;
	}
	
	public void setxMean(double[] value) 
	{
		this.xMean = value;
	}
	
	public void addEV(int evId)
	{
		EVs.add(evId);
	}
	
}
