package de.buw.fm4se;

import java.util.Map;
import java.util.List;
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
import edu.mit.csail.sdg.translator.TranslateAlloyToKodkod;

public class AlloyChecker {

	public static List<String> findDeadSignatures(String fileName, A4Options options, A4Reporter reporter) {
		return getSignatureListThatMatchPredefinedExpression(Op.SOME, fileName, options, reporter);
	}

	public static List<String> findCoreSignatures(String fileName, A4Options options, A4Reporter reporter) {
		return getSignatureListThatMatchPredefinedExpression(Op.NO, fileName, options, reporter);
	}

	private static List<String> getSignatureListThatMatchPredefinedExpression(Op operation, String fileName, A4Options options, A4Reporter reporter) {
		// Parse file
		Module world = CompUtil.parseEverything_fromFile(reporter, null, fileName);

		// Get first command & all signatures
		ConstList<Sig> allSignatures = world.getAllReachableUserDefinedSigs();
		Command firstCommand = world.getAllCommands().get(0);

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
	public static Map<String, Integer> findMinScope(String fileName, A4Options options, A4Reporter rep) {
		// Determine the minimum scope for each signature in an Alloy model, 
		// i.e., map each signature name to an integer scope for which the model is still satisfiable.
		// - Again, use the first command in the Alloy file.
		// - You may update the scope of a signature sig in a command cmd to integer i by using the returned Command of cmd.change(sig, false, i).
		// - Computing a maximal scope is a bit tricky and done for you in method getMaxScope.
		// * อาจารย์เริ่ม อธิบาย  ตั้งแต่นาที8:30
		return null;
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
