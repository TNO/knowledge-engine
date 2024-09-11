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
	SUPREME_LEVEL, ULTRA_LEVEL, ADVANCED_LEVEL, NORMAL_LEVEL, ENTRY_LEVEL;

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