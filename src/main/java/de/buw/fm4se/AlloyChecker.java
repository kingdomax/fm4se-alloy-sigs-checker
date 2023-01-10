package de.buw.fm4se;

import java.util.Map;
import java.util.List;
import java.util.HashMap;
import java.util.ArrayList;

import edu.mit.csail.sdg.ast.Sig;
import edu.mit.csail.sdg.ast.Module;
import edu.mit.csail.sdg.ast.Command;
import edu.mit.csail.sdg.parser.CompUtil;
import edu.mit.csail.sdg.ast.CommandScope;
import edu.mit.csail.sdg.ast.ExprUnary.Op;
import edu.mit.csail.sdg.alloy4.A4Reporter;
import edu.mit.csail.sdg.alloy4.ConstList;
import edu.mit.csail.sdg.translator.A4Options;
import edu.mit.csail.sdg.translator.A4Solution;
import edu.mit.csail.sdg.translator.TranslateAlloyToKodkod;

public class AlloyChecker {

	public static List<String> findDeadSignatures(String fileName, A4Options options, A4Reporter reporter) {
		return getSignatureListThatNotSatisfyPredefinedExpression(Op.SOME, fileName, options, reporter);
	}

	public static List<String> findCoreSignatures(String fileName, A4Options options, A4Reporter reporter) {
		return getSignatureListThatNotSatisfyPredefinedExpression(Op.NO, fileName, options, reporter);
	}

	private static List<String> getSignatureListThatNotSatisfyPredefinedExpression(Op operation, String fileName, A4Options options, A4Reporter reporter) {
		// Parse file
		Module world = CompUtil.parseEverything_fromFile(reporter, null, fileName);

		// Get first command & all signatures
		Command firstCommand = world.getAllCommands().get(0);
		ConstList<Sig> allSignatures = world.getAllReachableUserDefinedSigs();

		// Modify run command with predefined expression
		List<String> results = new ArrayList<>();
		for (Sig signature : allSignatures) {
			if (!TranslateAlloyToKodkod.execute_command(reporter,
													allSignatures,
													firstCommand.change(firstCommand.formula.and(operation == Op.SOME ? signature.some() : signature.no())),
													options).satisfiable()) {
				results.add(signature.label);
			}
		}
		return results;
	}

	/**
	 * Computes for each user-defines signature a minimal scope for which the model
	 * is still satisfiable. Note that the scopes will be independent, i.e., minimum
	 * 0 for sig A and 0 for sig B does not mean that both can be 0 together.
	 * 
	 * @param fileName
	 * @param options
	 * @param rep
	 * @return map from signature names to minimum scopes
	 */
	public static Map<String, Integer> findMinScope(String fileName, A4Options options, A4Reporter reporter) {
		Map <String, Integer> result = new HashMap<String, Integer>();
		Module world = CompUtil.parseEverything_fromFile(reporter, null, fileName);
		Command command = world.getAllCommands().get(0);
		ConstList<Sig> allSignatures = world.getAllReachableUserDefinedSigs();

		for (Sig signature : allSignatures) {
			var minScope = -1;
			var maxScope = getMaxScope(signature, command);
			A4Solution solution = TranslateAlloyToKodkod.execute_command(reporter, allSignatures, command, options);

			while (solution.satisfiable() || minScope == -1) { // -1 mean min scope value is never set
				try {
					var reducedScopeCommand = command.change(signature, true, --maxScope);
					solution = TranslateAlloyToKodkod.execute_command(reporter, allSignatures, reducedScopeCommand, options);
					if (solution.satisfiable()) { minScope = maxScope; }
				} catch (Exception e) {
					if (minScope != -1) { // if min scope value is already set, use that value
						break;
					} else if (maxScope < 0) { // if min scope value is never set but it already iterate below 0, use 0
						minScope = 0;
						break;
					}
				}
			}

			result.put(signature.label, minScope);
		}

		return result;
	}

	/**
	 * Computes the maximum scope for a signature in a command. This is either the
	 * default of 4, the overall scope, or the specific scope for the signature in
	 * the command. 
	 * 
	 * @param sig
	 * @param cmd
	 * @return
	 */
	public static int getMaxScope(Sig sig, Command cmd) {
		int scope = 4; // Alloy's default
		if (cmd.overall != -1) {
			scope = cmd.overall;
		}
		CommandScope cmdScope = cmd.getScope(sig);
		if (cmdScope != null) {
			scope = cmdScope.endingScope;
		}
		return scope;
	}
}
