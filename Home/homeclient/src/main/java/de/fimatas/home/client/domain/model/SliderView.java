package de.fimatas.home.client.domain.model;

import lombok.Data;

import java.util.LinkedList;
import java.util.List;

@Data
public class SliderView extends View {

    private String label = "";

    private String placeSubtitle = "";

    private String linkUpdate = "#";

    private String numericValue = "";

}
