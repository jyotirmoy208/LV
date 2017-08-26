/*
 * Title:        CloudSim Toolkit
 * Description:  CloudSim (Cloud Simulation) Toolkit for Modeling and Simulation of Clouds
 * Licence:      GPL - http://www.gnu.org/copyleft/gpl.html
 *
 * Copyright (c) 2009-2012, The University of Melbourne, Australia
 */

package org.cloudbus.cloudsim;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeMap;

import org.apache.commons.math3.ode.FirstOrderDifferentialEquations;
import org.apache.commons.math3.ode.FirstOrderIntegrator;
import org.apache.commons.math3.ode.nonstiff.DormandPrince853Integrator;
import org.apache.commons.math3.ode.sampling.StepHandler;
import org.apache.commons.math3.ode.sampling.StepInterpolator;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.CloudSimTags;
import org.cloudbus.cloudsim.core.SimEntity;
import org.cloudbus.cloudsim.core.SimEvent;
import org.cloudbus.cloudsim.examples.LotkaVolterra;
import org.cloudbus.cloudsim.examples.LotkaVolterraPojo;
import org.cloudbus.cloudsim.lists.CloudletList;
import org.cloudbus.cloudsim.lists.VmList;

/**
 * DatacentreBroker represents a broker acting on behalf of a user. It hides VM management, as vm
 * creation, sumbission of cloudlets to this VMs and destruction of VMs.
 * 
 * @author Rodrigo N. Calheiros
 * @author Anton Beloglazov
 * @since CloudSim Toolkit 1.0
 */
public class DatacenterBroker extends SimEntity {
	private int totalVmPool=100;
	private static final int CREATE_INTERMITTENT_VMS = 10458;
	private int vmStartingNumber=300;
	private int cloudNumber=700;
	/** The vm list. */
	protected List<? extends Vm> vmList;

	/** The vms created list. */
	protected List<? extends Vm> vmsCreatedList;

	/** The cloudlet list. */
	protected List<? extends Cloudlet> cloudletList;

	/** The cloudlet submitted list. */
	protected List<? extends Cloudlet> cloudletSubmittedList;

	/** The cloudlet received list. */
	protected List<? extends Cloudlet> cloudletReceivedList;

	/** The cloudlets submitted. */
	protected int cloudletsSubmitted;

	/** The vms requested. */
	protected int vmsRequested;

	/** The vms acks. */
	protected int vmsAcks;

	/** The vms destroyed. */
	protected int vmsDestroyed;

	/** The datacenter ids list. */
	protected List<Integer> datacenterIdsList;

	/** The datacenter requested ids list. */
	protected List<Integer> datacenterRequestedIdsList;

	/** The vms to datacenters map. */
	protected Map<Integer, Integer> vmsToDatacentersMap;

	/** The datacenter characteristics list. */
	protected Map<Integer, DatacenterCharacteristics> datacenterCharacteristicsList;
	/**
	 * this map holds SLA monitoring count since the beginning of the execution and the response time
	 */
	private Map<Integer,Double>  mapResponseTimeForPrediction=new HashMap<Integer,Double>();
	
	Datacenter datacenter=null;
	/**
	 * this variable keeps the count of SLA monitoring
	 */
	private Integer predictionCount=0;
	private double responseTimeUpperThreshold=400.0;
	private double responseTimeLowerThreshold=100.0;
	/**
	 * below map keeps track of number of vms created against time
	 * means Long is the time in millis and Integer is the number of VMs at that point of time.
	 */
	private Map<Long,Integer> vmVsTimeMap=new TreeMap<Long,Integer>();
	
	private Long vmTime=0L;
	private Long vmTime1=0L;
	
	private Map<Long,Double> utilizationVsTimeMap=new TreeMap<Long,Double>();
	
	private Map<Long,CloudletVsVm> mapOfVmVsCloudlet=new TreeMap<Long,CloudletVsVm>();
	private List<Integer> listOfRamUtil=new ArrayList<Integer>();
	private List<Long> listOfBwUtil=new ArrayList<Long>();
	
	
	/**
	 * Created a new DatacenterBroker object.
	 * 
	 * @param name name to be associated with this entity (as required by Sim_entity class from
	 *            simjava package)
	 * @throws Exception the exception
	 * @pre name != null
	 * @post $none
	 */
	public DatacenterBroker(String name) throws Exception {
		super(name);

		setVmList(new ArrayList<Vm>());
		setVmsCreatedList(new ArrayList<Vm>());
		setCloudletList(new ArrayList<Cloudlet>());
		setCloudletSubmittedList(new ArrayList<Cloudlet>());
		setCloudletReceivedList(new ArrayList<Cloudlet>());

		cloudletsSubmitted = 0;
		setVmsRequested(0);
		setVmsAcks(0);
		setVmsDestroyed(0);

		setDatacenterIdsList(new LinkedList<Integer>());
		setDatacenterRequestedIdsList(new ArrayList<Integer>());
		setVmsToDatacentersMap(new HashMap<Integer, Integer>());
		setDatacenterCharacteristicsList(new HashMap<Integer, DatacenterCharacteristics>());
	}

	/**
	 * This method is used to send to the broker the list with virtual machines that must be
	 * created.
	 * 
	 * @param list the list
	 * @pre list !=null
	 * @post $none
	 */
	public void submitVmList(List<? extends Vm> list) {
		getVmList().addAll(list);
	}

