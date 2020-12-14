package interconnect.ke.api;

public class GraphPatternRecipientSelector extends RecipientSelector {

	private final GraphPattern pattern;

	public GraphPatternRecipientSelector(GraphPattern aPattern) {
		this.pattern = aPattern;
	}

	public GraphPattern getPattern() {
		return pattern;
	}
}
