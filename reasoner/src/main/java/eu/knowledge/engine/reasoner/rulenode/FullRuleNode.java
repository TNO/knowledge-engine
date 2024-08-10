package eu.knowledge.engine.reasoner.rulenode;

import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import org.apache.jena.atlas.logging.Log;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.knowledge.engine.reasoner.AntSide;
import eu.knowledge.engine.reasoner.BaseRule;
import eu.knowledge.engine.reasoner.ConsSide;
import eu.knowledge.engine.reasoner.Match;
import eu.knowledge.engine.reasoner.Rule;
import eu.knowledge.engine.reasoner.api.TripleVarBindingSet;

/**
 * @author nouwtb
 *
 */
public class FullRuleNode extends RuleNode implements AntSide, ConsSide {

	private static final Logger LOG = LoggerFactory.getLogger(FullRuleNode.class);

	private BindingSetStore resultBindingSetInput;
	private BindingSetStore filterBindingSetInput;
	private TripleVarBindingSet resultBindingSetOutput;
	private TripleVarBindingSet filterBindingSetOutput;

	public FullRuleNode(BaseRule aRule) {
		super(aRule);
		this.resultBindingSetInput = new BindingSetStore(aRule.getAntecedent(), this.antecedentNeighbours.keySet());
		this.filterBindingSetInput = new BindingSetStore(aRule.getConsequent(), this.consequentNeighbours.keySet());
	}

	private Map<RuleNode, Set<Match>> antecedentNeighbours = new HashMap<>();
	private Map<RuleNode, Set<Match>> consequentNeighbours = new HashMap<>();

	@Override
	public void addConsequentNeighbour(RuleNode neighbour, Set<Match> matches) {
		this.consequentNeighbours.put(neighbour, matches);
	}

	@Override
	public void addAntecedentNeighbour(RuleNode neighbour, Set<Match> matches) {
		this.antecedentNeighbours.put(neighbour, matches);
	}

	@Override
	public Map<RuleNode, Set<Match>> getConsequentNeighbours() {
		return this.consequentNeighbours;
	}

	@Override
	public Map<RuleNode, Set<Match>> getAntecedentNeighbours() {
		return this.antecedentNeighbours;
	}

	@Override
	public Set<RuleNode> getAllNeighbours() {
		Set<RuleNode> result = new HashSet<>();

		result.addAll(this.getAntecedentNeighbours().keySet());
		result.addAll(this.getConsequentNeighbours().keySet());

		return result;
	}

	@Override
	public boolean addFilterBindingSetInput(RuleNode aNeighbor, TripleVarBindingSet bs) {
		return this.filterBindingSetInput.add(aNeighbor, bs);
	}

	@Override
	public boolean addResultBindingSetInput(RuleNode aNeighbor, TripleVarBindingSet bs) {

		TripleVarBindingSet filteredBS = bs;
		if (this.filterBindingSetOutput != null && !this.hasProactiveParent(aNeighbor)) {
			filteredBS = bs.keepCompatible(this.filterBindingSetOutput);
		}

		var changed = this.resultBindingSetInput.add(aNeighbor, filteredBS);
		if (changed && this.filterBindingSetOutput == null && this.hasProactiveParent(aNeighbor)) {
			var previousBindingSetOutput = this.filterBindingSetOutput;
			this.filterBindingSetOutput = this.resultBindingSetInput.get();

			if (!this.filterBindingSetOutput.equals(previousBindingSetOutput))
				this.isFilterBindingSetOutputDirty = true;
		}

		return changed;
	}

	@Override
	public TripleVarBindingSet getFilterBindingSetOutput() {
		return this.filterBindingSetOutput;
	}

	@Override
	public TripleVarBindingSet getFilterBindingSetInput() {
		return this.filterBindingSetInput.get();
	}

	@Override
	public TripleVarBindingSet getResultBindingSetOutput() {
		return this.resultBindingSetOutput;
	}

	@Override
	public TripleVarBindingSet getResultBindingSetInput() {
		return this.resultBindingSetInput.get();
	}

	@Override
	public boolean readyForTransformFilter() {
		Set<RuleNode> exceptNodes = this.getAllSameLoopNeighbors();
		return this.filterBindingSetInput.haveAllNeighborsContributedExcept(exceptNodes);
	}

