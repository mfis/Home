package homecontroller.domain.service;

import static org.junit.Assert.assertEquals;

import java.lang.reflect.Field;
import java.math.BigDecimal;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnitRunner;

import homecontroller.domain.model.HouseModel;
import homecontroller.domain.model.OutdoorClimate;
import homecontroller.domain.model.RoomClimate;
import homecontroller.domain.model.Tendency;

@RunWith(MockitoJUnitRunner.class)
public class HouseServiceTest {

	@InjectMocks
	private HouseService houseService;

	@Test
	public void testCalculateTendenciesNoReference() throws Exception {

		HouseModel oldModel = null;

		HouseModel newModel = new HouseModel();
		newModel.setClimateBathRoom(new RoomClimate());
		newModel.getClimateBathRoom().setTemperature(new BigDecimal("20"));

		houseService.calculateTendencies(oldModel, newModel);

		assertEquals(Tendency.EQUAL, newModel.getClimateBathRoom().getTemperatureTendency());
	}

	@Test
	public void testCalculateTendenciesEqualsExact() throws Exception {

		HouseModel oldModel = new HouseModel();
		oldModel.setClimateBathRoom(new RoomClimate());
		oldModel.getClimateBathRoom().setTemperatureReference(new BigDecimal("20"));

		HouseModel newModel = new HouseModel();
		newModel.setClimateBathRoom(new RoomClimate());
		newModel.getClimateBathRoom().setTemperature(new BigDecimal("20"));

		houseService.calculateTendencies(oldModel, newModel);

		assertEquals(Tendency.EQUAL, newModel.getClimateBathRoom().getTemperatureTendency());
	}

	@Test
	public void testCalculateTendenciesMinimalVariancePlus() throws Exception {

		HouseModel oldModel = new HouseModel();
		oldModel.setConclusionClimateFacadeMin(new OutdoorClimate());
		oldModel.getConclusionClimateFacadeMin().setTemperatureReference(new BigDecimal("20"));

		HouseModel newModel = new HouseModel();
		newModel.setConclusionClimateFacadeMin(new OutdoorClimate());
		newModel.getConclusionClimateFacadeMin().setTemperature(new BigDecimal("20.1"));

		houseService.calculateTendencies(oldModel, newModel);

		assertEquals(Tendency.EQUAL, newModel.getConclusionClimateFacadeMin().getTemperatureTendency());
	}

	@Test
	public void testCalculateTendenciesMinimalVarianceMinus() throws Exception {

		HouseModel oldModel = new HouseModel();
		oldModel.setConclusionClimateFacadeMin(new OutdoorClimate());
		oldModel.getConclusionClimateFacadeMin().setTemperatureReference(new BigDecimal("20"));

		HouseModel newModel = new HouseModel();
		newModel.setConclusionClimateFacadeMin(new OutdoorClimate());
		newModel.getConclusionClimateFacadeMin().setTemperature(new BigDecimal("19.9"));

		houseService.calculateTendencies(oldModel, newModel);

		assertEquals(Tendency.EQUAL, newModel.getConclusionClimateFacadeMin().getTemperatureTendency());
	}

	@Test
	public void testCalculateTendenciesBoundaryVariancePlus() throws Exception {

		HouseModel oldModel = new HouseModel();
		oldModel.setConclusionClimateFacadeMin(new OutdoorClimate());
		oldModel.getConclusionClimateFacadeMin().setTemperatureReference(new BigDecimal("20"));

		HouseModel newModel = new HouseModel();
		newModel.setConclusionClimateFacadeMin(new OutdoorClimate());
		newModel.getConclusionClimateFacadeMin().setTemperature(new BigDecimal("20.2"));

		houseService.calculateTendencies(oldModel, newModel);

		assertEquals(Tendency.RISE, newModel.getConclusionClimateFacadeMin().getTemperatureTendency());
	}

