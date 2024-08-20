package de.fimatas.home.controller.domain.service;

import static org.junit.Assert.assertEquals;

import java.math.BigDecimal;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnitRunner;

import de.fimatas.home.library.domain.model.HouseModel;
import de.fimatas.home.library.domain.model.OutdoorClimate;
import de.fimatas.home.library.domain.model.RoomClimate;
import de.fimatas.home.library.domain.model.Tendency;
import de.fimatas.home.library.domain.model.ValueWithTendency;

@RunWith(MockitoJUnitRunner.class)
public class HouseServiceTest {

    @InjectMocks
    private HouseService houseService;

    @Test
    public void testCalculateTendenciesNoReference() {

        HouseModel newModel = new HouseModel();
        newModel.setClimateBathRoom(new RoomClimate());
        newModel.getClimateBathRoom().setTemperature(new ValueWithTendency<>(new BigDecimal("20")));

        houseService.calculateTendencies(null, newModel);

        assertEquals(Tendency.EQUAL, newModel.getClimateBathRoom().getTemperature().getTendency());
    }

    @Test
    public void testCalculateTendenciesEqualsExact() {

        HouseModel oldModel = new HouseModel();
        oldModel.setClimateBathRoom(new RoomClimate());
        oldModel.getClimateBathRoom().setTemperature(new ValueWithTendency<>());
        oldModel.getClimateBathRoom().getTemperature().setReferenceValue(new BigDecimal("20"));

        HouseModel newModel = new HouseModel();
        newModel.setClimateBathRoom(new RoomClimate());
        newModel.getClimateBathRoom().setTemperature(new ValueWithTendency<>(new BigDecimal("20")));

        houseService.calculateTendencies(oldModel, newModel);

        assertEquals(Tendency.EQUAL, newModel.getClimateBathRoom().getTemperature().getTendency());
    }

    @Test
    public void testCalculateTendenciesMinimalVariancePlus() {

        HouseModel oldModel = new HouseModel();
        oldModel.setConclusionClimateFacadeMin(new OutdoorClimate());
        oldModel.getConclusionClimateFacadeMin().setTemperature(new ValueWithTendency<>());
        oldModel.getConclusionClimateFacadeMin().getTemperature().setReferenceValue(new BigDecimal("20"));

        HouseModel newModel = new HouseModel();
        newModel.setConclusionClimateFacadeMin(new OutdoorClimate());
        newModel.getConclusionClimateFacadeMin().setTemperature(new ValueWithTendency<>(new BigDecimal("20.1")));

        houseService.calculateTendencies(oldModel, newModel);

        assertEquals(Tendency.EQUAL, newModel.getConclusionClimateFacadeMin().getTemperature().getTendency());
    }

    @Test
    public void testCalculateTendenciesMinimalVarianceMinus() {

        HouseModel oldModel = new HouseModel();
        oldModel.setConclusionClimateFacadeMin(new OutdoorClimate());
        oldModel.getConclusionClimateFacadeMin().setTemperature(new ValueWithTendency<>());
        oldModel.getConclusionClimateFacadeMin().getTemperature().setReferenceValue(new BigDecimal("20"));

        HouseModel newModel = new HouseModel();
        newModel.setConclusionClimateFacadeMin(new OutdoorClimate());
        newModel.getConclusionClimateFacadeMin().setTemperature(new ValueWithTendency<>(new BigDecimal("19.9")));

        houseService.calculateTendencies(oldModel, newModel);

        assertEquals(Tendency.EQUAL, newModel.getConclusionClimateFacadeMin().getTemperature().getTendency());
    }

    @Test
    public void testCalculateTendenciesBoundaryVariancePlus() {

        HouseModel oldModel = new HouseModel();
        oldModel.setConclusionClimateFacadeMin(new OutdoorClimate());
        oldModel.getConclusionClimateFacadeMin().setTemperature(new ValueWithTendency<>());
        oldModel.getConclusionClimateFacadeMin().getTemperature().setReferenceValue(new BigDecimal("20"));

        HouseModel newModel = new HouseModel();
        newModel.setConclusionClimateFacadeMin(new OutdoorClimate());
        newModel.getConclusionClimateFacadeMin().setTemperature(new ValueWithTendency<>(new BigDecimal("20.2")));

        houseService.calculateTendencies(oldModel, newModel);

        assertEquals(Tendency.RISE, newModel.getConclusionClimateFacadeMin().getTemperature().getTendency());
    }

