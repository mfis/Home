package home.domain.model;

public class Synonym<T> {

	private final String synonym;

	private final T base;

	public Synonym(String synonym, T base) {
		this.synonym = synonym;
		this.base = base;
	}

	public T getBase() {
		return base;
	}

	public String getSynonym() {
		return synonym;
	}

}
