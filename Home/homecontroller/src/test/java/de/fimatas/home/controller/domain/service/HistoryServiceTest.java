package de.fimatas.home.controller.domain.service;

import de.fimatas.home.controller.command.HomematicCommand;
import de.fimatas.home.controller.command.HomematicCommandBuilder;
import de.fimatas.home.controller.command.HomematicCommandProcessor;
import de.fimatas.home.controller.dao.HistoryDatabaseDAO;
import de.fimatas.home.controller.database.mapper.TimestampValuePair;
import de.fimatas.home.controller.model.HistoryElement;
import de.fimatas.home.controller.model.HistoryValueType;
import de.fimatas.home.controller.service.DeviceQualifier;
import de.fimatas.home.library.domain.model.TimeRange;
import de.fimatas.home.library.homematic.model.Datapoint;
import de.fimatas.home.library.homematic.model.Device;
import de.fimatas.home.library.homematic.model.HistoryStrategy;
import org.assertj.core.api.AutoCloseableSoftAssertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class HistoryServiceTest {

    @InjectMocks
    private HistoryService historyService;

    @InjectMocks
    private HomematicCommandBuilder homematicCommandBuilder;

    @InjectMocks
    private HomematicCommandProcessor processor;

    @Mock
    private HistoryDatabaseDAO dao;

    @Mock
    private DeviceQualifier deviceQualifier;

    @Before
    public void before() {
        when(deviceQualifier.idFrom(any(Device.class))).thenReturn("<ID>");
        when(deviceQualifier.channelFrom(any(Device.class))).thenReturn(99);
    }

    @Test
    public void testMin() {
        TimestampValuePair result = historyService.min(createList());
        assertThat(result.getTimestamp()).isEqualTo(LocalDateTime.of(2019, 12, 7, 21, 0));
        assertThat(result.getValue()).isEqualTo(new BigDecimal(100));
        assertThat(result.getType()).isEqualTo(HistoryValueType.MIN);
    }

    @Test
    public void testMax()  {
        TimestampValuePair result = historyService.max(createList());
        assertThat(result.getTimestamp()).isEqualTo(LocalDateTime.of(2019, 12, 7, 22, 0));
        assertThat(result.getValue()).isEqualTo(new BigDecimal(200));
        assertThat(result.getType()).isEqualTo(HistoryValueType.MAX);
    }

    @Test
    public void testAvg() {
        TimestampValuePair result = historyService.avg(createList());
        assertThat(result.getTimestamp()).isEqualTo(LocalDateTime.of(2019, 12, 7, 21, 30));
        assertThat(result.getValue()).isEqualTo(new BigDecimal(160));
        assertThat(result.getType()).isEqualTo(HistoryValueType.AVG);
    }

    @Test
    public void testMinEmptyNull() {
        TimestampValuePair resultEmpty = historyService.min(new LinkedList<>());
        TimestampValuePair resultNull = historyService.min(new LinkedList<>());
        assertThat(resultEmpty).isNull();
        assertThat(resultNull).isNull();
    }

    @Test
    public void testMaxEmptyNull()  {
        TimestampValuePair resultEmpty = historyService.max(new LinkedList<>());
        TimestampValuePair resultNull = historyService.max(new LinkedList<>());
        assertThat(resultEmpty).isNull();
        assertThat(resultNull).isNull();
    }

    @Test
    public void testAvgEmptyNull(){
        TimestampValuePair resultEmpty = historyService.avg(new LinkedList<>());
        TimestampValuePair resultNull = historyService.avg(new LinkedList<>());
        assertThat(resultEmpty).isNull();
        assertThat(resultNull).isNull();
    }

    private List<TimestampValuePair> createList() {

        List<TimestampValuePair> list = new LinkedList<>();
        list.add(new TimestampValuePair(LocalDateTime.of(2019, 12, 7, 21, 0), new BigDecimal(100), HistoryValueType.SINGLE));
        list.add(new TimestampValuePair(LocalDateTime.of(2019, 12, 7, 21, 40), new BigDecimal(180), HistoryValueType.SINGLE));
        list.add(new TimestampValuePair(LocalDateTime.of(2019, 12, 7, 22, 0), new BigDecimal(200), HistoryValueType.SINGLE));
        return list;
    }

    private HomematicCommand createCommand() {
        HomematicCommand command = homematicCommandBuilder.read(Device.STROMZAEHLER_GESAMT, Datapoint.ENERGY_COUNTER);
        processor.buildVarName(command);
        return command;
    }

    @Test
    public void testReadExtremValueBetweenWithCache()  {

        Object[][] input = new Object[][] { //
            {new BigDecimal(400), HistoryValueType.MIN, null, 100}, //
            {new BigDecimal(400), HistoryValueType.MAX, null, 400}, //
            {new BigDecimal(80), HistoryValueType.MIN, null, 80}, //
            {new BigDecimal(80), HistoryValueType.MAX, null, 200}, //
            {new BigDecimal(80), HistoryValueType.MIN, List.of(TimeRange.EVENING), 100}, //
        };

        for (Object[] testcase : input) {

            HomematicCommand cmd = createCommand();
            historyService.getEntryCache().clear();
            historyService.getEntryCache().put(cmd, createList());

            LocalDateTime start = LocalDateTime.of(2019, 12, 7, 0, 0);
            LocalDateTime end = LocalDateTime.of(2019, 12, 7, 23, 59);

            TimestampValuePair dbPair =
                new TimestampValuePair(LocalDateTime.of(2019, 12, 7, 8, 0), (BigDecimal) testcase[0], HistoryValueType.SINGLE);
            when(dao.readExtremValueBetween(cmd, (HistoryValueType) testcase[1], start, end)).thenReturn(dbPair);

            BigDecimal result = historyService.readExtremValueBetweenWithCache(cmd, (HistoryValueType) testcase[1], start, end,
                    (List<TimeRange>) testcase[2]);
            assertThat(result.intValue()).isEqualTo(testcase[3]);
        }

    }

    @Test
    public void testReadFirstValueBeforeWithCache() {

        Object[][] input = new Object[][] { //
            {LocalDateTime.of(2019, 12, 7, 19, 0), new BigDecimal(400), 180}, //
            {LocalDateTime.of(2019, 12, 7, 21, 59), new BigDecimal(400), 400}, //
        };

        for (Object[] testcase : input) {

            HomematicCommand cmd = createCommand();
            historyService.getEntryCache().clear();
            historyService.getEntryCache().put(cmd, createList());

            LocalDateTime start = LocalDateTime.of(2019, 12, 7, 22, 0);

            TimestampValuePair dbPair =
                new TimestampValuePair((LocalDateTime) testcase[0], (BigDecimal) testcase[1], HistoryValueType.SINGLE);
            when(dao.readFirstValueBefore(cmd, start, 48)).thenReturn(dbPair);

            BigDecimal result = historyService.readFirstValueBeforeWithCache(cmd, start);
            assertThat(result.intValue()).isEqualTo(testcase[2]);
        }
    }

    @Test
    public void testReadValuesWithCache() {

        Object[][] input = new Object[][] { //
            {LocalDateTime.of(2019, 12, 7, 19, 0), new BigDecimal(400), null, 4}, //
            {LocalDateTime.of(2019, 12, 7, 19, 0), new BigDecimal(400), LocalDateTime.of(2019, 12, 7, 21, 30), 3}, //
        };

        for (Object[] testcase : input) {
            HomematicCommand cmd = createCommand();
            historyService.getEntryCache().clear();
            historyService.getEntryCache().put(cmd, createList());

            TimestampValuePair dbPair =
                new TimestampValuePair((LocalDateTime) testcase[0], (BigDecimal) testcase[1], HistoryValueType.SINGLE);
            when(dao.readValues(cmd, (LocalDateTime) testcase[2])).thenReturn(Collections.singletonList(dbPair));

            List<TimestampValuePair> result = historyService.readValuesWithCache(cmd, (LocalDateTime) testcase[2]);
            assertThat(result.size()).isEqualTo(testcase[3]);
        }
    }

    @Test
    public void testDiffValueCheckedAdd()  {

        final LocalDateTime baseTime = LocalDateTime.of(2019, 12, 7, 21, 30);
        final HistoryElement historyElement = new HistoryElement(
                homematicCommandBuilder.read(Device.AUSSENTEMPERATUR, Datapoint.TEMPERATURE), HistoryStrategy.AVG, new BigDecimal(1));

        class TestParams {
            final TimestampValuePair pair;
            final int expectation;
            final String description;
            TestParams(int plusMinutes, String value, boolean expectation, String description){
                this.pair = new TimestampValuePair(baseTime.plusMinutes(plusMinutes), new BigDecimal(value), HistoryValueType.SINGLE);
                this.expectation = expectation?1:0;
                this.description = description;
            }
        }

        List<TestParams> testValueMap = new LinkedList<>();

        testValueMap.add(new TestParams(1,"1", true, "always add first entry"));
        testValueMap.add(new TestParams(1, "4", true, "add diff > +1"));
        testValueMap.add(new TestParams(1, "4.2", false, "not add diff < +1"));
        testValueMap.add(new TestParams(1, "4.5", true, "add diff < +1 if rounded value is not equal"));
        testValueMap.add(new TestParams(1, "3.4", true, "add diff > -1"));
        testValueMap.add(new TestParams(1, "2.8", false, "not add diff < -1"));
        testValueMap.add(new TestParams(1, "2.4", true, "add diff < -1 if rounded value is not equal"));

        final AutoCloseableSoftAssertions softAssert = new AutoCloseableSoftAssertions();
        TimestampValuePair lastPair = null;
        for (TestParams params: testValueMap) {
            when(dao.readLatestValue(historyElement.getCommand())).thenReturn(lastPair);
            var result = new LinkedList<TimestampValuePair>();
            historyService.diffValueCheckedAdd(historyElement, params.pair, result);
            softAssert.assertThat(result.size()).describedAs(params.description).isEqualTo(params.expectation);
            lastPair = params.pair;
        }
        softAssert.assertAll();
    }

}
