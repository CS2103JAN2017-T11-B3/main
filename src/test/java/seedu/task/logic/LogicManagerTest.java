package seedu.task.logic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static seedu.task.commons.core.Messages.MESSAGE_INVALID_COMMAND_FORMAT;
import static seedu.task.commons.core.Messages.MESSAGE_INVALID_TASK_DISPLAYED_INDEX;
import static seedu.task.commons.core.Messages.MESSAGE_UNKNOWN_COMMAND;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.google.common.eventbus.Subscribe;

import seedu.task.commons.core.EventsCenter;
import seedu.task.commons.events.model.TaskListChangedEvent;
import seedu.task.commons.events.ui.JumpToListRequestEvent;
import seedu.task.commons.events.ui.ShowHelpRequestEvent;
import seedu.task.logic.commands.CommandResult;
import seedu.task.logic.commands.DeleteCommand;
import seedu.task.logic.commands.ExitCommand;
import seedu.task.logic.commands.FindCommand;
import seedu.task.logic.commands.HelpCommand;
import seedu.task.logic.commands.exceptions.CommandException;
import seedu.task.model.Model;
import seedu.task.model.ModelManager;
import seedu.task.model.ReadOnlyTaskList;
import seedu.task.model.TaskList;
import seedu.task.model.tag.Tag;
import seedu.task.model.tag.UniqueTagList;
import seedu.task.model.task.Description;
import seedu.task.model.task.Priority;
import seedu.task.model.task.ReadOnlyTask;
import seedu.task.model.task.RecurringFrequency;
import seedu.task.model.task.Task;
import seedu.task.model.task.Timing;
import seedu.task.storage.StorageManager;


public class LogicManagerTest {

    /**
     * See https://github.com/junit-team/junit4/wiki/rules#temporaryfolder-rule
     */
    @Rule
    public TemporaryFolder saveFolder = new TemporaryFolder();

    private Model model;
    private Logic logic;

    //These are for checking the correctness of the events raised
    private ReadOnlyTaskList latestSavedTaskManager;
    private boolean helpShown;
    private int targetedJumpIndex;

    @Subscribe
    private void handleLocalModelChangedEvent(TaskListChangedEvent abce) {
        latestSavedTaskManager = new TaskList(abce.data);
    }

    @Subscribe
    private void handleShowHelpRequestEvent(ShowHelpRequestEvent she) {
        helpShown = true;
    }

    @Subscribe
    private void handleJumpToListRequestEvent(JumpToListRequestEvent je) {
        targetedJumpIndex = je.targetIndex;
    }

    @Before
    public void setUp() {
        model = new ModelManager();
        String tempTaskManagerFile = saveFolder.getRoot().getPath() + "TempTaskManager.xml";
        String tempPreferencesFile = saveFolder.getRoot().getPath() + "TempPreferences.json";
        logic = new LogicManager(model, new StorageManager(tempTaskManagerFile, tempPreferencesFile), null);
        EventsCenter.getInstance().registerHandler(this);

        latestSavedTaskManager = new TaskList(model.getTaskList()); // last saved assumed to be up to date
        helpShown = false;
        targetedJumpIndex = -1; // non yet
    }

    @After
    public void tearDown() {
        EventsCenter.clearSubscribers();
    }

    @Test
    public void execute_invalid() {
        String invalidCommand = "       ";
        assertCommandFailure(invalidCommand, String.format(MESSAGE_INVALID_COMMAND_FORMAT,
                HelpCommand.MESSAGE_USAGE));
    }

    /**
     * Executes the command, confirms that a CommandException is not thrown and that the result message is correct.
     * Also confirms that both the 'task manager' and the 'last shown list' are as specified.
     * @see #assertCommandBehavior(boolean, String, String, ReadOnlyTaskList, List)
     */
    private void assertCommandSuccess(String inputCommand, String expectedMessage,
            ReadOnlyTaskList expectedTaskManager,
            List<? extends ReadOnlyTask> expectedShownList) {
        assertCommandBehavior(false, inputCommand, expectedMessage, expectedTaskManager, expectedShownList);
    }

