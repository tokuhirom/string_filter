package me.geso.string_filter;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class StringFilter {
    private final Map<String, Function< Matcher, String>> rules;
    private final Function<String, String> defaultRule;
    private final Pattern regexp;

    public static StringFilterBuilder builder() {
        return new StringFilterBuilder();
    }

    public StringFilter(Map<String, Function<Matcher, String>> rules, Function<String, String> defaultRule) {
        this.rules = rules;
        this.defaultRule = defaultRule;
        this.regexp = Pattern.compile("(" + rules.keySet().stream().collect(Collectors.joining("|")) + ")");
    }

    public String filter(String input) {
        int index = 0;

        ArrayList<String> matchList = new ArrayList<>();
        Matcher m = regexp.matcher(input);

        // Add segments before each match found
        while (m.find()) {
            if (index == 0 && index == m.start() && m.start() == m.end()) {
                // no empty leading substring included for zero-width match
                // at the beginning of the input char sequence.
                continue;
            }
            String match = input.subSequence(index, m.start()).toString();
            matchList.add(defaultRule.apply(match));
            matchList.add(processMatched(m.group()));
            index = m.end();
        }

        // If no match was found, return this
        if (index == 0) {
            return input;
        }

        // Add remaining segment
        matchList.add(defaultRule.apply(input.subSequence(index, input.length()).toString()));

        return matchList.stream().collect(Collectors.joining(""));
    }

    private String processMatched(String input) {
        for (Map.Entry<String, Function<Matcher, String>> rule : rules.entrySet()) {
            // TODO: precompile
            Pattern pattern = Pattern.compile(rule.getKey());
            Matcher matcher = pattern.matcher(input);
            if (matcher.matches()) {
                return rule.getValue().apply(matcher);
            }
        }
        throw new IllegalStateException("Unmatched pattern: '" + input + "'");
    }

    public static class StringFilterBuilder {
        private final Map<String, Function<Matcher, String>> rules;
        private Function<String, String> defaultRule;

        StringFilterBuilder() {
            rules = new LinkedHashMap<>();
            defaultRule = str -> str;
        }

        public StringFilterBuilder addRule(String pattern, Function<Matcher, String> callback) {
            rules.put(pattern, callback);
            return this;
        }

        public StringFilterBuilder defaultRule(Function<String, String> defaultRule) {
            this.defaultRule = defaultRule;
            return this;
        }

        public StringFilter build() {
            return new StringFilter(rules, defaultRule);
        }
    }
}
