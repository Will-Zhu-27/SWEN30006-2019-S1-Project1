# swen30006-project1
The strategy:

In MailPool.java:

When a mail item arrives to the mail pool, the mail pool create an object of Item (contain this mail item, mark whether it is a heavier mail item, how many robots it needs).

 1. If there is an unfinished item(need multi robots), the robot joins the team.
 
      1.1 if the unfinished item get enough robots, these robots are dispatched as a team to delivery the item. this unfinished item is finished.
      
 2. else get a new item from the mail pool (according to priority)
 
      2.1 if the item is marked heavier, the robot joins the team, this item is an unfinished item.
      
      2.2 else this item is light item, a robot hands it.
      
        2.2.1 get a light item from the mail pool (according to priority, if it is heavier item, skip it) and add to tube.
      
        2.2.2 dispatch the robot.
  
