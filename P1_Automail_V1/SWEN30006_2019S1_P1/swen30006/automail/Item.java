package automail;

import java.util.ArrayList;
import java.util.List;

import exceptions.ItemAllocationException;
import exceptions.ItemTooHeavyException;

public class Item {
		private int priority;
		private int destination;
		private MailItem mailItem;
		// represent the num of robots this mailItem needs
		private int numOfNeededRobots;
		private List<Robot> acquiredRobots;
		private boolean heavierMark;
		// Use stable sort to keep arrival time relative positions
		
		public Item(MailItem mailItem) throws ItemTooHeavyException {
			priority = (mailItem instanceof PriorityMailItem) ? ((PriorityMailItem) mailItem).getPriorityLevel() : 1;
			destination = mailItem.getDestFloor();
			this.mailItem = mailItem;
			if (mailItem.getWeight() <= Robot.INDIVIDUAL_MAX_WEIGHT) {
				heavierMark = false;
				numOfNeededRobots = 1;
			} else if (mailItem.getWeight() <= Robot.TRIPLE_MAX_WEIGHT) {
				heavierMark = true;
				numOfNeededRobots = mailItem.getWeight() > Robot.PAIR_MAX_WEIGHT ? 3:2;
			} else {
				throw new ItemTooHeavyException();
			}
			acquiredRobots = new ArrayList<Robot>();
		}
		
		public int getPriority() {
			return priority;
		}
		
		public int getDestination() {
			return destination;
		}
		
		public int getNumOfNeededRobots() {
			return numOfNeededRobots;
		}
		
		public boolean getHeavierMark() {
			return heavierMark;
		}
		
		public int getCurrentNumAcquiredRobots() {
			return acquiredRobots.size();
		}
		
		public MailItem getMailItem() {
			return mailItem;
		}
		
		public void robotAdd(Robot robot) throws ItemAllocationException  {
			if (acquiredRobots.contains(robot)) {
				throw new ItemAllocationException();
			} else {
				acquiredRobots.add(robot);
				if (heavierMark == true) {
					System.out.printf("T: %3d > %7s joins the team to delivery [%s]%n", Clock.Time(), robot.getIdTube(),
							mailItem.toString());
					int numOfStillNeeding = numOfNeededRobots - acquiredRobots.size();
					if (numOfStillNeeding > 0) {
						System.out.printf(
								"T: %3d > Heavier mail item(ID:%s) still needs %d extra robots to delivery.%n",
								Clock.Time(), mailItem.getId(), numOfNeededRobots - acquiredRobots.size());
					}
					if (numOfStillNeeding < 0) {
						acquiredRobotsDispatch();
						throw new ItemAllocationException();
					}
				}
			}
		}
		
		public void acquiredRobotsDispatch() {
			if (heavierMark == true) {
				System.out.printf("T: %3d > Heavier mail item(ID:%s) gets enough robots, robots as a team begin to dispatch.%n",
						Clock.Time(), mailItem.getId());
			}			
			for(Robot robot:acquiredRobots) {
				robot.dispatch();
			}
		}
	}