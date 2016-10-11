package tw.everest.pi.test01;


import com.pi4j.io.i2c.I2CBus;
import com.pi4j.io.i2c.I2CDevice;
import com.pi4j.io.i2c.I2CFactory;
import com.pi4j.util.Console;

import tw.everest.jni.test01.Pedometer;

public class Run {

	public static final int DEVICE_ADDR = 0x18; 
	public static final int PID_ADDR = 0x00; 
	public static final int MOTION_THRESHOLD=0x03;
	public static final int STATUS_REGISTER=0x05;
	public static final int DATA=0x04;
	
	final static Console console = new Console();
	
	private I2CBus i2c;
	private I2CDevice device;
	private byte[] status;
	private Pedometer pedo;
	private long stepCounter;
	
	public Run() throws Exception{
		stepCounter=0;
		i2c = I2CFactory.getInstance(I2CBus.BUS_1);		 
		device = i2c.getDevice(DEVICE_ADDR);
		pedo=new Pedometer();
		pedo.ResetAlgo();
		pedo.ResetStepCount();
		pedo.InitAlgo((char)3);
		
		 console.title("<-- The Pi4J Project -->", "I2C Example");
		 console.promptForExit();
		 status=getBooleanArray(device.read(STATUS_REGISTER));
		 Thread r=new Thread(checkStatus);
		 r.start();
	}
	
	public static void main(String[] args) throws Exception {		
		Run r =new Run();
		r.info();
		/*for(int i =0 ;i<100;i++){
			byte[] d=r.readData();
			console.println(String.format("%03d",d[3])+" "+String.format("%03d",d[4])
			+" "+String.format("%03d",d[5])+" "+String.format("%03d",d[6])
			+" "+String.format("%03d",d[7])+" "+String.format("%03d",d[8]));
			short x=Run.bytesToShortXY(d[4], d[3]);
			short y=Run.bytesToShortXY(d[6], d[5]);
			short z=Run.bytesToShortXY(d[8], d[7]);
			console.println(String.format("%03d",x)+" "+String.format("%03d",y)
			+" "+String.format("%03d",z));
			console.println(" ");
			Thread.sleep(3000);
		}*/
	}
	
	public short countStep(short x, short y,short z){
		return pedo.ProcessAccelarationData(x, y, z);
	}
	
	public byte[] readData()  throws Exception{
		byte[] buffer= new byte[11]; 
		int count=device.read(DATA,buffer,0,11);
		//console.println("READ DATA SIZE:"+count);
		return  buffer ;
	}
	
	public void info() throws Exception{
		int response=device.read(PID_ADDR);
		console.println("PID = " + String.format("0x%02x", response));
		response=device.read(MOTION_THRESHOLD);
		console.println("MOTION THRESHOLD = " +0.25*response +" g" );	
		if(status[5]==1){
			console.println("DSP is in continuous mode.");
		}else{
			console.println("DSP is in non-continuous mode.");
		}
		
	}
	
	
	Runnable checkStatus=new Runnable(){

		@Override
		public void run() {
			
			while(true){								
				try{	
					byte[] xyzData= readData();
					short x=bytesToShortXY(xyzData[4], xyzData[3]);
					short y=bytesToShortXY(xyzData[6], xyzData[5]);
					short z=bytesToShortZ(xyzData[8], xyzData[7]);
					short t=bytesToShortZ(xyzData[10], xyzData[9]);
					pedo.ProcessAccelarationData(x, y, z);
					long counter=pedo.GetStepCount();
					byte[]  nB=getBooleanArray(device.read(STATUS_REGISTER));
					if(counter!=stepCounter){
						stepCounter=counter;										
					}
					boolean changed=false;
					for(int i=0;i<8;i++){
						if(nB[i]!=status[i]){
							changed=true;
							switch(i){
							case 0:
								if(nB[i]==0){
									console.println("DSP is started");
								}else{
									console.println("DSP is stopped");
								}
								break;
							case 1:
								if(nB[i]==0){
									//console.println("programming OTP data is not completed");
								}else{
									console.println("programming OTP data is completed");
								}
								break;
							case 2:
								if(nB[i]==0){
									//console.println("loading OTP data to DSP is not completed");
								}else{
									console.println("loading OTP data to DSP is completed");
								}
								break;
							case 3:
								if(nB[i]==0){
									//console.println("OTP data is not valid when loading OTP to DSP");
								}else{
									console.println("OTP data is valid when loading OTP to DSP");
								}
								break;
							case 4:
								if(nB[i]==0){
									//console.println("no motion interrupt");
								}else{
									console.println("motion interrupt is asserted");																											
									console.println("X:"+x+"  Y:"+y+"  Z:"+z+"  T:"+t+" Step Counter :"+stepCounter);
									console.println("Bandwidth:"+pedo.GetBandwidthSwitchInfo());
								}
								break;
							case 5:
								if(nB[i]==0){
									//console.println("DSP is in the non-continuous mode");
								}else{
									console.println("DSP is in continuous mode");
								}
								break;
							case 6:
								if(nB[i]==0){
									//console.println("data has not been over-written");
								}else{
									console.println("data has been over-written before read");
								}
								break;
							case 7:
								if(nB[i]==0){
									//console.println("no new data arrival");
								}else{
									console.println("interrupt asserted by new data arrival");
								}
								break;
							}
						}
					}
					if(changed)
						status=nB;
					
					Thread.sleep(40);
				}catch (Exception e) {
					
				}
			}			
		}
		
	};
	
	
	public static void printBA(byte[] bs){
		 for(byte b:bs){
			 console.print(String.format("0x%02x", b)+" ");
		 }
	}

	 /** 
     * 将int转换为一个长度为8的byte数组，数组每个值代表bit 
     */
    public static byte[] getBooleanArray(int b) {  
        byte[] array = new byte[8];  
        for (int i = 7; i >= 0; i--) {  
            array[i] = (byte)(b & 1);  
            b =  (b >> 1);  
        }  
        return array;  
    } 
	
	 /** 
     * 将byte转换为一个长度为8的byte数组，数组每个值代表bit 
     */
    public static byte[] getBooleanArray(byte b) {  
        byte[] array = new byte[8];  
        for (int i = 7; i >= 0; i--) {  
            array[i] = (byte)(b & 1);  
            b = (byte) (b >> 1);  
        }  
        return array;  
    }  
    /** 
     * 把byte转为字符串的bit 
     */  
    public static String byteToBit(byte b) {  
        return ""  
                + (byte) ((b >> 7) & 0x1) + (byte) ((b >> 6) & 0x1)  
                + (byte) ((b >> 5) & 0x1) + (byte) ((b >> 4) & 0x1)  
                + (byte) ((b >> 3) & 0x1) + (byte) ((b >> 2) & 0x1)  
                + (byte) ((b >> 1) & 0x1) + (byte) ((b >> 0) & 0x1);  
    }  
    
    public static short bytesToShortXY(byte high,byte low){
    	short retuenValue=0;
    	retuenValue=(short) (((retuenValue&high)<<8) + (low>>1));
    	return retuenValue;
    }

    public static short bytesToShortZ(byte high,byte low){
    	short retuenValue=0;
    	retuenValue=(short) (((retuenValue&high)<<8) + low);
    	return retuenValue;
    }
}