	/**
	 * This method is used to send to the broker the list of cloudlets.
	 * 
	 * @param list the list
	 * @pre list !=null
	 * @post $none
	 */
	public void submitCloudletList(List<? extends Cloudlet> list) {
		getCloudletList().addAll(list);
	}

	/**
	 * Specifies that a given cloudlet must run in a specific virtual machine.
	 * 
	 * @param cloudletId ID of the cloudlet being bount to a vm
	 * @param vmId the vm id
	 * @pre cloudletId > 0
	 * @pre id > 0
	 * @post $none
	 */
	public void bindCloudletToVm(int cloudletId, int vmId) {
		CloudletList.getById(getCloudletList(), cloudletId).setVmId(vmId);
	}

	/**
	 * Processes events available for this Broker.
	 * 
	 * @param ev a SimEvent object
	 * @pre ev != null
	 * @post $none
	 */
	@Override
	public void processEvent(SimEvent ev) {
		switch (ev.getTag()) {
		// Resource characteristics request
			case CloudSimTags.RESOURCE_CHARACTERISTICS_REQUEST:
				processResourceCharacteristicsRequest(ev);
				break;
			// Resource characteristics answer
			case CloudSimTags.RESOURCE_CHARACTERISTICS:
				processResourceCharacteristics(ev);
				break;
			// VM Creation answer
			case CloudSimTags.VM_CREATE_ACK:
				processVmCreate(ev);
				break;
			// A finished cloudlet returned
			case CloudSimTags.CLOUDLET_RETURN:
				processCloudletReturn(ev);
				break;
			// if the simulation finishes
			case CloudSimTags.END_OF_SIMULATION:
				shutdownEntity();
				break;
			case CloudSimTags.CREATENEWVMS:
				createnewVMs();
				break;
			case CREATE_INTERMITTENT_VMS:
				processVmCreate(ev);
			// other unknown tags are processed by this method
			default:
				processOtherEvent(ev);
				break;
		}
	}

	/**
	 * Process the return of a request for the characteristics of a PowerDatacenter.
	 * 
	 * @param ev a SimEvent object
	 * @pre ev != $null
	 * @post $none
	 */
	protected void processResourceCharacteristics(SimEvent ev) {
		DatacenterCharacteristics characteristics = (DatacenterCharacteristics) ev.getData();
		getDatacenterCharacteristicsList().put(characteristics.getId(), characteristics);

		if (getDatacenterCharacteristicsList().size() == getDatacenterIdsList().size()) {
			setDatacenterRequestedIdsList(new ArrayList<Integer>());
			createVmsInDatacenter(getDatacenterIdsList().get(0));
		}
	}

	/**
	 * Process a request for the characteristics of a PowerDatacenter.
	 * 
	 * @param ev a SimEvent object
	 * @pre ev != $null
	 * @post $none
	 */
	protected void processResourceCharacteristicsRequest(SimEvent ev) {
		setDatacenterIdsList(CloudSim.getCloudResourceList());
		setDatacenterCharacteristicsList(new HashMap<Integer, DatacenterCharacteristics>());

		Log.printLine(CloudSim.clock() + ": " + getName() + ": Cloud Resource List received with "
				+ getDatacenterIdsList().size() + " resource(s)");

		for (Integer datacenterId : getDatacenterIdsList()) {
			sendNow(datacenterId, CloudSimTags.RESOURCE_CHARACTERISTICS, getId());
		}
	}

	/**
	 * Process the ack received due to a request for VM creation.
	 * 
	 * @param ev a SimEvent object
	 * @pre ev != null
	 * @post $none
	 */
	protected void processVmCreate(SimEvent ev) {
		int[] data = (int[]) ev.getData();
		int datacenterId = data[0];
		int vmId = data[1];
		int result = data[2];

		if (result == CloudSimTags.TRUE) {
			//Integer dataCenterId=getVmsToDatacentersMap().get(vmId);
			if(!getVmsToDatacentersMap().containsKey(vmId)){
			getVmsToDatacentersMap().put(vmId, datacenterId);
			getVmsCreatedList().add(VmList.getById(getVmList(), vmId));
			Log.printLine(CloudSim.clock() + ": " + getName() + ": VM #" + vmId
					+ " has been created in Datacenter #" + datacenterId + ", Host #"
					+ VmList.getById(getVmsCreatedList(), vmId).getHost().getId());
			}
			
		} else {
			//Log.printLine(CloudSim.clock() + ": " + getName() + ": Creation of VM #" + vmId
				//	+ " failed in Datacenter #" + datacenterId);
		}

		incrementVmsAcks();

		// all the requested VMs have been created
		if (getVmsCreatedList().size() == getVmList().size() - getVmsDestroyed()) {
			submitCloudlets();
		} else {
			// all the acks received, but some VMs were not created
			if (getVmsRequested() == getVmsAcks()) {
				// find id of the next datacenter that has not been tried
				for (int nextDatacenterId : getDatacenterIdsList()) {
					if (!getDatacenterRequestedIdsList().contains(nextDatacenterId)) {
						createVmsInDatacenter(nextDatacenterId);
						return;
					}
				}

				// all datacenters already queried
				if (getVmsCreatedList().size() > 0) { // if some vm were created
					submitCloudlets();
				} else { // no vms created. abort
					Log.printLine(CloudSim.clock() + ": " + getName()
							+ ": none of the required VMs could be created. Aborting");
					finishExecution();
				}
			}
		}
	}

