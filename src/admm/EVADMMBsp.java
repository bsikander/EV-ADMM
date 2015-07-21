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
	MasterContext masterContext;
	SlaveContext slaveContext;
	
	private static int DEFAULT_ADMM_ITERATIONS_MAX;
	private static double RHO;
	private static String AGGREGATOR_PATH;
	private static String EV_PATH;
	private static int EV_COUNT;
	
	
	List<Result> resultList = new ArrayList<Result>();
	List<ResultMaster> resultMasterList = new ArrayList<ResultMaster>();
	
	@Override
	public void bsp(BSPPeer<NullWritable, NullWritable,IntWritable, Text, Text> peer) throws IOException, SyncException, InterruptedException {
		int k = 0; //iteration counter
		
		System.out.println("BEFORE STRTING -->" + peer.getPeerName());
		System.out.println("BEFORE STRTING -->" + this.masterTask);
		
		if(peer.getPeerName().equals(this.masterTask)) //The master task
		{	
			System.out.println("MASTER: Starting Iteration Loop");
			LOG.info("MASTER: Starting Iteration Loop");
			
			//Calculate rnorm, snorm
			//Calculate eps_pri and eps_dual
			//Check convergence
			//Update rho value
			masterContext = new MasterContext(AGGREGATOR_PATH,EV_COUNT,RHO, peer.getConfiguration());
			
			while(k != DEFAULT_ADMM_ITERATIONS_MAX) //TODO: Or convergence is met
			{	
				double[] slaveAverageOptimalValue = new double[masterContext.getT()]; //Moved up here to retain the value of old average of all the xoptimal
				
				try {
					//Optimize + return the cost + save the optimal X* in the x matrix last column
					int currentEV = 0;
					
					//Create a list of network object but only upto total peers -1 (excluding master)
					//Set the u and xMean for each object
					//For each peer, populate the EVs that it has to process
					//In the end, send only one object per machine
					System.out.println(peer.getPeerName() + "MASTER 1.0:: Starting master");
					List<NetworkObjectMaster> listOfNetworkMasterObjects = new ArrayList<NetworkObjectMaster>();
					for(int i=0; i < peer.getNumPeers() -1; i++) {
						listOfNetworkMasterObjects.add(new NetworkObjectMaster(masterContext.getu(), masterContext.getxMean(), new ArrayList<Integer>()));
					}
					
					System.out.println(peer.getPeerName() + "MASTER 1.1:: Starting to send data to all EV's");
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
					
					System.out.println(peer.getPeerName() + "MASTER 1.2:: Before sending data to peer");
					peer.sync(); //Send messages which are equal to peer.getNumPeers - 1 
					
					System.out.println(peer.getPeerName() + "Master 1.3:: Receiving optimal values from Slaves => Iteration: " + currentEV);
					
					peer.sync();
					//Receive message
					//double[] s = new double[masterContext.getN()];
					double[] oldXAverage = slaveAverageOptimalValue;
					Pair<double[], double[][]> result = receiveSlaveOptimalValuesAndUpdateX(peer, masterContext.getN());
					//slaveAverageOptimalValue = receiveSlaveOptimalValuesAndUpdateX(peer, slaveAverageOptimalValue, masterContext.getN());
					
					slaveAverageOptimalValue = result.first();
					
					
//					System.out.println("--------- AVERAGE AT MASTER ---------" );
//					Utils.PrintArray(slaveAverageOptimalValue);
//					System.out.println("--------- AVERAGE AT MASTER ---------" );
//					System.out.println(peer.getPeerName() + "Master 1.4:: Data sending complete. Doing Master optimiation");
					
					double costvalue = masterContext.optimize(masterContext.getXOptimal(),k);
					
					//Do convergence stuff.
					System.out.println(peer.getPeerName() + "Master 1.5:: Calculating Mean");

					masterContext.setXMean(Utils.calculateMean(masterContext.getXOptimal(), slaveAverageOptimalValue, masterContext.getN())); 	//Take Mean
					
					System.out.println("Master:: Calculating U");
					
					masterContext.setU(Utils.vectorAdd(masterContext.getu(), masterContext.getxMean())); //Update u
				
					System.out.println("---->xMean: " + k + "<----");
					Utils.PrintArray(masterContext.getxMean());
					System.out.println("---->xMean -END<----");
					
					System.out.println("---->U: " + k + "<----");
					Utils.PrintArray(masterContext.getu());
					System.out.println("---->U -END<----");
					
					resultMasterList.add(new ResultMaster(peer.getPeerName(),k,0,masterContext.getu(),masterContext.getxMean(),masterContext.getXOptimal(),costvalue,slaveAverageOptimalValue));
					
					
					//TODO:Uncomment the convergence logic here
					//Add the master optimal value int he matrix to check the convergence
//					double[][] xDifferenceMatrix = result.second();
//					int time = 0;
//					for(double d: masterContext.getXOptimal()) {
//						xDifferenceMatrix[time][xDifferenceMatrix[0].length - 1] = d; //Add the xoptimal value at the end
//						time++;
//					}
//					
//					boolean converged = checkConvergence(xDifferenceMatrix, result.first(), oldXAverage, masterContext.getN(), masterContext.getu());
//					//checkConvergence(x, x_old, xMean, xMean_old, N)
//					if(converged == true) {
//						System.out.println("////////////Converged/////////");
//						break;
//					}
//					
//					//Update rho is the code has not converged



				} catch (IloException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					System.out.println(peer.getPeerName() + "Master 1.7 :: " + e.getMessage());
				}
				
				k++;
			}
			
			System.out.println(peer.getPeerName() + "Master 1.8:: Sending finishing message");
			
			//Send finish message
			sendFinishMessage(peer);
			peer.sync();
			peer.sync();
			
			System.out.println("\\\\\\\\MASTER OUTPUT\\\\\\\\");
			int count=0;
			String printResult = "";
			for(ResultMaster r : resultMasterList){
				 printResult = r.printResult(count);
				 peer.write(new IntWritable(1), new Text(printResult));
				 //LOG.info(printResult);
				
				count++;
			}
			System.out.println("\\\\\\\\MASTER OUTPUT - END\\\\\\\\");
			
		}
		else //master task else
		{	
			boolean finish = false;
			boolean isFirstIteration = true;
			while(true)
			{
				peer.sync();
				System.out.println(peer.getPeerName() + "Slave: 1 " + peer.getPeerName() + ":: Out of Sync. Receiving master data and updating u and x");
				
				NetworkObjectMaster masterData = receiveMasterUAndXMeanList(peer);
				
				System.out.println(peer.getPeerName() + "Slave: 2 Code after receiveing the message ");
				
				if(masterData.getEV().isEmpty()) {
					finish = true;
				}
					
				for(Integer evId: masterData.getEV()) {
					System.out.println(peer.getPeerName() + "Slave: 1.3 Inside the slave loop");
					
					peer.write(new IntWritable(1), new Text(EV_PATH + (evId +1) + ".mat"));
					slaveContext = new SlaveContext(EV_PATH + (evId +1) + ".mat", 
							masterData.getxMean(), 
							masterData.getU(),
							evId,
							RHO, isFirstIteration, peer);
					
					try {
						System.out.println("Slave: 1.4 " + peer.getPeerName() + ":: Starting Slave optimization");
						//Do optimization and write the x_optimal to mat file
						double cost = slaveContext.optimize();
						
						peer.write(new IntWritable(1), new Text("Optimized"));
						resultList.add(new Result(peer.getPeerName(),k,evId, slaveContext.getX(),masterData.getxMean(),masterData.getU(),slaveContext.getXOptimalSlave(),cost));
						
						NetworkObjectSlave slave = new NetworkObjectSlave(slaveContext.getXOptimalSlave(), slaveContext.getCurrentEVNo(), slaveContext.getXOptimalDifference());
						System.out.println("Slave: 1.5 " + peer.getPeerName() + ":: Sending slave x* to Master");
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
						//LOG.info(printResult);
					}
					
					System.out.println(peer.getPeerName() + "Slave 1.5: Sending the finishing message");
					
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
		// Choose one as a master
		System.out.println(peer.getPeerName() + " -- Selecting Master -> Selected Master = " + peer.getPeerName(0) + "  --Total peers: " + peer.getNumPeers());
		this.masterTask = peer.getPeerName(0); //0 is our master
		
		for(String peerName: peer.getAllPeerNames()) {
			System.out.println(peerName);
		}
		
		AGGREGATOR_PATH = peer.getConfiguration().get(Constants.EVADMM_AGGREGATOR_PATH);
		EV_PATH = peer.getConfiguration().get(Constants.EVADMM_EV_PATH);
		DEFAULT_ADMM_ITERATIONS_MAX = Integer.parseInt(peer.getConfiguration().get(Constants.EVADMM_MAX_ITERATIONS));
		RHO = Double.parseDouble(peer.getConfiguration().get(Constants.EVADMM_RHO));
		EV_COUNT = Integer.parseInt(peer.getConfiguration().get(Constants.EVADMM_EV_COUNT));
	}
	
	private boolean checkConvergence(double[][] x, double[] xMean, double[] xMean_old, int N, double[] u)
	{
		//double[][] x_xoldMatrix = Utils.calculateMatrixSubtraction(x,x_old);
		double[] xMeanOld_xMean = Utils.calculateVectorSubtraction(xMean_old, xMean);
		double[] result = Utils.addMatrixAndVector (x, xMeanOld_xMean);
		double[] s = Utils.scalerMultiply(result, -1*this.RHO*N);
		double s_norm= Utils.calculateNorm(s);
		//double r_norm = Utils.calculateNorm(Utils.scaleVector(xMean, N));
		double r_norm = Utils.calculateNorm(xMean);
		
		//eps_pri = sqrt(T*N);
		double eps_pri = Math.sqrt(xMean.length * N);
		
		//eps_dual= sqrt(N*T)*1e-4 + 1e-4 *norm(rho*u);
		double eps_dual = (eps_pri * 0.0001) + (0.0001 * Utils.calculateNorm(Utils.scalerMultiply(u, EVADMMBsp.RHO)));
		
		if(r_norm <= eps_pri && s_norm <= eps_dual)
			return true;
		
		//if r_norm > 10*s_norm
		//	     rho=2*rho;
	    // end
	    // if s_norm > 10*r_norm
		//	     rho=rho/2;
	    // end
		
		if(r_norm > 10* s_norm) {
			EVADMMBsp.RHO = 2*EVADMMBsp.RHO;
		}
		else if(s_norm > 10*r_norm) {
			EVADMMBsp.RHO = EVADMMBsp.RHO/2;
		}
		
		masterContext.setRho(EVADMMBsp.RHO);
		
		System.out.println("CONVERGED VALUES ////////// s_norm: " + s_norm + " -- r_norm: " + r_norm + " --eps_dual: " + eps_dual + " -- eps_pri" + eps_pri);
		return false;
//		double[][] x_xoldMatrix = Utils.calculateMatrixSubtraction(x,x_old);
//		double[] xMeanOld_xMean = Utils.calculateVectorSubtraction(xMean_old, xMean);
//		double[] result = Utils.addMatrixAndVector (x_xoldMatrix, xMeanOld_xMean);
//		double[] s = Utils.scalerMultiply(result, -1*this.RHO*N);
//		double s_norm= Utils.calculateNorm(s);
//		double r_norm = Utils.calculateNorm(Utils.scaleVector(xMean, N));
//		
//		if(r_norm <= 0.1 && s_norm <= 0.1)
//			return true;
//		
//		return false;
	}
	
	private void sendFinishMessage(BSPPeer<NullWritable, NullWritable, IntWritable, Text, Text> peer) throws IOException
	{
		for(String peerName: peer.getAllPeerNames()) {
			if(!peerName.equals(this.masterTask)) {
				peer.send(peerName, new Text(Utils.networkMasterToJson(new NetworkObjectMaster(null,null,new ArrayList<Integer>()))));
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
	
	private Pair<double[],double[][]> receiveSlaveOptimalValuesAndUpdateX(BSPPeer<NullWritable, NullWritable, IntWritable, Text, Text> peer, int totalN) throws IOException
	{
		//List<MasterContext> list = new ArrayList<MasterContext>();
		NetworkObjectSlave slave;
		Text receivedJson;
		
		double[][] s = new double[this.masterContext.getXOptimal().length][totalN];
		double[] averageXReceived = Utils.getZeroArray(this.masterContext.getXOptimal().length); 
		
		int ev = 0;
		
		while ((receivedJson = peer.getCurrentMessage()) != null) //Receive initial array 
		{
			slave = Utils.jsonToNetworkSlave(receivedJson.toString());
			averageXReceived =  Utils.vectorAdd(averageXReceived, slave.getXi());
			
			int i = 0;
			for(double d: slave.getXiDifference()) {
				s[i][ev] = d;
				i++;
			}
			ev++;
		}
	
		return new Pair<double[],double[][]>(averageXReceived,s);
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
		System.out.println(peer.getPeerName() + "Slave: 1.1.0 Going to receive the data");
		while ((receivedJson = peer.getCurrentMessage()) != null) //Receive initial array 
		{	
			master = Utils.jsonToNetworkMaster(receivedJson.toString());
			System.out.println(peer.getPeerName() + "Slave: 1.1.1 Data received. Breaking the loop");
			break;
		}
		
		System.out.println(peer.getPeerName() + "Slave: 1.1.2 Out of receving the data");
		
		return master;
	}
	
	enum EVADMMCounters {
		Iterations,
		TotalFilesProcessedByEVs,
		TotalXReceivedByMaster
	}
	
	class Pair<T,U> {
	    private final T m_first;
	    private final U m_second;

	    public Pair(T first, U second) {
	        m_first = first;
	        m_second = second;
	    }

	    public T first() {
	        return m_first;
	    }

	    public U second() {
	        return m_second;
	    }
	}
}

