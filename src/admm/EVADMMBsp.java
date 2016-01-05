package admm;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hama.bsp.BSP;
import org.apache.hama.bsp.BSPPeer;
import org.apache.hama.bsp.sync.SyncException;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ilog.concert.IloException;
import ilog.cplex.IloCplex;

public class EVADMMBsp extends BSP<LongWritable, Text, IntWritable, Text, Text>{
	public static final Log LOG = LogFactory.getLog(EVADMMBsp.class);
	
	/*
	 * Stores the name of master node. See the setup method for the selection logic of master task.
	 */
	private String masterTask;
	
	/*
	 * This object is responsible for solving the Valley Filling model at master.
	 */
	MasterContextValley masterContext;
	
	/*
	 * This object is used to load and solve the EV models.
	 */
	SlaveContext slaveContext;
	
	double[] xsum;
	ArrayList<Double> cost;
	
	/*
	 * Stores the total iterations of the algorithm. This value is set by user using the command line parameter.
	 */
	private static int DEFAULT_ADMM_ITERATIONS_MAX;
	
	/*
	 * Stores the current value of RHO. User can specify the default RHO value through the command line parameter.
	 * This algorithm updates the RHO after each iteration to speed up the algorithm. See checkConvergence function
	 * for the RHO update logic.
	 */
	private static double RHO;
	
	/*
	 * It stores the current path of Aggregator file. This path is used to load the initial values at master.
	 */
	private static String AGGREGATOR_PATH;
	
	/*
	 * Contains the path where all the EV's are located.
	 */
	//private static String EV_PATH;
	
	/*
	 * This value is set by the user through command line and its value represents the total EV's we have to process.
	 */
	private static int EV_COUNT;
	double s_norm;
	double r_norm;
	double v = 0;
	
//	List<Result> resultList = new ArrayList<Result>();
//	List<ResultMaster> resultMasterList = new ArrayList<ResultMaster>();
	