    @Test
    public void testCalculateTendenciesBoundaryVarianceMinus() {

        HouseModel oldModel = new HouseModel();
        oldModel.setConclusionClimateFacadeMin(new OutdoorClimate());
        oldModel.getConclusionClimateFacadeMin().setTemperature(new ValueWithTendency<>());
        oldModel.getConclusionClimateFacadeMin().getTemperature().setReferenceValue(new BigDecimal("20"));

        HouseModel newModel = new HouseModel();
        newModel.setConclusionClimateFacadeMin(new OutdoorClimate());
        newModel.getConclusionClimateFacadeMin().setTemperature(new ValueWithTendency<>(new BigDecimal("19.8")));

        houseService.calculateTendencies(oldModel, newModel);

        assertEquals(Tendency.FALL, newModel.getConclusionClimateFacadeMin().getTemperature().getTendency());
    }

    @Test
    public void testCalculateTendenciesCourseRise() {

        HouseModel modelA = new HouseModel();
        setDateTime(modelA, 0);
        modelA.setConclusionClimateFacadeMin(new OutdoorClimate());
        modelA.getConclusionClimateFacadeMin().setTemperature(new ValueWithTendency<>(new BigDecimal("20")));

        houseService.calculateTendencies(null, modelA);
        assertEquals(Tendency.EQUAL, modelA.getConclusionClimateFacadeMin().getTemperature().getTendency());

        HouseModel modelB = new HouseModel();
        setDateTime(modelB, modelA.getTimestamp() + Tendency.Constants.ONE_MINUTE);
        modelB.setConclusionClimateFacadeMin(new OutdoorClimate());
        modelB.getConclusionClimateFacadeMin().setTemperature(new ValueWithTendency<>(new BigDecimal("20.1")));

        houseService.calculateTendencies(modelA, modelB);
        assertEquals(Tendency.EQUAL, modelB.getConclusionClimateFacadeMin().getTemperature().getTendency());

        HouseModel modelC = new HouseModel();
        setDateTime(modelC, modelB.getTimestamp() + Tendency.Constants.ONE_MINUTE);
        modelC.setConclusionClimateFacadeMin(new OutdoorClimate());
        modelC.getConclusionClimateFacadeMin().setTemperature(new ValueWithTendency<>(new BigDecimal("20.2")));

        houseService.calculateTendencies(modelB, modelC);
        assertEquals(Tendency.RISE, modelC.getConclusionClimateFacadeMin().getTemperature().getTendency());

        HouseModel modelD = new HouseModel();
        setDateTime(modelD, modelC.getTimestamp() + Tendency.Constants.ONE_MINUTE);
        modelD.setConclusionClimateFacadeMin(new OutdoorClimate());
        modelD.getConclusionClimateFacadeMin().setTemperature(new ValueWithTendency<>(new BigDecimal("20.2")));

        houseService.calculateTendencies(modelC, modelD);
        assertEquals(Tendency.RISE, modelD.getConclusionClimateFacadeMin().getTemperature().getTendency());

        HouseModel modelE = new HouseModel();
        setDateTime(modelE, modelD.getTimestamp() + Tendency.RISE_SLIGHT.getTimeDiff());
        modelE.setConclusionClimateFacadeMin(new OutdoorClimate());
        modelE.getConclusionClimateFacadeMin().setTemperature(new ValueWithTendency<>(new BigDecimal("20.2")));

        houseService.calculateTendencies(modelC, modelE);
        assertEquals(Tendency.RISE_SLIGHT, modelE.getConclusionClimateFacadeMin().getTemperature().getTendency());

        HouseModel modelF = new HouseModel();
        setDateTime(modelF, modelE.getTimestamp() + Tendency.EQUAL.getTimeDiff() - Tendency.RISE_SLIGHT.getTimeDiff());
        modelF.setConclusionClimateFacadeMin(new OutdoorClimate());
        modelF.getConclusionClimateFacadeMin().setTemperature(new ValueWithTendency<>(new BigDecimal("20.2")));

        houseService.calculateTendencies(modelE, modelF);
        assertEquals(Tendency.EQUAL, modelF.getConclusionClimateFacadeMin().getTemperature().getTendency());

        HouseModel modelG = new HouseModel();
        setDateTime(modelG, modelF.getTimestamp() + Tendency.EQUAL.getTimeDiff());
        modelG.setConclusionClimateFacadeMin(new OutdoorClimate());
        modelG.getConclusionClimateFacadeMin().setTemperature(new ValueWithTendency<>(new BigDecimal("20.2")));

        houseService.calculateTendencies(modelF, modelG);
        assertEquals(Tendency.EQUAL, modelG.getConclusionClimateFacadeMin().getTemperature().getTendency());
    }