    /**
     * Executes the command, confirms that a CommandException is thrown and that the result message is correct.
     * Both the 'task manager' and the 'last shown list' are verified to be unchanged.
     * @see #assertCommandBehavior(boolean, String, String, ReadOnlyTaskList, List)
     */
    private void assertCommandFailure(String inputCommand, String expectedMessage) {
        TaskList expectedTaskManager = new TaskList(model.getTaskList());
        List<ReadOnlyTask> expectedShownList = new ArrayList<>(model.getFilteredTaskList());
        assertCommandBehavior(true, inputCommand, expectedMessage, expectedTaskManager, expectedShownList);
    }

    /**
     * Executes the command, confirms that the result message is correct
     * and that a CommandException is thrown if expected
     * and also confirms that the following three parts of the LogicManager object's state are as expected:<br>
     *      - the internal task manager data are same as those in the {@code expectedTaskManager} <br>
     *      - the backing list shown by UI matches the {@code shownList} <br>
     *      - {@code expectedTaskManager} was saved to the storage file. <br>
     */
    private void assertCommandBehavior(boolean isCommandExceptionExpected,
            String inputCommand,
            String expectedMessage,
            ReadOnlyTaskList expectedTaskManager,
            List<? extends ReadOnlyTask> expectedShownList) {

        try {
            CommandResult result = logic.execute(inputCommand);
            assertFalse("CommandException expected but was not thrown.", isCommandExceptionExpected);
            assertEquals(expectedMessage, result.feedbackToUser);
        } catch (CommandException e) {
            assertTrue("CommandException not expected but was thrown.", isCommandExceptionExpected);
            assertEquals(expectedMessage, e.getMessage());
        }

        //Confirm the ui display elements should contain the right data
        assertEquals(expectedShownList, model.getFilteredTaskList());

        //Confirm the state of data (saved and in-memory) is as expected
        assertEquals(expectedTaskManager, model.getTaskList());
        assertEquals(expectedTaskManager, latestSavedTaskManager);
    }

    @Test
    public void execute_unknownCommandWord() {
        String unknownCommand = "uicfhmowqewca";
        assertCommandFailure(unknownCommand, MESSAGE_UNKNOWN_COMMAND);
    }

    @Test
    public void execute_help() {
        assertCommandSuccess("help", HelpCommand.SHOWING_HELP_MESSAGE, new TaskList(), Collections.emptyList());
        assertTrue(helpShown);
    }

    @Test
    public void execute_exit() {
        assertCommandSuccess("exit", ExitCommand.MESSAGE_EXIT_ACKNOWLEDGEMENT,
                new TaskList(), Collections.emptyList());
    }

    // @Test
    // public void execute_clear() throws Exception {
    //     TestDataHelper helper = new TestDataHelper();
    //     model.addTask(helper.generateTask(1));
    //     model.addTask(helper.generateTask(2));
    //     model.addTask(helper.generateTask(3));

    //     assertCommandSuccess("clear", ClearCommand.MESSAGE_SUCCESS, new TaskList(), Collections.emptyList());
    // }


    // @Test
    // public void execute_add_invalidArgsFormat() {
    //     String expectedMessage = String.format(MESSAGE_INVALID_COMMAND_FORMAT, AddCommand.MESSAGE_USAGE);
    //     assertCommandFailure("add wrong args wrong args", expectedMessage);
    // }

    // @Test
    // public void execute_add_invalidTaskData() {
    //     assertCommandFailure("add []\\[;] p/12345 e/valid@e.mail a/valid, address",
    //             Description.MESSAGE_DESCRIPTION_CONSTRAINTS);
    //     assertCommandFailure("add Valid Description p/not_numbers e/valid@e.mail a/valid, address",
    //             Priority.MESSAGE_PRIORITY_CONSTRAINTS);
    //     assertCommandFailure("add Valid Description p/12345 e/notAnEmail a/valid, address",
    //             Timing.MESSAGE_TIMING_CONSTRAINTS);
    //     assertCommandFailure("add Valid Description p/12345 e/valid@e.mail a/valid, address t/invalid_-[.tag",
    //             Tag.MESSAGE_TAG_CONSTRAINTS);

