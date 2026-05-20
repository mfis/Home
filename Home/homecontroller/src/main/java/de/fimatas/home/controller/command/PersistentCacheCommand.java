package de.fimatas.home.controller.command;

import de.fimatas.home.library.model.PersistentCacheKey;
import lombok.Getter;
import lombok.Setter;
import org.springframework.util.Assert;

public class PersistentCacheCommand extends AbstractCommand {

    @Getter
    @Setter
    private Object instantToWrite;

    public String id() {
        Assert.notNull(varName, "id(): varName is null!");
        return "VAR_" + varName.replace("_", "");
    }

    public PersistentCacheCommand(PersistentCacheKey persistentCacheKey) {
        this.setVarName(persistentCacheKey.name());
    }

    public PersistentCacheCommand(PersistentCacheKey persistentCacheKey, Object value) {
        this.setVarName(persistentCacheKey.name());
        this.setInstantToWrite(value);
    }

    public PersistentCacheKey getPersistentCacheKey() {
        return PersistentCacheKey.valueOf(varName);
    }
}
