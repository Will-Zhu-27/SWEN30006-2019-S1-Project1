package strategies;

import java.util.LinkedList;
import java.util.Comparator;
import java.util.ListIterator;

import automail.Item;
import automail.MailItem;
import automail.Robot;
import exceptions.ItemAllocationException;
import exceptions.ItemTooHeavyException;

public class MailPool implements IMailPool {
	/**
	 * item is waiting for enough delivery robots to delivery
	 */
	private Item unfinishedItem = null;
	
	
	
	public class ItemComparator implements Comparator<Item> {
		@Override
		public int compare(Item i1, Item i2) {
			int order = 0;
			if (i1.getPriority() < i2.getPriority()) {
				order = 1;
			} else if (i1.getPriority() > i2.getPriority()) {
				order = -1;
			} else if (i1.getDestination() < i2.getDestination()) {
				order = 1;
			} else if (i1.getDestination() > i2.getDestination()) {
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
	public void step() {
		ListIterator<Robot> availableRobotList = robots.listIterator();
		while (availableRobotList.hasNext())
			try {
				loadRobot(availableRobotList);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ItemAllocationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		 
	}
	
	private void loadRobot(ListIterator<Robot> availableRobotList) throws Exception, ItemAllocationException {
		// meet the heavier mail item request
		if (unfinishedItem != null) {
			continueUnfinishedItem(availableRobotList);
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
	 * @throws ItemAllocationException 
	 */
	private void continueUnfinishedItem (ListIterator<Robot> availableRobotList) throws ItemAllocationException, ItemTooHeavyException {
		Robot robot = availableRobotList.next();
		assert (robot.isEmpty());
		if (unfinishedItem == null) {
			throw new ItemAllocationException();
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
	 * @throws ItemAllocationException 
	 */
	private void newMailItemAllocation(ListIterator<Robot> availableRobotList) throws Exception, ItemAllocationException {
		ListIterator<Item> j = pool.listIterator();
		Robot robot = availableRobotList.next();
		assert (robot.isEmpty());
		if (pool.size() > 0) {
			try {
				Item nextItem = j.next();
				// hand first as we want higher priority delivered first
				nextItem.robotAdd(robot);
				robot.addToHand(nextItem.getMailItem()); 
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