    // }

    // @Test
    // public void execute_add_successful() throws Exception {
    //     // setup expectations
    //     TestDataHelper helper = new TestDataHelper();
    //     Task toBeAdded = helper.adam();
    //     TaskList expectedAB = new TaskList();
    //     expectedAB.addTask(toBeAdded);

    //     // execute command and verify result
    //     assertCommandSuccess(helper.generateAddCommand(toBeAdded),
    //             String.format(AddCommand.MESSAGE_SUCCESS, toBeAdded),
    //             expectedAB,
    //             expectedAB.getTaskList());
    // }

    // @Test
    // public void execute_addDuplicate_notAllowed() throws Exception {
    //     // setup expectations
    //     TestDataHelper helper = new TestDataHelper();
    //     Task toBeAdded = helper.adam();

    //     // setup starting state
    //     model.addTask(toBeAdded); // task already in internal task manager

    //     // execute command and verify result
    //     assertCommandFailure(helper.generateAddCommand(toBeAdded),  AddCommand.MESSAGE_DUPLICATE_TASK);

    // }


    // @Test
    // public void execute_list_showsAllTasks() throws Exception {
    //     // prepare expectations
    //     TestDataHelper helper = new TestDataHelper();
    //     TaskList expectedAB = helper.generateTaskManager(2);
    //     List<? extends ReadOnlyTask> expectedList = expectedAB.getTaskList();

    //     // prepare task manager state
    //     helper.addToModel(model, 2);

    //     assertCommandSuccess("list",
    //             ListCommand.MESSAGE_SUCCESS,
    //             expectedAB,
    //             expectedList);
    // }


    /**
     * Confirms the 'invalid argument index number behaviour' for the given command
     * targeting a single task in the shown list, using visible index.
     * @param commandWord to test assuming it targets a single task in the last shown list
     *                    based on visible index.
     */
    private void assertIncorrectIndexFormatBehaviorForCommand(String commandWord, String expectedMessage)
            throws Exception {
        assertCommandFailure(commandWord , expectedMessage); //index missing
        assertCommandFailure(commandWord + " +1", expectedMessage); //index should be unsigned
        assertCommandFailure(commandWord + " -1", expectedMessage); //index should be unsigned
        assertCommandFailure(commandWord + " 0", expectedMessage); //index cannot be 0
        assertCommandFailure(commandWord + " not_a_number", expectedMessage);
    }

    /**
     * Confirms the 'invalid argument index number behaviour' for the given command
     * targeting a single task in the shown list, using visible index.
     * @param commandWord to test assuming it targets a single task in the last shown list
     *                    based on visible index.
     */
    private void assertIndexNotFoundBehaviorForCommand(String commandWord) throws Exception {
        String expectedMessage = MESSAGE_INVALID_TASK_DISPLAYED_INDEX;
        TestDataHelper helper = new TestDataHelper();
        List<Task> taskList = helper.generateTaskList(2);

        // set AB state to 2 tasks
        model.resetData(new TaskList());
        for (Task p : taskList) {
            model.addTask(p);
        }

        assertCommandFailure(commandWord + " 3", expectedMessage);
    }

    // @Test
    // public void execute_selectIndexNotFound_errorMessageShown() throws Exception {
    //     assertIndexNotFoundBehaviorForCommand("select");
    // }

    @Test
    public void execute_deleteInvalidArgsFormat_errorMessageShown() throws Exception {
        String expectedMessage = String.format(MESSAGE_INVALID_COMMAND_FORMAT, DeleteCommand.MESSAGE_USAGE);
        assertIncorrectIndexFormatBehaviorForCommand("delete", expectedMessage);
    }

    // @Test
    // public void execute_deleteIndexNotFound_errorMessageShown() throws Exception {
    //     assertIndexNotFoundBehaviorForCommand("delete");
    // }

