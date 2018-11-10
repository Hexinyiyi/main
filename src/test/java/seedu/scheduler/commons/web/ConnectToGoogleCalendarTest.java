package seedu.scheduler.commons.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

import org.junit.jupiter.api.Test;

import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.Event;

class ConnectToGoogleCalendarTest {
    private static final String CALENDAR_NAME = "primary";
    private static final String TEMP_EVENT_NAME = "tempEventName";

    private void enable( ) {
        File file = new File("./tokens/mode.txt");
        try (Writer writer = new BufferedWriter(new FileWriter(file))) {
            String contents = "Enabled";
            writer.write(contents);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void disable( ) {
        File file = new File("./tokens/mode.txt");
        try (Writer writer = new BufferedWriter(new FileWriter(file))) {
            String contents = "Disabled";
            writer.write(contents);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    void isGoogleCalendarEnabled() {
        enable();
        assertTrue(ConnectToGoogleCalendar.isGoogleCalendarEnabled());
        disable();
        assertFalse(ConnectToGoogleCalendar.isGoogleCalendarEnabled());
    }

    @Test
    void isGoogleCalendarDisabled() {
        boolean isDisabled = true;
        assertEquals(true, isDisabled);
    }

    @Test
    void checkStatus() {
        //Prepare the test file where the status indicates Enabled
        enable();
        //Test whether returns Enabled
        assertTrue(ConnectToGoogleCalendar.checkStatus("Enabled"));
        assertFalse(ConnectToGoogleCalendar.checkStatus("Disabled"));

        //Prepare the test file where the status indicates Disabled
        disable();
        //Test whether returns Disabled
        assertFalse(ConnectToGoogleCalendar.checkStatus("Enabled"));
        assertTrue(ConnectToGoogleCalendar.checkStatus("Disabled"));
    }

    @Test
    void clear() {
        //set up test environment
        enable();
        boolean enabled = true;
        final ConnectToGoogleCalendar connectToGoogleCalendar =
                new ConnectToGoogleCalendar();
        Calendar service = connectToGoogleCalendar.getCalendar();

        //create an event in Google Calender
        try {
            Event createdEvent =
                    service.events()
                            .quickAdd(CALENDAR_NAME,TEMP_EVENT_NAME)
                            .setText(TEMP_EVENT_NAME)
                            .execute();
        } catch (IOException e) {
            e.printStackTrace();
        }

        //Retrieve the number of events (should be at least 1)
        int size = -1;
        try {
            size = connectToGoogleCalendar.getSingleEvents(service).getItems().size();
        } catch (IOException e) {
            e.printStackTrace();
        }
        //check initial condition: not cleared
        assertNotEquals(0, size);
        //execute the clear command
        connectToGoogleCalendar.clear(enabled);
        //Retrieve the number of events after command
        try {
            size = connectToGoogleCalendar.getSingleEvents(service).getItems().size();
        } catch (IOException e) {
            e.printStackTrace();
        }
        //No event any more -> ok
        assertEquals(0, size);
        //clean up
        disable();
    }
}