	@Override
	public void transformFilterBS() {
		assert this.readyForTransformFilter();
		assert this.getRule() instanceof Rule;
		var handler = ((Rule) this.getRule()).getInverseBindingSetHandler();
		try {
			var result = handler.handle(this.filterBindingSetInput.get().toBindingSet()).get();

			var previousBindingSetOutput = this.filterBindingSetOutput;

			this.filterBindingSetOutput = result.toTripleVarBindingSet(this.getRule().getAntecedent());

			if (!this.filterBindingSetOutput.equals(previousBindingSetOutput))
				this.isFilterBindingSetOutputDirty = true;

		} catch (InterruptedException | ExecutionException e) {
			LOG.error("{}", e);
		}
	}

	@Override
	public boolean readyForApplyRule() {
		boolean isReady;
		// TODO: This (the "Except" part) was needed to make transitivity work, but not
		// sure if it is correct

		if (!this.resultBindingSetInput.get().isEmpty()) {
			Set<RuleNode> exceptNodes = this.getAllSameLoopNeighbors();
			isReady = this.resultBindingSetInput.haveAllNeighborsContributedExcept(exceptNodes);
		} else {
			isReady = false;
		}
		return isReady;
	}

	@Override
	public Future<Void> applyRule() {
		assert this.readyForApplyRule();
		assert this.getRule() instanceof Rule;
		var handler = ((Rule) this.getRule()).getBindingSetHandler();
		TripleVarBindingSet fullBindingSet = this.resultBindingSetInput.get().getFullBindingSet();

		var previousBindingSetOutput = this.resultBindingSetOutput;

		CompletableFuture<Void> f;
		if (!fullBindingSet.isEmpty()) {
			f = handler.handle(fullBindingSet.toBindingSet()).thenAccept(result -> {
				this.resultBindingSetOutput = result.toTripleVarBindingSet(this.getRule().getConsequent());
				if (!this.resultBindingSetOutput.equals(previousBindingSetOutput))
					this.isResultBindingSetOutputDirty = true;

			});
		} else {
			f = new CompletableFuture<>();
			this.resultBindingSetOutput = new TripleVarBindingSet(this.getRule().getConsequent());
			f.complete(null);
			if (!this.resultBindingSetOutput.equals(previousBindingSetOutput))
				this.isResultBindingSetOutputDirty = true;
		}

		return f;
	}

	/**
	 * Determine loop nodes that are our neighbors and are part of the same loop as
	 * this node.
	 * 
	 */
	@Override
	public Set<RuleNode> getAllSameLoopNeighbors() {
		var nodes = new HashSet<RuleNode>();

		this.getConsequentNeighbours().keySet().stream().filter((r) -> r instanceof FullRuleNode)
				.map((r) -> ((FullRuleNode) r)).forEach((neighbor) -> {

					Set<FullRuleNode> visited = new HashSet<>();

					Deque<FullRuleNode> stack = new LinkedList<>();
					stack.addAll(neighbor.getConsequentNeighbours().keySet().stream()
							.filter((r) -> r instanceof FullRuleNode).map((r) -> ((FullRuleNode) r))
							.collect(Collectors.toSet()));

					FullRuleNode current;
					while (!stack.isEmpty()) {
						current = stack.pop();
						if (visited.contains(current)) {
							continue;
						} else if (current == this) {
							nodes.add(neighbor);
							break;
						}

						stack.addAll(current.getConsequentNeighbours().keySet().stream()
								.filter((r) -> r instanceof FullRuleNode).map((r) -> ((FullRuleNode) r))
								.collect(Collectors.toSet()));
						visited.add(current);

					}
				});

		this.getAntecedentNeighbours().keySet().stream().filter((r) -> r instanceof FullRuleNode)
				.map((r) -> ((FullRuleNode) r)).forEach((neighbor) -> {

					Set<FullRuleNode> visited = new HashSet<>();

					Deque<FullRuleNode> stack = new LinkedList<>();
					stack.addAll(neighbor.getAntecedentNeighbours().keySet().stream()
							.filter((r) -> r instanceof FullRuleNode).map((r) -> ((FullRuleNode) r))
							.collect(Collectors.toSet()));

					FullRuleNode current;
					while (!stack.isEmpty()) {
						current = stack.pop();
						if (visited.contains(current)) {
							continue;
						} else if (current == this) {
							nodes.add(neighbor);
							break;
						}

						stack.addAll(current.getAntecedentNeighbours().keySet().stream()
								.filter((r) -> r instanceof FullRuleNode).map((r) -> ((FullRuleNode) r))
								.collect(Collectors.toSet()));
						visited.add(current);
					}
				});

		return nodes;
	}

	@Override
	public boolean shouldPropagateFilterBindingSetOutput() {
		return this.isFilterBindingSetOutputDirty;
	}

	@Override
	public boolean shouldPropagateResultBindingSetOutput() {
		return this.isResultBindingSetOutputDirty;
	}
}
