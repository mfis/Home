package homecontroller.domain.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedList;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnitRunner;

import homecontroller.database.mapper.TimestampValuePair;
import homecontroller.model.HistoryValueType;

@RunWith(MockitoJUnitRunner.class)
public class HistoryServiceTest {

	@InjectMocks
	private HistoryService historyService;

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
		assertThat(result.getTimestamp()).isEqualTo(LocalDateTime.of(2019, 12, 7, 21, 20));
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
		list.add(new TimestampValuePair(LocalDateTime.of(2019, 12, 7, 22, 0), new BigDecimal(200),
				HistoryValueType.SINGLE));
		list.add(new TimestampValuePair(LocalDateTime.of(2019, 12, 7, 21, 40), new BigDecimal(180),
				HistoryValueType.SINGLE));
		return list;
	}

}
