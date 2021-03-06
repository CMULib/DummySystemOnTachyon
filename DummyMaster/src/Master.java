import java.util.ArrayList;
import java.util.LinkedList;
import cmu.core.Mat;
import cmu.core.MatOp;
import cmu.decomp.svd.Master_SVD;
import cmu.decomp.svd.Master_Spliter;
import cmu.help.Tag;
import java.io.IOException;
import java.io.*;

import edu.cmu.cmulib.communication.CommonPacket;
import com.yiranf.TachyonTest.*;

/*
   How to use this
   Master master = new Master(4, "/BinData", 8000);
   master.init();
   do {
     String str = master.excute();
   while(!master.isCompleted());
    String final = master.dispFinal();
  */
public class Master {
	
	private Mat score;
	private Mat Like;
	private Master_Spliter split;
	private Master_SVD svd;
	public int slaveNum;
	public String filePath;
	public int port;
	
	public Master() {
		this.slaveNum = 1;
		this.filePath = "/BinData";
		this.port = 8000;
	}
	public Master(int slaveNum, String filePath, int port) {
		this.slaveNum = slaveNum;
		this.filePath = filePath;
		this.port = port;
	}
	public void init() {
        double[] test = new double[1000*1000];
        //int q = 0;
        LinkedList<Double[]> mList = new LinkedList<Double[]>();
        String masterLocation = "tachyon://localhost:19998";
        try {
              DataHandler t = new DataHandler(masterLocation, filePath);
              test = t.readData();
        } catch (IOException e) {}

        // initialize original matrix
        int rows = 1000;
        int cols = 1000;
        this.score = new Mat(rows, cols ,test);
            
        MasterMiddleWare commu = new MasterMiddleWare(port);
        commu.register(Double[].class,mList);
        commu.startMaster();
                    
        this.split = new Master_Spliter(score, slaveNum);
        this.svd = new Master_SVD(score, slaveNum);
        while(commu.slaveNum()<slaveNum){System.out.println(commu.slaveNum());}
        this.Like = svd.initL();
	}
	public String execute() {
		Tag tag;
		Mat slaveL = null;
        // compute the first eigenvector iterately
            int remain = slaveNum;
            svd.setL(Like);
            String output = dispArray(Like.data);   // information need to show 
            // send L
            for (int i = 1; i <= slaveNum; i++){
                sendMat(Like,i,commu);
             }
            //send Tag
                ArrayList<Tag> index = split.split();
                for(int i = 0; i < index.size(); i++) {
                    tag = index.get(i);
                    CommonPacket packet = new CommonPacket(-1,tag);
                    commu.sendPacket(i+1, packet);
                }
            // receive L and update
              while (remain > 0) {
                synchronized (mList) {
                    if (mList.size() > 0) {
                        slaveL = getMat(mList);
                        svd.update_SVD(slaveL);
                        remain--;
                    }
                }
              }
                    
            Like = svd.getUpdateL();
            MatOp.vectorNormalize(Like, MatOp.NormType.NORM_L2);
        return output;
	}
	
