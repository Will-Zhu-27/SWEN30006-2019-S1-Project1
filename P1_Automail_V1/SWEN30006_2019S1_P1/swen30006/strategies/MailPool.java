package strategies;

import java.util.LinkedList;
import java.util.List;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.ListIterator;

import automail.Clock;
import automail.MailItem;
import automail.PriorityMailItem;
import automail.Robot;
import exceptions.HeavierItemAllocationException;
import exceptions.ItemTooHeavyException;

public class MailPool implements IMailPool {
	/**
	 * item is waiting for enough delivery robots to delivery
	 */
	private Item unfinishedItem = null;
	
	private class Item {
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
		
		public void robotAdd(Robot robot) throws HeavierItemAllocationException {
			if (acquiredRobots.contains(robot)) {
				throw new HeavierItemAllocationException();
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
						throw new HeavierItemAllocationException();
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
	
	public class ItemComparator implements Comparator<Item> {
		@Override
		public int compare(Item i1, Item i2) {
			int order = 0;
			if (i1.priority < i2.priority) {
				order = 1;
			} else if (i1.priority > i2.priority) {
				order = -1;
			} else if (i1.destination < i2.destination) {
				order = 1;
			} else if (i1.destination > i2.destination) {
				order = -1;
			}
			return order;
		}
	}
	
	private LinkedList<Item> pool;
	private LinkedList<Robot> robots;

	public MailPool(int nrobots){
		// Start empty
		pool = new LinkedList<Item>();
		robots = new LinkedList<Robot>();
	}

	public void addToPool(MailItem mailItem) {
		Item item;
		try {
			item = new Item(mailItem);
			pool.add(item);
		} catch (ItemTooHeavyException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		pool.sort(new ItemComparator());
	}
	
	@Override
	public void step() throws ItemTooHeavyException, HeavierItemAllocationException {
		ListIterator<Robot> availableRobotList = robots.listIterator();
		while (availableRobotList.hasNext())
			try {
				loadRobot(availableRobotList);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		 
	}
	
	private void loadRobot(ListIterator<Robot> availableRobotList) throws HeavierItemAllocationException, Exception {
		// meet the heavier mail item request
		if (unfinishedItem != null) {
			responseHeavierItemRequest(availableRobotList);
		} 
		// start a new item allocation
		else {
			newMailItemAllocation(availableRobotList);
		}
	}
	
	/**
	 * Robot responses heavier item request 
	 * @param availableRobotList
	 * @throws ItemTooHeavyException
	 * @throws HeavierItemAllocationException
	 */
	private void responseHeavierItemRequest(ListIterator<Robot> availableRobotList) throws ItemTooHeavyException, HeavierItemAllocationException {
		Robot robot = availableRobotList.next();
		assert (robot.isEmpty());
		if (unfinishedItem == null) {
			throw new HeavierItemAllocationException();
		}
		robot.addToHand(unfinishedItem.getMailItem());
		unfinishedItem.robotAdd(robot);
		availableRobotList.remove();
		if (unfinishedItem.getCurrentNumAcquiredRobots() == unfinishedItem.getNumOfNeededRobots()) {
			unfinishedItem.acquiredRobotsDispatch();
			unfinishedItem = null;
		}
	}
	
	/**
	 * 
	 * @param availableRobotList
	 * @throws Exception
	 * @throws HeavierItemAllocationException 
	 */
	private void newMailItemAllocation(ListIterator<Robot> availableRobotList) throws Exception, HeavierItemAllocationException {
		ListIterator<Item> j = pool.listIterator();
		Robot robot = availableRobotList.next();
		assert (robot.isEmpty());
		if (pool.size() > 0) {
			try {
				Item nextItem = j.next();
				// hand first as we want higher priority delivered first
				nextItem.robotAdd(robot);
				robot.addToHand(nextItem.mailItem); 
				j.remove();

				MailItem tubeItem = null;
				// only add tube item when hand a light item
				if (nextItem.getHeavierMark() == false && (tubeItem = getLightMailItem()) != null) {
					robot.addToTube(tubeItem);
				}

				// begin to dispatch if the item is not a heavier item
				if (nextItem.getCurrentNumAcquiredRobots() == nextItem.getNumOfNeededRobots()) {
					nextItem.acquiredRobotsDispatch();
				} else {
					unfinishedItem = nextItem;
				}
				availableRobotList.remove(); // remove from mailPool queue
			} catch (Exception e) {
				throw e;
			}
		}
	}
	
	/**
	 * Get a mail which can be sent by a delivery robot. It is used when a robot
	 * needs to add a mail item to its tube. 
	 * 
	 * @return a mail item which can be sent by a delivery robot or null if pool
	 * is empty or no light mail.
	 * 
	 * @author yuqiangz
	 */
	private MailItem getLightMailItem() {
		if (pool.size() == 0) {
			return null;
		}
		ListIterator<Item> poolItr = pool.listIterator();
		Item lightItem = poolItr.next();
		while(lightItem.getHeavierMark() == true) {
			if(poolItr.hasNext()) {
				lightItem = poolItr.next();
			} else {
				lightItem = null;
				break;
			}
		}
		if (lightItem == null) {
			return null;
		}	
		poolItr.remove();
		return lightItem.getMailItem();
	}
	
	@Override
	public void registerWaiting(Robot robot) { // assumes won't be there already
		robots.add(robot);
	}

}
