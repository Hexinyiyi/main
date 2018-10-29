package seedu.scheduler.commons.web;

import static com.google.api.client.util.DateTime.parseRfc3339;


import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.security.GeneralSecurityException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.CalendarScopes;
import com.google.api.services.calendar.model.EventDateTime;
import com.google.api.services.calendar.model.EventReminder;
import com.google.api.services.calendar.model.Event.Reminders;
import com.google.api.services.calendar.model.Events;

import seedu.scheduler.commons.core.LogsCenter;
import seedu.scheduler.commons.util.EventFormatUtil;
import seedu.scheduler.logic.commands.CommandResult;
import seedu.scheduler.logic.commands.GetGoogleCalendarEventsCommand;
import seedu.scheduler.model.event.Event;
import seedu.scheduler.model.event.ReminderDurationList;
import seedu.scheduler.model.event.RepeatType;
import seedu.scheduler.ui.UiManager;

/**
 * Methods related to the connection with Google Calendar.
 */
public class ConnectToGoogleCalendar {
    /**
     * Global instance of the scopes required.
     */
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
    private static final List<String> SCOPES = Collections.singletonList(CalendarScopes.CALENDAR);
    private static final String CREDENTIALS_FILE_PATH = "/credentials/credentials.json";
    private static final String CALENDAR_NAME = "primary";
    private static final String MESSAGE_INTERNET_ERROR = "Internet connection error. Please check your network.";
    private static final Logger logger = LogsCenter.getLogger(UiManager.class);

    private boolean googleCalendarEnabled = false;

    public boolean isGoogleCalendarEnabled() {
        return googleCalendarEnabled;
    }

    public void setGoogleCalendarEnabled() {
        this.googleCalendarEnabled = true;
    }


