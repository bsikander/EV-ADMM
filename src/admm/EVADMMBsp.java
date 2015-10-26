package admm;

import ilog.concert.IloException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hama.bsp.BSP;
import org.apache.hama.bsp.BSPPeer;
import org.apache.hama.bsp.sync.SyncException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class EVADMMBsp extends BSP<NullWritable, NullWritable, IntWritable, Text, Text>{
	public static final Log LOG = LogFactory.getLog(EVADMMBsp.class);
	
	private String masterTask;
	//MasterContext masterContext;
	MasterContextValley masterContext;
	SlaveContext slaveContext;
	
	double[] xsum;
	
	private static int DEFAULT_ADMM_ITERATIONS_MAX;
	private static double RHO;
	private static String AGGREGATOR_PATH;
	private static String EV_PATH;
	private static int EV_COUNT;
	double s_norm;
	double r_norm;
	
	List<Result> resultList = new ArrayList<Result>();
	List<ResultMaster> resultMasterList = new ArrayList<ResultMaster>();
	
	@Override
	public void bsp(BSPPeer<NullWritable, NullWritable,IntWritable, Text, Text> peer) throws IOException, SyncException, InterruptedException {
		int k = 0;
		
		if(peer.getPeerName().equals(this.masterTask)) //The master task
		{	
			//masterContext = new MasterContext(AGGREGATOR_PATH,EV_COUNT,RHO, peer.getConfiguration());
			masterContext = new MasterContextValley(AGGREGATOR_PATH,EV_COUNT,RHO, peer.getConfiguration());
			
			xsum = new double[masterContext.getT()];
			
			while(k != DEFAULT_ADMM_ITERATIONS_MAX)
			{	
				double[] slaveAverageOptimalValue = new double[masterContext.getT()]; //Moved up here to retain the value of old average of all the xoptimal
				
				try {
					int currentEV = 0;
					
					//Create a list of network object but only upto total peers -1 (excluding master)
					//Set the u and xMean for each object
					//For each peer, populate the EVs that it has to process
					//In the end, send only one object per machine
					List<NetworkObjectMaster> listOfNetworkMasterObjects = new ArrayList<NetworkObjectMaster>();
					for(int i=0; i < peer.getNumPeers() -1; i++) {
						listOfNetworkMasterObjects.add(new NetworkObjectMaster(masterContext.getu(), masterContext.getxMean(), new ArrayList<Integer>(),masterContext.getMasterData().getDelta()));
						
						System.out.println("U");
						Utils.PrintArray(masterContext.getu());
						System.out.println("xMean");
						Utils.PrintArray(masterContext.getxMean());
					}
					
					while(currentEV != masterContext.getN() - 1)
					{	
						listOfNetworkMasterObjects.get(currentEV % (peer.getNumPeers()-1)).addEV(currentEV);
						currentEV++;
					}
					
					int peerCount = 1;
					for(NetworkObjectMaster obj : listOfNetworkMasterObjects) {
						sendxMeanAndUToSlaves(peer, obj, peer.getPeerName(peerCount));
						peerCount++;
					}
					
					peer.sync(); //Send messages which are equal to peer.getNumPeers - 1 
					peer.sync();
					
					Pair<double[], double[][], Double> result = receiveSlaveOptimalValuesAndUpdateX(peer, masterContext.getN());
					slaveAverageOptimalValue = result.first();
					
					xsum = Utils.vectorAdd(xsum, slaveAverageOptimalValue); //TODO: Added by me to check the total xsum
					
					double[] masterXOptimalOld = masterContext.getXOptimal();
					double[] oldXMean = masterContext.getxMean(); 
					double costvalue = masterContext.optimize(masterContext.getXOptimal(),k);
					
					System.out.println("Master optimizal value - START");
					Utils.PrintArray(masterContext.getXOptimal());
					System.out.println("Master optimizal value - END");
					
					
					double totalcost = costvalue + result.cost();

					masterContext.setXMean(Utils.calculateMean(masterContext.getXOptimal(), slaveAverageOptimalValue, masterContext.getN())); 	//Take Mean
					masterContext.setU(Utils.vectorAdd(masterContext.getu(), masterContext.getxMean())); //Update u

					//TODO:Uncomment the convergence logic here
					//Add the master optimal value in the matrix to check the convergence
//					double[][] xDifferenceMatrix = result.second();
//					int time = 0;
//					double[] masterXOptimalDifference = Utils.calculateVectorSubtraction(masterContext.getXOptimal(), masterXOptimalOld);
//					
//					for(double d: masterXOptimalDifference) {
//						xDifferenceMatrix[time][xDifferenceMatrix[0].length - 1] = d; //Add the xoptimal value at the end
//						time++;
//					}
//					
//					boolean converged = checkConvergence(xDifferenceMatrix, masterContext.getxMean(), oldXMean, masterContext.getN(), masterContext.getu());
//					
//					if(converged == true) {
//						System.out.println("////////////Converged/////////");
//						break;
//					}
					
					resultMasterList.add(new ResultMaster(peer.getPeerName(),k,0,masterContext.getu(),masterContext.getxMean(),masterContext.getXOptimal(),costvalue,slaveAverageOptimalValue, s_norm,r_norm, totalcost));

				} catch (IloException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					System.out.println(peer.getPeerName() + "Master 1.7 :: " + e.getMessage());
					peer.write(new IntWritable(1), new Text(peer.getPeerName() + "Master 1.7 :: " + e.getMessage()));
				}
				
				k++;
			}
			
			//Send finish message
			sendFinishMessage(peer);
			peer.sync();
			peer.sync();
			
			//System.out.println("\\\\\\\\MASTER OUTPUT\\\\\\\\");
			peer.write(new IntWritable(1), new Text("~~~~~~~MASTER OUTPUT~~~~~~~"));
			int count=0;
			String printResult = "";
			for(ResultMaster r : resultMasterList){
				 printResult = r.printResult(count);
				 peer.write(new IntWritable(1), new Text(printResult));
				count++;
			}
			//System.out.println("\\\\\\\\MASTER OUTPUT - END\\\\\\\\");
			peer.write(new IntWritable(1), new Text("~~~~~~~~MASTER OUTPTU- END ~~~~~~~"));
			
//			System.out.println("\n\n XSUMMMMMM \n\n");
//			Utils.PrintArray(xsum);
//			System.out.println("\n\n XSUMMMMMM END\n\n");
			
		}
		else //master task else
		{	
			boolean finish = false;
			boolean isFirstIteration = true;
			while(true)
			{
				peer.sync();
				
				NetworkObjectMaster masterData = receiveMasterUAndXMeanList(peer);
				
				if(masterData.getEV().isEmpty()) {
					finish = true;
				}
					
				for(Integer evId: masterData.getEV()) {
					peer.write(new IntWritable(1), new Text(EV_PATH + (evId +1) + ".mat"));
					slaveContext = new SlaveContext(EV_PATH + (evId +1) + ".mat", 
							masterData.getxMean(), 
							masterData.getU(),
							evId,
							RHO, isFirstIteration, peer, masterData.getDelta());
					
					try {
						//Do optimization and write the x_optimal to mat file
						double cost = slaveContext.optimize();
					
						resultList.add(new Result(peer.getPeerName(),k,evId, slaveContext.getX(),masterData.getxMean(),masterData.getU(),slaveContext.getXOptimalSlave(),cost));
						
						NetworkObjectSlave slave = new NetworkObjectSlave(slaveContext.getXOptimalSlave(), slaveContext.getCurrentEVNo(), slaveContext.getXOptimalDifference(), cost);
						sendXOptimalToMaster(peer, slave);
					} catch (IloException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
						peer.write(new IntWritable(1), new Text(e.getMessage()));
					}
				}
				isFirstIteration = false;
				
				peer.sync(); //Send all the data
				if(finish == true) {
					String printResult = "";
					for(Result r : resultList){
						printResult = r.printResult();
						peer.write(new IntWritable(1), new Text(printResult));
					}
					break;
				}
			}
		}
	}
	
	@Override
    public void cleanup(
        BSPPeer<NullWritable, NullWritable, IntWritable, Text, Text> peer)
        throws IOException {
		System.out.println(peer.getPeerName() + " is shutting down");
      }
	
	@Override
    public void setup(BSPPeer<NullWritable, NullWritable, IntWritable, Text, Text> peer) throws IOException, SyncException, InterruptedException {
		this.masterTask = peer.getPeerName(0); //0 is our master
		
		AGGREGATOR_PATH = peer.getConfiguration().get(Constants.EVADMM_AGGREGATOR_PATH);
		EV_PATH = peer.getConfiguration().get(Constants.EVADMM_EV_PATH);
		DEFAULT_ADMM_ITERATIONS_MAX = Integer.parseInt(peer.getConfiguration().get(Constants.EVADMM_MAX_ITERATIONS));
		RHO = Double.parseDouble(peer.getConfiguration().get(Constants.EVADMM_RHO));
		EV_COUNT = Integer.parseInt(peer.getConfiguration().get(Constants.EVADMM_EV_COUNT));
	}
	
	private boolean checkConvergence(double[][] x, double[] xMean, double[] xMean_old, int N, double[] u)
	{	
		double[] xMeanOld_xMean = Utils.calculateVectorSubtraction(xMean_old, xMean);
		double[] result = Utils.addMatrixAndVector (x, xMeanOld_xMean);
		double[] s = Utils.scalerMultiply(result, -1*this.RHO*N);
		s_norm= Utils.calculateNorm(s);
		r_norm = Utils.calculateNorm(Utils.scaleVector(xMean, N));
		
		double eps_pri = Math.sqrt(xMean.length * N);
		double eps_dual = (eps_pri * 0.0001) + (0.0001 * Utils.calculateNorm(Utils.scalerMultiply(u, EVADMMBsp.RHO)));
		
		if(r_norm <= eps_pri && s_norm <= eps_dual)
			return true;
		
		if(r_norm > 10* s_norm) {
			EVADMMBsp.RHO = 2*EVADMMBsp.RHO;
		}
		else if(s_norm > 10*r_norm) {
			EVADMMBsp.RHO = EVADMMBsp.RHO/2;
		}
		
		masterContext.setRho(EVADMMBsp.RHO);
		
		//System.out.println("CONVERGED VALUES ////////// s_norm: " + s_norm + " -- r_norm: " + r_norm + " --eps_dual: " + eps_dual + " -- eps_pri" + eps_pri);
		return false;
	}
	
	private void sendFinishMessage(BSPPeer<NullWritable, NullWritable, IntWritable, Text, Text> peer) throws IOException
	{
		for(String peerName: peer.getAllPeerNames()) {
			if(!peerName.equals(this.masterTask)) {
				peer.send(peerName, new Text(Utils.networkMasterToJson(new NetworkObjectMaster(null,null,new ArrayList<Integer>(),0))));
			}	
		}
	}
	
	private void sendxMeanAndUToSlaves(BSPPeer<NullWritable, NullWritable, IntWritable, Text, Text> peer, NetworkObjectMaster object, String peerName) throws IOException
	{
		peer.send(peerName, new Text(Utils.networkMasterToJson(object)));
	}
	
	private void sendXOptimalToMaster(BSPPeer<NullWritable, NullWritable, IntWritable, Text, Text> peer, NetworkObjectSlave object) throws IOException
	{	
		peer.send(this.masterTask, new Text(Utils.networkSlaveToJson(object)));
	}
	
	private Pair<double[],double[][], Double> receiveSlaveOptimalValuesAndUpdateX(BSPPeer<NullWritable, NullWritable, IntWritable, Text, Text> peer, int totalN) throws IOException
	{
		NetworkObjectSlave slave;
		Text receivedJson;
		
		double[][] s = new double[this.masterContext.getXOptimal().length][totalN];
		double[] averageXReceived = Utils.getZeroArray(this.masterContext.getXOptimal().length); 
		
		int ev = 0;
		double cost = 0;
		
		while ((receivedJson = peer.getCurrentMessage()) != null) //Receive initial array 
		{
			slave = Utils.jsonToNetworkSlave(receivedJson.toString());
			averageXReceived =  Utils.vectorAdd(averageXReceived, slave.getXi());
			cost = cost + slave.getCost();
			
			int i = 0;
			for(double d: slave.getXiDifference()) {
				s[i][ev] = d;
				i++;
			}
			ev++;
		}
	
		return new Pair<double[],double[][], Double>(averageXReceived,s, cost);
	}
	
	private NetworkObjectMaster receiveMasterUAndXMean(BSPPeer<NullWritable, NullWritable, IntWritable, Text, Text> peer) throws IOException
	{	
		NetworkObjectMaster master = null;
		Text receivedJson;
		while ((receivedJson = peer.getCurrentMessage()) != null) //Receive initial array 
		{	
			master = Utils.jsonToNetworkMaster(receivedJson.toString());
			break;
		}
		
		return master;
	}
	
	private NetworkObjectMaster receiveMasterUAndXMeanList(BSPPeer<NullWritable, NullWritable, IntWritable, Text, Text> peer) throws IOException
	{
		NetworkObjectMaster master = new NetworkObjectMaster();
		Text receivedJson;
		
		while ((receivedJson = peer.getCurrentMessage()) != null) //Receive initial array 
		{	
			master = Utils.jsonToNetworkMaster(receivedJson.toString());
			break;
		}
		
		return master;
	}
	
	enum EVADMMCounters {
		Iterations,
		TotalFilesProcessedByEVs,
		TotalXReceivedByMaster,
		Cost
	}
	
	class Pair<T,U,C> {
	    private final T m_first;
	    private final U m_second;
	    private final C m_cost;

	    public Pair(T first, U second, C cost) {
	        m_first = first;
	        m_second = second;
	        m_cost = cost;
	    }

	    public T first() {
	        return m_first;
	    }

	    public U second() {
	        return m_second;
	    }
	    
	    public C cost() {
	    	return m_cost;
	    }
	}
}

