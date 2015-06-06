package admm;

import ilog.concert.*;
import ilog.cplex.*;
public class TestCPLEX {
public static void main(String[] args) {
	
	
//	double[][] matrix = new double[5][6];
//	int m = 1;
//	for(int i=0; i< 5; i++)
//		for(int j =0; j< 6; j++)
//			matrix[i][j] = m++;
//	
//	double[] result = Utils.calculateMean(matrix);
//	
//	Utils.PrintArray(result);
	
	double[] v1 = new double[5];
	double[] v2 = new double[5];
	for(int i=1;i<6; i++)
	{
		v1[i-1] = i;
		v2[i-1] = i+5;
	}
	
	
	Utils.PrintArray(Utils.vectorAdd(v1, v2));
	
}
// try {
//		 IloCplex cplex = new IloCplex();
//		 double[] lb = {0.0, 0.0, 0.0};
//		 double[] ub = {40.0, Double.MAX_VALUE, Double.MAX_VALUE};
//		 IloNumVar[] x = cplex.numVarArray(3, lb, ub);
//		 double[] objvals = {1.0, 2.0, 3.0};
//		
//		 cplex.addMaximize(cplex.scalProd(x, objvals)); 
//		 
//		 cplex.addLe(cplex.sum(cplex.prod(-1.0, x[0]),
//		 cplex.prod( 1.0, x[1]),
//		 cplex.prod( 1.0, x[2])), 20.0);
//		 cplex.addLe(cplex.sum(cplex.prod( 1.0, x[0]),
//		 cplex.prod(-3.0, x[1]),
//		 cplex.prod( 1.0, x[2])), 30.0);
//		 cplex.exportModel("model2.lp");
//		 if ( cplex.solve() ) {
//			 cplex.output().println("Solution status = " + cplex.getStatus());
//			 cplex.output().println("Solution value = " + cplex.getObjValue());
//		 
//		 double[] val = cplex.getValues(x);
//		 int ncols = cplex.getNcols();
//		 
//		 for (int j = 0; j < ncols; ++j)
//			 cplex.output().println("Column: " + j + " Value = " + val[j]);
//		 }
//		 cplex.end();
//	}
//	catch (IloException e) {
//		System.err.println("Concert exception '" + e + "' caught");
//	}
//	}
} 

