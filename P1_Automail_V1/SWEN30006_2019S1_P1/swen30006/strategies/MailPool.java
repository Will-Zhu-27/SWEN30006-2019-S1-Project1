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
			System.out.printf("T: %3d > Heavier mail item(ID:%s) request %d robots to delivery.%n",
				Clock.Time(), mailItem.getId(), numOfNeededRobots);
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
				System.out.printf("T: %3d > %7s joins the team to delivery [%s]%n",
					Clock.Time(), robot.getIdTube(), mailItem.toString());
				if (numOfNeededRobots - teamRobots.size() != 0) {
					System.out.printf("T: %3d > Heavier mail item(ID:%s) still needs %d extra robots to delivery.%n",
						Clock.Time(), mailItem.getId(),
						numOfNeededRobots - teamRobots.size());
				}		
				return true;
			}
		}
		
		public void teamRobotsDispatch() {
			System.out.printf("T: %3d > Heavier mail item(ID:%s) gets enough robots, robots as a team begin to dispatch.%n",
						Clock.Time(), mailItem.getId());
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
		// meet the heavier mail item request
		if (heavierItem != null) {
			heavierItem.teamRobotsAdd(robot);
			robot.addToHand(heavierItem.getMailItem());
			MailItem tubeItem = null;
			if ((tubeItem = getLightMailItem()) != null) {
				robot.addToTube(tubeItem);
			}
			i.remove();
			if (heavierItem.getCurrentNumTeamRobots() == heavierItem.getNumOfNeededRobots()) {
				heavierItem.teamRobotsDispatch();
				heavierItem = null;
			}
		} 
		// start a new mail item delivery
		else {
			ListIterator<Item> j = pool.listIterator();
			if (pool.size() > 0) {
				try {
					MailItem nextItem = j.next().mailItem;
					// a heavier mail item
					if (nextItem.getWeight() > Robot.INDIVIDUAL_MAX_WEIGHT) {
						heavierItem = new HeavierMailItem(nextItem);
						heavierItem.teamRobotsAdd(robot);
					}
					robot.addToHand(nextItem); // hand first as we want higher priority delivered first
					j.remove();
					
					MailItem tubeItem = null;
					if ((tubeItem = getLightMailItem()) != null) {
						robot.addToTube(tubeItem);
					}
					
					// begin to dispatch if the item is not a heavier item
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
		MailItem lightItem = poolItr.next().mailItem;
		while(lightItem.getWeight() > Robot.INDIVIDUAL_MAX_WEIGHT) {
			if(poolItr.hasNext()) {
				lightItem = poolItr.next().mailItem;
			} else {
				lightItem = null;
				break;
			}
		}
		if (lightItem == null) {
			return null;
		}	
		poolItr.remove();
		return lightItem;
	}
	
	@Override
	public void registerWaiting(Robot robot) { // assumes won't be there already
		robots.add(robot);
	}

}
