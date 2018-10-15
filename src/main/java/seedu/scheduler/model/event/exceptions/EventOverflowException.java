package seedu.scheduler.model.event.exceptions;

/**
 * Signals that the operation will result in the scheduler having an overflow of events.
 */
public class EventOverflowException extends RuntimeException {
    public EventOverflowException() {
        super("Operation would result in scheduler having too many events");
    }
}
