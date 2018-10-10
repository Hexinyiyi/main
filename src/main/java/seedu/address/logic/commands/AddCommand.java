package seedu.address.logic.commands;

import static java.util.Objects.requireNonNull;
import static seedu.address.logic.parser.CliSyntax.PREFIX_DESCRIPTION;
import static seedu.address.logic.parser.CliSyntax.PREFIX_END_DATE_TIME;
import static seedu.address.logic.parser.CliSyntax.PREFIX_EVENT_NAME;
import static seedu.address.logic.parser.CliSyntax.PREFIX_START_DATE_TIME;
import static seedu.address.logic.parser.CliSyntax.PREFIX_TAG;

import seedu.address.logic.CommandHistory;
import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.model.Model;
import seedu.address.model.event.Event;

/**
 * Adds an event to the scheduler.
 */
public class AddCommand extends Command {

    public static final String COMMAND_WORD = "add";

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

        model.addEvents(Event.generateAllRepeatedEvents(toAdd));
        model.commitScheduler();
        return new CommandResult(String.format(MESSAGE_SUCCESS, toAdd));
    }

    @Override
    public boolean equals(Object other) {
        return other == this // short circuit if same object
                || (other instanceof AddCommand // instanceof handles nulls
                && toAdd.equals(((AddCommand) other).toAdd));
    }
}
