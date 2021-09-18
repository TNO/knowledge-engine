package eu.interconnectproject.knowledge_engine.reasonerprototype;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import eu.interconnectproject.knowledge_engine.reasonerprototype.api.Triple;
import eu.interconnectproject.knowledge_engine.reasonerprototype.api.Triple.Value;

public class RuleAlt {

	public Set<Triple> antecedent;
	public Set<Triple> consequent;

	public RuleAlt(Set<Triple> anAntecedent, Set<Triple> aConsequent) {
		this.antecedent = anAntecedent;
		this.consequent = aConsequent;
	}

	/**
	 * Should match the biggest possible overlap.
	 * 
	 * @param toBeMatched
	 * @return
	 */
	public Set<Map<Triple, Triple>> findMatchesWithConsequent(Set<Triple> toBeMatchedGP) {

		List<Triple> consequentList = new ArrayList<>(consequent);
		List<Triple> toBeMatchedList = new ArrayList<>(toBeMatchedGP);

		boolean[][] m = getMatrix(consequentList, toBeMatchedList);
		printMatrix(m);

		Set<Map<Integer, Integer>> matches = findDiagonals(m);

		return null;
	}

	public Set<Map<Integer, Integer>> findDiagonals(boolean[][] m) {

		Set<Map<Integer, Integer>> matches = new HashSet<>();

		int total = m.length * m[0].length;

		for (int p = 0; p < total; p++) {

			int i = p / m[0].length; // col
			int j = p % m[0].length; // row

			if (j == 0) {
				System.out.println();
			}

			System.out.print(i + "," + j + "\t");

			if (m[i][j]) {
				// see if i+1,j-1 or i+1,j+1 is also true
				// see if i+2,j-2 or i+2,j+2 is also true
				// see if i+3,j-3 or i+3,j+3 is also true

				boolean continueLeft = true, continueRight = true;

				Map<Integer, Integer> leftRange = new HashMap<>();
				leftRange.put(i, j);
				Map<Integer, Integer> rightRange = new HashMap<>();
				rightRange.put(i, j);
				for (int d = 1; d < m[0].length; d++) {

					if ()
					
				}
				
				//add ranges to matches
			}

		}
		System.out.println();
		return matches;
	}

	private void printMatrix(boolean[][] m) {
		for (int i = 0; i < m.length; i++) {
			for (int j = 0; j < m[0].length; j++) {
				System.out.print(m[i][j] + "\t");
			}
			System.out.println();
		}
	}

	public Set<Map<Triple, Triple>> findMatches(Set<Triple> toBeMatchedGP) {
		Stack<Context> matchingStack = new Stack<>();

		Context root = new Context(toBeMatchedGP, consequent);
		matchingStack.push(root);
		Context c;
		while (!matchingStack.empty()) {

			c = matchingStack.peek();
			if (c.hasNextConsequent()) {
				Map<Value, Value> map = c.consequentCurrent.matchesWithSubstitutionMap(c.toBeMatchedCurrent);

				// push a new reduced context onto stack
				if (map != null) {

					Map<Triple, Triple> match = new HashMap<>();
					match.put(c.consequentCurrent, c.toBeMatchedCurrent);
					c.addMatch(match);
					matchingStack.push(c.getReducedContext());
				}

			} else if (c.toBeMatchedIter.hasNext()) {
				c.nextToBeMatched();
			} else {
				matchingStack.pop();
			}
		}
		return root.getMatches();
	}

	private boolean[][] getMatrix(List<Triple> consequent, List<Triple> toBeMatched) {
		boolean[][] m = new boolean[consequent.size()][toBeMatched.size()];

		for (int i = 0; i < consequent.size(); i++) {
			Triple consequentTriple = consequent.get(i);
			for (int j = 0; j < toBeMatched.size(); j++) {
				Triple toBeMatchedTriple = toBeMatched.get(j);
				m[i][j] = consequentTriple.matches(toBeMatchedTriple);
			}
		}

		return m;
	}

	static class Context {

		Triple toBeMatchedCurrent;
		Triple consequentCurrent;
		Set<Triple> toBeMatched;
		Set<Triple> consequent;
		Iterator<Triple> toBeMatchedIter;
		Iterator<Triple> consequentIter;
		Set<Map<Triple, Triple>> matches;

		public Context(Set<Triple> aToBeMatched, Set<Triple> aConsequent) {

			assert !aToBeMatched.isEmpty();
			assert !aConsequent.isEmpty();

			matches = new HashSet<>();

			this.toBeMatched = aToBeMatched;
			this.consequent = aConsequent;

			this.toBeMatchedIter = aToBeMatched.iterator();
			this.consequentIter = aConsequent.iterator();

			this.toBeMatchedCurrent = toBeMatchedIter.next();
			this.consequentCurrent = consequentIter.next();
		}

		public Set<Map<Triple, Triple>> getMatches() {
			return this.matches;
		}

		public boolean hasNextToBeMatched() {
			return this.toBeMatchedIter.hasNext();
		}

		public Triple nextToBeMatched() {
			return (this.toBeMatchedCurrent = this.toBeMatchedIter.next());
		}

		public boolean hasNextConsequent() {
			return this.consequentIter.hasNext();
		}

		public Triple nextConsequent() {
			return (this.consequentCurrent = this.consequentIter.next());
		}

		public void resetConsequentIter() {
			this.consequentIter = this.consequent.iterator();
		}

		public void addMatch(Map<Triple, Triple> match) {
			this.matches.add(match);
		}

		public Context getReducedContext() {
			Set<Triple> reducedToBeMatched = new HashSet<>(this.toBeMatched);
			Set<Triple> reducedConsequent = new HashSet<>(this.consequent);

			reducedToBeMatched.remove(this.toBeMatchedCurrent);
			reducedConsequent.remove(this.consequentCurrent);

			return new Context(reducedToBeMatched, reducedConsequent);
		}

	}

}