	@Test
	public void testCalculateTendenciesBoundaryVarianceMinus() throws Exception {

		HouseModel oldModel = new HouseModel();
		oldModel.setConclusionClimateFacadeMin(new OutdoorClimate());
		oldModel.getConclusionClimateFacadeMin().setTemperatureReference(new BigDecimal("20"));

		HouseModel newModel = new HouseModel();
		newModel.setConclusionClimateFacadeMin(new OutdoorClimate());
		newModel.getConclusionClimateFacadeMin().setTemperature(new BigDecimal("19.8"));

		houseService.calculateTendencies(oldModel, newModel);

		assertEquals(Tendency.FALL, newModel.getConclusionClimateFacadeMin().getTemperatureTendency());
	}

	@Test
	public void testCalculateTendenciesCourseRise() throws Exception {

		HouseModel modelA = new HouseModel();
		setDateTime(modelA, 0);
		modelA.setConclusionClimateFacadeMin(new OutdoorClimate());
		modelA.getConclusionClimateFacadeMin().setTemperature(new BigDecimal("20"));

		HouseModel modelB = new HouseModel();
		setDateTime(modelB, modelA.getDateTime() + Tendency.Constants.ONE_MINUTE);
		modelB.setConclusionClimateFacadeMin(new OutdoorClimate());
		modelB.getConclusionClimateFacadeMin().setTemperature(new BigDecimal("20.1"));

		houseService.calculateTendencies(modelA, modelB);
		assertEquals(Tendency.EQUAL, modelB.getConclusionClimateFacadeMin().getTemperatureTendency());

		HouseModel modelC = new HouseModel();
		setDateTime(modelC, modelB.getDateTime() + Tendency.Constants.ONE_MINUTE);
		modelC.setConclusionClimateFacadeMin(new OutdoorClimate());
		modelC.getConclusionClimateFacadeMin().setTemperature(new BigDecimal("20.2"));

		houseService.calculateTendencies(modelB, modelC);
		assertEquals(Tendency.RISE, modelC.getConclusionClimateFacadeMin().getTemperatureTendency());

		HouseModel modelD = new HouseModel();
		setDateTime(modelD, modelC.getDateTime() + Tendency.Constants.ONE_MINUTE);
		modelD.setConclusionClimateFacadeMin(new OutdoorClimate());
		modelD.getConclusionClimateFacadeMin().setTemperature(new BigDecimal("20.2"));

		houseService.calculateTendencies(modelC, modelD);
		assertEquals(Tendency.RISE, modelC.getConclusionClimateFacadeMin().getTemperatureTendency());

		HouseModel modelE = new HouseModel();
		setDateTime(modelE, modelD.getDateTime() + Tendency.RISE_SLIGHT.getTimeDiff());
		modelE.setConclusionClimateFacadeMin(new OutdoorClimate());
		modelE.getConclusionClimateFacadeMin().setTemperature(new BigDecimal("20.2"));

		houseService.calculateTendencies(modelC, modelE);
		assertEquals(Tendency.RISE_SLIGHT, modelE.getConclusionClimateFacadeMin().getTemperatureTendency());

		HouseModel modelF = new HouseModel();
		setDateTime(modelF,
				modelE.getDateTime() + Tendency.EQUAL.getTimeDiff() - Tendency.RISE_SLIGHT.getTimeDiff());
		modelF.setConclusionClimateFacadeMin(new OutdoorClimate());
		modelF.getConclusionClimateFacadeMin().setTemperature(new BigDecimal("20.2"));

		houseService.calculateTendencies(modelE, modelF);
		assertEquals(Tendency.EQUAL, modelF.getConclusionClimateFacadeMin().getTemperatureTendency());

		HouseModel modelG = new HouseModel();
		setDateTime(modelG, modelF.getDateTime() + Tendency.EQUAL.getTimeDiff());
		modelG.setConclusionClimateFacadeMin(new OutdoorClimate());
		modelG.getConclusionClimateFacadeMin().setTemperature(new BigDecimal("20.2"));

		houseService.calculateTendencies(modelF, modelG);
		assertEquals(Tendency.EQUAL, modelG.getConclusionClimateFacadeMin().getTemperatureTendency());
	}

	private void setDateTime(HouseModel model, long dateTime) throws Exception {
		Field fieldDateTime = HouseModel.class.getDeclaredField("dateTime");
		fieldDateTime.setAccessible(true);
		fieldDateTime.setLong(model, dateTime);
	}

}