	/**
	 * Process a cloudlet return event.
	 * 
	 * @param ev a SimEvent object
	 * @pre ev != $null
	 * @post $none
	 */
	protected void processCloudletReturn(SimEvent ev) {
		Cloudlet cloudlet = (Cloudlet) ev.getData();
		getCloudletReceivedList().add(cloudlet);
		Log.printLine(CloudSim.clock() + ": " + getName() + ": Cloudlet " + cloudlet.getCloudletId()
				+ " received");
		cloudletsSubmitted--;
		if (getCloudletList().size() == 0 && cloudletsSubmitted == 0) { // all cloudlets executed
			Log.printLine(CloudSim.clock() + ": " + getName() + ": All Cloudlets executed. Finishing...");
			clearDatacenters();
			finishExecution();
		} else { // some cloudlets haven't finished yet
			if (getCloudletList().size() > 0 && cloudletsSubmitted == 0) {
				// all the cloudlets sent finished. It means that some bount
				// cloudlet is waiting its VM be created
				clearDatacenters();
				createVmsInDatacenter(0);
			}

		}
	}

	/**
	 * Overrides this method when making a new and different type of Broker. This method is called
	 * by {@link #body()} for incoming unknown tags.
	 * 
	 * @param ev a SimEvent object
	 * @pre ev != null
	 * @post $none
	 */
	protected void processOtherEvent(SimEvent ev) {
		if (ev == null) {
			Log.printLine(getName() + ".processOtherEvent(): " + "Error - an event is null.");
			return;
		}

		Log.printLine(getName() + ".processOtherEvent(): "
				+ "Error - event unknown by this DatacenterBroker.");
	}

	/**
	 * Create the virtual machines in a datacenter.
	 * 
	 * @param datacenterId Id of the chosen PowerDatacenter
	 * @pre $none
	 * @post $none
	 */
	protected void createVmsInDatacenter(int datacenterId) {
		// send as much vms as possible for this datacenter before trying the next one
		int requestedVms = 0;
		String datacenterName = CloudSim.getEntityName(datacenterId);
		for (Vm vm : getVmList()) {
			if (!getVmsToDatacentersMap().containsKey(vm.getId())) {
				Log.printLine(CloudSim.clock() + ": " + getName() + ": Trying to Create VM #" + vm.getId()
						+ " in " + datacenterName);
				sendNow(datacenterId, CloudSimTags.VM_CREATE_ACK, vm);
				requestedVms++;
			}
		}

		getDatacenterRequestedIdsList().add(datacenterId);

		setVmsRequested(requestedVms);
		setVmsAcks(0);
	}

	/**
	 * Submit cloudlets to the created VMs.
	 * 
	 * @pre $none
	 * @post $none
	 */
	protected void submitCloudlets() {
		int vmIndex = 0;
		Random rand=new Random();
		int min=1;
		int max=1000;
		int n=(max - min) + 1;
		
		int delay =10;
		synchronized(this){
		for (Cloudlet cloudlet : getCloudletList()) {
			
			Vm vm;
			// if user didn't bind this cloudlet and it has not been executed yet
			if (cloudlet.getVmId() == -1) {
				vm = getVmsCreatedList().get(vmIndex);
			} else { // submit to the specific vm
				vm = VmList.getById(getVmsCreatedList(), cloudlet.getVmId());
				if (vm == null) { // vm was not created
					Log.printLine(CloudSim.clock() + ": " + getName() + ": Postponing execution of cloudlet "
							+ cloudlet.getCloudletId() + ": bount VM not available");
					continue;
				}
			}
			
//			schedule(vm.getId(), CloudSim.clock(), CloudSimTags.CREATENEWVMS);
			Log.printLine(CloudSim.clock() + ": " + getName() + ": Sending cloudlet "
					+ cloudlet.getCloudletId() + " to VM #" + vm.getId());
			cloudlet.setVmId(vm.getId());
			schedule(getVmsToDatacentersMap().get(vm.getId()),delay ,CloudSimTags.CLOUDLET_SUBMIT, cloudlet);
			//sendNow(getVmsToDatacentersMap().get(vm.getId()), CloudSimTags.CLOUDLET_SUBMIT, cloudlet);
			cloudletsSubmitted++;
			vmIndex = (vmIndex + 1) % getVmsCreatedList().size();
			getCloudletSubmittedList().add(cloudlet);
			//uncomment below code once makespan compution over
			 delay =rand.nextInt(n)+min;
			// delay=delay+delay1;
		}
		}

		// remove submitted cloudlets from waiting list
		for (Cloudlet cloudlet : getCloudletSubmittedList()) {
			getCloudletList().remove(cloudlet);
		}
		
	}

	/**
	 * Destroy the virtual machines running in datacenters.
	 * 
	 * @pre $none
	 * @post $none
	 */
	protected void clearDatacenters() {
		for (Vm vm : getVmsCreatedList()) {
			Log.printLine(CloudSim.clock() + ": " + getName() + ": Destroying VM #" + vm.getId());
			sendNow(getVmsToDatacentersMap().get(vm.getId()), CloudSimTags.VM_DESTROY, vm);
		}

		getVmsCreatedList().clear();
	}

	/**
	 * Send an internal event communicating the end of the simulation.
	 * 
	 * @pre $none
	 * @post $none
	 */
	protected void finishExecution() {
		sendNow(getId(), CloudSimTags.END_OF_SIMULATION);
	}

	/*
	 * (non-Javadoc)
	 * @see cloudsim.core.SimEntity#shutdownEntity()
	 */
	@Override
	public void shutdownEntity() {
		Log.printLine(getName() + " is shutting down...");
	}

