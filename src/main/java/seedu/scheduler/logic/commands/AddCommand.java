package seedu.scheduler.logic.commands;

import static java.util.Objects.requireNonNull;
import static seedu.scheduler.logic.parser.CliSyntax.PREFIX_DESCRIPTION;
import static seedu.scheduler.logic.parser.CliSyntax.PREFIX_END_DATE_TIME;
import static seedu.scheduler.logic.parser.CliSyntax.PREFIX_EVENT_NAME;
import static seedu.scheduler.logic.parser.CliSyntax.PREFIX_START_DATE_TIME;
import static seedu.scheduler.logic.parser.CliSyntax.PREFIX_TAG;

import java.util.logging.Logger;

import seedu.scheduler.commons.core.LogsCenter;
import seedu.scheduler.commons.web.ConnectToGoogleCalendar;
import seedu.scheduler.logic.CommandHistory;
import seedu.scheduler.logic.RepeatEventGenerator;
import seedu.scheduler.logic.commands.exceptions.CommandException;
import seedu.scheduler.model.Model;
import seedu.scheduler.model.event.Event;
import seedu.scheduler.ui.UiManager;

/**
 * Adds an event to the scheduler.
 */
public class AddCommand extends Command {

    public static final String COMMAND_WORD = "add";
    public static final String COMMAND_ALIAS_ONE = "ad";
    public static final String COMMAND_ALIAS_TWO = "a";

    public static final String MESSAGE_USAGE = COMMAND_WORD + ": Adds an event to the scheduler. "
            + "Parameters: "
            + PREFIX_EVENT_NAME + "NAME "
            + "[" + PREFIX_START_DATE_TIME + "DATETIME in natural language] "
            + "[" + PREFIX_END_DATE_TIME + "DATETIME in natural language] "
            + "[" + PREFIX_DESCRIPTION + "DESCRIPTION]\n"
            + "[" + PREFIX_TAG + "TAG]...\n"
            + "Example: " + COMMAND_WORD + " "
            + PREFIX_EVENT_NAME + "Study with me "
            + PREFIX_START_DATE_TIME + "today 5pm "
            + PREFIX_END_DATE_TIME + "tomorrow 3am "
            + PREFIX_DESCRIPTION + "Studying time "
            + PREFIX_TAG + "study "
            + PREFIX_TAG + "ad-hoc";

    public static final String MESSAGE_SUCCESS = "New event added: %1$s";
    public static final String MESSAGE_DUPLICATE_EVENT = "This event already exists in the scheduler";
    private static final Logger logger = LogsCenter.getLogger(UiManager.class);

    private final ConnectToGoogleCalendar connectToGoogleCalendar =
            new ConnectToGoogleCalendar();

    private final Event toAdd;

    /**
     * Creates an AddCommand to add the specified {@code Event}
     */
    public AddCommand(Event event) {
        requireNonNull(event);
        toAdd = event;
    }

    @Override
    public CommandResult execute(Model model, CommandHistory history) throws CommandException {
        requireNonNull(model);

        if (model.hasEvent(toAdd)) {
            throw new CommandException(MESSAGE_DUPLICATE_EVENT);
        }

        model.addEvents(RepeatEventGenerator.getInstance().generateAllRepeatedEvents(toAdd));
        model.commitScheduler();

        logger.info("Starting to push events Google Calendar");
        pushToGoogleCal();

        return new CommandResult(String.format(MESSAGE_SUCCESS, toAdd));
    }

    /**
     * Pushes the even to Google Calendar.
     */
    private void pushToGoogleCal() {
     //blank
    }

    /**
     * Converts a local Event's starting data and time to Google format.
     *
     * @param event a local Event.
     *
     * @return a String in Google format.
     */
    private String convertStartDateTimeToGoogleFormat(Event event) {
        //local format:2018-10-20 17:00:00
        //target :2018-10-21T22:30:00+08:00
        return event.getStartDateTime()
                .getPrettyString()
                .substring(0, 19)
                .replaceFirst(" ", "T")
                + "+08:00";
    }

    /**
     * Converts a local Event's ending data and time to Google format.
     *
     * @param event a local Event.
     *
     * @return a String in Google format.
     */
    private String convertEndDateTimeToGoogleFormat(Event event) {
        return event.getEndDateTime()
                .getPrettyString()
                .substring(0, 19)
                .replaceFirst(" ", "T")
                + "+08:00";
    }

    @Override
    public boolean equals(Object other) {
        return other == this // short circuit if same object
                || (other instanceof AddCommand // instanceof handles nulls
                && toAdd.getEventName().equals(((AddCommand) other).toAdd.getEventName())
                && toAdd.getStartDateTime().equals(((AddCommand) other).toAdd.getStartDateTime())
                && toAdd.getEndDateTime().equals(((AddCommand) other).toAdd.getEndDateTime())
                && toAdd.getDescription().equals(((AddCommand) other).toAdd.getDescription())
                && toAdd.getVenue().equals(((AddCommand) other).toAdd.getVenue())
                && toAdd.getRepeatType().equals(((AddCommand) other).toAdd.getRepeatType())
                && toAdd.getRepeatUntilDateTime().equals(((AddCommand) other).toAdd.getRepeatUntilDateTime())
                && toAdd.getTags().equals(((AddCommand) other).toAdd.getTags()));
    }
}
