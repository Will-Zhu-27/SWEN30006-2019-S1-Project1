package exceptions;

/**
 * An exception thrown when a mail that is already delivered attempts to be delivered again.
 */
public class HeavierItemAllocationException extends Throwable {
    public HeavierItemAllocationException(){
        super("Error happens at allocation of a heavier item");
    }
}