	public boolean isCompleted() {
		return svd.isPerformed();
	}
    public String dispFinal() {
        String finalout = "final  " + dispArray(this.Like.data);   // final information
        return finalout;
    }

	
	// Original main method
		public static void main (String[] args) throws IOException {
        // 4 slaves assumed
        double[] test = new double[1000*1000];
        //int q = 0;
		int slaveNum = 4;
        LinkedList<Double[]> mList = new LinkedList<Double[]>();

        String masterLocation = "tachyon://localhost:19998";
        String filePath = "/BinData";
        try {
              DataHandler t = new DataHandler(masterLocation, filePath);
              test = t.readData();
        } catch (IOException e) {}


        // initialize original matrix
        int rows = 1000;
        int cols = 1000;
        Mat score = new Mat(rows, cols ,test);
        Tag tag;
        Mat Like, slaveL;

        int port = Integer.parseInt(args[0]);
            
        MasterMiddleWare commu = new MasterMiddleWare(port);
        commu.register(Double[].class,mList);
        commu.startMaster();
            
            
        Master_Spliter split = new Master_Spliter(score, slaveNum);
        Master_SVD svd = new Master_SVD(score, slaveNum);
        while(commu.slaveNum()<slaveNum){System.out.println(commu.slaveNum());}
        Like = svd.initL();
        slaveL = null;
         
        // compute the first eigenvector iterately
        do {
            int remain = slaveNum;
            svd.setL(Like);
            printArray(Like.data);
            // send L
            for (int i = 1; i <= slaveNum; i++){
                sendMat(Like,i,commu);
             }
            //send Tag
                ArrayList<Tag> index = split.split();
                for(int i = 0; i < index.size(); i++) {
                    tag = index.get(i);
                    CommonPacket packet = new CommonPacket(-1,tag);
                    commu.sendPacket(i+1, packet);
                }
            // receive L and update
              while (remain > 0) {
                synchronized (mList) {
                    if (mList.size() > 0) {
                        slaveL = getMat(mList);
                        svd.update_SVD(slaveL);
                        remain--;
                    }
                }
              }
                    
            Like = svd.getUpdateL();
            MatOp.vectorNormalize(Like, MatOp.NormType.NORM_L2);
        } while (!svd.isPerformed(Like));     //termination of iteration
        System.out.println("final  ");
        printArray(Like.data);
    }    
        /*
        System.out.println("PPPPPPPPPPPPPPPP");
        double [] a = {1.1, 2.2, 3.3, 4.4};
        int count =0;
        while (count<10){
        	count++;
        	int remain = 4;
            while (commu.slaveNum() != slaveNum){System.out.println(commu.slaveNum());}
            for (int i = 1; i <= slaveNum; i++){
            	Mat mat = new Mat(2,2,a);
        	    sendMat(mat,i,commu);
                
            }
            
            while (remain > 0) {
            	
                synchronized (mList) {
                    if (mList.size() > 0) {                
                    	Mat mat = getMat(mList);
                    	a = mat.data;
                    	remain--;
                    	
                    }
               }
            }
            System.out.println(a[0]+" "+a[1]+" "+a[2]+" "+a[3]);
        }
        */
        /*
		Master_Spliter split = new Master_Spliter(score, slaveNum);
		Master_SVD svd = new Master_SVD(score, slaveNum);
		
		Like = svd.initL();
		slaveL = null;
		do {
			svd.setL(Like);
			commu.push(Like);
			ArrayList<Tag> index = split.split();
			for(int i = 0; i < index.size(); i++) {
				tag = index.get(i);
				commu.push(tag);
			}
			for (int i = 0; i < slaveNum; i++) {
//				do {
					slaveL = commu.pull();
//				} while (slaveL == null);
				svd.update_SVD(slaveL);
			}
			Like = svd.getUpdateL();
			MatOp.vectorNormalize(Like, MatOp.NormType.NORM_L2);
//			System.out.println(Like.data[0] + "  " + Like.data[1]+ "  " + Like.data[2]);
		} while (!svd.isPerformed(Like));		
		System.out.println("final  " + Like.data[0] + "  " + Like.data[1]+ "  " + Like.data[2]);
		*/
        /*Double[] a = {1.1, 2.2, 3.3, 4.4};

        while (a[0] + a[1] + a[2] + a[3] < 100.0) {
            int remain = 4;
            while (commu.slaveNum() != slaveNum) {System.out.print(commu.slaveNum());}
            System.out.println("\n");

            for (int i = 1; i <= slaveNum; i++) {
            	CommonPacket packet = new CommonPacket(-1,a[i - 1]);
            	System.out.println("before send packet");
                commu.sendPacket(i, packet);
                System.out.println("after send packet");
            }

            while (remain > 0) {
                synchronized (commu.msgs) {
                    if (commu.msgs.size() > 0) {
                        System.out.println(commu.msgs.peek().para);
                        a[commu.msgs.peek().fromId - 1] = commu.msgs.peek().para;
                        commu.msgs.remove();
                        remain--;
                    }
                }
            }

            double sum = a[0] + a[1] + a[2] + a[3];*/
            //System.out.println("sum :" + sum);

		public static void printArray(double[] arr){
			for(double i: arr)
				System.out.print(i+" ");
			System.out.println();
		}
		public static String dispArray(double[] arr){
			String s = "";
			for(double i: arr)
				s+= i + " ";
			s+= "\n";
			return s;
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
		
		
		public static void sendMat(Mat mat,int id,MasterMiddleWare m){
			Double [] array = new Double[mat.data.length+2];
		    array[0] = Double.valueOf(mat.rows);
		    array[1] = Double.valueOf(mat.cols);
		    
		    for(int k=0; k<mat.data.length;k++)
		    	array[k+2] = Double.valueOf(mat.data[k]);
	        CommonPacket packet = new CommonPacket(-1, array);
	        
	        m.sendPacket(id, packet);
			
		}
	
}
