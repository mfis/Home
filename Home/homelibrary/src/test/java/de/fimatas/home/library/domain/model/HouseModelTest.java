package de.fimatas.home.library.domain.model;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import de.fimatas.home.library.annotation.EnableHomekit;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;

public class HouseModelTest {

    @Test
    public void testEnableHomekitAnnotation(){

        final Set<Integer> ids = new HashSet<>();
        final Field[] fields = HouseModel.class.getDeclaredFields();
        for(Field field : fields){
            final EnableHomekit annotation = field.getAnnotation(EnableHomekit.class);
            if(annotation!=null){
                assertThat(ids.contains(annotation.accessoryId()), is(false));
                ids.add(annotation.accessoryId());
            }
        }
    }

}
