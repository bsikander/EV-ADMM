package admm;

import ilog.concert.IloException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;



//import org.apache.commons.math3.linear.MatrixUtils;
//import org.apache.commons.math3.linear.RealMatrix;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hama.bsp.BSP;
import org.apache.hama.bsp.BSPPeer;
import org.apache.hama.bsp.sync.SyncException;

public class EVADMMBsp extends BSP<NullWritable, NullWritable, NullWritable, NullWritable, Text>{
	private String masterTask;
	MasterContext masterContext;
	SlaveContext slaveContext;
	//private static final int DEFAULT_ADMM_ITERATIONS_MAX = 4;
	//private static final double rho = 0.01;
	private static int DEFAULT_ADMM_ITERATIONS_MAX;
	private static double RHO;
	private static String AGGREGATOR_PATH;
	private static String EV_PATH;
	private static int EV_COUNT;
	
	
	List<Result> resultList = new ArrayList<Result>();
	List<ResultMaster> resultMasterList = new ArrayList<ResultMaster>();
	
	@Override
	public void bsp(BSPPeer<NullWritable, NullWritable, NullWritable, NullWritable, Text> peer) throws IOException, SyncException, InterruptedException {
		int k = 0; //iteration counter
		
		if(peer.getPeerName().equals(this.masterTask)) //The master task
		{	
			System.out.println("MASTER: Starting Iteration Loop");
			
			//Calculate rnorm, snorm
			//Calculate eps_pri and eps_dual
			//Check convergence
			//Update rho value
			masterContext = new MasterContext(AGGREGATOR_PATH,EV_COUNT,RHO);
			
			while(k != DEFAULT_ADMM_ITERATIONS_MAX) //TODO: Or convergence is met
			{	
				try {
					//Optimize + return the cost + save the optimal X* in the x matrix last column
					int currentEV = 0;
					
					System.out.println("MASTER:: Starting to send data to all EV's");
					while(currentEV != masterContext.getN() - 1)
					{	
						NetworkObjectMaster objectToSend = new NetworkObjectMaster(masterContext.getu(), masterContext.getxMean(), currentEV);
						
						//Never send a message to peer 0 since it is the master
						sendxMeanAndUToSlaves(peer, objectToSend, peer.getPeerName((currentEV % (peer.getNumPeers()-1) + 1) ));
												
						currentEV++;
					}
					
					peer.sync(); //Send messages which are equal to peer.getNumPeers - 1 
					
					System.out.println("Master:: Receiving optimal values from Slaves => Iteration: " + currentEV);
					
					peer.sync();
					//Receive message
					double[] slaveAverageOptimalValue = receiveSlaveOptimalValuesAndUpdateX(peer);
					
					//TODO:
					System.out.println("Master:: Data sending complete. Doing Master optimiation");
					
					//Get the old x_optimal
					//double[] xold = Utils.getColumn(masterContext.getx(), masterContext.getN() - 1);
					
					double costvalue = masterContext.optimize(masterContext.getXOptimal(),k);
					
					
					//Do convergence stuff.
					System.out.println("Master:: Calculating Mean");

					masterContext.setXMean(Utils.calculateMean(masterContext.getXOptimal(), slaveAverageOptimalValue, masterContext.getN())); 	//Take Mean
					
					
					System.out.println("Master:: Calculating U");
					
					masterContext.setU(Utils.vectorAdd(masterContext.getu(), masterContext.getxMean())); //Update u
					
					//if(k >= 2) {
						System.out.println("---->xMean: " + k + "<----");
						Utils.PrintArray(masterContext.getxMean());
						System.out.println("---->xMean -END<----");
						
						System.out.println("---->U: " + k + "<----");
						Utils.PrintArray(masterContext.getu());
						System.out.println("---->U -END<----");
					//}
					
					resultMasterList.add(new ResultMaster(peer.getPeerName(),k,0,masterContext.getu(),masterContext.getxMean(),masterContext.getXOptimal(),costvalue,slaveAverageOptimalValue));
					
					//Utils.PrintArray(masterContext.getxMean());
					//Utils.PrintArray(masterContext.getu());
				} catch (IloException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				k++;
			}
			
			//Send finish message
			sendFinishMessage(peer);
			peer.sync();
			peer.sync();
			
			System.out.println("\\\\\\\\MASTER OUTPUT\\\\\\\\");
			int count=0;
			for(ResultMaster r : resultMasterList){
				r.printResult(count);
				count++;
			}
			System.out.println("\\\\\\\\MASTER OUTPUT - END\\\\\\\\");
			
		}
		else //master task else
		{
			//Receive intial values
			//Optimize slave equation
			//Send slave data
			boolean finish = false;
			while(true)
			{
				peer.sync();
				System.out.println("Slave: " + peer.getPeerName() + ":: Out of Sync. Receiving master data and updating u and x");
				
				
				List<NetworkObjectMaster> masterDataList = receiveMasterUAndXMeanList(peer);
				System.out.println("================== PeerName:" + peer.getPeerName() + "   Received EVS: " + masterDataList.size());
				for(NetworkObjectMaster masterData: masterDataList)
				{
					if(masterData.getEVId() == -1) {
						finish = true;
						break;
					}
						
					
//					//TODO: REmove
//					System.out.println("CurrentEV: " + masterData.getEVId() + "   === Iteration: "+ k);
//					Utils.PrintArray(masterData.getx());
//					Utils.PrintArray(masterData.getxMean());
//					Utils.PrintArray(masterData.getU());
//					//TODO:Remove

					slaveContext = new SlaveContext(EV_PATH + (masterData.getEVId() +1) + ".mat", 
							masterData.getxMean(), 
							masterData.getU(),
							masterData.getEVId(),
							RHO);
					try {
						System.out.println("Slave: " + peer.getPeerName() + ":: Starting Slave optimization");
						//Do optimization and write the x_optimal to mat file
						double cost = slaveContext.optimize();
						
						resultList.add(new Result(peer.getPeerName(),k,masterData.getEVId(), slaveContext.getX(),masterData.getxMean(),masterData.getU(),slaveContext.getXOptimalSlave(),cost));
						
						NetworkObjectSlave slave = new NetworkObjectSlave(slaveContext.getXOptimalSlave(), slaveContext.getCurrentEVNo());
						System.out.println("Slave: " + peer.getPeerName() + ":: Sending slave x* to Master");
						sendXOptimalToMaster(peer, slave);
						
					} catch (IloException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				peer.sync(); //Send all the data
				if(finish == true) {
					//System.out.println("PeerName \t Iteration \t EV \t CostValue \t\t x_optimal \t\t xold \t\t\t xMean \t U");
					for(Result r : resultList){
						r.printResult();
					}
					
					break;
				}
			}
			
		}
	}
	
	@Override
    public void cleanup(
        BSPPeer<NullWritable, NullWritable, NullWritable, NullWritable, Text> peer)
        throws IOException {
		//System.out.println("Output: " + peer.getPeerName());
      }
	
	@Override
    public void setup(BSPPeer<NullWritable, NullWritable, NullWritable, NullWritable, Text> peer) throws IOException, SyncException, InterruptedException {
		// Choose one as a master
		this.masterTask = peer.getPeerName(0); //0 is out master
		
		AGGREGATOR_PATH = peer.getConfiguration().get(Constants.EVADMM_AGGREGATOR_PATH);
		EV_PATH = peer.getConfiguration().get(Constants.EVADMM_EV_PATH);
		DEFAULT_ADMM_ITERATIONS_MAX = Integer.parseInt(peer.getConfiguration().get(Constants.EVADMM_MAX_ITERATIONS));
		RHO = Double.parseDouble(peer.getConfiguration().get(Constants.EVADMM_RHO));
		EV_COUNT = Integer.parseInt(peer.getConfiguration().get(Constants.EVADMM_EV_COUNT));
	}
	
	private boolean checkConvergence(double[][] x, double[][] x_old, double[] xMean, double[] xMean_old, int N)
	{
		double[][] x_xoldMatrix = Utils.calculateMatrixSubtraction(x,x_old);
		double[] xMeanOld_xMean = Utils.calculateVectorSubtraction(xMean_old, xMean);
		double[] result = Utils.addMatrixAndVector (x_xoldMatrix, xMeanOld_xMean);
		double[] s = Utils.scalerMultiply(result, -1*this.RHO*N);
		double s_norm= Utils.calculateNorm(s);
		double r_norm = Utils.calculateNorm(Utils.scaleVector(xMean, N));
		
		if(r_norm <= 0.1 && s_norm <= 0.1)
			return true;
		
		return false;
	}
	
	private void sendFinishMessage(BSPPeer<NullWritable, NullWritable, NullWritable, NullWritable, Text> peer) throws IOException
	{
		for(String peerName: peer.getAllPeerNames()) {
			if(!peerName.equals(this.masterTask)) {
				peer.send(peerName, new Text(Utils.networkMasterToJson(new NetworkObjectMaster(null,null,-1))));
			}	
		}
	}
	
	private void sendxMeanAndUToSlaves(BSPPeer<NullWritable, NullWritable, NullWritable, NullWritable, Text> peer, NetworkObjectMaster object, String peerName) throws IOException
	{
		peer.send(peerName, new Text(Utils.networkMasterToJson(object)));
	}
	
	private void sendXOptimalToMaster(BSPPeer<NullWritable, NullWritable, NullWritable, NullWritable, Text> peer, NetworkObjectSlave object) throws IOException
	{	
		peer.send(this.masterTask, new Text(Utils.networkSlaveToJson(object)));
	}
	
	private double[] receiveSlaveOptimalValuesAndUpdateX(BSPPeer<NullWritable, NullWritable, NullWritable, NullWritable, Text> peer) throws IOException
	{
		//List<MasterContext> list = new ArrayList<MasterContext>();
		NetworkObjectSlave slave;
		Text receivedJson;
		double[] averageXReceived = Utils.getZeroArray(this.masterContext.getXOptimal().length); 
				
		while ((receivedJson = peer.getCurrentMessage()) != null) //Receive initial array 
		{
			//System.out.println("Master Received Message");
			slave = Utils.jsonToNetworkSlave(receivedJson.toString());
			//masterContext.setX(Utils.setColumnInMatrix(this.masterContext.getx(), slave.getXi(), slave.getEVId()));
			//Utils.setColumnInMatrix(this.masterContext.getx(), slave.getXi(), slave.getEVId());
			averageXReceived =  Utils.vectorAdd(averageXReceived, slave.getXi()); 
		}
	
		return averageXReceived;
	}
	
	private NetworkObjectMaster receiveMasterUAndXMean(BSPPeer<NullWritable, NullWritable, NullWritable, NullWritable, Text> peer) throws IOException
	{	
		NetworkObjectMaster master = null;
		Text receivedJson;
		while ((receivedJson = peer.getCurrentMessage()) != null) //Receive initial array 
		{
			//System.out.println("Master Received Message");
			master = Utils.jsonToNetworkMaster(receivedJson.toString());
			break;
		}
		
		return master;
	}
	
	private List<NetworkObjectMaster> receiveMasterUAndXMeanList(BSPPeer<NullWritable, NullWritable, NullWritable, NullWritable, Text> peer) throws IOException
	{	
		List<NetworkObjectMaster> master = new ArrayList<NetworkObjectMaster>();
		Text receivedJson;
		while ((receivedJson = peer.getCurrentMessage()) != null) //Receive initial array 
		{
			//System.out.println("Master Received Message");
			master.add(Utils.jsonToNetworkMaster(receivedJson.toString()));
		}
		
		return master;
	}
	
	enum EVADMMCounters {
		Iterations,
		TotalFilesProcessedByEVs,
		TotalXReceivedByMaster
	}
}

