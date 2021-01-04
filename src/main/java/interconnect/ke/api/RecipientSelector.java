package interconnect.ke.api;

import java.net.URI;

/**
 * An object of this class is responsible for deciding which knowledge base(s)
 * is/are valid recipient(s) of a message.
 */
public class RecipientSelector {
  private final GraphPattern pattern;

	public RecipientSelector() {
		this.pattern = new GraphPattern("TODO");
	}
	
	public RecipientSelector(GraphPattern aPattern) {
		this.pattern = aPattern;
  }
  
  public RecipientSelector(URI knowledgeBase) {
		this.pattern = new GraphPattern("TODO");
	}

	public GraphPattern getPattern() {
		return pattern;
	}
}
