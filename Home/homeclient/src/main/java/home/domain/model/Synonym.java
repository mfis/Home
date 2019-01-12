package home.domain.model;

public class Synonym<T> {

	private final String synonymWord;

	private final T base;

	public Synonym(String synonymWord, T base) {
		this.synonymWord = synonymWord;
		this.base = base;
	}

	public T getBase() {
		return base;
	}

	public String getSynonymWord() {
		return synonymWord;
	}

}
