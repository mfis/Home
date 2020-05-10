package de.fimatas.home.library.domain.model;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

public class HintsTest {

    private static final long ONE_MINUTE = 1000 * 60;

    private static final long TEN_MINUTES = ONE_MINUTE * 10;

    @Test
    public void test1NewHint() {
        long time = 0;

        Hints h1 = new Hints();
        h1.giveHint(Hint.OPEN_WINDOW, time);
        assertThat(h1.formatAsText(false, false, null).get(0), containsString(Hint.OPEN_WINDOW.getText()));
    }

    @Test
    public void test2NewHintIsNotSolid() {
        long time = 0;

        Hints h1 = new Hints();
        h1.giveHint(Hint.OPEN_WINDOW, time);
        assertThat(h1.formatAsText(true, false, null).size(), is(0));
    }

    @Test
    public void test3HintBecomesSolid() {
        long time = 0;

        Hints h1 = new Hints();
        h1.giveHint(Hint.OPEN_WINDOW, time);

        time += ONE_MINUTE;
        Hints h2 = new Hints();
        h2.overtakeOldHints(h1, time);
        h2.giveHint(Hint.OPEN_WINDOW, time);
        assertThat(h2.formatAsText(true, false, null).size(), is(0));

        time += ONE_MINUTE;
        time += ONE_MINUTE;
        time += TEN_MINUTES;
        Hints h3 = new Hints();
        h3.overtakeOldHints(h2, time);
        h3.giveHint(Hint.OPEN_WINDOW, time);
        assertThat(h3.formatAsText(true, false, null).get(0), containsString(Hint.OPEN_WINDOW.getText()));
    }

    @Test
    public void test4HintBecomesUnSolid() {
        long time = 0;

        Hints h1 = new Hints();
        h1.giveHint(Hint.OPEN_WINDOW, time);

        time += ONE_MINUTE;
        Hints h2 = new Hints();
        h2.overtakeOldHints(h1, time);
        h2.giveHint(Hint.OPEN_WINDOW, time);
        assertThat(h2.formatAsText(true, false, null).size(), is(0));

        time += TEN_MINUTES;
        Hints h3 = new Hints();
        h3.overtakeOldHints(h2, time);
        h3.giveHint(Hint.OPEN_WINDOW, time);
        assertThat(h3.formatAsText(true, false, null).get(0), containsString(Hint.OPEN_WINDOW.getText()));

        time += TEN_MINUTES;
        Hints h4 = new Hints();
        h4.overtakeOldHints(h3, time);
        assertThat(h4.formatAsText(false, false, null).size(), is(0));
        assertThat(h4.formatAsText(true, false, null).get(0), containsString(Hint.OPEN_WINDOW.getText()));

        time += TEN_MINUTES;
        time += TEN_MINUTES;
        Hints h5 = new Hints();
        h5.overtakeOldHints(h4, time);
        assertThat(h5.formatAsText(false, false, null).size(), is(0));
        assertThat(h5.formatAsText(true, false, null).size(), is(0));
    }
}