    @Test
    public void testCalculateHumidityTendencies() {

        HouseModel modelA = new HouseModel();
        setDateTime(modelA, 0);
        modelA.setConclusionClimateFacadeMin(new OutdoorClimate());
        modelA.getConclusionClimateFacadeMin().setHumidity(new ValueWithTendency<>(new BigDecimal("50")));

        houseService.calculateTendencies(null, modelA);
        assertEquals(Tendency.EQUAL, modelA.getConclusionClimateFacadeMin().getHumidity().getTendency());

        HouseModel modelB = new HouseModel();
        setDateTime(modelB, modelA.getTimestamp() + Tendency.Constants.ONE_MINUTE);
        modelB.setConclusionClimateFacadeMin(new OutdoorClimate());
        modelB.getConclusionClimateFacadeMin().setHumidity(new ValueWithTendency<>(new BigDecimal("51")));

        houseService.calculateTendencies(modelA, modelB);
        assertEquals(Tendency.EQUAL, modelB.getConclusionClimateFacadeMin().getHumidity().getTendency());

        HouseModel modelC = new HouseModel();
        setDateTime(modelC, modelB.getTimestamp() + Tendency.Constants.ONE_MINUTE);
        modelC.setConclusionClimateFacadeMin(new OutdoorClimate());
        modelC.getConclusionClimateFacadeMin().setHumidity(new ValueWithTendency<>(new BigDecimal("52")));

        houseService.calculateTendencies(modelB, modelC);
        assertEquals(Tendency.RISE, modelC.getConclusionClimateFacadeMin().getHumidity().getTendency());

        HouseModel modelD = new HouseModel();
        setDateTime(modelD, modelC.getTimestamp() + Tendency.Constants.ONE_MINUTE);
        modelD.setConclusionClimateFacadeMin(new OutdoorClimate());
        modelD.getConclusionClimateFacadeMin().setHumidity(new ValueWithTendency<>(new BigDecimal("52")));

        houseService.calculateTendencies(modelC, modelD);
        assertEquals(Tendency.RISE, modelD.getConclusionClimateFacadeMin().getHumidity().getTendency());

        HouseModel modelE = new HouseModel();
        setDateTime(modelE, modelD.getTimestamp() + Tendency.RISE_SLIGHT.getTimeDiff());
        modelE.setConclusionClimateFacadeMin(new OutdoorClimate());
        modelE.getConclusionClimateFacadeMin().setHumidity(new ValueWithTendency<>(new BigDecimal("52")));

        houseService.calculateTendencies(modelC, modelE);
        assertEquals(Tendency.RISE_SLIGHT, modelE.getConclusionClimateFacadeMin().getHumidity().getTendency());

        HouseModel modelF = new HouseModel();
        setDateTime(modelF, modelE.getTimestamp() + Tendency.EQUAL.getTimeDiff() - Tendency.RISE_SLIGHT.getTimeDiff());
        modelF.setConclusionClimateFacadeMin(new OutdoorClimate());
        modelF.getConclusionClimateFacadeMin().setHumidity(new ValueWithTendency<>(new BigDecimal("52")));

        houseService.calculateTendencies(modelE, modelF);
        assertEquals(Tendency.EQUAL, modelF.getConclusionClimateFacadeMin().getHumidity().getTendency());

        HouseModel modelG = new HouseModel();
        setDateTime(modelG, modelF.getTimestamp() + Tendency.EQUAL.getTimeDiff());
        modelG.setConclusionClimateFacadeMin(new OutdoorClimate());
        modelG.getConclusionClimateFacadeMin().setHumidity(new ValueWithTendency<>(new BigDecimal("52")));

        houseService.calculateTendencies(modelF, modelG);
        assertEquals(Tendency.EQUAL, modelG.getConclusionClimateFacadeMin().getHumidity().getTendency());
    }

