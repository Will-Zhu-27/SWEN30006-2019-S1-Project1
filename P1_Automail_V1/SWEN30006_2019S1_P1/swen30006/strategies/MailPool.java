package strategies;

import java.util.LinkedList;
import java.util.List;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.ListIterator;

import automail.MailItem;
import automail.PriorityMailItem;
import automail.Robot;
import exceptions.ItemTooHeavyException;

public class MailPool implements IMailPool {
	/**
	 * a heavier mail item waits for enough delivery robots to 
	 * delivery
	 */
	private HeavierMailItem heavierItem = null;
	
	/**
	 * Represent a heavier mail item
	 * 
	 * @author yuqiangz
	 */
	private class HeavierMailItem {
		private MailItem mailItem;
		private int numOfNeededRobots;
		private List<Robot> teamRobots;
		public HeavierMailItem(MailItem mailItem) {
			this.mailItem = mailItem;
			if (mailItem.getWeight() <= Robot.PAIR_MAX_WEIGHT) {
				numOfNeededRobots = 2;
			} else {
				numOfNeededRobots = 3;
			}
			teamRobots = new ArrayList<Robot>();
		}
		
		public int getNumOfNeededRobots() {
			return numOfNeededRobots;
		}
		
		public int getCurrentNumTeamRobots() {
			return teamRobots.size();
		}
		
		public MailItem getMailItem() {
			return mailItem;
		}
		
		public boolean teamRobotsAdd(Robot robot) {
			if (teamRobots.contains(robot)) {
				return false;
			} else {
				teamRobots.add(robot);
				return true;
			}
		}
		
		public void teamRobotsDispatch() {
			for(Robot robot:teamRobots) {
				robot.dispatch();
			}
		}
	}
	
	private class Item {
		int priority;
		int destination;
		MailItem mailItem;
		// Use stable sort to keep arrival time relative positions
		
		public Item(MailItem mailItem) {
			priority = (mailItem instanceof PriorityMailItem) ? ((PriorityMailItem) mailItem).getPriorityLevel() : 1;
			destination = mailItem.getDestFloor();
			this.mailItem = mailItem;
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
		Item item = new Item(mailItem);
		pool.add(item);
		pool.sort(new ItemComparator());
	}
	
	@Override
	public void step() throws ItemTooHeavyException {
		try{
			ListIterator<Robot> i = robots.listIterator();
			while (i.hasNext()) loadRobot(i);
		} catch (Exception e) { 
            throw e; 
        } 
	}
	
	private void loadRobot(ListIterator<Robot> i) throws ItemTooHeavyException {
		Robot robot = i.next();
		assert(robot.isEmpty());
		// System.out.printf("P: %3d%n", pool.size());
		if (heavierItem != null) {
			heavierItem.teamRobotsAdd(robot);
			robot.addToHand(heavierItem.getMailItem());
			if (pool.size() > 0) {
				robot.addToTube(getNormalMailItem());
			}
			i.remove();
			if (heavierItem.getCurrentNumTeamRobots() == heavierItem.getNumOfNeededRobots()) {
				heavierItem.teamRobotsDispatch();
				heavierItem = null;
			}
		} else {
			ListIterator<Item> j = pool.listIterator();
			if (pool.size() > 0) {
				try {
					MailItem nextItem = j.next().mailItem;
					if (nextItem.getWeight() > Robot.INDIVIDUAL_MAX_WEIGHT) {
						heavierItem = new HeavierMailItem(nextItem);
						heavierItem.teamRobotsAdd(robot);
					}
					robot.addToHand(nextItem); // hand first as we want higher priority delivered first
					j.remove();
					if (pool.size() > 0) {
						robot.addToTube(getNormalMailItem());
					}
					if (heavierItem == null) {
						robot.dispatch(); // send the robot off if it has any items to deliver
					}
					i.remove(); // remove from mailPool queue
				} catch (Exception e) {
					throw e;
				}
			}
		}
	}
	
	/**
	 * get a mail which can be sent by a delivery robot and resort the mail
	 * pool. It is used when a robot needs to add a mail item to its tube. 
	 * Before it is used, make sure pool.size() > 0.
	 * 
	 * @return a mail item which can be sent by a delivery robot
	 * 
	 * @author yuqiangz
	 */
	private MailItem getNormalMailItem() {
		ListIterator<Item> poolItr = pool.listIterator();
		MailItem normalItem = poolItr.next().mailItem;
		while(normalItem.getWeight() > Robot.INDIVIDUAL_MAX_WEIGHT) {
			normalItem = poolItr.next().mailItem;
		}
		poolItr.remove();
		pool.sort(new ItemComparator());
		return normalItem;
	}
	
	@Override
	public void registerWaiting(Robot robot) { // assumes won't be there already
		robots.add(robot);
	}

}
