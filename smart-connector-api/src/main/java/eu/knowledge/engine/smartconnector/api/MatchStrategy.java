package eu.knowledge.engine.smartconnector.api;

import java.util.EnumSet;

import eu.knowledge.engine.reasoner.BaseRule.MatchFlag;

/**
 * the match strategy determines how many matches will be found between graph
 * patterns. This is a very costly operation (especially with bigger (10+)
 * triples per graph pattern. For some use cases it suffices to use ENTRY_LEVEL
 * matching which is much faster, while others require SUPREME_LEVEL, which is
 * extremely slow and often throws out of memory exceptions.
 */
public enum MatchStrategy {
	/**
	 * Fastest but finds least matches between rules. Matches cannot consist of
	 * multiple rules and should also fully cover the target graph pattern.
	 */
	ENTRY_LEVEL,

	/**
	 * Faster but finds only limited matches between rules. Matches can consist of
	 * multiple rules, but should still fully cover the target graph pattern which
	 * means detecting knowledge gaps is not supported.
	 */
	NORMAL_LEVEL,

	/**
	 * Slower but finds more matches between rules. Matches can consist of multiple
	 * rules and transitive rules (that refer to themselves) are supported as well,
	 * but matches should still fully cover the target graph pattern which means
	 * detecting knowledge gaps is not supported.
	 */
	ADVANCED_LEVEL,

	/**
	 * Even slower but finds almost all matches between rules. Matches can consist
	 * of multiple rules and transitive rules (that refer to themselves) are
	 * supported as well and matches do not need to fully cover the target graph
	 * pattern and this means knowledge gap detection is supported.
	 */
	ULTRA_LEVEL,

	/**
	 * Slowest, but finds all possible matches between rules. Matches can consist of
	 * multiple rules and transitive rules (that refer to themselves) are supported.
	 * Matches do not need to fully cover the target graph pattern and this means
	 * knowledge gap detection is supported. This level also finds matches that are
	 * already encompassed by other matches.
	 */
	SUPREME_LEVEL;

	public EnumSet<MatchFlag> toConfig(boolean antecedentOfTarget) {
		EnumSet<MatchFlag> config;
		switch (this) {
		case ENTRY_LEVEL:
			config = EnumSet.of(MatchFlag.ONE_TO_ONE, MatchFlag.ONLY_BIGGEST, MatchFlag.SINGLE_RULE,
					MatchFlag.FULLY_COVERED);
			break;
		case NORMAL_LEVEL:
			config = EnumSet.of(MatchFlag.ONE_TO_ONE, MatchFlag.ONLY_BIGGEST);

			// disable fully covered when matching consequents.
			if (antecedentOfTarget)
				config.add(MatchFlag.FULLY_COVERED);
			break;
		case ADVANCED_LEVEL:
			config = EnumSet.of(MatchFlag.ONLY_BIGGEST);

			// disable fully covered when matching consequents.
			if (antecedentOfTarget)
				config.add(MatchFlag.FULLY_COVERED);
			break;
		case ULTRA_LEVEL:
			config = EnumSet.of(MatchFlag.ONLY_BIGGEST);
			break;
		case SUPREME_LEVEL:
			config = EnumSet.noneOf(MatchFlag.class);
			break;
		default:
			config = EnumSet.noneOf(MatchFlag.class);
		}
		return config;
	}
}