	/*
	 * (non-Javadoc)
	 * @see cloudsim.core.SimEntity#startEntity()
	 */
	@Override
	public void startEntity() {
		Log.printLine(getName() + " is starting...");
		schedule(getId(), 0, CloudSimTags.RESOURCE_CHARACTERISTICS_REQUEST);
	}

	/**
	 * Gets the vm list.
	 * 
	 * @param <T> the generic type
	 * @return the vm list
	 */
	@SuppressWarnings("unchecked")
	public <T extends Vm> List<T> getVmList() {
		//System.out.println("size here::::::"+vmList.size());
		return (List<T>) vmList;
	}
	

	/**
	 * Sets the vm list.
	 * 
	 * @param <T> the generic type
	 * @param vmList the new vm list
	 */
	protected <T extends Vm> void setVmList(List<T> vmList) {
		this.vmList = vmList;
	}

	/**
	 * Gets the cloudlet list.
	 * 
	 * @param <T> the generic type
	 * @return the cloudlet list
	 */
	@SuppressWarnings("unchecked")
	synchronized public <T extends Cloudlet> List<T> getCloudletList() {
		return (List<T>) cloudletList;
	}

	/**
	 * Sets the cloudlet list.
	 * 
	 * @param <T> the generic type
	 * @param cloudletList the new cloudlet list
	 */
	protected <T extends Cloudlet> void setCloudletList(List<T> cloudletList) {
		this.cloudletList = cloudletList;
	}

	/**
	 * Gets the cloudlet submitted list.
	 * 
	 * @param <T> the generic type
	 * @return the cloudlet submitted list
	 */
	@SuppressWarnings("unchecked")
	public <T extends Cloudlet> List<T> getCloudletSubmittedList() {
		return (List<T>) cloudletSubmittedList;
	}

	/**
	 * Sets the cloudlet submitted list.
	 * 
	 * @param <T> the generic type
	 * @param cloudletSubmittedList the new cloudlet submitted list
	 */
	protected <T extends Cloudlet> void setCloudletSubmittedList(List<T> cloudletSubmittedList) {
		this.cloudletSubmittedList = cloudletSubmittedList;
	}

	/**
	 * Gets the cloudlet received list.
	 * 
	 * @param <T> the generic type
	 * @return the cloudlet received list
	 */
	@SuppressWarnings("unchecked")
	public <T extends Cloudlet> List<T> getCloudletReceivedList() {
		return (List<T>) cloudletReceivedList;
	}

	/**
	 * Sets the cloudlet received list.
	 * 
	 * @param <T> the generic type
	 * @param cloudletReceivedList the new cloudlet received list
	 */
	protected <T extends Cloudlet> void setCloudletReceivedList(List<T> cloudletReceivedList) {
		this.cloudletReceivedList = cloudletReceivedList;
	}

	/**
	 * Gets the vm list.
	 * 
	 * @param <T> the generic type
	 * @return the vm list
	 */
	@SuppressWarnings("unchecked")
	synchronized public <T extends Vm> List<T> getVmsCreatedList() {
		//if(vmsCreatedList.size()>1)
		//System.out.println("size here:::::: vmsCreatedList"+vmsCreatedList.size());
		return (List<T>) vmsCreatedList;
	}

	/**
	 * Sets the vm list.
	 * 
	 * @param <T> the generic type
	 * @param vmsCreatedList the vms created list
	 */
	protected <T extends Vm> void setVmsCreatedList(List<T> vmsCreatedList) {
		this.vmsCreatedList = vmsCreatedList;
	}

	/**
	 * Gets the vms requested.
	 * 
	 * @return the vms requested
	 */
	protected int getVmsRequested() {
		return vmsRequested;
	}

	/**
	 * Sets the vms requested.
	 * 
	 * @param vmsRequested the new vms requested
	 */
	protected void setVmsRequested(int vmsRequested) {
		this.vmsRequested = vmsRequested;
	}

	/**
	 * Gets the vms acks.
	 * 
	 * @return the vms acks
	 */
	protected int getVmsAcks() {
		return vmsAcks;
	}

	/**
	 * Sets the vms acks.
	 * 
	 * @param vmsAcks the new vms acks
	 */
	protected void setVmsAcks(int vmsAcks) {
		this.vmsAcks = vmsAcks;
	}

	/**
	 * Increment vms acks.
	 */
	protected void incrementVmsAcks() {
		vmsAcks++;
	}

	/**
	 * Gets the vms destroyed.
	 * 
	 * @return the vms destroyed
	 */
	protected int getVmsDestroyed() {
		return vmsDestroyed;
	}

	/**
	 * Sets the vms destroyed.
	 * 
	 * @param vmsDestroyed the new vms destroyed
	 */
	protected void setVmsDestroyed(int vmsDestroyed) {
		this.vmsDestroyed = vmsDestroyed;
	}

	/**
	 * Gets the datacenter ids list.
	 * 
	 * @return the datacenter ids list
	 */
	protected List<Integer> getDatacenterIdsList() {
		return datacenterIdsList;
	}

	/**
	 * Sets the datacenter ids list.
	 * 
	 * @param datacenterIdsList the new datacenter ids list
	 */
	protected void setDatacenterIdsList(List<Integer> datacenterIdsList) {
		this.datacenterIdsList = datacenterIdsList;
	}

	/**
	 * Gets the vms to datacenters map.
	 * 
	 * @return the vms to datacenters map
	 */
	protected Map<Integer, Integer> getVmsToDatacentersMap() {
		return vmsToDatacentersMap;
	}