    @Test
    public void testCalculateHumidityTendenciesLongRise() {

        HouseModel modelA = new HouseModel();
        setDateTime(modelA, 0);
        modelA.setConclusionClimateFacadeMin(new OutdoorClimate());
        modelA.getConclusionClimateFacadeMin().setHumidity(new ValueWithTendency<>(new BigDecimal("50")));

        houseService.calculateTendencies(null, modelA);
        assertEquals(Tendency.EQUAL, modelA.getConclusionClimateFacadeMin().getHumidity().getTendency());

        HouseModel modelB = new HouseModel();
        setDateTime(modelB, modelA.getTimestamp() + Tendency.Constants.ONE_MINUTE);
        modelB.setConclusionClimateFacadeMin(new OutdoorClimate());
        modelB.getConclusionClimateFacadeMin().setHumidity(new ValueWithTendency<>(new BigDecimal("51")));

        houseService.calculateTendencies(modelA, modelB);
        assertEquals(Tendency.EQUAL, modelB.getConclusionClimateFacadeMin().getHumidity().getTendency());

        HouseModel modelC = new HouseModel();
        setDateTime(modelC, modelB.getTimestamp() + Tendency.Constants.ONE_MINUTE);
        modelC.setConclusionClimateFacadeMin(new OutdoorClimate());
        modelC.getConclusionClimateFacadeMin().setHumidity(new ValueWithTendency<>(new BigDecimal("52")));

        houseService.calculateTendencies(modelB, modelC);
        assertEquals(Tendency.RISE, modelC.getConclusionClimateFacadeMin().getHumidity().getTendency());

        HouseModel modelD = new HouseModel();
        setDateTime(modelD, modelC.getTimestamp() + Tendency.Constants.ONE_MINUTE);
        modelD.setConclusionClimateFacadeMin(new OutdoorClimate());
        modelD.getConclusionClimateFacadeMin().setHumidity(new ValueWithTendency<>(new BigDecimal("52")));

        houseService.calculateTendencies(modelC, modelD);
        assertEquals(Tendency.RISE, modelD.getConclusionClimateFacadeMin().getHumidity().getTendency());

        HouseModel modelE = new HouseModel();
        setDateTime(modelE, modelD.getTimestamp() + Tendency.RISE_SLIGHT.getTimeDiff());
        modelE.setConclusionClimateFacadeMin(new OutdoorClimate());
        modelE.getConclusionClimateFacadeMin().setHumidity(new ValueWithTendency<>(new BigDecimal("52")));

        houseService.calculateTendencies(modelC, modelE);
        assertEquals(Tendency.RISE_SLIGHT, modelE.getConclusionClimateFacadeMin().getHumidity().getTendency());

        HouseModel modelF = new HouseModel();
        setDateTime(modelF, modelE.getTimestamp() + Tendency.EQUAL.getTimeDiff() - Tendency.RISE_SLIGHT.getTimeDiff());
        modelF.setConclusionClimateFacadeMin(new OutdoorClimate());
        modelF.getConclusionClimateFacadeMin().setHumidity(new ValueWithTendency<>(new BigDecimal("52")));

        houseService.calculateTendencies(modelE, modelF);
        assertEquals(Tendency.EQUAL, modelF.getConclusionClimateFacadeMin().getHumidity().getTendency());

        HouseModel modelG = new HouseModel();
        setDateTime(modelG, modelF.getTimestamp() + Tendency.EQUAL.getTimeDiff());
        modelG.setConclusionClimateFacadeMin(new OutdoorClimate());
        modelG.getConclusionClimateFacadeMin().setHumidity(new ValueWithTendency<>(new BigDecimal("52")));

        houseService.calculateTendencies(modelF, modelG);
        assertEquals(Tendency.EQUAL, modelG.getConclusionClimateFacadeMin().getHumidity().getTendency());
    }

    private void setDateTime(HouseModel model, long dateTime) {
        model.setTimestamp(dateTime);
    }

}
