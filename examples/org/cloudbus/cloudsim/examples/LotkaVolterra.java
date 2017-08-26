package org.cloudbus.cloudsim.examples;

import org.apache.commons.math3.exception.DimensionMismatchException;
import org.apache.commons.math3.exception.MaxCountExceededException;
import org.apache.commons.math3.ode.FirstOrderDifferentialEquations;

public class LotkaVolterra implements FirstOrderDifferentialEquations{
	/*a is vmCoeff*/
	private double a;
	/*b is cloudlet coeff*/
	private double b;
	
	public LotkaVolterra(double a, double b){
		this.a=a;
		this.b=b;
		
	}

	@Override
	public void computeDerivatives(double arg0, double[] arg1, double[] yDot)
			throws MaxCountExceededException, DimensionMismatchException {
		yDot[0]=a*arg1[0]-arg1[0]*arg1[1];
		yDot[1]=-b*arg1[1]+arg1[1]*arg1[0];
		
		
	}

	@Override
	public int getDimension() {
		// TODO Auto-generated method stub
		return 2;
	}
	

}