	/**
	 * Sets the vms to datacenters map.
	 * 
	 * @param vmsToDatacentersMap the vms to datacenters map
	 */
	protected void setVmsToDatacentersMap(Map<Integer, Integer> vmsToDatacentersMap) {
		this.vmsToDatacentersMap = vmsToDatacentersMap;
	}

	/**
	 * Gets the datacenter characteristics list.
	 * 
	 * @return the datacenter characteristics list
	 */
	protected Map<Integer, DatacenterCharacteristics> getDatacenterCharacteristicsList() {
		return datacenterCharacteristicsList;
	}

	/**
	 * Sets the datacenter characteristics list.
	 * 
	 * @param datacenterCharacteristicsList the datacenter characteristics list
	 */
	protected void setDatacenterCharacteristicsList(
			Map<Integer, DatacenterCharacteristics> datacenterCharacteristicsList) {
		this.datacenterCharacteristicsList = datacenterCharacteristicsList;
	}

	/**
	 * Gets the datacenter requested ids list.
	 * 
	 * @return the datacenter requested ids list
	 */
	protected List<Integer> getDatacenterRequestedIdsList() {
		return datacenterRequestedIdsList;
	}

	/**
	 * Sets the datacenter requested ids list.
	 * 
	 * @param datacenterRequestedIdsList the new datacenter requested ids list
	 */
	protected void setDatacenterRequestedIdsList(List<Integer> datacenterRequestedIdsList) {
		this.datacenterRequestedIdsList = datacenterRequestedIdsList;
	}
	protected void createnewVMs() {

		List<Vm> vmList2;

		List<Vm> vmListnew;

		Vm vmm;

		vmList2 = getVmList();

		vmm = vmList2.get(10-1);



		int brokerid= vmm.getUserId();
		vmListnew = createVM(brokerid, 10,10);

		submitVmList(vmListnew);





		createVmsInDatacenter(3);





		//CREATENEWVMS =1;



		return;





		}
	private static List<Vm> createVM(int userId, int vms, int idShift) {
		//Creates a container to store VMs. This list is passed to the broker later
		LinkedList<Vm> list = new LinkedList<Vm>();

		//VM Parameters
		long size = 10000; //image size (MB)
		int ram = 124; //vm memory (MB)
		int mips = 100;
		long bw = 100;
		int pesNumber = 1; //number of cpus
		String vmm = "Xen"; //VMM name

		//create VMs
		Vm[] vm = new Vm[vms];

		for(int i=0;i<vms;i++){
			vm[i] = new Vm(idShift + i, userId, mips, pesNumber, ram, bw, size, vmm, new CloudletSchedulerTimeShared());
			list.add(vm[i]);
		}

		return list;
	}
	public static boolean isStillRunning=true;
	Timer time =null;
	public void monitor(){
		System.out.println("inside elastic data center monitor");
		
		 time = new Timer();
		ScheduledTask st = new ScheduledTask();
		time.schedule(st, 0, 100);
		
		
	}
	/**
	 * This monitor for reactive scaling
	 */
	public void monitorReactiveScaling(){
		Log.printLine("inside ReactiveScaling");
		time = new Timer();
		ReactiveScalingTask rsTask=new ReactiveScalingTask();
		//scheduler start at 0 seconds and every 100 milliseconds
		time.schedule(rsTask, 0, 100);
	}
	
	/**
	 * This method calculate the future response time using WMA method using 
	 * the past response times
	 * @return
	 */
	public double predictResponseTime(){
		/**
		 * this variable holds the maximum response time after iterating the list
		 */
		double finalResponseTime=0.0;
		Integer weigthSum=0;
		double weightedResponseSum=0.0;
		double predictedResponseTime=0.0;
		/**
		 * we are assigning list of cloudlet receivedlist at that moment of time to listOfRecCloudlet
		 */
		List<Cloudlet> listOfRecCloudlet=getCloudletReceivedList();
		Log.printLine("cloudlet receivedlist here"+listOfRecCloudlet.size());
		/**
		 * we are gonna interate the list and will find the maximum response time 
		 * The formulae for response time= finishing time-processing time-submission time(Arrival time)
		 */
		for(Cloudlet cl:listOfRecCloudlet){
			//double respTime=cl.getFinishTime()-cl.getActualCPUTime()-cl.getSubmissionTime();
			double respTime=cl.getFinishTime()-cl.getSubmissionTime();
			finalResponseTime=Math.max(finalResponseTime, respTime);
		}
		//below code should execute in case the response time more than 0
		if(finalResponseTime>0.0){
		Set<Integer> keyset=mapResponseTimeForPrediction.keySet();
		Integer currentCount=keyset.size()+1;
		for(Integer key:keyset){
			weigthSum=weigthSum+key;
			weightedResponseSum=weightedResponseSum+key*mapResponseTimeForPrediction.get(key);
		}
		weigthSum=weigthSum+currentCount;
		weightedResponseSum=weightedResponseSum+finalResponseTime*currentCount;
		mapResponseTimeForPrediction.put(currentCount, finalResponseTime);
		predictedResponseTime=weightedResponseSum/weigthSum;
		}
		//Log.printLine("predicted response time ***-"+predictedResponseTime);
		return predictedResponseTime;
	}
	/**
	 * below method calculates the utilization after certain interval
	 */
 public double getUtilization(){
	double totalVmUtilization=0.0;
	double sumOfCloudletLength=0.0;
	int cloudletLetNumber=0;
	//int ramUtilization=0;
	long bwUtil=0;
	CloudletVsVm vmVsCloudlet=null;
	List<Vm> vmList15=getVmsCreatedList();
	List<Vm> vmListFromDc=getDCVmList();
	if(vmListFromDc.size()>0)
		synchronized(this){
	  for(Vm vm:vmListFromDc){
		  cloudletLetNumber=cloudletLetNumber+vm.getCloudletScheduler().getCloudletExecList4Elastic().size();
		 // ramUtilization=ramUtilization+vm.getCurrentAllocatedRam();
		  bwUtil=bwUtil+ vm.getCurrentAllocatedBw();
	 sumOfCloudletLength=  vm.getCloudletScheduler().getElasticUtilization();
	 //Log.printLine("total sumOfCloudletLength is "+sumOfCloudletLength);
	 totalVmUtilization=totalVmUtilization+(sumOfCloudletLength/(vm.getNumberOfPes()*vm.getMips()));
	// Log.printLine("here is the vm mips"+vm.getNumberOfPes()*vm.getMips());
	// Log.printLine("totalVmUtilization for vm"+vm.getId()+"here"+totalVmUtilization);
		
	  }
		}

	//listOfRamUtil.add(ramUtilization);
	listOfBwUtil.add(bwUtil);
	  double finalUtilization=totalVmUtilization/vmListFromDc.size();
	  utilizationVsTimeMap.put(vmTime, new Double(finalUtilization));
	  vmTime=vmTime+100;
	   vmVsCloudlet=new CloudletVsVm();
	   vmVsCloudlet.setCloudletNumber(cloudletLetNumber);
	   vmVsCloudlet.setVmNumber(vmListFromDc.size());
	  mapOfVmVsCloudlet.put(vmTime,vmVsCloudlet);
	  
	 // Log.printLine("overall utilization of online VMs"+finalUtilization+"total vms in pool"+totalVmPool);
	  return finalUtilization;
 }
 