    // @Test
    // public void execute_delete_removesCorrectTask() throws Exception {
    //     TestDataHelper helper = new TestDataHelper();
    //     List<Task> threeTasks = helper.generateTaskList(3);

    //     TaskList expectedAB = helper.generateTaskManager(threeTasks);
    //     expectedAB.removeTask(threeTasks.get(1));
    //     helper.addToModel(model, threeTasks);

    //     assertCommandSuccess("delete 2",
    //             String.format(DeleteCommand.MESSAGE_DELETE_TASK_SUCCESS, threeTasks.get(1)),
    //             expectedAB,
    //             expectedAB.getTaskList());
    // }


    @Test
    public void execute_find_invalidArgsFormat() {
        String expectedMessage = String.format(MESSAGE_INVALID_COMMAND_FORMAT, FindCommand.MESSAGE_USAGE);
        assertCommandFailure("find ", expectedMessage);
    }

    // @Test
    // public void execute_find_onlyMatchesFullWordsInNames() throws Exception {
    //     TestDataHelper helper = new TestDataHelper();
    //     Task pTarget1 = helper.generateTaskWithName("bla bla KEY bla");
    //     Task pTarget2 = helper.generateTaskWithName("bla KEY bla bceofeia");
    //     Task p1 = helper.generateTaskWithName("KE Y");
    //     Task p2 = helper.generateTaskWithName("KEYKEYKEY sduauo");

    //     List<Task> fourTasks = helper.generateTaskList(p1, pTarget1, p2, pTarget2);
    //     TaskList expectedAB = helper.generateTaskManager(fourTasks);
    //     List<Task> expectedList = helper.generateTaskList(pTarget1, pTarget2);
    //     helper.addToModel(model, fourTasks);

    //     assertCommandSuccess("find KEY",
    //             Command.getMessageForTaskListShownSummary(expectedList.size()),
    //             expectedAB,
    //             expectedList);
    // }

    // @Test
    // public void execute_find_isNotCaseSensitive() throws Exception {
    //     TestDataHelper helper = new TestDataHelper();
    //     Task p1 = helper.generateTaskWithName("bla bla KEY bla");
    //     Task p2 = helper.generateTaskWithName("bla KEY bla bceofeia");
    //     Task p3 = helper.generateTaskWithName("key key");
    //     Task p4 = helper.generateTaskWithName("KEy sduauo");

    //     List<Task> fourTasks = helper.generateTaskList(p3, p1, p4, p2);
    //     TaskList expectedAB = helper.generateTaskManager(fourTasks);
    //     List<Task> expectedList = fourTasks;
    //     helper.addToModel(model, fourTasks);

    //     assertCommandSuccess("find KEY",
    //             Command.getMessageForTaskListShownSummary(expectedList.size()),
    //             expectedAB,
    //             expectedList);
    // }

    // @Test
    // public void execute_find_matchesIfAnyKeywordPresent() throws Exception {
    //     TestDataHelper helper = new TestDataHelper();
    //     Task pTarget1 = helper.generateTaskWithName("bla bla KEY bla");
    //     Task pTarget2 = helper.generateTaskWithName("bla rAnDoM bla bceofeia");
    //     Task pTarget3 = helper.generateTaskWithName("key key");
    //     Task p1 = helper.generateTaskWithName("sduauo");

    //     List<Task> fourTasks = helper.generateTaskList(pTarget1, p1, pTarget2, pTarget3);
    //     TaskList expectedAB = helper.generateTaskManager(fourTasks);
    //     List<Task> expectedList = helper.generateTaskList(pTarget1, pTarget2, pTarget3);
    //     helper.addToModel(model, fourTasks);

    //     assertCommandSuccess("find key rAnDoM",
    //             Command.getMessageForTaskListShownSummary(expectedList.size()),
    //             expectedAB,
    //             expectedList);
    // }


    /**
     * A utility class to generate test data.
     */
    class TestDataHelper {

