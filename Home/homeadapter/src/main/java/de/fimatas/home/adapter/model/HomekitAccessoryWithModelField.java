package de.fimatas.home.adapter.model;

import de.fimatas.home.library.dao.ModelObjectDAO;
import de.fimatas.home.library.domain.model.AbstractDeviceModel;
import de.fimatas.home.library.domain.model.HouseModel;
import io.github.hapjava.accessories.HomekitAccessory;
import io.github.hapjava.characteristics.HomekitCharacteristicChangeCallback;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.apachecommons.CommonsLog;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

@CommonsLog
public abstract class HomekitAccessoryWithModelField implements HomekitAccessory {

    @Setter protected String modelFieldName;
    @Setter protected int accessoryId;
    private HomekitCharacteristicChangeCallback callback = null;
    private Object updateCheckCompareValue = null;
    @Getter @Setter private boolean isAddedToBridge = false;

    @Override
    public int getId() {
        return accessoryId;
    }

    @Override
    public CompletableFuture<String> getSerialNumber() {
        return CompletableFuture.completedFuture("S" + accessoryId);
    }

    @Override
    public abstract CompletableFuture<String> getModel();

    @Override
    public CompletableFuture<String> getManufacturer() {
        return CompletableFuture.completedFuture("fimatas.de");
    }

    @Override
    public CompletableFuture<String> getFirmwareRevision() {
        return CompletableFuture.completedFuture("1.0");
    }

    @Override
    public void identify() {}

    protected void subscribe(HomekitCharacteristicChangeCallback homekitCharacteristicChangeCallback) {
        callback = homekitCharacteristicChangeCallback;
    }

    protected void unsubscribe() {
        callback = null;
    }

    protected abstract Object actualValue();

    public void checkValueUpdate(){
        if (!Objects.equals(updateCheckCompareValue, actualValue())) {
            if(callback != null){
                callback.changed();
            }
        }
        updateCheckCompareValue = actualValue();
    }

    protected AbstractDeviceModel lookupDeviceModel(boolean ignoringAge){
        HouseModel model = ignoringAge?
                ModelObjectDAO.getInstance().readHouseModelIgnoringAge():ModelObjectDAO.getInstance().readHouseModel();
        if(model==null){
            return null;
        }else{
            return model.lookupField(modelFieldName, AbstractDeviceModel.class);
        }
    }

    protected String lookupPlaceName(){
        final AbstractDeviceModel adm = lookupDeviceModel(true);
        HouseModel model = ModelObjectDAO.getInstance().readHouseModelIgnoringAge();
        if(model.getPlaceSubtitles().containsKey(adm.getDevice().getPlace())){
            return adm.getDevice().getPlace().getPlaceName() + " " + model.getPlaceSubtitles().get(adm.getDevice().getPlace());
        }else{
            return adm.getDevice().getPlace().getPlaceName();
        }
    }
}