 /**
	 * This is the common method for prey Increasing/ decreasing which runs the lotka volterra ode and generated list of solutions
	 * @param vmNumber available vmNumber in the system
	 * @param cloudLetNumber cloudlets in the data center
	 * @param vmCoeff coeff of the vm in the ode
	 * @param cloudCoeff coeff of the cloudlet in the ode
	 * @return List of LotkaVolterra pojo generated by ode
	 */
	private List<LotkaVolterraPojo> callLotkaVolterra(double vmNumber,double cloudLetNumber,double vmCoeff,double cloudCoeff){
		
		
		final List<LotkaVolterraPojo> lkPojoList=new ArrayList<LotkaVolterraPojo>();
		FirstOrderIntegrator dp853 = new DormandPrince853Integrator(1.0e-8, 0.1, 1.0e-10, 1.0e-10);
		FirstOrderDifferentialEquations ode =new LotkaVolterra(vmCoeff, cloudCoeff);
		double[] y = new double[] { vmNumber, cloudLetNumber };
		StepHandler stepHandler = new StepHandler() {
			LotkaVolterraPojo lkPojo=null;
		    public void init (double t0, double[] y0, double t) {
		    }

		    public void handleStep (StepInterpolator interpolator, boolean isLast) {
		      double t = interpolator.getCurrentTime();
		      double[] y = interpolator.getInterpolatedState();
		      lkPojo=new LotkaVolterraPojo();
		      lkPojo.setVmNUmber(new Double(y[0]).intValue());
		      lkPojo.setCloudletNumber(new Double(y[1]).intValue());
		      lkPojoList.add(lkPojo);
		      //System.out.println("->" + t + " " + y[0] + " " + y[1]);
		    }
		  };
		  dp853.addStepHandler(stepHandler);
		dp853.integrate(ode, 0.0, y, 18.0, y);
		System.out.println(y[0]);
		System.out.println(y[1]);
		return lkPojoList;
		
	}
	/**
	 * This method returns the selected lk generated pojo, which contains the required vmnumber and cloudlet number
	 * @param vmNumber
	 * @param cloudLetNumber
	 * @return
	 */
	public LotkaVolterraPojo selectLkPojoForVmIncreasingCloudletDecreasing(double vmNumber,double cloudLetNumber){
		LotkaVolterraPojo lkPojo=new LotkaVolterraPojo();
		
		if(vmNumber==0.0)
			vmNumber=1.0;
		double vmCoeff=vmNumber+100;
		double cloudCoeff=cloudLetNumber-20;
		List<LotkaVolterraPojo> callLotkaVolterra=callLotkaVolterra(vmNumber,cloudLetNumber,vmCoeff,cloudCoeff);
		Log.printLine("total Vm number"+cloudLetNumber+"vmNumber"+vmNumber);
		for(LotkaVolterraPojo lotkaVolterraPojo:callLotkaVolterra){
			if( lotkaVolterraPojo.getVmNUmber()>vmList.size() &&(int) lotkaVolterraPojo.getVmNUmber()<new Double(vmNumber+totalVmPool).intValue()){
				lkPojo.setCloudletNumber(lotkaVolterraPojo.getCloudletNumber());
				lkPojo.setVmNUmber(lotkaVolterraPojo.getVmNUmber());
			}
		}
		
		if(lkPojo.getVmNUmber()==null)
			Log.printLine("Cloudnot find pojo try again and change the vm and cloudlet coeff");
		
		Log.printLine("lk pojo"+lkPojo);
		return lkPojo;
		
	}
	
