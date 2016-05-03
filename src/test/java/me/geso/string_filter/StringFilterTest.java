package me.geso.string_filter;

import org.apache.commons.lang3.StringEscapeUtils;
import org.junit.Test;

import static org.apache.commons.lang3.StringEscapeUtils.escapeHtml4;
import static org.junit.Assert.assertEquals;

/**
 * Created by tokuhirom on 5/3/16.
 */
public class StringFilterTest {
    @Test
    public void filterNop() throws Exception {
        StringFilter filter = StringFilter.builder()
                .build();
        assertEquals(filter.filter("hoge"), "hoge");
    }

    @Test
    public void filterSimple() throws Exception {
        StringFilter filter = StringFilter.builder()
                .addRule("http://[A-Za-z0-9_\\-\\~\\.\\%\\?\\#\\@/]+", m -> {
                    String url = m.group(0);

                    return "<a href=\""
                            + escapeHtml4(url) +
                            "\">" + escapeHtml4(url) + "</a>";
                })
                .addRule("((?:^|\\s)@)([A-Za-z0-9_]+)", m -> {
                    String prefix = m.group(1);
                    String user = m.group(2);

                    return prefix + "<a href=\"http://twitter.com/"
                            + escapeHtml4(user) +
                            "\">" + escapeHtml4(user) + "</a>";
                })
                .addRule("((?:^|\\s))(#[A-Za-z0-9_]+)", m -> {
                    String prefix = m.group(1);
                    String hashtag = m.group(2);
                    return prefix + "<a href=\"http://twitter.com/search?q="
                            + escapeHtml4(hashtag)
                            + "\">"
                            + escapeHtml4(hashtag)
                            + "</a>";
                })
                .defaultRule(StringEscapeUtils::escapeHtml4)
                .build();

        assertEquals("aaa @<a href=\"http://twitter.com/hoge\">hoge</a> bbb", filter.filter("aaa @hoge bbb"));
        assertEquals("aaa <a href=\"http://twitter.com/search?q=#hoge\">#hoge</a> bbb&gt;", filter.filter("aaa #hoge bbb>"));
        assertEquals("&lt; <a href=\"http://mixi.jp/\">http://mixi.jp/</a> bbb&gt;", filter.filter("< http://mixi.jp/ bbb>"));
    }

}