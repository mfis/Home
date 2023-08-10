package de.fimatas.home.controller.dao;

import de.fimatas.home.controller.model.LiveActivityModel;
import lombok.Getter;
import java.util.*;

@Getter
public class LiveActivityDAO {

    private static LiveActivityDAO instance;

    private LiveActivityDAO() {
        super();
    }

    public static synchronized LiveActivityDAO getInstance() {
        if (instance == null) {
            instance = new LiveActivityDAO();
        }
        return instance;
    }

    private final Map<String, LiveActivityModel> activeLiveActivities = new HashMap<>();
}
