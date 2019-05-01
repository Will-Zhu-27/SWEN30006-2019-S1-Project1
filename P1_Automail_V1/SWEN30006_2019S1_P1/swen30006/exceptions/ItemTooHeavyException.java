package exceptions;

/**
 * This exception is thrown when a mail item is too weight for robots to bear.
 */
public class ItemTooHeavyException extends Exception {
    public ItemTooHeavyException(){
        super("Item too heavy! Robots cannot bear.");
    }
}
