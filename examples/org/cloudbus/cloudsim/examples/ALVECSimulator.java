/*
 * Title:        CloudSim Toolkit
 * Description:  CloudSim (Cloud Simulation) Toolkit for Modeling and Simulation
 *               of Clouds
 * Licence:      GPL - http://www.gnu.org/copyleft/gpl.html
 *
 * Copyright (c) 2009, The University of Melbourne, Australia
 */

package org.cloudbus.cloudsim.examples;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.CloudletSchedulerTimeShared;
import org.cloudbus.cloudsim.CloudletVsVm;
import org.cloudbus.cloudsim.Datacenter;
import org.cloudbus.cloudsim.DatacenterBroker;
import org.cloudbus.cloudsim.DatacenterCharacteristics;
import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Pe;
import org.cloudbus.cloudsim.Storage;
import org.cloudbus.cloudsim.UtilizationModel;
import org.cloudbus.cloudsim.UtilizationModelFull;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.VmAllocationPolicySimple;
import org.cloudbus.cloudsim.VmSchedulerTimeShared;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.CloudSimTags;
import org.cloudbus.cloudsim.core.SimEntity;
import org.cloudbus.cloudsim.core.SimEvent;
import org.cloudbus.cloudsim.provisioners.BwProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.PeProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;

/**
 * This class is responsible for simulating the LV model.. both the scenarios
 * reactive and proactive are incorporated in the same code
 * 
 * @author Jyotirmoy
 * 
 */
public class ALVECSimulator {

	/** The cloudlet list. */
	private static List<Cloudlet> cloudletList;

	/** The vmList. */
	private static List<Vm> vmList;
	private static Datacenter datacenter1 = null;

	private static List<Vm> createVM(int userId, int vms, int idShift) {
		// Creates a container to store VMs. This list is passed to the broker
		// later
		LinkedList<Vm> list = new LinkedList<Vm>();

		// VM Parameters
		long size = 10000; // image size (MB)
		int ram = 124; // vm memory (MB)
		int mips = 100;
		long bw = 100;
		int pesNumber = 1; // number of cpus
		String vmm = "Xen"; // VMM name

		// create VMs
		Vm[] vm = new Vm[vms];

		for (int i = 0; i < vms; i++) {
			vm[i] = new Vm(idShift + i, userId, mips, pesNumber, ram, bw, size,
					vmm, new CloudletSchedulerTimeShared());
			list.add(vm[i]);
		}

		return list;
	}

	private static List<Cloudlet> createCloudlet(int userId, int cloudlets,
			int idShift) {
		// Creates a container to store Cloudlets
		LinkedList<Cloudlet> list = new LinkedList<Cloudlet>();

		// cloudlet parameters
		long length = 12000;
		long fileSize = 300;
		long outputSize = 300;
		int pesNumber = 1;
		UtilizationModel utilizationModel = new UtilizationModelFull();

		Cloudlet[] cloudlet = new Cloudlet[cloudlets];

		for (int i = 0; i < cloudlets; i++) {
			cloudlet[i] = new Cloudlet(idShift + i, length, pesNumber,
					fileSize, outputSize, utilizationModel, utilizationModel,
					utilizationModel);
			// setting the owner of these Cloudlets
			cloudlet[i].setUserId(userId);
			list.add(cloudlet[i]);
		}

		return list;
	}

	// //////////////////////// STATIC METHODS ///////////////////////

