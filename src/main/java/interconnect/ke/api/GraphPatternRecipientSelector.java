package interconnect.ke.api;

/**
 * This is a RecipientSelector that selects recipient knowledge bases based on a
 * graph pattern that they should match to.
 */
public class GraphPatternRecipientSelector extends RecipientSelector {

	private final GraphPattern pattern;

	public GraphPatternRecipientSelector(GraphPattern aPattern) {
		this.pattern = aPattern;
	}

	public GraphPattern getPattern() {
		return pattern;
	}
}
