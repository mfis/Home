package homecontroller.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import homecontroller.dao.HistoryDatabaseDAO;
import homecontroller.database.mapper.TimestampValuePair;
import homecontroller.domain.model.TimeRange;
import homecontroller.model.HistoryValueType;
import homelibrary.homematic.model.Datapoint;
import homelibrary.homematic.model.Device;
import homelibrary.homematic.model.History;
import homelibrary.homematic.model.HomematicCommand;

@RunWith(MockitoJUnitRunner.class)
public class HistoryServiceTest {

	@InjectMocks
	private HistoryService historyService;

	@Mock
	private HistoryDatabaseDAO dao;

	@Test
	public void testMin() throws Exception {
		TimestampValuePair result = historyService.min(createList());
		assertThat(result.getTimestamp()).isEqualTo(LocalDateTime.of(2019, 12, 7, 21, 0));
		assertThat(result.getValue()).isEqualTo(new BigDecimal(100));
		assertThat(result.getType()).isEqualTo(HistoryValueType.MIN);
	}

	@Test
	public void testMax() throws Exception {
		TimestampValuePair result = historyService.max(createList());
		assertThat(result.getTimestamp()).isEqualTo(LocalDateTime.of(2019, 12, 7, 22, 0));
		assertThat(result.getValue()).isEqualTo(new BigDecimal(200));
		assertThat(result.getType()).isEqualTo(HistoryValueType.MAX);
	}

	@Test
	public void testAvg() throws Exception {
		TimestampValuePair result = historyService.avg(createList());
		assertThat(result.getTimestamp()).isEqualTo(LocalDateTime.of(2019, 12, 7, 21, 30));
		assertThat(result.getValue()).isEqualTo(new BigDecimal(160));
		assertThat(result.getType()).isEqualTo(HistoryValueType.AVG);
	}

	@Test
	public void testMinEmptyNull() throws Exception {
		TimestampValuePair resultEmpty = historyService.min(new LinkedList<>());
		TimestampValuePair resultNull = historyService.min(new LinkedList<>());
		assertThat(resultEmpty).isNull();
		assertThat(resultNull).isNull();
	}

	@Test
	public void testMaxEmptyNull() throws Exception {
		TimestampValuePair resultEmpty = historyService.max(new LinkedList<>());
		TimestampValuePair resultNull = historyService.max(new LinkedList<>());
		assertThat(resultEmpty).isNull();
		assertThat(resultNull).isNull();
	}

	@Test
	public void testAvgEmptyNull() throws Exception {
		TimestampValuePair resultEmpty = historyService.avg(new LinkedList<>());
		TimestampValuePair resultNull = historyService.avg(new LinkedList<>());
		assertThat(resultEmpty).isNull();
		assertThat(resultNull).isNull();
	}

	private List<TimestampValuePair> createList() {

		List<TimestampValuePair> list = new LinkedList<>();
		list.add(new TimestampValuePair(LocalDateTime.of(2019, 12, 7, 21, 0), new BigDecimal(100),
				HistoryValueType.SINGLE));
		list.add(new TimestampValuePair(LocalDateTime.of(2019, 12, 7, 21, 40), new BigDecimal(180),
				HistoryValueType.SINGLE));
		list.add(new TimestampValuePair(LocalDateTime.of(2019, 12, 7, 22, 0), new BigDecimal(200),
				HistoryValueType.SINGLE));
		return list;
	}

	private HomematicCommand createCommand() {
		return HomematicCommand.read(Device.STROMZAEHLER, Datapoint.ENERGY_COUNTER);
	}