    public Calendar getCalendar() {
        NetHttpTransport httpTransport = getNetHttpTransport();

        // Build Google calender service object.
        Calendar service = null;
        try {
            //re-direct the stdout
            //One of the Google APIs outputs to stdout which causes warning
            System.setOut(new PrintStream(new OutputStream() {
                @Override
                public void write(int b) {
                    //no code needed here
                }
            }));

            service = new Calendar.Builder(httpTransport, JSON_FACTORY,
                    getCredentials(httpTransport))
                    .setApplicationName("iScheduler Xs Max")
                    .build();
            //re-direct back to the stdout
            System.setOut(System.out);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return service;
    }

    public NetHttpTransport getNetHttpTransport() {
        // Build a new authorized API client service.
        NetHttpTransport httpTransport = null;
        try {
            httpTransport = GoogleNetHttpTransport.newTrustedTransport();
        } catch (GeneralSecurityException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return httpTransport;
    }

    /**
     * Creates an authorized Credential object.
     *
     * @param httpTransport The network HTTP Transport.
     *
     * @return An authorized Credential object.
     *
     * @throws IOException If the credentials.json file cannot be found.
     */
    public static Credential getCredentials(final NetHttpTransport httpTransport) throws IOException {
        GoogleClientSecrets clientSecrets = getGoogleClientSecrets();

        // Build flow and trigger user authorization request.
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                httpTransport, JSON_FACTORY, clientSecrets,
                SCOPES)
                .setDataStoreFactory(new FileDataStoreFactory(new File("tokens")))
                .setAccessType("offline")
                .build();
        return new AuthorizationCodeInstalledApp(flow, new LocalServerReceiver()).authorize("user");
    }

    public static GoogleClientSecrets getGoogleClientSecrets() throws IOException {
        // Load client secrets.
        InputStream in = GetGoogleCalendarEventsCommand.class
                .getResourceAsStream(CREDENTIALS_FILE_PATH);
        return GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));
    }

    /**
     * To check whether internet is available.
     *
     * @return true if available, false otherwise.
     */
    public static boolean netIsAvailable() {
        try {
            final URL url = new URL("http://www.google.com");
            final URLConnection conn = url.openConnection();
            conn.connect();
            conn.getInputStream().close();
            return true;
        } catch (MalformedURLException e) {
            return false;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Pushes the event(s) to Google Calendar.
     */
    public void pushToGoogleCal(List<Event> toAddList) {

        logger.info("Starting to push events Google Calendar");
        Calendar service = getCalendar();

        com.google.api.services.calendar.model.Event gEvent = new com.google.api.services.calendar.model.Event();
        for (Event toAddEvent : toAddList) {
            gEvent.setSummary(String.valueOf(toAddEvent.getEventName()));
            gEvent.setLocation(String.valueOf(toAddEvent.getVenue()));
            gEvent.setDescription(String.valueOf(toAddEvent.getDescription()));
            String startDateTime = EventFormatUtil.convertStartDateTimeToGoogleFormat(toAddEvent);

            DateTime start = parseRfc3339(startDateTime);
            gEvent.setStart(new EventDateTime().setDateTime(start).setTimeZone("Singapore"));

            String endDateTime = EventFormatUtil.convertEndDateTimeToGoogleFormat(toAddEvent);
            DateTime end = parseRfc3339(endDateTime);
            gEvent.setEnd(new EventDateTime().setDateTime(end).setTimeZone("Singapore"));

            gEvent.setId(String.valueOf(toAddEvent.getUid()).replaceAll("-", ""));

            ReminderDurationList reminderDurationList = toAddEvent.getReminderDurationList();
            List<EventReminder> reminderOverrides = new ArrayList<>();

            Map<Duration, Boolean> reminderMap = reminderDurationList.get();
            for (Duration key : reminderMap.keySet()) {
                EventReminder eventReminder = new EventReminder();
                eventReminder.setMethod("popup");
                eventReminder.setMinutes(Math.toIntExact(key.toMinutes()));
                reminderOverrides.add(eventReminder);
            }
            Reminders reminder = new Reminders();
            reminder.setOverrides(reminderOverrides);
            gEvent.setReminders(reminder);

            if (toAddEvent.getRepeatType() == RepeatType.NONE) {


            } else {//repeated event
                gEvent.setRecurringEventId(String.valueOf(toAddEvent.getUuid()));
                String eventRepeatType = String.valueOf(toAddEvent.getRepeatType());
                seedu.scheduler.model.event.DateTime eventUntilDt = toAddEvent.getRepeatUntilDateTime();
                String eventUntilDate = eventUntilDt.getPrettyString()
                        //local:2019-01-01 18:51:52
                        .replaceAll("-","")
                        //local:20190101 18:51:52
                        .replaceFirst(" ","T")
                        //local:20190101T18:51:52
                        .replaceAll(":","")
                //local:20190101T185152
                .concat("Z");

                String temp = start.toString();
                //temp 2018-10-29T21:00:00.000+08:00
                String input_date=temp.substring(9,10)
                        +"/"
                        +temp.substring(6,7)
                        +"/"
                        +temp.substring(0,4);
                         // "01/08/2012";
                SimpleDateFormat format1=new SimpleDateFormat("dd/MM/yyyy");
                Date dt1= null;
                try {
                    dt1 = format1.parse(input_date);
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                DateFormat format2=new SimpleDateFormat("EEEE");
                String finalDay=format2.format(dt1);

String command = "RRULE:FREQ="
        +eventRepeatType
        +";UNTIL="
        +eventUntilDate;        ;
                //Google format:20110701T170000Z
                gEvent.setRecurrence(Arrays.asList(command));
//                        +"BYDAY="
//                        +finalDay.substring(0,1).toUpperCase()
//                        +"\""));
            }

            try {
                service.events().insert("primary", gEvent).execute();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Edits the details of an existing event in the Google Calendar.
     *
     * @param eventToEdit a local Event.
     * @param editedEvent an edited local Event.
     */
    public void updateGoogleEvent(Event eventToEdit, Event editedEvent) {
        assert eventToEdit != null;
        assert editedEvent != null;
        Calendar service = getCalendar();
        String gEventId = String.valueOf(eventToEdit.getUuid()).replaceAll("-", "");

        com.google.api.services.calendar.model.Event gEvent = null;

        //retrieve event
        boolean found = false;
        try {

            Events events = null;
            DateTime now = new DateTime(System.currentTimeMillis());

            events = service.events().list("primary")//set the source calendar on google
                    .setMaxResults(99) //set upper limit for number of events
                    .setTimeMin(now)//set the starting time
                    .setOrderBy("startTime")//if not specified, stable order
                    //TODO: further development can be done for repeated event, more logic must be written
                    .setSingleEvents(true)//not the repeated ones
                    //TODO: how to use setSynctoken, to prevent adding the same event multiples times
                    .execute();
            List<com.google.api.services.calendar.model.Event> eventsItems = events.getItems();
            for (com.google.api.services.calendar.model.Event tempGEvent : eventsItems) {
                if (tempGEvent.getId() == gEventId) {
                    found = true;
                }
            }
            if (found) {
                gEvent = service.events().get("primary", gEventId).execute();
            }

        } catch (IOException e3) {
            e3.printStackTrace();
        }

        if (found) {
            assert found;
            assert gEvent != null;
            gEvent.setSummary(String.valueOf(editedEvent.getEventName()));
            gEvent.setLocation(String.valueOf(editedEvent.getVenue()));
            gEvent.setDescription(String.valueOf(editedEvent.getDescription()));

            String startDateTime = EventFormatUtil.convertStartDateTimeToGoogleFormat(editedEvent);

            gEvent.setStart(new EventDateTime()
                    .setDateTime(parseRfc3339(startDateTime)));

            String endDateTime = EventFormatUtil.convertEndDateTimeToGoogleFormat(editedEvent);

            gEvent.setEnd(
                    new EventDateTime().setDateTime(parseRfc3339(endDateTime)));

            com.google.api.services.calendar.model.Event updatedgEvent = null;
            com.google.api.services.calendar.model.Event event = null;
            try {
                event = service.events().get("primary", gEventId).execute();
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (event != null) {
                try {
                    updatedgEvent = service.events().update("primary", gEventId, gEvent).execute();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                assert updatedgEvent != null;
                updatedgEvent.getUpdated();
            }
        }
    }

    public Events getEvents(Calendar service) throws UnknownHostException {
        //TODO:Currently number is hardcoded, maybe can ask user to imputthis.
        //max 2500 by Google
        //default value is 250 if not specified
        int numberOfEventsToBeDownloaded = 999;

        // List the next [userinput] events from calendar name specified by CALENDAR_NAME.
        DateTime now = new DateTime(System.currentTimeMillis());
        Events events = null;
        try {
            events = service.events().list(CALENDAR_NAME)//set the source calendar on google
                    .setMaxResults(numberOfEventsToBeDownloaded) //set upper limit for number of events
                    .setTimeMin(now)//set the starting time
                    //.setOrderBy("startTime")//if not specified, stable order
                    //TODO: further development can be done for repeated event, more logic must be written
                    //.setSingleEvents(true)//not the repeated ones
                    //TODO: how to use setSynctoken, to prevent adding the same event multiples times
                    .execute();
        } catch (UnknownHostException e) {
            throw e;
        } catch (java.net.SocketException e2) {
            new CommandResult(MESSAGE_INTERNET_ERROR);
            e2.printStackTrace();
        } catch (IOException e3) {
            new CommandResult(MESSAGE_INTERNET_ERROR);
            e3.printStackTrace();
        }
        return events;
    }


}