	/**
	 * Creates main() to run this simulator
	 */
	public static void main(String[] args) {
		Log.printLine("Starting ALVECSimulator...");

		try {
			// First step: Initialize the CloudSim package. It should be called
			// before creating any entities.
			int num_user = 2; // number of grid users
			Calendar calendar = Calendar.getInstance();
			boolean trace_flag = false; // mean trace events

			// Initialize the CloudSim library
			CloudSim.init(num_user, calendar, trace_flag);

			GlobalBroker globalBroker = new GlobalBroker("GlobalBroker");

			// Second step: Create Datacenters
			// Datacenters are the resource providers in CloudSim. We need at
			// list one of them to run a CloudSim simulation
			@SuppressWarnings("unused")
			Datacenter datacenter0 = createDatacenter("Datacenter_0");

			// @SuppressWarnings("unused")
			datacenter1 = createDatacenter("Datacenter_1");

			// Third step: Create Broker
			MyDataCenterBroker broker = createBroker("Broker_0");
			int brokerId = broker.getId();
			// ElasticDataCenter elasDC=new ElasticDataCenter("Elastic_Broker");

			// Fourth step: Create VMs and Cloudlets and send them to broker
			vmList = createVM(brokerId, 14, 0); // creating 14 vms
			cloudletList = createCloudlet(brokerId, 98, 0); // creating 98
															// cloudlets

			broker.submitVmList(vmList);
			broker.submitCloudletList(cloudletList);
			broker.submitDataCenterObj(datacenter0);
			/* below broker.monitor needs to uncomment when to run LK algorithm 
			 * 
			 * below line initiates the LK model algorithm*/
			broker.monitor();
			// Below broker.monitorReactiveScaling run reactive scaling
			// algorithm and the same can run proactive scaling but need to
			// uncomment few if code
			// comment another set of if code
			// broker.monitorReactiveScaling();
			// Fifth step: Starts the simulation
			CloudSim.startSimulation();
			// Runnable monitor = new Runnable() {
			// @Override
			// public void run() {
			// CloudSim.pauseSimulation(200);
			// while (true) {
			// if (CloudSim.isPaused()) {
			// break;
			// }
			// try {
			// Thread.sleep(100);
			// } catch (InterruptedException e) {
			// e.printStackTrace();
			// }
			// }
			//
			// Log.printLine("\n\n\n" + CloudSim.clock() +
			// ": The simulation is paused for 5 sec \n\n");
			//
			// try {
			// Thread.sleep(5000);
			// } catch (InterruptedException e) {
			// e.printStackTrace();
			// }
			//
			// MyDataCenterBroker broker = createBroker("Broker_1");
			// int brokerId = broker.getId();
			//
			// //Create VMs and Cloudlets and send them to broker
			// // List<Vm> vmlist1 = createVM(brokerId, 5, 40); //creating 5 vms
			// cloudletList = createCloudlet(brokerId, 10, 40); // creating 10
			// cloudlets
			//
			// //broker.submitVmList(vmlist1);
			// broker.submitCloudletList(cloudletList);
			//
			// CloudSim.resumeSimulation();
			// }
			// };
			//
			// new Thread(monitor).start();
			// try {
			// Thread.sleep(1000);
			// } catch (InterruptedException e) {
			// // TODO Auto-generated catch block
			// e.printStackTrace();
			// }

			// Final step: Print results when simulation is over

			List<Cloudlet> newList = broker.getCloudletReceivedList();
			newList.addAll(globalBroker.getBroker().getCloudletReceivedList());

			CloudSim.stopSimulation();
			MyDataCenterBroker.isStillRunning = false;
			Map<Long, Integer> mapVmVsTime = broker.getVmVsTimeMap();
			Map<Long, Double> mapUtilVsTime = broker.getutilizationVsTimeMap();
			Map<Long, CloudletVsVm> mapVmVsCloudlet = broker.getVmVsCloudlet();
			/* List<Integer> listOfRamUtil = broker.getRamUtilList(); */
			List<Long> listOfBwUtil = broker.getBwUtilList();
			printCloudletList(newList, mapVmVsTime, mapUtilVsTime,
					mapVmVsCloudlet, listOfBwUtil);

			Log.printLine("ALVECSimulator finished!");
		} catch (Exception e) {
			e.printStackTrace();
			Log.printLine("The simulation has been terminated due to an unexpected error");
		}
	}

