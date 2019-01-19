package home.domain.model;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

public class Synonym<T> {

	private final String expression;

	private final T base;

	public Synonym(String expression, T base) {
		this.expression = expression;
		this.base = base;
	}

	public static <T> Set<T> lookup(Class<T> t, List<String> words) {

		Map<T, SynonymMatch> matchingItems = new HashMap<>();
		boolean hasOptionals = false;
		boolean hasMatches = false;
		for (Synonym<T> entry : SynonymeRepository.getSynonymes(t)) {
			SynonymMatch match = Synonym.matches(entry, words);
			if (match == SynonymMatch.OPTIONAL) {
				hasOptionals = true;
			} else if (match == SynonymMatch.YES) {
				hasMatches = true;
			}
			if (match != SynonymMatch.NO && !matchingItems.keySet().contains((entry.getBase()))) {
				matchingItems.put(entry.getBase(), match);
			}
		}

		if (matchingItems.size() > 1 && hasOptionals && hasMatches) {
			removeOptionals(matchingItems);
		}

		return matchingItems.keySet();

	}

	private static <T> void removeOptionals(Map<T, SynonymMatch> matchingItems) {

		Set<T> toRemove = new HashSet<>();
		for (Entry<T, SynonymMatch> entry : matchingItems.entrySet()) {
			if (entry.getValue() == SynonymMatch.OPTIONAL) {
				toRemove.add(entry.getKey());
			}
		}
		for (T item : toRemove) {
			matchingItems.remove(item);
		}
	}

	private static SynonymMatch matches(Synonym<?> synonym, List<String> words) {

		SynonymMatch match = SynonymMatch.NO;
		String[] rawTokens = StringUtils.splitByWholeSeparator(synonym.expression.toLowerCase(),
				StringUtils.SPACE);

		for (String rawToken : rawTokens) {
			boolean isOptional = StringUtils.contains(rawToken, '(');
			if (isOptional) {
				rawToken = StringUtils.remove(rawToken, '(');
				rawToken = StringUtils.remove(rawToken, ')');
			}
			String[] tokens = StringUtils.splitByWholeSeparator(rawToken, "|");
			boolean tokenMatch = false;
			for (String token : tokens) {
				if (words.contains(token)) {
					tokenMatch = true;
					break;
				}
			}
			if (tokenMatch && match != SynonymMatch.OPTIONAL) {
				match = SynonymMatch.YES;
			} else if (isOptional) {
				match = SynonymMatch.OPTIONAL;
			} else {
				match = SynonymMatch.NO;
				break;
			}
		}

		return match;
	}

	public T getBase() {
		return base;
	}

	public String getExpression() {
		return expression;
	}

}
