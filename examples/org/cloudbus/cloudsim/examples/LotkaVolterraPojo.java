package org.cloudbus.cloudsim.examples;

public class LotkaVolterraPojo {
	private Integer vmNUmber;
	private Integer cloudletNumber;

	public Integer getVmNUmber() {
		return vmNUmber;
	}

	public void setVmNUmber(Integer vmNUmber) {
		this.vmNUmber = vmNUmber;
	}

	public Integer getCloudletNumber() {
		return cloudletNumber;
	}

	public void setCloudletNumber(Integer cloudletNumber) {
		this.cloudletNumber = cloudletNumber;
	}

	@Override
	public String toString() {
		return "LotkaVolterraPojo [vmNUmber=" + vmNUmber + ", cloudletNumber="
				+ cloudletNumber + "]";
	}

}
