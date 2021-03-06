import cmu.core.Mat;
import cmu.decomp.svd.Slave_SVD;
import cmu.decomp.svd.Slave_getSplitedMatrix;
import cmu.help.Tag;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.util.LinkedList;
import java.io.*;

import edu.cmu.cmulib.communication.CommonPacket;
import com.yiranf.TachyonTest.*;

public class Slave {
	public int SlaveId;
	public double workspan = Double.MAX_VALUE;
	
	public Slave (int SlaveId, double workspan) {
		this.SlaveId = SlaveId;	
		this.workspan = workspan;
	}
	
	public static void printArray(double[] arr){
		for(double i: arr)
			System.out.print(i+" ");
		System.out.println();
	}
	
	public static void main (String[] args) throws IOException {
        
        // initialize original matrix
        double[] test = new double[1000*1000];
		int rows = 1000;
		int cols = 1000;
        String address = args[0];
        int port = Integer.parseInt(args[1]);
        int q = 0;

        String masterLocation = "tachyon://localhost:19998";
        String filePath = "/BinData";
        try {
            DataHandler t = new DataHandler(masterLocation, filePath);
            test = t.readData();
        } catch (IOException e) {}
    
		LinkedList<Double[]> mList = new LinkedList<Double[]>();
        LinkedList<Tag> tagList = new LinkedList<Tag>();
        
		Mat score = new Mat(rows, cols ,test);
        Mat S, L;

        //String address = InetAddress.getLocalHost().getHostAddress();
        SlaveMiddleWare sdSlave = new SlaveMiddleWare(address, port);
        sdSlave.register(Double[].class, mList);
        sdSlave.register(Tag.class, tagList);
        System.out.println(address + " " + port);
        sdSlave.startSlave();
        
		Slave_getSplitedMatrix split = new Slave_getSplitedMatrix(score);
		Slave_SVD svd = new Slave_SVD();

        // update L using equation L=SS(trans)L
        while(true){
            //receive tag and compute L
            synchronized (tagList) {
                if (tagList.size() > 0) {
                    split.setTag(tagList.peek());
                    tagList.remove();
                    S = split.construct();
                    L = svd.Slave_UpdateL(S);
                    printArray(L.data);
                    sendMat(L,sdSlave);

                }
            }
            //receive L
            synchronized (mList) {
                if (mList.size() > 0) {
                    System.out.println("enter slave synchronized");
                    L = getMat(mList);
                    svd.setL(L);

                    
                }
            }
            
        }
//		tag = commu.pullTag();
//		split.setTag(tag);
//		S = split.construct();
//		svd.setS(S);
//		L = commu.pullL();
//		svd.setL(L);
//		L = svd.Slave_UpdateL(S);
//		commu.push(L);
		

        /*
        while(true){
        	synchronized (mList) {
                if (mList.size() > 0) {
                	System.out.println("enter slvae synchronized");           
                    Mat n = getMat(mList);
                	printArray(n.data);
                	sendMat(n.mul(2),sdSlave);               	
                	
                }
            }

        }
        */
	}
	
	
	public static Mat getMat(LinkedList<Double[]> mList){
		Double [] temp = mList.peek();
    	double row = temp[0];
    	double col = temp[1];
    	double [] arr = new double[temp.length-2];
    	for(int k=0;k<arr.length;k++){
    		arr[k] = temp[k+2];
    	}
    	Mat mat = new Mat((int)row,(int)col,arr);    	
        mList.remove();
        return mat;
		
	}
	
	public static void sendMat(Mat mat,SlaveMiddleWare m){
		Double [] array = new Double[mat.data.length+2];
	    array[0] = Double.valueOf(mat.rows);
	    array[1] = Double.valueOf(mat.cols);
	    
	    for(int k=0; k<mat.data.length;k++)
	    	array[k+2] = Double.valueOf(mat.data[k]);
        CommonPacket packet = new CommonPacket(-1, array);
        
        m.sendPacket(packet);
		
	}
	
}
