package eu.knowledge.engine.reasoner2;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.knowledge.engine.reasoner.BaseRule;
import eu.knowledge.engine.reasoner.ProactiveRule;
import eu.knowledge.engine.reasoner.BaseRule.MatchStrategy;
import eu.knowledge.engine.reasoner.api.BindingSet;
import eu.knowledge.engine.reasoner.rulestore.RuleStore;
import eu.knowledge.engine.reasoner2.reasoningnode.ActiveAntRuleNode;
import eu.knowledge.engine.reasoner2.reasoningnode.ActiveConsRuleNode;
import eu.knowledge.engine.reasoner2.reasoningnode.FullRuleNode;
import eu.knowledge.engine.reasoner2.reasoningnode.PassiveAntRuleNode;
import eu.knowledge.engine.reasoner2.reasoningnode.PassiveConsRuleNode;
import eu.knowledge.engine.reasoner2.reasoningnode.RuleNode;

public class Reasoner {

  private static final Logger LOG = LoggerFactory.getLogger(Reasoner.class);
  private final RuleStore store;
  private final ProactiveRule start;
  private final Map<BaseRule, RuleNode> ruleToRuleNode;

  public Reasoner(RuleStore aStore, ProactiveRule aStartRule) {
    this.store = aStore;
    this.start = aStartRule;
    this.ruleToRuleNode = new HashMap<>();
  }
  
  public void execute(BindingSet bindingSet) {
    RuleNode startNode = createOrGetReasonerNode(this.start);

    Stack<RuleNode> stack = new Stack<>();
    Set<RuleNode> visited = new HashSet<>();
    Set<RuleNode> changed = new HashSet<>();

    changed.add(startNode);

    while (!changed.isEmpty()) {
      stack.clear();
      visited.clear();
      changed.clear();

      stack.push(startNode);

      while (!stack.empty()) {
        RuleNode current = stack.pop();

        current.getAllNeighbours().stream()
          .filter(n -> !stack.contains(n))
          .filter(n -> !visited.contains(n))
          .forEach(n -> stack.push(n));
        
        if (current.readyForTransformFilter()) {
          current.transformFilterBS();
        }

        if (current.readyForApplyRule()) {
          current.applyRule();
        }

        var toBeFilterPropagated = current.getFBOutput();
        if (toBeFilterPropagated != null) {
          // TODO: Propagate the FBOs to the current node's antecedent
          // neighbours, and if they changed, mark them as changed
        }

        var toBeResultPropagated = current.getRBOutput();
        if (toBeResultPropagated != null) {
          // TODO: Propagate the RBOs to the current node's consequent
          // neighbours, and if they changed, mark them as changed
        }

        visited.add(current);
      }
    }
  }

  public RuleNode newNode(BaseRule rule) {
    // based on the rule properties, return the appropriate rulenode
    if (!rule.getAntecedent().isEmpty()) {
      if (!rule.getConsequent().isEmpty()) {
        return new FullRuleNode();
      } else {
        // no consequent, yes antecedent
        if (rule.isProactive()) {
          return new PassiveAntRuleNode();
        } else {
          return new ActiveAntRuleNode();
        }
      }
    } else {
      assert !rule.getConsequent().isEmpty();
      // no antecedent, yes consequent
      if (rule.isProactive()) {
        return new PassiveConsRuleNode();
      } else {
        return new ActiveConsRuleNode();
      }
    }
  }

  public boolean isBackward() {
		return !this.start.getAntecedent().isEmpty() && this.start.getConsequent().isEmpty();
	}

  private RuleNode createOrGetReasonerNode(BaseRule aRule) {

		final RuleNode reasonerNode;
		if (this.ruleToRuleNode.containsKey(aRule))
			return this.ruleToRuleNode.get(aRule);
    else {
      reasonerNode = this.newNode(aRule);
    }

		// build the reasoner node graph
		this.ruleToRuleNode.put(aRule, reasonerNode);

		if (isBackward()) {
			// for now we only are interested in antecedent neighbors.
			// TODO for looping we DO want to consider consequent neighbors as well.

      this.store.getAntecedentNeighbors(aRule, MatchStrategy.FIND_ALL_MATCHES).forEach((rule, matches) -> {
        if (!(rule instanceof ProactiveRule)) {
          assert reasonerNode instanceof AntSide;
          ((AntSide) reasonerNode).addAntecedentNeighbour(createOrGetReasonerNode(rule), matches);
				} else {
					LOG.trace("Skipped proactive rule: {}", rule);
				}
      });
		} else {
			// interested in both consequent and antecedent neighbors
      this.store.getConsequentNeighbors(aRule, MatchStrategy.FIND_ALL_MATCHES).forEach((rule, matches) -> {
        if (!(rule instanceof ProactiveRule)) {
          assert reasonerNode instanceof ConsSide;
          ((ConsSide) reasonerNode).addConsequentNeighbour(createOrGetReasonerNode(rule), matches);
				} else {
					LOG.trace("Skipped proactive rule: {}", rule);
				}
      });

			// antecedent neighbors to propagate bindings further via backward chaining
			this.store.getAntecedentNeighbors(aRule, MatchStrategy.FIND_ALL_MATCHES).forEach((rule, matches) -> {
        if (!(rule instanceof ProactiveRule)) {
          assert reasonerNode instanceof AntSide;
          ((AntSide) reasonerNode).addAntecedentNeighbour(createOrGetReasonerNode(rule), matches);
				} else {
					LOG.trace("Skipped proactive rule: {}", rule);
				}
      });
		}

		return reasonerNode;
	}
}