	/**
	 * This method returns the selected lk generated pojo, which contains the required vmnumber and cloudlet number
	 * @param vmNumber
	 * @param cloudLetNumber
	 * @return
	 */
	public LotkaVolterraPojo selectLkPojoForVmDecreasingCloudletIncreasing(double vmNumber,double cloudLetNumber){
		LotkaVolterraPojo lkPojo=new LotkaVolterraPojo();
		double vmCoeff=0.0;
		if(vmNumber==0.0)
			vmNumber=1.0;
		if(vmNumber<20){
			vmCoeff=10;	
		}else{
			vmCoeff=vmNumber-20;
		}
			
		
		double cloudCoeff=cloudLetNumber+20;
		Log.printLine("vmCoeff"+vmCoeff+"cloudCoeff"+cloudCoeff);
		if(vmCoeff<=0)
		vmCoeff=5;
		List<LotkaVolterraPojo> callLotkaVolterra=callLotkaVolterra(vmNumber,cloudLetNumber,vmCoeff,cloudCoeff);
		Log.printLine("total Vm number"+cloudLetNumber+"vmNumber"+vmNumber);
		for(LotkaVolterraPojo lotkaVolterraPojo:callLotkaVolterra){
			if( lotkaVolterraPojo.getVmNUmber()<vmList.size() /*&& lotkaVolterraPojo.getVmNUmber()<getVmsCreatedList().size()*/){
				lkPojo.setCloudletNumber(lotkaVolterraPojo.getCloudletNumber());
				lkPojo.setVmNUmber(lotkaVolterraPojo.getVmNUmber());
			}
		}
		
		if(lkPojo.getVmNUmber()==null)
			Log.printLine("Cloudnot find pojo try again and change the vm and cloudlet coeff");
		
		Log.printLine("lk pojo"+lkPojo);
		return lkPojo;
		
	}
	public class ScheduledTask extends TimerTask {
		
			double totalUtilization=0; 
			double maxUtilization=0.8;
			double minUtilization=0.2;
			double minUtilizationMinThreshold=0.0;
			
			double futureResponseTime=0.0;

			// Add your task here
			public void run() {
				CloudSim.pauseSimulation();
				LotkaVolterraPojo lkpojo=null;
				List<Vm> newList=null;
				cloudNumber=cloudNumber+20;
				//restrciting cloudlet creation
				if(cloudNumber<800){
				List<Cloudlet> listOfCloudlets=createCloudlet(getId(),20,cloudNumber);
				submitCloudletList(listOfCloudlets);
				}
				totalUtilization=getUtilization();
				/**
				 * uncomment the below code for proactive  scaling and comment  above code
				 */
				//futureResponseTime=predictResponseTime();
				
				if(totalUtilization>maxUtilization && totalVmPool>0){
				/**
				 * uncomment below if in case of proactive scaling comment above if
				 */
					/*if(futureResponseTime>responseTimeUpperThreshold && totalVmPool>0){*/
					List<Vm> vmList2 = getVmList();

					Vm vmm = vmList2.get(vmList2.size()-1);



					int brokerid= vmm.getUserId();
					//need to add more VMS from resource pool
					//call LK algorithm
					 lkpojo=selectLkPojoForVmIncreasingCloudletDecreasing(vmList.size(),cloudletSubmittedList.size());
					 if(lkpojo.getVmNUmber()>totalVmPool){
						 newList= createVM(brokerid,totalVmPool,vmStartingNumber);
						 vmStartingNumber=vmStartingNumber+totalVmPool;
						 totalVmPool=0;
					 }else{
						 newList= createVM(brokerid,lkpojo.getVmNUmber(),vmStartingNumber); 
						 totalVmPool=totalVmPool-new Double(lkpojo.getVmNUmber()).intValue();
						 vmStartingNumber=vmStartingNumber+lkpojo.getVmNUmber();
					 }
					
					 Log.printLine("vm number here after addition"+vmStartingNumber+"totalVmPool ****"+totalVmPool);
					 submitVmList(newList);
					 createVmsInDatacenter(getDatacenterIdsList().get(0));
				}
				if(totalUtilization<minUtilization && totalUtilization>minUtilizationMinThreshold&& vmList.size()>10 && cloudletSubmittedList.size()>5){
					/*if(futureResponseTime<responseTimeLowerThreshold && futureResponseTime>50.0&&  getVmsCreatedList().size()>2 && cloudletSubmittedList.size()>5){*/
					//kill VMS iterate over createdlist and find unoccupied vms pick and distroy them
				// increase the total vm count in pool
					Log.printLine("vm number under minimization"+vmList.size()+"cloudlet number"+cloudletSubmittedList.size());
					 lkpojo=selectLkPojoForVmDecreasingCloudletIncreasing(vmList.size(),cloudletSubmittedList.size());
					 
						 //number of vm needs to distroy
					 int vmDeductNum=vmList.size()-lkpojo.getVmNUmber();
					 Log.printLine("number of vmdeduction require"+vmDeductNum);
					 int deductCount=0;
					for(Vm vm:vmList){
						if(vm.getCloudletScheduler().getCloudletExecList4Elastic().size()==0 && deductCount<vmDeductNum){
							//destroy the vm
							sendNow(getVmsToDatacentersMap().get(vm.getId()), CloudSimTags.VM_DESTROY, vm);
							deductCount=deductCount+1;
							if(totalVmPool<100)
							totalVmPool=totalVmPool+1;
						}
							
						
					}
				}
					
					vmVsTimeMap.put(vmTime1, getDCVmList().size());
					vmTime1=vmTime1+100;
				if(!isStillRunning)
					time.cancel();
				CloudSim.resumeSimulation();
			}
		}
	
