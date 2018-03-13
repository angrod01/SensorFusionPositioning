package com.hkr.android.sensorfusion;

public class KalmanFilter {
	
	
	double[] x_corrected = new double[2];
	
	private double [][] A = new double [2][2];
	private double [][] A_transpose = new double [2][2];
	private double [] B = new double [2];
	private double [] H = new double [2];
	private double [] H_transpose = new double [2];
	private double u;
	
	private double [][] I = new double [2][2];
	
	private double R;
	private double [][] Q = new double [2][2];
	

	private double [][] P = new double [2][2];
	
	
	
	public KalmanFilter(double r, double[][] q, double u){
		A[0][0] = 0; A[0][1] = 0; A[1][0] = 0; A[1][1] = 0;
		A_transpose[0][0] = A[0][0]; A_transpose[0][1] = A[1][0]; A_transpose[1][0] = A[0][1]; A_transpose[1][1] = A[1][1];
		B[0] = 0; B[1] = 0;
		
		H[0] = 0; H[1] = 0;
		H_transpose[0] = H[1]; H_transpose[1] = H[0];
		
		I[0][0] = 1; I[0][1] = 0; I[1][0] = 0; I[1][1] = 1;
		
		R = r;
		Q = q.clone();
		this.u = u; 
	}
	
	public double[] predictAndCorrect(double u_Updated){
		
		this.u = u_Updated;
		
		double[] x_predicted = new double[2];
		double [][] P_predicted = new double [2][2];
		double [][] K = new double [2][2];
		
		
		//predict state vector
		
		x_predicted[0] = A[0][0]*x_corrected[0] + A[0][1]*x_corrected[1] + B[0]*u;
		x_predicted[1] = A[1][0]*x_corrected[0] + A[1][1]*x_corrected[1] + B[1]*u;

		//predict covariance matrix
		
		P_predicted[0][0] = A[0][0]*P[0][0] + A[0][1]*P[1][0];
		P_predicted[0][0] = P_predicted[0][0]*A_transpose[0][0] + P_predicted[0][1]*A_transpose[1][0];
		P_predicted[0][0] = P_predicted[0][0] + Q[0][0];
		
		P_predicted[0][1] = A[0][0]*P[0][1] + A[0][1]*P[1][1];
		P_predicted[0][1] = P_predicted[0][0]*A_transpose[0][1] + P_predicted[0][1]*A_transpose[1][1];
		P_predicted[0][1] = P_predicted[0][1] + Q[0][1];
		
		P_predicted[1][0] = A[1][0]*P[0][0] + A[1][1]*P[1][0];
		P_predicted[1][0] = P_predicted[1][0]*A_transpose[0][0] + P_predicted[1][1]*A_transpose[1][0];
		P_predicted[1][0] = P_predicted[1][0] + Q[1][0];
		
		P_predicted[1][1] = A[1][0]*P[0][1] + A[1][1]*P[1][1];
		P_predicted[1][1] = P_predicted[1][0]*A_transpose[0][1] + P_predicted[1][1]*A_transpose[1][1];
		P_predicted[1][1] = P_predicted[1][1] + Q[1][1];
		
		
		//kalman gain
		
		double[][] S = new double[2][2];
		
		
		
/*	    // Predict the next state
	    xHat_t = A * xHat_t + B * u_t;

	    // Predict the next covariance
	    EHat_t = A * EHat_t * A' + E_x;

	    // Kalman gain
	    K_t    = EHat_t * C' * inv(C * EHat_t * C' + E_z);

	    // Update state estimate
	    x_t = xHat_t + K_t *(z_t(t) - C*xHat_t);

	    // Update covariance estimate
	    E_t = (eye(2) - K_t * C)*EHat_t;

	    // Store for plotting
	    xHat_t1 = [xHat_t1; xHat_t(1)];
	    xHat_t2 = [xHat_t2; xHat_t(2)];
	    EHat_t_mag = [EHat_t_mag; EHat_t(1)];*/

		
		
		
		
		return null;
	}

}
