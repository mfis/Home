package de.fimatas.home.controller.command;

import lombok.Setter;
import org.springframework.util.Assert;

public abstract class AbstractCommand {

    @Setter
    String varName = null;

    public abstract String id();

    @Override
    public String toString() {
        return "AbstractCommand [id()=" + id() + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + id().hashCode();
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        AbstractCommand other = (AbstractCommand) obj;
        Assert.notNull(other.id(), "other.id() is null!");
        return id().equals(id());
    }

}