	public class ReactiveScalingTask extends TimerTask {
		
		double totalUtilization=0; 
		double maxUtilization=0.8;
		double minUtilization=0.2;
		double minUtilizationMinThreshold=0.1;
		double futureResponseTime=0.0;

		// Add your task here
		public void run() {
			CloudSim.pauseSimulation();
			
		
			//int cloudNumber=vmStartingNumber+400;
			cloudNumber=cloudNumber+20;
			if(cloudNumber<800){
			List<Cloudlet> listOfCloudlets=createCloudlet(getId(),20,cloudNumber);
			submitCloudletList(listOfCloudlets);
			}
			List<Vm> newList=null;
			totalUtilization=getUtilization();
			/**
			 * uncomment below line and comment above line for proactive scaling
			 */
			futureResponseTime=predictResponseTime();
//			if(totalUtilization>maxUtilization && totalVmPool>0){
			/**
			 * uncomment below line comment above line in case of proactive scaling
			 */
				if(futureResponseTime>responseTimeUpperThreshold && totalVmPool>0){
				List<Vm> vmList2 = getVmList();

				Vm vmm = vmList2.get(vmList2.size()-1);



				int brokerid= vmm.getUserId();
				 
					 newList= createVM(brokerid,1,vmStartingNumber); 
					 totalVmPool=totalVmPool-1;
					 vmStartingNumber=vmStartingNumber+1;
					 Log.printLine("vm number here after addition"+vmStartingNumber+"totalVmPool ****"+totalVmPool);
				 
				
				 
				 submitVmList(newList);
				 createVmsInDatacenter(getDatacenterIdsList().get(0));
				 /*for(Vm vm:newList){
					 sendNow(0, CREATE_INTERMITTENT_VMS, vm); 
					
				 }*/
				// schedule(getId(), 0, CREATE_INTERMITTENT_VMS);
				// sendNow(getId(),  CREATE_INTERMITTENT_VMS);
				 
			}
			/*if(totalUtilization<minUtilization && totalUtilization>minUtilizationMinThreshold&& getVmsCreatedList().size()>2 && cloudletSubmittedList.size()>5){*/
				/**
				 * in case of proactive scaling uncomment below if and comment above if
				 */
				if(futureResponseTime<responseTimeLowerThreshold && futureResponseTime>50.0&& getVmsCreatedList().size()>2 && cloudletSubmittedList.size()>5){
			//kill VMS iterate over createdlist and find unoccupied vms pick and distroy them
			// increase the total vm count in pool
				Log.printLine("vm number under minimization"+vmList.size()+"cloudlet number"+cloudletSubmittedList.size());
				 
				 
					 //number of vm needs to distroy
				 int vmDeductNum=1;
				 Log.printLine("number of vmdeduction require"+vmDeductNum);
				 int deductCount=0;
				for(Vm vm:getVmsCreatedList()){
					if(vm.getCloudletScheduler().getCloudletExecList4Elastic().size()==0 && deductCount<vmDeductNum){
						//destroy the vm
						sendNow(getVmsToDatacentersMap().get(vm.getId()), CloudSimTags.VM_DESTROY, vm);
						deductCount=deductCount+1;
						if(totalVmPool<100)
						totalVmPool=totalVmPool+1;
					}
						
					
				}
			}
			Log.printLine("insde run method created list size"+getVmsCreatedList().size());
			System.out.println("inside run method size vmListSize:::"+vmList.size());
			vmVsTimeMap.put(vmTime1, getDCVmList().size());
			vmTime1=vmTime1+100;
			if(!isStillRunning)
				time.cancel();
			CloudSim.resumeSimulation();
		}
	}
	
	
	private static List<Cloudlet> createCloudlet(int userId, int cloudlets, int idShift){
		// Creates a container to store Cloudlets
		LinkedList<Cloudlet> list = new LinkedList<Cloudlet>();

		//cloudlet parameters
		long length = 12000;
		long fileSize = 300;
		long outputSize = 300;
		int pesNumber = 1;
		UtilizationModel utilizationModel = new UtilizationModelFull();

		Cloudlet[] cloudlet = new Cloudlet[cloudlets];

		for(int i=0;i<cloudlets;i++){
			cloudlet[i] = new Cloudlet(idShift + i, length, pesNumber, fileSize, outputSize, utilizationModel, utilizationModel, utilizationModel);
			// setting the owner of these Cloudlets
			cloudlet[i].setUserId(userId);
			list.add(cloudlet[i]);
		}

		return list;
	}
	/**
	 * Below method returns the vmsVsTime map
	 * @return
	 */
 public Map<Long,Integer> getVmVsTimeMap(){
	 return vmVsTimeMap;
 }
 /**
  * this method retuns vm utilization against time
  * @return
  */
 public Map<Long,Double> getutilizationVsTimeMap(){
	 return utilizationVsTimeMap;
 }
 
 
 public Map<Long,CloudletVsVm> getVmVsCloudlet(){
	 return mapOfVmVsCloudlet;
 }
 
 public void submitDataCenterObj(Datacenter dc){
	 datacenter=dc;
 }
 
 private List<Vm> getDCVmList(){
	 return datacenter.getVmList();
 }
 public List<Integer> getRamUtilList(){
	 return listOfRamUtil;
 }
 public List<Long> getBwUtilList(){
	 return listOfBwUtil;
 }
}