	/*
	 * (non-Javadoc)
	 * @see org.apache.hama.bsp.BSP#bsp(org.apache.hama.bsp.BSPPeer)
	 */
	@Override
	public void bsp(BSPPeer<LongWritable, Text,IntWritable, Text, Text> peer) throws IOException, SyncException, InterruptedException {
		int k = 0;
		
		if(peer.getPeerName().equals(this.masterTask)) //The master task
		{	
			masterContext = new MasterContextValley(AGGREGATOR_PATH,EV_COUNT,RHO, peer.getConfiguration());
			System.out.println("DELTA ---> " + masterContext.getMasterData().getDelta());
			
			xsum = new double[masterContext.getT()];
			cost = new ArrayList<Double>();
			
			// Create a list of network object but only up to total peers -1 (excluding master)
			// Set the u and xMean for each object
			// For each peer, populate the EVs that it has to process
			// In the end, send only one object per machine
			List<NetworkObjectMaster> listOfNetworkMasterObjects = createMasterNetworkObjectList(peer);
			
			while(k != DEFAULT_ADMM_ITERATIONS_MAX)
			{	
//				System.out.println();
//				System.out.println("--------> " + (k +1) + " <--------");
//				System.out.println();
				
				double[] slaveAverageOptimalValue = new double[masterContext.getT()]; //Moved up here to retain the value of old average of all the xoptimal
				
				try {
					int peerCount = 1;
					for(NetworkObjectMaster obj : listOfNetworkMasterObjects) {
						//Set new U and XMean for each peer object after each iteration
						listOfNetworkMasterObjects.get(peerCount - 1).setU(masterContext.getu());
						listOfNetworkMasterObjects.get(peerCount - 1).setxMean(masterContext.getxMean());
						
						sendxMeanAndUToSlaves(peer, obj, peer.getPeerName(peerCount));
						peerCount++;
					}
//					Utils.PrintArray(masterContext.getu());
//					Utils.PrintArray(masterContext.getxMean());
					
					peer.sync(); //Send messages which are equal to peer.getNumPeers - 1 
					
					long startBarrierTime = System.currentTimeMillis();
					peer.sync();
					peer.incrementCounter(EVADMMCounters.AGGREGATED_TIME_MS_MASTER_WAIT_FOR_SLAVES,(System.currentTimeMillis() - startBarrierTime));
					//long lStartTime = System.nanoTime();
					
					Pair<double[], double[][], Double, double[][]> result = receiveSlaveOptimalValuesAndUpdateX(peer, masterContext.getN());
					slaveAverageOptimalValue = result.averageXReceived();

					xsum = Utils.calculateEVSum(result.xsum());
//					System.out.println("------- XSUM ----");
//					Utils.PrintArray(xsum);
//					System.out.println("------- XSUM END----");
					
					double[] masterXOptimalOld = masterContext.getXOptimal();
					double[] oldXMean = masterContext.getxMean(); 
					masterContext.optimize(masterContext.getXOptimal(),k);
					
					double costtemp = Utils.calculateNorm( Utils.vectorAdd(masterContext.getMasterData().getD(), xsum) );
				    
				    //costtemp = Math.round ((costtemp * costtemp) * 1000000000.0) / 1000000000.0; //round off to 3 decimal places.
					//cost.add(costtemp);
				    //costtemp = ((costtemp * costtemp) * 1000000000.0) / 1000000000.0; //round off to 3 decimal places.
				    
					cost.add(costtemp*costtemp);
					
					System.out.print(String.format("%03d", (k+1)) + " > COST -> " + String.format("%.9f",cost.get(cost.size() -1)) );
					//System.out.println(String.format("%.9f",cost.get(cost.size() -1)));

					masterContext.setXMean(Utils.calculateMean(masterContext.getXOptimal(), slaveAverageOptimalValue, masterContext.getN())); 	//Take Mean
					masterContext.setU(Utils.vectorAdd(masterContext.getu(), masterContext.getxMean())); //Update u

					//Add the master optimal value in the matrix to check the convergence
					double[][] xDifferenceMatrix = result.xDifferenceMatrix();
					double[][] xMatrix = result.xsum();
					
					int time = 0;
					double[] masterXOptimalDifference = Utils.calculateVectorSubtraction(masterContext.getXOptimal(), masterXOptimalOld);
					double[] masterXOptimal = masterContext.getXOptimal();
					
					for(double d: masterXOptimalDifference) {
						//Fill the master optimal difference value in xDifference Matrix 
						xDifferenceMatrix[time][xDifferenceMatrix[0].length - 1] = d; //Add the xoptimal value at the end
						
						xMatrix[time][xMatrix[0].length - 1] =  masterXOptimal[time]; //Fill the master optimal value in XMatrix
						time++;
					}
					
					boolean converged = checkConvergence(xMatrix,xDifferenceMatrix, masterContext.getxMean(), oldXMean, masterContext.getN(), masterContext.getu(), cost, k);
					
//					long lEndTime = System.nanoTime();
//					long difference = lEndTime - lStartTime;
//					long milliseconds = difference/1000000;
//					System.out.println("Elapsed milliseconds: " + milliseconds);
//					System.out.println("Elapsed seconds: " + milliseconds/1000);
					
					if(converged == true) {
						System.out.println("////////////Converged/////////");
						System.out.println(">>>>>>>>>>> XSUMMM");
						Utils.PrintArray(xsum);
						System.out.println(">>>>>>> D"); 
						Utils.PrintArray(masterContext.getMasterData().getD());
						break;
					}
				} catch (IloException e) {
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
		}
		else //master task else
		{	
			boolean finish = false;
			boolean isFirstIteration = true;
			IloCplex cplex = null;
			
			try {
				cplex = new IloCplex();
			} catch (IloException e1) {
				e1.printStackTrace();
			}
			
			Map<Integer, double[]> optimalEVValues = new HashMap<Integer, double[]>(); // To store and override the optimal value of each EV
			
			while(true)
			{
				long startBarrierTimeSlaveSync = System.currentTimeMillis();
				peer.sync();
				peer.incrementCounter(EVADMMCounters.AGGREGATED_TIME_MS_SLAVE_WAIT_FOR_MASTER,(System.currentTimeMillis() - startBarrierTimeSlaveSync));

				long startBarrierTimeSlaveOptimization = System.currentTimeMillis();
				
				NetworkObjectMaster masterData = receiveMasterUAndXMeanList(peer);
				
				LongWritable key = new LongWritable();
				Text value = new Text();
				
				if(masterData.getDelta() == -1.0) { 
					finish = true;
				}
				else
				{
					while(peer.readNext(key, value) != false) {
						//Load the current input
						
						SlaveData slaveData = new SlaveData(value.toString());
						
						slaveContext = null;
						slaveContext = new SlaveContext(slaveData,
								masterData.getxMean(),
								masterData.getU(),
								slaveData.getEVNo(),
								RHO, isFirstIteration, peer, masterData.getDelta(), optimalEVValues.get(slaveData.getEVNo())); //Take old XOptimal value from the HashMap and send it to slaveContext
						
						try {
							//Do optimization and write the x_optimal to mat file
							
							
							
							double cost = slaveContext.optimize(cplex);
							
							optimalEVValues.put(slaveData.getEVNo(), slaveContext.getXOptimalSlave());
							
							//Utils.PrintArray(slaveContext.getXOptimalSlave());
							
							//System.out.println(">> Slave sending EV No -> " + slaveData.getEVNo());
							
							NetworkObjectSlave slave = new NetworkObjectSlave(slaveContext.getXOptimalSlave(), slaveData.getEVNo(), slaveContext.getXOptimalDifference(), cost);
							sendXOptimalToMaster(peer, slave);
						} catch (IloException e) {
							e.printStackTrace();
							peer.write(new IntWritable(1), new Text(e.getMessage()));
						}
					}
				}
				peer.incrementCounter(EVADMMCounters.AGGREGATED_TIME_MS_SLAVE_OPTIMIZATION,(System.currentTimeMillis() - startBarrierTimeSlaveOptimization));
				isFirstIteration = false;
				
				peer.sync(); //Send all the data
				peer.reopenInput(); //Start the input again
				
				if(finish == true) {
					break;
				}
			}
			
			cplex.end(); //clear the cplex object
		}
	}	
	
	/*
	 * (non-Javadoc)
	 * @see org.apache.hama.bsp.BSP#cleanup(org.apache.hama.bsp.BSPPeer)
	 */
	@Override
    public void cleanup(
        BSPPeer<LongWritable, Text, IntWritable, Text, Text> peer)
        throws IOException {
		System.out.println(peer.getPeerName() + " is shutting down");
      }
	
	/*
	 * (non-Javadoc)
	 * @see org.apache.hama.bsp.BSP#setup(org.apache.hama.bsp.BSPPeer)
	 */
	@Override
    public void setup(BSPPeer<LongWritable, Text, IntWritable, Text, Text> peer) throws IOException, SyncException, InterruptedException {
		this.masterTask = peer.getPeerName(0); //0 is our master
		
		AGGREGATOR_PATH = peer.getConfiguration().get(Constants.EVADMM_AGGREGATOR_PATH);
		//EV_PATH = peer.getConfiguration().get(Constants.EVADMM_EV_PATH);
		DEFAULT_ADMM_ITERATIONS_MAX = Integer.parseInt(peer.getConfiguration().get(Constants.EVADMM_MAX_ITERATIONS));
		RHO = Double.parseDouble(peer.getConfiguration().get(Constants.EVADMM_RHO));
		EV_COUNT = Integer.parseInt(peer.getConfiguration().get(Constants.EVADMM_EV_COUNT));
		
		System.out.println(peer.getPeerName() + " > PID ID: " + Main.getPidId());
	}
	
	/*
	 * This function runs only for Master node and creates a list of NetworkMasterObject. It populates 
	 * the EV Id array and delta before the start of iterations. The object returned is used in each iteration
	 * to send the data to each peer.
	 */
	private List<NetworkObjectMaster>  createMasterNetworkObjectList(BSPPeer<LongWritable, Text, IntWritable, Text, Text> peer) {
		List<NetworkObjectMaster> listOfNetworkMasterObjects = new ArrayList<NetworkObjectMaster>();
		//int currentEV = 0;
		
		for(int i=0; i < peer.getNumPeers() -1; i++) {
			listOfNetworkMasterObjects.add(
//					new NetworkObjectMaster(
//								null, 
//								null, 
//								new ArrayList<Integer>(),
//								masterContext.getMasterData().getDelta()));
			new NetworkObjectMaster(
					null, 
					null,
					masterContext.getMasterData().getDelta()));
		}
		
//		while(currentEV != masterContext.getN() - 1)
//		{	
//			listOfNetworkMasterObjects.get(currentEV % (peer.getNumPeers()-1)).addEV(currentEV);
//			currentEV++;
//		}
		return listOfNetworkMasterObjects;
	}
	
	/*
	 * This function checks the convergence of algorithm on master node. If the algorithm does not converges then it updates
	 * the RHO parameter and continues. 
	 * 
	 * It checks the cost variance for last 6 iterations and if it remains below 1e-9 then the algorithm stops otherwise
	 * it continues. Currently, the algorithm is only working based on the cost varaince. 
	 */
	private boolean checkConvergence(double[][] xMatrix, double[][] xDifferenceMatrix, double[] xMean, double[] xMean_old, int N, double[] u, ArrayList<Double> cost, int currentIteration)
	{	
		double[] xMeanOld_xMean = Utils.calculateVectorSubtraction(xMean_old, xMean);
		double[] result = Utils.addMatrixAndVector (xDifferenceMatrix, xMeanOld_xMean);
		double[] s = Utils.scalerMultiply(result, -1*EVADMMBsp.RHO*N);
		s_norm= Utils.calculateNorm(s);
		r_norm = Utils.calculateNorm(Utils.scaleVector(xMean, N));
		
		double cost_variance = 0;
		
		/* TODO: REMOVE THIS SECTION
		double sum = 0;
		for(double d: xMean)
			sum+=d;
		
		double sum1 = 0;
		for(double d: xMean_old)
			sum1+=d;

		double sum2 = 0;
		for(double d: s)
			sum2+=d;
		
		double sum3 = 0;
		for(double d: Utils.scaleVector(xMean, N))
			sum3+=d;
		
		double sum4 = 0;
		for(int i = 0; i < xMatrix[0].length; i++)
			for(int j =0 ; j < xMatrix.length; j++)
				sum4+=xMatrix[j][i];
		
		double sumMasterOptimal = 0;
		//for(int i = 0; i < xMatrix[0].length; i++)
			for(int j =0 ; j < xMatrix.length; j++)
			{
				sumMasterOptimal += xMatrix[j][xMatrix[0].length - 1];
			}
		
		double sum5 = 0;
		for(int i = 0; i < xMatrix[0].length-1; i++)
			for(int j =0 ; j < xMatrix.length; j++)
				sum5+=xMatrix[j][i];
		
		double sum6 = 0;
		for(int i = 0; i < xDifferenceMatrix[0].length; i++)
			for(int j =0 ; j < xDifferenceMatrix.length; j++)
				sum6+=xDifferenceMatrix[j][i];
		
		double sum7 = 0;
		for(double d: xMeanOld_xMean)
			sum7+=d;
		
		double sum8 = 0;
		for(double d: result)
			sum8+=d;
		
		double sumU=0;
		for(double d: u)
			sumU+=d;
		
		
END*/	
		if(currentIteration < 5)
			cost_variance = 1;
		else
		{	
			List<Double> costLast5Elements = cost.subList(cost.size()-5, cost.size());
			double[] last5 = ArrayUtils.toPrimitive(costLast5Elements.toArray(new Double[costLast5Elements.size()]));

			//cost_variance = Utils.getVariance(last5);
			cost_variance = Utils.calculateVariance(last5);
			System.out.print(" VARIANCE -> " + String.format("%.9f",cost_variance) );
		}

		//eps_pri= sqrt(N*T)*1 + 1e-3 * max([norm(x), norm(x(:)- repmat(xmean,N,1))]);
		double max = Math.max( Utils.calculateNorm(xMatrix), Utils.calculateNorm( Utils.addMatrixAndVector(xMatrix, Utils.scalerMultiply(xMean, -1))) );
		
		//System.out.println("<><><> Norm X: " + Utils.calculateNorm(xMatrix) + "  Norm Second: " + Utils.calculateNorm( Utils.addMatrixAndVector(xMatrix, Utils.scalerMultiply(xMean, -1))));
		double eps_pri = Math.sqrt(xMean.length * N) + (0.001 * max);
		
		//eps_dual= sqrt(N*T)*1 + 1e-3 *norm(rho*u);
		double eps_dual = (Math.sqrt(xMean.length * N) * 1) + (0.001 * Utils.calculateNorm(Utils.scalerMultiply(u, EVADMMBsp.RHO)));
		
		//Utils.PrintArray(u);
		
		//Uncomment
		System.out.print(" RHO: " +EVADMMBsp.RHO  + " R_NORM: " + r_norm + " eps_pri: " + eps_pri + " s_norm: " + s_norm + " eps_dual: " + eps_dual);
		
		
		//System.out.println("RESULT -> " + sum8 + " X -> " + sum4 +" X Without -> " + sum5 +" XDiffSUM -> " + sum6 + " XMEANOLD-XMEAN-> " + sum7 + "Xmean -> " + sum + "  Xmeanold -> " + sum1 + "  S -> " + sum2 + "  R -> " + sum3);
		
		//Uncomment
		System.out.println("");
//		System.out.println(" Total X -> " + sum4 + " Master X->" + sumMasterOptimal + " EV_X -> " + sum5 +" XDiffSUM -> " + sum6 +  " Xmean -> " + sum + " U -> " + sumU +"  Xmeanold -> " + sum1 + "  S -> " + sum2 + "  R -> " + sum3);
//		System.out.println("");
		
		if(r_norm <= eps_pri && s_norm <= eps_dual || (cost_variance <= 1e-9))
			return true;
		
//		double lambda = 0.1;
//		double mu = 0.1;
//		double vold = v;
//		v = ((EVADMMBsp.RHO * (r_norm/s_norm)) -1);
//		EVADMMBsp.RHO =  EVADMMBsp.RHO * Math.exp( (lambda* v) + (mu*(v-vold)) );
//		
//		masterContext.setRho(EVADMMBsp.RHO);
		
		//TODO: REMOVE THIS
		masterContext.setRho(0.01);
				
		//System.out.println("<><<><><><<<>>>>> New RHO: " + EVADMMBsp.RHO);
		
		return false;
	}
	
	/*
	 * This function is used to stop the code on all nodes. If the convergence criteria is met or total iterations have
	 * been fulfilled then a message is sent to all the peers to stop the code.
	 */
	private void sendFinishMessage(BSPPeer<LongWritable, Text, IntWritable, Text, Text> peer) throws IOException
	{
		for(String peerName: peer.getAllPeerNames()) {
			if(!peerName.equals(this.masterTask)) {
				//peer.send(peerName, new Text(Utils.networkMasterToJson(new NetworkObjectMaster(null,null,new ArrayList<Integer>(),0))));
				peer.send(peerName, new Text(Utils.networkMasterToJson(new NetworkObjectMaster(null,null,-1.0))));
			}	
		}
	}
	
	/*
	 * This function is responsible for sending the latest xMean and U to all the slaves.
	 */
	private void sendxMeanAndUToSlaves(BSPPeer<LongWritable, Text, IntWritable, Text, Text> peer, NetworkObjectMaster object, String peerName) throws IOException
	{
		peer.send(peerName, new Text(Utils.networkMasterToJson(object)));
	}
	
	/*
	 * This function is used by all the slaves to send the latest xOptimal value to master.
	 */
	private void sendXOptimalToMaster(BSPPeer<LongWritable, Text, IntWritable, Text, Text> peer, NetworkObjectSlave object) throws IOException
	{	
		peer.send(this.masterTask, new Text(Utils.networkSlaveToJson(object)));
	}
	
	/*
	 * This function is used to collect all the NetworkObjectSlave objects at the master node. It stores the all the
	 * values in a double array and this array is used to check the convergence. It also averages all the received 
	 * optimal value, adds up all the costs and stores all the difference of x_old* and x*.
	 */
	private Pair<double[],double[][], Double, double[][]> receiveSlaveOptimalValuesAndUpdateX(BSPPeer<LongWritable, Text, IntWritable, Text, Text> peer, int totalN) throws IOException
	{
		NetworkObjectSlave slave;
		Text receivedJson;
		
		double[][] s = new double[this.masterContext.getXOptimal().length][totalN];
		//double[] averageXReceived = Utils.getZeroArray(this.masterContext.getXOptimal().length);
		double[] averageXReceived = new double[this.masterContext.getXOptimal().length];
		double[][] allOptimalSlaveXReceived = new double[this.masterContext.getXOptimal().length][totalN];
		
		double cost = 0;
		
		while ((receivedJson = peer.getCurrentMessage()) != null) //Receive initial array 
		{
			slave = Utils.jsonToNetworkSlave(receivedJson.toString());
			averageXReceived =  Utils.vectorAdd(averageXReceived, slave.getXi());
			cost = cost + slave.getCost();
			
			//System.out.println(">> Master received EV No -> " + slave.getEVId());
			
			int i = 0;
			for(double d: slave.getXiDifference()) {
				s[i][slave.getEVId()] = d;
				i++;
			}
			
			int j = 0;
			for(double d: slave.getXi()) { //Store all the optimizal slave values recieved inside this array
				allOptimalSlaveXReceived[j][slave.getEVId()] = d;
				j++;
			}
			
			//ev++;
		}
	
		//return new Pair<double[],double[][], Double>(averageXReceived,s, cost);
		return new Pair<double[],double[][], Double,double[][]>(averageXReceived,s, cost,allOptimalSlaveXReceived); //For time being, I have removed the s from return. If required then use it.
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
	
	/*
	 * This function is responsible for receiving data at slave which is sent by the master.
	 */
	private NetworkObjectMaster receiveMasterUAndXMeanList(BSPPeer<LongWritable, Text, IntWritable, Text, Text> peer) throws IOException
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
		AGGREGATED_TIME_MS_MASTER_WAIT_FOR_SLAVES,
		AGGREGATED_TIME_MS_SLAVE_WAIT_FOR_MASTER,
		AGGREGATED_TIME_MS_SLAVE_OPTIMIZATION
	}
	
	/*
	 * This class is used to store values received from all the peers. 
	 */
	class Pair<T,U,C,X> {
	    private final T m_first;
	    private final U m_second;
	    private final C m_cost;
	    private final X m_xsum;

	    public Pair(T first, U second, C cost, X m_xsum) {
	        m_first = first;
	        m_second = second;
	        m_cost = cost;
	        this.m_xsum = m_xsum;
	    }

	    public T averageXReceived() {
	        return m_first;
	    }

	    public U xDifferenceMatrix() {
	        return m_second;
	    }
	    
	    public C cost() {
	    	return m_cost;
	    }
	    
	    public X xsum()
	    {
	    	return m_xsum;
	    }
	}
}

