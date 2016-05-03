package me.geso.string_filter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class StringFilter {
    private final List<Rule> rules;
    private final Function<String, String> defaultRule;
    private final Pattern regexp;

    public static StringFilterBuilder builder() {
        return new StringFilterBuilder();
    }

    public StringFilter(List<Rule> rules, Function<String, String> defaultRule) {
        this.rules = rules;
        this.defaultRule = defaultRule;
        this.regexp = Pattern.compile(
                "(" + rules.stream()
                        .map(Rule::getRule)
                        .collect(Collectors.joining("|"))
                        + ")");
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
        for (Rule rule : rules) {
            Matcher matcher = rule.getPattern().matcher(input);
            if (matcher.matches()) {
                return rule.getFilter().apply(matcher);
            }
        }
        throw new IllegalStateException("Unmatched pattern: '" + input + "'");
    }

    private static class Rule {
        private final String rule;
        private final Pattern pattern;
        private final Function<Matcher, String> filter;

        private Rule(String rule, Function<Matcher, String> filter) {
            this.rule = rule;
            this.filter = filter;
            this.pattern = Pattern.compile(rule);
        }

        public String getRule() {
            return rule;
        }

        public Pattern getPattern() {
            return pattern;
        }

        public Function<Matcher, String> getFilter() {
            return filter;
        }
    }

    public static class StringFilterBuilder {
        private final List<Rule> rules;
        private Function<String, String> defaultRule;

        StringFilterBuilder() {
            rules = new ArrayList<>();
            defaultRule = str -> str;
        }

        public StringFilterBuilder addRule(String pattern, Function<Matcher, String> filter) {
            rules.add(new Rule(pattern, filter));
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