	@Test
	public void testReadExtremValueBetweenWithCache() throws Exception {

		Object[][] input = new Object[][] { //
				{ new BigDecimal(400), HistoryValueType.MIN, null, Integer.valueOf(100) }, //
				{ new BigDecimal(400), HistoryValueType.MAX, null, Integer.valueOf(400) }, //
				{ new BigDecimal(80), HistoryValueType.MIN, null, Integer.valueOf(80) }, //
				{ new BigDecimal(80), HistoryValueType.MAX, null, Integer.valueOf(200) }, //
				{ new BigDecimal(80), HistoryValueType.MIN, TimeRange.EVENING, Integer.valueOf(100) }, //
		};

		for (Object[] testcase : input) {

			HomematicCommand cmd = createCommand();
			historyService.getEntryCache().clear();
			historyService.getEntryCache().put(cmd, createList());

			LocalDateTime start = LocalDateTime.of(2019, 12, 7, 0, 0);
			LocalDateTime end = LocalDateTime.of(2019, 12, 7, 23, 59);

			TimestampValuePair dbPair = new TimestampValuePair(LocalDateTime.of(2019, 12, 7, 8, 0),
					(BigDecimal) testcase[0], HistoryValueType.SINGLE);
			when(dao.readExtremValueBetween(cmd, (HistoryValueType) testcase[1], start, end))
					.thenReturn(dbPair);

			BigDecimal result = historyService.readExtremValueBetweenWithCache(cmd,
					(HistoryValueType) testcase[1], start, end, (TimeRange) testcase[2]);
			assertThat(result.intValue()).isEqualTo(testcase[3]);
		}

	}

	@Test
	public void testReadFirstValueBeforeWithCache() throws Exception {

		Object[][] input = new Object[][] { //
				{ LocalDateTime.of(2019, 12, 7, 19, 00), new BigDecimal(400), 180 }, //
				{ LocalDateTime.of(2019, 12, 7, 21, 59), new BigDecimal(400), 400 }, //
		};

		for (Object[] testcase : input) {

			HomematicCommand cmd = createCommand();
			historyService.getEntryCache().clear();
			historyService.getEntryCache().put(cmd, createList());

			LocalDateTime start = LocalDateTime.of(2019, 12, 7, 22, 0);

			TimestampValuePair dbPair = new TimestampValuePair((LocalDateTime) testcase[0],
					(BigDecimal) testcase[1], HistoryValueType.SINGLE);
			when(dao.readFirstValueBefore(cmd, start, 48)).thenReturn(dbPair);

			BigDecimal result = historyService.readFirstValueBeforeWithCache(cmd, start, 48);
			assertThat(result.intValue()).isEqualTo(testcase[2]);
		}
	}

	@Test
	public void testReadValuesWithCache() throws Exception {

		Object[][] input = new Object[][] { //
				{ LocalDateTime.of(2019, 12, 7, 19, 00), new BigDecimal(400), null, 4 }, //
				{ LocalDateTime.of(2019, 12, 7, 19, 00), new BigDecimal(400),
						LocalDateTime.of(2019, 12, 7, 21, 30), 3 }, //
		};

		for (Object[] testcase : input) {
			HomematicCommand cmd = createCommand();
			historyService.getEntryCache().clear();
			historyService.getEntryCache().put(cmd, createList());

			TimestampValuePair dbPair = new TimestampValuePair((LocalDateTime) testcase[0],
					(BigDecimal) testcase[1], HistoryValueType.SINGLE);
			when(dao.readValues(cmd, (LocalDateTime) testcase[2])).thenReturn(Arrays.asList(dbPair));

			List<TimestampValuePair> result = historyService.readValuesWithCache(cmd,
					(LocalDateTime) testcase[2]);
			assertThat(result.size()).isEqualTo(testcase[3]);
		}
	}

	@Test
	public void testDiffValueCheckedAdd() throws Exception {

		LocalDateTime dbTs = LocalDateTime.of(2019, 12, 7, 21, 30);
		Object[][] input = new Object[][] { //
				{ new TimestampValuePair(dbTs, new BigDecimal(100), HistoryValueType.SINGLE), 1 }, //
				{ new TimestampValuePair(dbTs, new BigDecimal(9999), HistoryValueType.SINGLE), 0 }, //
				{ new TimestampValuePair(dbTs.minusHours(14), new BigDecimal(9999), HistoryValueType.SINGLE),
						1 }, //
		};

		for (Object[] testcase : input) {

			when(dao.readLatestValue(History.STROM_ZAEHLERSTAND.getCommand()))
					.thenReturn((TimestampValuePair) testcase[0]);

			TimestampValuePair newPair = new TimestampValuePair(LocalDateTime.of(2019, 12, 7, 21, 40),
					new BigDecimal(10000), HistoryValueType.SINGLE);

			List<TimestampValuePair> result = new LinkedList<>();
			historyService.diffValueCheckedAdd(History.STROM_ZAEHLERSTAND, newPair, result);
			assertThat(result.size()).isEqualTo(testcase[1]);
		}
	}

}