        Task adam() throws Exception {
            Description description = new Description("Kiss Adam Brown on the lips");
            Priority priority = new Priority("1");
            Timing startDate = new Timing("21/03/2017");
            Timing endDate = new Timing("25/03/2017");
            Tag tag1 = new Tag("tag1");
            Tag tag2 = new Tag("longertag2");
            UniqueTagList tags = new UniqueTagList(tag1, tag2);
            RecurringFrequency frequency = new RecurringFrequency(null);
            Task task = new Task(description, priority, startDate, endDate, tags, false, frequency);
            return task;
        }

        /**
         * Generates a valid task using the given seed.
         * Running this function with the same parameter values guarantees the returned task
           will have the same state.
         * Each unique seed will generate a unique Task object.
         *
         * @param seed used to generate the task data field values
         */
        Task generateTask(int seed) throws Exception {
            return new Task(
                    new Description("Task " + seed),
                    new Priority("" + Math.abs(seed) % 3),
                    new Timing("" + (seed % 28) + "/" + (seed % 12) + "/" + "2017"),
                    new Timing("" + (seed % 28) + "/" + (seed % 12) + "/" + "2018"),
                    new UniqueTagList(new Tag("tag" + Math.abs(seed)), new Tag("tag" + Math.abs(seed + 1))),
                    false,
                    new RecurringFrequency(null));
        }

        /** Generates the correct add command based on the task given */
        String generateAddCommand(Task t) {
            StringBuffer cmd = new StringBuffer();

            cmd.append("add ");

            cmd.append(t.getDescription().toString());
            cmd.append(" sd/").append(t.getStartTiming().toString());
            cmd.append(" ed/").append(t.getEndTiming().toString());
            cmd.append(" p/").append(t.getPriority().toString());

            UniqueTagList tags = t.getTags();
            for (Tag tag: tags) {
                cmd.append(" t/").append(tag.tagName);
            }

            return cmd.toString();
        }

        /**
         * Generates an TaskList with auto-generated tasks.
         */
        TaskList generateTaskManager(int numGenerated) throws Exception {
            TaskList taskList = new TaskList();
            addToTaskManager(taskList, numGenerated);
            return taskList;
        }

        /**
         * Generates an TaskList based on the list of Tasks given.
         */
        TaskList generateTaskManager(List<Task> tasks) throws Exception {
            TaskList taskList = new TaskList();
            addToTaskManager(taskList, tasks);
            return taskList;
        }

        /**
         * Adds auto-generated Task objects to the given TaskList
         * @param taskList The TaskList to which the Tasks will be added
         */
        void addToTaskManager(TaskList taskList, int numGenerated) throws Exception {
            addToTaskManager(taskList, generateTaskList(numGenerated));
        }

        /**
         * Adds the given list of Tasks to the given TaskList
         */
        void addToTaskManager(TaskList taskList, List<Task> tasksToAdd) throws Exception {
            for (Task p: tasksToAdd) {
                taskList.addTask(p);
            }
        }

        /**
         * Adds auto-generated Task objects to the given model
         * @param model The model to which the Tasks will be added
         */
        void addToModel(Model model, int numGenerated) throws Exception {
            addToModel(model, generateTaskList(numGenerated));
        }

        /**
         * Adds the given list of Tasks to the given model
         */
        void addToModel(Model model, List<Task> tasksToAdd) throws Exception {
            for (Task p: tasksToAdd) {
                model.addTask(p);
            }
        }

        /**
         * Generates a list of Tasks based on the flags.
         */
        List<Task> generateTaskList(int numGenerated) throws Exception {
            List<Task> tasks = new ArrayList<>();
            for (int i = 1; i <= numGenerated; i++) {
                tasks.add(generateTask(i));
            }
            return tasks;
        }

        List<Task> generateTaskList(Task... tasks) {
            return Arrays.asList(tasks);
        }

        /**
         * Generates a Task object with given name. Other fields will have some dummy values.
         */
        Task generateTaskWithName(String name) throws Exception {
            return new Task(
                    new Description(name),
                    new Priority("1"),
                    new Timing("01/01/2017"),
                    new Timing("01/01/2017"),
                    new UniqueTagList(new Tag("tag")),
                    false,
                    new RecurringFrequency(null));
        }
    }
}
