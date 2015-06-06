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
	List<String> fileList;
	private static final int DEFAULT_ADMM_ITERATIONS_MAX = 1;
	
	
	@Override
	public void bsp(BSPPeer<NullWritable, NullWritable, NullWritable, NullWritable, Text> peer) throws IOException, SyncException, InterruptedException {
		int k = 0; //iteration counter
		
		if(peer.getPeerName().equals(this.masterTask)) //The master task
		{	
			System.out.println("MASTER: Starting Iteration Loop");
			
			//TODO: Add convergence
			//TODO: Slave quite karao
			//TODO: rho value update
			
			//Calculate rnorm, snorm
			//Calculate eps_pri and eps_dual
			//Check convergence
			//Update rho value
			masterContext = new MasterContext();
			double[] xMeanOld;
			double[] uOld;
			
			while(k != DEFAULT_ADMM_ITERATIONS_MAX) //TODO: Or convergence is met
			{
				//RealMatrix xold = MatrixUtils.createColumnRealMatrix(masterContext.getx().getColumn(this.masterContext.getN()-1));
				//double[] xold = masterContext.getx().getColumn(this.masterContext.getN()-1);
				
				try {
					//Optimize + return the cost + save the optimal X* in the x matrix last column
					int currentEV = 0;
					double[] x_ev;
					
					System.out.println("MASTER:: Starting to send data to all EV's");
					while(currentEV != masterContext.getN() - 1)
					{	
						x_ev = Utils.getColumn(masterContext.getx(), currentEV);
						NetworkObjectMaster objectToSend = new NetworkObjectMaster(masterContext.getu(), masterContext.getxMean(),x_ev, currentEV);
						
						//Never send a message to peer 0 since it is the master
						sendxMeanAndUToSlaves(peer, objectToSend, peer.getPeerName((currentEV % (peer.getNumPeers()-1) + 1) ));
												
						currentEV++;
					}
					
					peer.sync(); //Send messages which are equal to peer.getNumPeers - 1 
					
					System.out.println("Master:: Receiving optimal values from Slaves => Iteration: " + currentEV);
					
					peer.sync();
					//Receive message
					receiveSlaveOptimalValuesAndUpdateX(peer);
					
					//TODO:
					System.out.println("Master:: Data sending complete. Doing Master optimiation");
					
					double[] xold = Utils.getColumn(masterContext.getx(), masterContext.getN() - 1);
					masterContext.optimize(xold);
					
					
					//Do convergence stuff.
					System.out.println("Master:: Calculating Mean");
					//Take Mean
					xMeanOld = masterContext.getxMean();
					masterContext.setXMean(Utils.calculateMean(masterContext.getx()));
					System.out.println("---->xMean<----");
					Utils.PrintArray(masterContext.getxMean());
					System.out.println("---->xMean -END<----");
					
					
					System.out.println("Master:: Calculating U");
					//Update u
					uOld = masterContext.getu();
					masterContext.setU(Utils.vectorAdd(uOld, masterContext.getxMean()));
					
					Utils.PrintArray(masterContext.getxMean());
					Utils.PrintArray(masterContext.getu());
					
					
				} catch (IloException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				k++;
			}
			
			System.out.println("---->>>Final X Value<<<<------");
			for(int i=0; i< this.masterContext.getx().length ;i++ ){
				for(int j=0; j< this.masterContext.getx()[0].length ;j++ ){
					System.out.print(this.masterContext.getx()[i][j] + "\t");
				}
				System.out.println();
			}
			
			
			
		}
		else //master task else
		{
			//Receive intial values
			//Optimize slave equation
			//Send slave data
			while(true)
			{
				peer.sync();
				System.out.println("Slave: " + peer.getPeerName() + ":: Out of Sync. Receiving master data and updating u and x");
				
				
				List<NetworkObjectMaster> masterDataList = receiveMasterUAndXMeanList(peer);
				System.out.println("================== PeerName:" + peer.getPeerName() + "   Received EVS: " + masterDataList.size());
				for(NetworkObjectMaster masterData: masterDataList)
				{
					slaveContext = new SlaveContext("/Users/raja/Documents/Thesis/ADMM_matlab/EVs/home/" + (masterData.getEVId() +1) + ".mat", 
										masterData.getxMean(), 
										masterData.getU(), 
										masterData.getx(), 
										masterData.getEVId());
					try {
						System.out.println("Slave: " + peer.getPeerName() + ":: Starting Slave optimization");
						slaveContext.optimize();
						NetworkObjectSlave slave = new NetworkObjectSlave(slaveContext.getXOptimalSlave(), slaveContext.getCurrentEVNo());
						System.out.println("Slave: " + peer.getPeerName() + ":: Sending slave x* to Master");
						sendXOptimalToMaster(peer, slave);
						
					} catch (IloException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				peer.sync(); //Send all the data
				
//				NetworkObjectMaster masterData = receiveMasterUAndXMean(peer);
//				slaveContext = new SlaveContext("/Users/raja/Documents/Thesis/ADMM_matlab/EVs/home/" + (masterData.getEVId() +1) + ".mat", 
//									masterData.getxMean(), 
//									masterData.getU(), 
//									masterData.getx(), 
//									masterData.getEVId());
//				try {
//					System.out.println("Slave: " + peer.getPeerName() + ":: Starting Slave optimization");
//					slaveContext.optimize();
//					NetworkObjectSlave slave = new NetworkObjectSlave(slaveContext.getXOptimalSlave(), slaveContext.getCurrentEVNo());
//					System.out.println("Slave: " + peer.getPeerName() + ":: Sending slave x* to Master");
//					sendXOptimalToMaster(peer, slave);
//					peer.sync();
//					
//					
//				} catch (IloException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				}
			}
			
		}
	}
	
	@Override
    public void cleanup(
        BSPPeer<NullWritable, NullWritable, NullWritable, NullWritable, Text> peer)
        throws IOException {
		
      }
	
	@Override
    public void setup(BSPPeer<NullWritable, NullWritable, NullWritable, NullWritable, Text> peer) throws IOException, SyncException, InterruptedException {
		// Choose one as a master
		//this.masterTask = peer.getPeerName(peer.getNumPeers() / 2);
		this.masterTask = peer.getPeerName(0); //0 is out master
		
		//Divide the files
		fileList = new ArrayList<String>();
		int startIndex=0;
		int endIndex=0;
		
		if(peer.getPeerName().equals("local:1")){
			startIndex = 1;
			endIndex = 333;
		}
		else if(peer.getPeerName().equals("local:2")){
			startIndex = 334;
			endIndex = 666;
		}
		else if(peer.getPeerName().equals("local:3")){
			startIndex = 667;
			endIndex = 1000;
		}
		
		for(int i = startIndex; i <= endIndex; i++)
			fileList.add("/Users/raja/Documents/Thesis/ADMM_matlab/EVs/home/" + Integer.toString(i) + ".mat");
		
	}
	
	private void sendxMeanAndUToSlaves(BSPPeer<NullWritable, NullWritable, NullWritable, NullWritable, Text> peer, NetworkObjectMaster object, String peerName) throws IOException
	{
//		for(String otherPeer : peer.getAllPeerNames())
//		{
//			if(!otherPeer.equals(this.masterTask))
//			{
//				//Send incentive signal
//				System.out.println("MASTER: Sending data to slaves");
//				
//				peer.send(otherPeer, new Text(Utils.networkMasterToJson(object)));
//			}
//		}
		
		peer.send(peerName, new Text(Utils.networkMasterToJson(object)));
	}
	
	private void sendXOptimalToMaster(BSPPeer<NullWritable, NullWritable, NullWritable, NullWritable, Text> peer, NetworkObjectSlave object) throws IOException
	{	
		peer.send(this.masterTask, new Text(Utils.networkSlaveToJson(object)));
	}
	
	private void receiveSlaveOptimalValuesAndUpdateX(BSPPeer<NullWritable, NullWritable, NullWritable, NullWritable, Text> peer) throws IOException
	{
		//List<MasterContext> list = new ArrayList<MasterContext>();
		NetworkObjectSlave slave;
		Text receivedJson;
		while ((receivedJson = peer.getCurrentMessage()) != null) //Receive initial array 
		{
			//System.out.println("Master Received Message");
			slave = Utils.jsonToNetworkSlave(receivedJson.toString());
			masterContext.setX(Utils.setColumnInMatrix(this.masterContext.getx(), slave.getXi(), slave.getEVId()));
		}
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
	
}

//while(currentEV != masterContext.getN() - 1)
//{	
//	int currentPeer = (currentEV+1) % (peer.getNumPeers());
//	if( currentPeer == 0 || currentEV == masterContext.getN() - 1)
//	{	//Send all the messages
//		currentEV--; 
//		//We are back to the first peer. Send the message to peer 1
//		System.out.println("Master:: Goint to Sync => Iteration: " + currentEV);
//		peer.sync(); //Send messages which are equal to peer.getNumPeers - 1 
//		
//		System.out.println("Master:: Receiving optimal values from Slaves => Iteration: " + currentEV);
//		
//		peer.sync();
//		//Receive message
//		receiveSlaveOptimalValuesAndUpdateX(peer);
//	}
//	else
//	{
//		x_ev = Utils.getColumn(masterContext.getx(), currentEV);
//		NetworkObjectMaster objectToSend = new NetworkObjectMaster(masterContext.getu(), masterContext.getxMean(),x_ev, currentEV);
//		
//		sendxMeanAndUToSlaves(peer, objectToSend, peer.getPeerName((currentEV+1) % peer.getNumPeers()));
//	}