	/**
	 * This method creates datacenter in cloudsim
	 * 
	 * @param name
	 * @return
	 */
	private static Datacenter createDatacenter(String name) {

		// Here are the steps needed to create a PowerDatacenter:
		// 1. We need to create a list to store one or more
		// Machines
		List<Host> hostList = new ArrayList<Host>();

		// 2. A Machine contains one or more PEs or CPUs/Cores. Therefore,
		// should
		// create a list to store these PEs before creating
		// a Machine.
		List<Pe> peList1 = new ArrayList<Pe>();

		int mips = 4000;

		// 3. Create PEs and add these into the list.
		// for a quad-core machine, a list of 4 PEs is required:
		peList1.add(new Pe(0, new PeProvisionerSimple(mips))); // need to store
																// Pe id and
																// MIPS Rating
		peList1.add(new Pe(1, new PeProvisionerSimple(mips)));
		peList1.add(new Pe(2, new PeProvisionerSimple(mips)));
		peList1.add(new Pe(3, new PeProvisionerSimple(mips)));

		// Another list, for a dual-core machine
		List<Pe> peList2 = new ArrayList<Pe>();

		peList2.add(new Pe(0, new PeProvisionerSimple(mips)));
		peList2.add(new Pe(1, new PeProvisionerSimple(mips)));

		// 4. Create Hosts with its id and list of PEs and add them to the list
		// of machines
		int hostId = 0;
		int ram = 16384; // host memory (MB)
		long storage = 1000000; // host storage
		int bw = 10000;//kbps

		hostList.add(new Host(hostId, new RamProvisionerSimple(ram),
				new BwProvisionerSimple(bw), storage, peList1,
				new VmSchedulerTimeShared(peList1))); // This is  first host
														// machine quad core

		hostId++;

		hostList.add(new Host(hostId, new RamProvisionerSimple(ram),
				new BwProvisionerSimple(bw), storage, peList2,
				new VmSchedulerTimeShared(peList2))); // Second host machine dual core

		// 5. Create a DatacenterCharacteristics object that stores the
		// properties of a data center: architecture, OS, list of
		// Machines, allocation policy: time- or space-shared, time zone
		// and its price (G$/Pe time unit).
		String arch = "x86"; // system architecture
		String os = "Linux"; // operating system
		String vmm = "Xen";
		double time_zone = 10.0; // time zone this resource located
		double cost = 3.0; // the cost of using processing in this resource
		double costPerMem = 0.05; // the cost of using memory in this resource
		double costPerStorage = 0.1; // the cost of using storage in this
										// resource
		double costPerBw = 0.1; // the cost of using bw in this resource
		LinkedList<Storage> storageList = new LinkedList<Storage>(); // we are
																		// not
																		// adding
																		// SAN
																		// devices
																		// by
																		// now

		DatacenterCharacteristics characteristics = new DatacenterCharacteristics(
				arch, os, vmm, hostList, time_zone, cost, costPerMem,
				costPerStorage, costPerBw);

		// 6. Finally, we need to create a PowerDatacenter object.
		Datacenter datacenter = null;
		try {
			datacenter = new Datacenter(name, characteristics,
					new VmAllocationPolicySimple(hostList), storageList, 0);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return datacenter;
	}

	/**
	 * Create datacenter broker
	 * @param name
	 * @return
	 */
	private static MyDataCenterBroker createBroker(String name) {

		MyDataCenterBroker broker = null;
		try {
			broker = new MyDataCenterBroker(name);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		return broker;
	}

	/**
	 * Prints the Cloudlet objects
	 * 
	 * @param list
	 *            list of Cloudlets
	 */
	private static void printCloudletList(List<Cloudlet> list,
			Map<Long, Integer> vmVsTime, Map<Long, Double> utilVsTime,
			Map<Long, CloudletVsVm> mapVmVsCloudlet, List<Long> listOfBwUtil) {
		int size = list.size();
		Cloudlet cloudlet;
		List<Double> listOf1stRange = new ArrayList<Double>();
		List<Double> listOf2ndRange = new ArrayList<Double>();
		List<Double> listOf3rdRange = new ArrayList<Double>();
		List<Double> listSubmitTime1stRange = new ArrayList<Double>();
		List<Double> listCompletionTime1stRange = new ArrayList<Double>();
		List<Double> listSubmitTime2ndRange = new ArrayList<Double>();
		List<Double> listCompletionTime2ndRange = new ArrayList<Double>();
		List<Double> listSubmitTime3ndRange = new ArrayList<Double>();
		List<Double> listCompletionTime3ndRange = new ArrayList<Double>();
		List<Double> listOf4thRange = new ArrayList<Double>();
		List<Double> listSubmitTime4thRange = new ArrayList<Double>();
		List<Double> listCompletionTime4thRange = new ArrayList<Double>();
		double stDeadline = 1500.0;
		int stSLACount = 0;
		double secndDeadline = 2000.0;
		int sndSLACount = 0;
		double thrdDeadline = 2000.0;
		int thrdSLACount = 0;
		List<Cloudlet> uniqueCloudlet = new ArrayList<Cloudlet>();
		String indent = "    ";
		Log.printLine();
		Log.printLine("========== OUTPUT ==========");
		Log.printLine("Cloudlet ID" + indent + "STATUS" + indent
				+ "Data center ID" + indent + "VM ID" + indent + indent
				+ "Time" + indent + "Start Time" + indent + "Finish Time");

		DecimalFormat dft = new DecimalFormat("###.##");
		for (int i = 0; i < size; i++) {
			cloudlet = list.get(i);
			Log.print(indent + cloudlet.getCloudletId() + indent + indent);

			if (cloudlet.getCloudletStatus() == Cloudlet.SUCCESS) {
				Log.print("SUCCESS");

				if (cloudlet.getCloudletId() < 100
						&& !eleAlreadyExistInList(cloudlet.getCloudletId(),
								uniqueCloudlet)) {
					if (cloudlet.getActualCPUTime() > stDeadline)
						stSLACount = stSLACount + 1;
					listOf1stRange.add(cloudlet.getActualCPUTime());
					listSubmitTime1stRange.add(cloudlet.getSubmissionTime());
					listCompletionTime1stRange.add(cloudlet.getFinishTime());
					uniqueCloudlet.add(cloudlet);
				} else if (cloudlet.getCloudletId() >= 100
						&& cloudlet.getCloudletId() < 500
						&& !eleAlreadyExistInList(cloudlet.getCloudletId(),
								uniqueCloudlet)) {
					if (cloudlet.getActualCPUTime() > secndDeadline)
						sndSLACount = sndSLACount + 1;
					listOf2ndRange.add(cloudlet.getActualCPUTime());
					listSubmitTime2ndRange.add(cloudlet.getSubmissionTime());
					listCompletionTime2ndRange.add(cloudlet.getFinishTime());
					uniqueCloudlet.add(cloudlet);
				} else if (cloudlet.getCloudletId() >= 500
						&& !eleAlreadyExistInList(cloudlet.getCloudletId(),
								uniqueCloudlet)) {
					if (cloudlet.getActualCPUTime() > thrdDeadline)
						thrdSLACount = thrdSLACount + 1;
					listSubmitTime3ndRange.add(cloudlet.getSubmissionTime());
					listCompletionTime3ndRange.add(cloudlet.getFinishTime());

					listOf3rdRange.add(cloudlet.getActualCPUTime());
					uniqueCloudlet.add(cloudlet);
				}
				Log.printLine(indent + indent + cloudlet.getResourceId()
						+ indent + indent + indent + cloudlet.getVmId()
						+ indent + indent + indent
						+ dft.format(cloudlet.getActualCPUTime()) + indent
						+ indent + dft.format(cloudlet.getExecStartTime())
						+ indent + indent + indent
						+ dft.format(cloudlet.getFinishTime()));
			}
		}
		/**
		 * below code will calculate the avg execution time across the batches
		 * sla violation and makespan time
		 */
		double sumOfexecutionTimeforAllBatches = 0.0;
		List<Double> listSubmitTimefinalRange = new ArrayList<Double>();
		List<Double> listCompletionTimefinalRange = new ArrayList<Double>();
		double finalDeadline = 2000;
		int finalSlaCount = 0;
		for (Cloudlet cl : uniqueCloudlet) {
			if (cl.getActualCPUTime() > finalDeadline)
				finalSlaCount = finalSlaCount + 1;
			sumOfexecutionTimeforAllBatches = sumOfexecutionTimeforAllBatches
					+ cl.getActualCPUTime();
			listSubmitTimefinalRange.add(cl.getSubmissionTime());
			listCompletionTimefinalRange.add(cl.getFinishTime());
		}
		double avgFinal = sumOfexecutionTimeforAllBatches
				/ uniqueCloudlet.size();
		Log.printLine("all batch cloudlets avg execution time" + avgFinal
				+ "size of list" + uniqueCloudlet.size());
		Log.printLine(" all sla violation rate" + new Double(finalSlaCount)
				/ new Double(uniqueCloudlet.size()));
		Log.printLine("all  batches makespan time"
				+ (Collections.max(listCompletionTimefinalRange) - Collections
						.min(listSubmitTimefinalRange)));
		double sumSt = 0;
		for (double st : listOf1stRange) {
			sumSt = sumSt + st;
		}
		double avg1st = sumSt / listOf1stRange.size();
		Log.printLine("1st batch cloudlets avg execution time" + avg1st
				+ "size of list" + listOf1stRange.size());
		Log.printLine("1st batch sla violation rate" + new Double(stSLACount)
				/ new Double(listOf1stRange.size()));
		double sumNd = 0;
		for (double st : listOf2ndRange) {
			sumNd = sumNd + st;
		}
		double avg2nd = sumNd / listOf2ndRange.size();
		Log.printLine("2nd batch cloudlets avg execution time" + avg2nd
				+ "size of list" + listOf2ndRange.size());
		Log.printLine("2nd batch sla violation rate" + new Double(sndSLACount)
				/ new Double(listOf2ndRange.size()));

		double sum3rd = 0;
		for (double st : listOf3rdRange) {
			sum3rd = sum3rd + st;
		}
		double avg3rd = sum3rd / listOf3rdRange.size();
		Log.printLine("3rd batch cloudlets avg execution time" + avg3rd
				+ "size of list" + listOf3rdRange.size());
		Log.printLine("3rd batch sla violation rate" + new Double(thrdSLACount)
				/ new Double(listOf3rdRange.size()));
		Log.printLine("Final unique size of list" + uniqueCloudlet.size());
		Log.printLine("First batch makespan time"
				+ (Collections.max(listCompletionTime1stRange) - Collections
						.min(listSubmitTime1stRange)));
		Log.printLine("Second batch makespan time"
				+ (Collections.max(listCompletionTime2ndRange) - Collections
						.min(listSubmitTime2ndRange)));
		Log.printLine("Third batch makespan time"
				+ (Collections.max(listCompletionTime3ndRange) - Collections
						.min(listSubmitTime3ndRange)));
		/**
		 * Below code prints the data for time vs Vm number
		 */

		for (Long key : vmVsTime.keySet()) {
			Log.printLine("At time in milliseconds----" + key
					+ "the number of vms created ++++++" + vmVsTime.get(key));
		}

		for (Long key : utilVsTime.keySet()) {
			Log.printLine("At time in milliseconds----" + key
					+ "vms utilization here is ++++++" + utilVsTime.get(key));
		}

		for (Long key : mapVmVsCloudlet.keySet()) {
			Log.printLine("at time here----" + key
					+ "cloudlet number is ++++++"
					+ mapVmVsCloudlet.get(key).getCloudletNumber()
					+ "vm number is " + mapVmVsCloudlet.get(key).getVmNumber());
		}
		/*
		 * Long ramUtilAvg = 0L; for (Integer ramUtil : listOfRamUtil) {
		 * ramUtilAvg = ramUtilAvg + ramUtil; }
		 */
		/*
		 * Log.printLine("Avg Ram utilization:::"+ramUtilAvg/listOfRamUtil.size()
		 * ); ramUtilAvg= ramUtilAvg/listOfRamUtil.size();
		 */
		/* Double ramUtilAvgPer=Double.valueOf((ramUtilAvg/(32768.0))*100); */
		// Log.printLine("Avg Ram utilization percentage:::"+ramUtilAvgPer);

		Long bwUtilAvg = 0L;
		for (Long bwUtil : listOfBwUtil) {
			bwUtilAvg = bwUtilAvg + bwUtil;
		}
		Log.printLine("Avg BW utilization:::" + bwUtilAvg / listOfBwUtil.size());
		bwUtilAvg = bwUtilAvg / listOfBwUtil.size();
		Double bwUtilAvgPer = Double.valueOf((bwUtilAvg / (20000.0)) * 100);
		Log.printLine("Avg bw utilization percentage:::" + bwUtilAvgPer);
	}

	public static class MyDataCenterBroker extends DatacenterBroker {

		public MyDataCenterBroker(String name) throws Exception {
			super(name);
			// TODO Auto-generated constructor stub
		}

		@Override
		public void submitCloudletList(List<? extends Cloudlet> list) {
			// getCloudletList().addAll(list);
			// pause(1000);
			super.submitCloudletList(list);
			// super.submitCloudlets();
			// super.setVmsCreatedList(vmList);
			sendNow(datacenter1.getId(), CloudSimTags.VM_DATACENTER_EVENT);
		}

	}

	public static class GlobalBroker extends SimEntity {

		private static final int CREATE_BROKER = 0;
		private static final int CREATE_NEWBROKER = 10456;
		private List<Vm> vmList;
		private List<Cloudlet> cloudletList;
		private MyDataCenterBroker broker;

		public GlobalBroker(String name) {
			super(name);
		}

		@Override
		public void processEvent(SimEvent ev) {
			switch (ev.getTag()) {
			case CREATE_BROKER:
				setBroker(createBroker(super.getName() + "_"));
				// schedule(getId(), 200, CloudSimTags.CREATENEWVMS);
				// Create VMs and Cloudlets and send them to broker

				setVmList(createVM(getBroker().getId(), 11, 100)); // creating 5
																	// vms
				setCloudletList(createCloudlet(getBroker().getId(), 72, 100)); // creating
																				// 10
																				// cloudlets

				broker.submitVmList(getVmList());
				broker.submitCloudletList(getCloudletList());

				CloudSim.resumeSimulation();

				break;

			case CREATE_NEWBROKER:
				// setBroker(createBroker(super.getName()+"_"));
				setVmList(createVM(broker.getId(), 8, 200));
				setCloudletList(createCloudlet(getBroker().getId(), 60, 500));
				broker.submitVmList(getVmList());

				broker.submitCloudletList(getCloudletList());
				CloudSim.resumeSimulation();
				System.out.println("herer i am");

				break;

			default:
				Log.printLine(getName() + ": unknown event type");
				break;
			}
		}

		@Override
		public void startEntity() {
			Log.printLine(super.getName() + " is starting...");
			schedule(getId(), 0, CREATE_BROKER);
			schedule(getId(), 0, CREATE_NEWBROKER);

			// new java.util.Timer().schedule(
			// new java.util.TimerTask() {
			// @Override
			// public void run() {
			// startEntity();
			// }
			// },
			// 100000
			// );
		}

		@Override
		public void shutdownEntity() {
		}

		public List<Vm> getVmList() {
			return vmList;
		}

		protected void setVmList(List<Vm> vmList) {
			this.vmList = vmList;
		}

		public List<Cloudlet> getCloudletList() {
			return cloudletList;
		}

		protected void setCloudletList(List<Cloudlet> cloudletList) {
			this.cloudletList = cloudletList;
		}

		public MyDataCenterBroker getBroker() {
			return broker;
		}

		protected void setBroker(MyDataCenterBroker broker) {
			this.broker = broker;
		}

	}

	public static boolean eleAlreadyExistInList(int cloudletId,
			List<Cloudlet> listOfCloudlet) {
		boolean isExist = false;
		for (Cloudlet cl : listOfCloudlet) {
			if (cloudletId == cl.getCloudletId()) {
				isExist = true;
				break;
			}

		}
		return isExist;
	}

}
