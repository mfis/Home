package de.fimatas.home.client.domain.model;

import de.fimatas.home.library.domain.model.Place;
import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

@Data
public class WidgetGroupView extends View {

    public WidgetGroupView(String viewKey, Place place, Object modelDOA){
        var unreach = modelDOA == null;
        this.setId(viewKey);
        this.setPlaceEnum(place);
        this.setUnreach(Boolean.toString(unreach));
    }

    public WidgetGroupView(String viewKey, Place place){
        this.setId(viewKey);
        this.setPlaceEnum(place);
        this.setUnreach(Boolean.toString(false));
    }

    public boolean isUnreach(){
        return Boolean.parseBoolean(getUnreach());
    }

    private Map<String, View> captionAndValue = new LinkedHashMap<>();
}
