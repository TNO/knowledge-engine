package eu.knowledge.engine.reasoner;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.apache.jena.graph.Node;

import org.apache.jena.sparql.core.Var;

import eu.knowledge.engine.reasoner.BindingSetHandler;
import eu.knowledge.engine.reasoner.Rule;
import eu.knowledge.engine.reasoner.api.Binding;
import eu.knowledge.engine.reasoner.api.BindingSet;
import eu.knowledge.engine.reasoner.api.TriplePattern;

public class Rule {

	public static enum MatchStrategy {
		FIND_ALL_MATCHES, FIND_ONLY_BIGGEST_MATCHES, FIND_ONLY_FULL_MATCHES
	}

	public Set<TriplePattern> antecedent;
	public Set<TriplePattern> consequent;

	public BindingSetHandler bindingSetHandler;

	public Rule(Set<TriplePattern> anAntecedent, Set<TriplePattern> aConsequent, BindingSetHandler aBindingSetHandler) {
		this.antecedent = anAntecedent;
		this.consequent = aConsequent;
		bindingSetHandler = aBindingSetHandler;
	}

	public Rule(Set<TriplePattern> anAntecedent, Set<TriplePattern> aConsequent) {
		this.antecedent = anAntecedent;
		this.consequent = aConsequent;
		bindingSetHandler = new BindingSetHandler() {
			@Override
			public CompletableFuture<BindingSet> handle(BindingSet bs) {

				BindingSet newBS = new BindingSet();

				Binding newB;

				Set<Var> vars = Rule.this.getVars(Rule.this.consequent);
				for (Binding b : bs) {
					newB = new Binding();
					for (Var v : vars) {
						if (b.containsKey(v)) {
							newB.put(v, b.get(v));
						} else {
							throw new IllegalArgumentException(
									"Not all variable in the consequent are available in the antecedent of the rule. This type of rule should use a custom BindingHandler.");
						}
					}
					newBS.add(newB);
				}

				CompletableFuture<BindingSet> future = new CompletableFuture<>();
				future.complete(newBS);
				return future;
			}
		};
	}

	public Set<Var> getVars(Set<TriplePattern> aPattern) {
		Set<Var> vars = new HashSet<Var>();
		for (TriplePattern t : aPattern) {
			vars.addAll(t.getVariables());
		}
		return vars;
	}

	public Set<Match> consequentMatches(Set<TriplePattern> anAntecedent, MatchStrategy aMatchStrategy) {
		if (!this.consequent.isEmpty())
			return matches(anAntecedent, this.consequent, aMatchStrategy);
		return new HashSet<>();
	}

	public Set<Match> antecedentMatches(Set<TriplePattern> aConsequent, MatchStrategy aMatchStrategy) {
		if (!this.antecedent.isEmpty())
			return matches(aConsequent, this.antecedent, aMatchStrategy);
		return new HashSet<>();
	}

	public BindingSetHandler getBindingSetHandler() {
		return bindingSetHandler;
	}

	/**
	 * FInd the biggest matches bewteen two graph patterns.
	 * 
	 * @param aFirstPattern
	 * @param aSecondPattern
	 * @param aMatchStrategy
	 * @return
	 */
	private Set<Match> matches(Set<TriplePattern> aFirstPattern, Set<TriplePattern> aSecondPattern,
			MatchStrategy aMatchStrategy) {

		assert aFirstPattern != null;
		assert aSecondPattern != null;
		assert !aFirstPattern.isEmpty();
		assert !aSecondPattern.isEmpty();

		long start = System.currentTimeMillis();

		// first find all triples in the consequent that match each triple in the
		// antecedent
		List<Match> allMatches = new LinkedList<>();

		Map<TriplePattern, Set<Match>> matchesPerTriple = new HashMap<>();
		Set<Match> findMatches;
		for (TriplePattern anteTriple : aFirstPattern) {
			// find all possible matches of the current antecedent triple in the consequent
			findMatches = findMatches(anteTriple, aSecondPattern);
			if (!findMatches.isEmpty())
				matchesPerTriple.put(anteTriple, findMatches);
		}

		// if not every triple pattern can be matched, we stop the process if we require
		// a full match.
		if (aMatchStrategy.equals(MatchStrategy.FIND_ONLY_FULL_MATCHES)
				&& matchesPerTriple.keySet().size() < aFirstPattern.size())
			return new HashSet<>(allMatches);

		// next, correctly combine all found matches
		Match mergedMatch = null;
		Set<Match> matches = null;
		List<Match> newMatches = null, removeMatches = null;

		Iterator<Map.Entry<TriplePattern, Set<Match>>> matchIter = matchesPerTriple.entrySet().iterator();

		// always add first matches
		if (matchIter.hasNext()) {
			allMatches.addAll(matchIter.next().getValue());
		}

		while (matchIter.hasNext()) {
			Map.Entry<TriplePattern, Set<Match>> entry = matchIter.next();

			// keep a set of new matches, so we can add them at the end of this loop
			newMatches = new LinkedList<>();
			removeMatches = new LinkedList<>();

			matches = entry.getValue();
			assert matches != null;

			for (Match m1 : matches) {
				// check if we need to merge with existing matches

				for (Match m2 : allMatches) {
					mergedMatch = m2.merge(m1);
					if (mergedMatch != null) {
						newMatches.add(mergedMatch);
						removeMatches.add(m2);
					}
				}
			}

			if (aMatchStrategy.equals(MatchStrategy.FIND_ALL_MATCHES)
					|| (aMatchStrategy.equals(MatchStrategy.FIND_ONLY_BIGGEST_MATCHES) && newMatches.isEmpty())) {
				newMatches.addAll(matches);
			}

			if (!aMatchStrategy.equals(MatchStrategy.FIND_ALL_MATCHES))
				allMatches.removeAll(removeMatches);

			allMatches.addAll(newMatches);

			long end = System.currentTimeMillis();
			newMatches = null;

		}
		assert allMatches != null;

		long finalEnd = System.currentTimeMillis();

		return new HashSet<>(allMatches);
	}

	private Set<Match> findMatches(TriplePattern antecedent, Set<TriplePattern> consequent) {

		assert consequent != null;
		assert antecedent != null;
		assert !consequent.isEmpty();

		Set<Match> matchingTriplePatterns = new HashSet<>();
		Map<Node, Node> map;
		for (TriplePattern tp : consequent) {
			map = antecedent.findMatches(tp);
			if (map != null) {
				matchingTriplePatterns.add(new Match(antecedent, tp, map));
			}
		}

		assert matchingTriplePatterns != null;
		return matchingTriplePatterns;
	}

	@Override
	public String toString() {
		return "Rule [antecedent=" + antecedent + ", consequent=" + consequent + "]";
	}

	public Set<Var> getVars() {

		Set<Var> vars = new HashSet<>();
		;
		if (this.antecedent != null)
			vars.addAll(this.getVars(this.antecedent));

		if (this.consequent != null)
			vars.addAll(this.getVars(this.consequent));

		return vars;
	}

}
