Initiate the Lotka-Volterra simulator with ALVECSimulator class.
This class starts the LV simulator and it incorporates both reactive and proactive scaling.
The Same class is responsible for invocation of reactive and proactive scaling.
Few lines of code are needed to comment/uncomment to run either of the algorithms.
Some cases the java.util.ConcurrentModificationException may occur, in that case re-run the simulator again as we have not done anything to fine tune the code or to address the issue. The exception appears when multiple threads try to execute the same line of code. It doesn't happen always. Read the comments carefully before executing a piece of code.
Many places we have modified the existing code to support our simulation such as DatacenterBroker.java
