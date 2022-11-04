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
import eu.knowledge.engine.reasoner.api.TripleVarBindingSet;
import eu.knowledge.engine.reasoner.rulestore.RuleStore;
import eu.knowledge.engine.reasoner2.reasoningnode.ActiveAntRuleNode;
import eu.knowledge.engine.reasoner2.reasoningnode.ActiveConsRuleNode;
import eu.knowledge.engine.reasoner2.reasoningnode.FullRuleNode;
import eu.knowledge.engine.reasoner2.reasoningnode.PassiveAntRuleNode;
import eu.knowledge.engine.reasoner2.reasoningnode.PassiveConsRuleNode;
import eu.knowledge.engine.reasoner2.reasoningnode.RuleNode;

public class ReasonerPlan {

  private static final Logger LOG = LoggerFactory.getLogger(ReasonerPlan.class);
  private final RuleStore store;
  private final ProactiveRule start;
  private final Map<BaseRule, RuleNode> ruleToRuleNode;

  public ReasonerPlan(RuleStore aStore, ProactiveRule aStartRule) {
    this.store = aStore;
    this.start = aStartRule;
    this.ruleToRuleNode = new HashMap<>();
  }
  
  public void execute(BindingSet bindingSet) {
    RuleNode startNode = createOrGetReasonerNode(this.start);

    // TODO: Deque
    Stack<RuleNode> stack = new Stack<>();
    Set<RuleNode> visited = new HashSet<>();
    Set<RuleNode> changed = new HashSet<>();

    do {
      stack.clear();
      visited.clear();
      changed.clear();

      stack.push(startNode);

      while (!stack.isEmpty()) {
        final RuleNode current = stack.pop();

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

        TripleVarBindingSet toBeFilterPropagated = current.getFilterBindingSetOutput();
        if (toBeFilterPropagated != null) {
          assert current instanceof AntSide;
          ((AntSide) current).getAntecedentNeighbours().forEach(n -> {
            boolean itChanged = ((ConsSide) n).addFilterBindingSetInput(current, toBeFilterPropagated);
            if (itChanged) {
              changed.add(n);
            }
          });
        }

        TripleVarBindingSet toBeResultPropagated = current.getResultBindingSetOutput();
        if (toBeResultPropagated != null) {
          assert current instanceof ConsSide;
          ((ConsSide) current).getConsequentNeighbours().forEach(n -> {
            boolean itChanged = ((AntSide) n).addResultBindingSetInput(current, toBeResultPropagated);
            if (itChanged) {
              changed.add(n);
            }
          });
        }

        visited.add(current);
      }
    } while (!changed.isEmpty());
  }

  public RuleNode newNode(BaseRule rule) {
    // based on the rule properties, return the appropriate rulenode
    if (!rule.getAntecedent().isEmpty()) {
      if (!rule.getConsequent().isEmpty()) {
        return new FullRuleNode(rule);
      } else {
        // no consequent, yes antecedent
        if (rule.isProactive()) {
          return new PassiveAntRuleNode(rule);
        } else {
          return new ActiveAntRuleNode(rule);
        }
      }
    } else {
      assert !rule.getConsequent().isEmpty();
      // no antecedent, yes consequent
      if (rule.isProactive()) {
        return new PassiveConsRuleNode(rule);
      } else {
        return new ActiveConsRuleNode(rule);
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
          var newNode = createOrGetReasonerNode(rule);
          assert newNode instanceof ConsSide;
          ((AntSide) reasonerNode).addAntecedentNeighbour(newNode, matches);

          // TODO: Validate with Barry if we can use the same `matches` object here
          ((ConsSide) newNode).addConsequentNeighbour(reasonerNode, matches);
				} else {
					LOG.trace("Skipped proactive rule: {}", rule);
				}
      });
		} else {
			// interested in both consequent and antecedent neighbors
      this.store.getConsequentNeighbors(aRule, MatchStrategy.FIND_ALL_MATCHES).forEach((rule, matches) -> {
        if (!(rule instanceof ProactiveRule)) {
          assert reasonerNode instanceof ConsSide;
          var newNode = createOrGetReasonerNode(rule);
          ((ConsSide) reasonerNode).addConsequentNeighbour(newNode, matches);

          // TODO: Validate with Barry if we can use the same `matches` object here
          ((AntSide) newNode).addAntecedentNeighbour(reasonerNode, matches);
				} else {
					LOG.trace("Skipped proactive rule: {}", rule);
				}
      });

			// antecedent neighbors to propagate bindings further via backward chaining
			this.store.getAntecedentNeighbors(aRule, MatchStrategy.FIND_ALL_MATCHES).forEach((rule, matches) -> {
        if (!(rule instanceof ProactiveRule)) {
          assert reasonerNode instanceof AntSide;
          var newNode = createOrGetReasonerNode(rule);
          assert newNode instanceof ConsSide;
          ((AntSide) reasonerNode).addAntecedentNeighbour(newNode, matches);

          // TODO: Validate with Barry if we can use the same `matches` object here
          ((ConsSide) newNode).addConsequentNeighbour(reasonerNode, matches);
				} else {
					LOG.trace("Skipped proactive rule: {}", rule);
				}
      });
		}

		return reasonerNode;
	}
}
