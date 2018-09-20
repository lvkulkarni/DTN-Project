
package routing;

import java.util.Random;

import com.sun.org.apache.xerces.internal.util.SynchronizedSymbolTable;

import core.*;

public class SocialEnergyRouter extends EnergyAwareRouter implements ModuleCommunicationListener{

	public static final String INIT_ENERGY_S = "intialEnergy";
	
	public static final String SCAN_ENERGY_S = "scanEnergy";
	
	public static final String TRANSMIT_ENERGY_S = "transmitEnergy";
	
	public static final String WARMUP_S = "energyWarmup";

	public static final String ENERGY_PERCENTAGE_S = "energyPercentage";
	
	public static final String DECAY_TIME = "decayTime";
	
	public static final String ENERGY_VALUE = "Energy.value";
	
	public static final String SELFSIH_NODES = "selfishHosts";
	//Distribution pf energy with nodes
	public static final String S_ENERGY = "SocialEnergyDistribution";
	// Generate energy N(k,l)
	public static final String G_ENERGY = "GenerateEnergy";
	// Contribute partial energy "p" of generated energy
	//public static final String C_ENERGY = "ContributeEnergy";
	// Holds remaining part of energy
	public static final String H_ENERGY = "HoldEnergy";

	private final double[] initEnergy;
	private double warmupEnergy;
	private double currentEnergy;
	private double scanEnergy;
	private double transmitEnergy;
	private double lastScanUpdate;
	private double lastUpdate;
	private double scanInterval;
	private double energyPercentage=0.9;
	private int decayTime;
	private int selfishHosts;
	private double SocialEnergyDistribution;
	private double GenerateEnergy=90;
	//private double ContributeEnergy;
	private double HoldEnergy;
	private ModuleCommunicationBus comBus;
	private static Random rng = null;

	 
	public SocialEnergyRouter(Settings s) {
		super(s);
		this.initEnergy = s.getCsvDoubles(INIT_ENERGY_S);
		
		if (this.initEnergy.length != 1 && this.initEnergy.length != 2) {
			throw new SettingsError(INIT_ENERGY_S + " setting must have " + 
					"either a single value or two comma separated values");
		}
		
		this.scanEnergy = s.getDouble(SCAN_ENERGY_S);
		this.transmitEnergy = s.getDouble(TRANSMIT_ENERGY_S);
		this.scanInterval  = s.getDouble(SimScenario.SCAN_INTERVAL_S);
		this.energyPercentage = s.getDouble(ENERGY_PERCENTAGE_S);
		this.decayTime = s.getInt(DECAY_TIME);
		this.selfishHosts = s.getInt(SELFSIH_NODES);
		this.SocialEnergyDistribution = s.getDouble(S_ENERGY);
		this.GenerateEnergy = s.getDouble(G_ENERGY);
		//this.ContributeEnergy = s.getDouble(C_ENERGY);
		this.HoldEnergy = s.getDouble(H_ENERGY);
		
		
		if (s.contains(WARMUP_S)) {
			if (this.warmupEnergy == 1) {
				this.warmupEnergy = new Settings(report.Report.REPORT_NS).getInt(report.Report.WARMUP_S);
			}
		}
		else {
			this.warmupEnergy = 0;
		}
		
		if(s.contains(ENERGY_PERCENTAGE_S)) {
			if(this.energyPercentage == 1) {
				this.energyPercentage = Math.random();
			}
		}
		
	}
	
	public void SocialEnergyRouter1(Settings s) {
		HoldEnergy = (1-energyPercentage)*GenerateEnergy/2;
		System.out.println(HoldEnergy);
	}
	

	protected void setEnergy(double range[]) {
		if (range.length == 1) {
			this.currentEnergy = range[0];
		}
		else {
			if (rng == null) {
				rng = new Random((int)(range[0] + range[1]));
			}
			this.currentEnergy = range[0] + 
				rng.nextDouble() * (range[1] - range[0]);
		}
	}
	
	
	protected SocialEnergyRouter(SocialEnergyRouter r) {
		super(r);
		this.initEnergy = r.initEnergy;
		setEnergy(this.initEnergy);
		this.scanEnergy = r.scanEnergy;
		this.transmitEnergy = r.transmitEnergy;
		this.scanInterval = r.scanInterval;
		this.warmupEnergy  = r.warmupEnergy;
		this.comBus = null;
		this.lastScanUpdate = 0;
		this.lastUpdate = 0;
	}
	
	
	protected void reduceEnergy(double warmupEnergy) {
		if (SimClock.getTime() < this.warmupEnergy) {
			return;
		}
		
		comBus.updateDouble(ENERGY_VALUE, warmupEnergy);
		if (this.currentEnergy < 0) {
			comBus.updateProperty(ENERGY_VALUE, 0);
		}
	}
	
	
	protected int checkReceiving(Message m) {
		if (this.currentEnergy < 0) {
			return DENIED_UNSPECIFIED;
		}
		else {
			 return super.checkReceiving(m);
		}
	}
		
	
	protected void reduceSendingAndScanningEnergy() {
		double simTime = SimClock.getTime();
		
		if (this.comBus == null) {
			this.comBus = getHost().getComBus();
			this.comBus.addProperty(ENERGY_VALUE, this.currentEnergy);
			this.comBus.subscribe(ENERGY_VALUE, this);
		}
		
		if (this.currentEnergy <= 0) {
			
			this.comBus.updateProperty(NetworkInterface.RANGE_ID, 0.0);
			return; /* no more energy to start new transfers */
		}
		
		if (simTime > this.lastUpdate && sendingConnections.size() > 0) {
			
			reduceEnergy((simTime - this.lastUpdate) * this.transmitEnergy);
		}
		this.lastUpdate = simTime;
		
		if (simTime > this.lastScanUpdate + this.scanInterval) {
			
			reduceEnergy(this.scanEnergy);
			this.lastScanUpdate = simTime;
		}
	}
	
	@Override
	public void update() {
		super.update();
		reduceSendingAndScanningEnergy();
				
		if (isTransferring() || !canStartTransfer()) {
			return; 
		}
		
		
		if (exchangeDeliverableMessages() != null) {
			return;
		}
		
		this.tryAllMessagesToAllConnections();
	}
		
	@Override
	public SocialEnergyRouter replicate() {
		return new SocialEnergyRouter(this);
	}
	
	
	public void moduleValueChanged(String key, Object newValue) {
		this.currentEnergy = (Double)newValue;
	}

	
	@Override
	public String toString() {
		return super.toString() + " energy level = " + this.currentEnergy;
	}	
}