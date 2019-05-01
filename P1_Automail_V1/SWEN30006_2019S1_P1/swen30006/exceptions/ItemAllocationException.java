package exceptions;

/**
 * An exception thrown when a mail that is already delivered attempts to be delivered again.
 */
public class ItemAllocationException extends Throwable {
    public ItemAllocationException(){
        super("Error happens at allocation of an item");
    }
}