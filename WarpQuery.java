import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.*;

public class WarpQuery {

	protected ArrayList<String> operators = new ArrayList<String>(Arrays.asList(";", "&", "|", "^", "==", "!=", "><", "<<", ">>", "<>", ".ij.", ".lj.", ".rj.", ".cj.", ">=", "<=", ">", "<", "?=", ":=", ".ge.", ".le.", ".gt.", ".lt.", "%3E%3E", "%3C%3C", "%3E%3C", "%3C%3E", "%3E=", "%3C=", "%3E", "%3C", "="));
	protected ArrayList<String> central_operators = new ArrayList<String>(Arrays.asList("==", "!=", "><", "<<", ">>", "<>", ">=", "<=", ">", "<", "?=", ":=", "="));
	protected ArrayList<String> compare_operators = new ArrayList<String>(Arrays.asList("==", "!=", ">=", "<=", ">", "<", "?="));
	protected ArrayList<String> logical_operators = new ArrayList<String>(Arrays.asList("&", "|", "^"));
	protected ArrayList<String> join_operators = new ArrayList<String>(Arrays.asList("><", "<<", ">>", "<>"));

	public ArrayList<ArrayList<String>> decode(String _query)
	{
		ArrayList<ArrayList<String>> queries = new ArrayList<ArrayList<String>>();
		// check Empty Query String
		if (_query.isEmpty()) {
			return queries;
		}

		// form Query String for Parsing
		String query_string = '&' + _query + ';';

		// for Proxy Parameters
		while (true) {
			// check Curly Brackets
			Pattern pattern = Pattern.compile("^(?:|(.*?)([&|]))(\\{.*?[^%]\\})(.*)$");
			Matcher matches = pattern.matcher(query_string);
			
			if (! matches.find()) {
				break;
			}

			// to Semantics Variables
			String all_match = matches.group();
			String pre_match = matches.group(1);
			String process = matches.group(2);
			String proxy = matches.group(3);
			String post_match = matches.group(4);

			// delete Curly Bracket
			proxy = proxy.replaceAll("^\\{(.*)\\}$", "$1");

			// for Virtical Proxy Module
			String location;
			if (0 == proxy.indexOf('/')) {
				location = "self";
			// for Horizontal Proxy Module
			} else {
				pattern = Pattern.compile("(?i)^(http(?:|s):\\/\\/.+?)\\/");
				matches = pattern.matcher(proxy);
				if (! matches.find()) {
					// error
				}
				location = matches.group(1);
				proxy = proxy.replace(location, "");
			}

			// to Queries
			ArrayList<String> list = new ArrayList<>();
			list.add(process);
			list.add(location);
			list.add("{}");
			list.add(proxy);
			queries.add(list);

			// reform Query String for Parsing
			query_string = pre_match + post_match;
		}

		// escape Operators
		ArrayList<String> esc_operators = new ArrayList<>();
		String aaa = "";
		String delimiter = "";
		for (int i = 0; i < operators.size(); i++) {
			delimiter = i == 0 ? "" : "|";
			aaa += delimiter + Pattern.quote(operators.get(i));
		}

		// form Operators Regex
		String operators_regex = "(.*?)(" + aaa + ")(.*?)$";
		Pattern pattern = Pattern.compile(operators_regex);
		Matcher matches = pattern.matcher(query_string);

		// Query to Parts
		ArrayList<String> query_parts = new ArrayList<>();
		while (true) {
			// matching Operators
			if (! matches.find()) {
				break;
			}

			// to Semantics Variables
			String all_match = matches.group();
			String operand = matches.group(1);
			String operator = matches.group(2);
			String post_match = matches.group(3);

			// from Alias Operators to Master Operators
			if (operator == ".ge." || operator == "%3E=") {
				operator = ">=";
			} else if (operator ==".le." || operator == "%3C=") {
				operator = "<=";
			} else if (operator == ".gt." || operator == "%3E") {
				operator = ">";
			} else if (operator == ".lt." || operator == "%3C") {
				operator = "<";
			} else if (operator == ".ij." || operator == "%3E%3C") {
				operator = "><";
			} else if (operator == ".lj." || operator == "%3C%3C") {
				operator = "<<";
			} else if (operator == ".rj." || operator == "%3E%3E") {
				operator = ">>";
			} else if (operator == ".cj." || operator == "%3C%3E") {
				operator = "<>";
			}

			// map to Query Parts
			if (operand != "") {
				query_parts.add(operand);
			}
			query_parts.add(operator);

			// from Post Matcher to Query String
			query_string = post_match;
		}

		// check Data-Type-Head Module
		int data_type_id = 0;
		for (int i = 0; i < query_parts.size(); i++) {
			if (query_parts.get(i) != "data-type") {
				continue;
			}
			// Notice: The last data-type field must be get.
			data_type_id = i;
		}

		String data_type = "";
		if ((data_type_id != 0) && (query_parts.get(data_type_id + 1) == ":=")) {
			data_type = query_parts.get(data_type_id + 2);
		}

		// map to Queries
		for (int i = 0; i < query_parts.size(); i++) {

			// not Central Operators
			if (! central_operators.contains(query_parts.get(i))) {
				continue;
			}

			// to Semantics Variables
			String logical_operator = query_parts.get(i - 2);
			String left_operand = query_parts.get(i - 1);
			String central_operator = query_parts.get(i);
			String right_operand = query_parts.get(i + 1);

			// for Data-Type-Head Module
			// for Strict Data Type
			if (data_type == "true") {
				String regex = "/^(?:%22|%27|[\"\'])(.*?)(?:%22|%27|[\"\'])$";
				// delete first and last quotes for String Data Type
				if (right_operand.matches(regex)) {
					String right_operanded = right_operand.replaceAll(regex, "$1");
				// for Not String Type
				} else {
					// to Boolean
					if (right_operand == "true") {
						boolean right_operanded = Boolean.valueOf(right_operand);
					// to Boolean
					} else if (right_operand == "false") {
						boolean right_operanded = false;
					// to Null
					} else if (right_operand == "null") {
						String right_operanded = null;
					// to Integer
					} else if (right_operand.matches("^\\d$|^[1-9]\\d+$")) {
						int right_operanded = Integer.parseInt(right_operand);
					// to Float
					} else if (right_operand.matches("^\\d\\.\\d+$|^[1-9]\\d+\\.\\d+$")) {
						double right_operanded = Double.parseDouble(right_operand);
					}
				}
			} else {
				right_operand = "";
				String right_operanded = right_operand;
			}

			// validate Left Operand
			if (operators.contains(left_operand)) {
				//throw new WarpException(WarpException::SYNTAX_ERROR . 'The parameter is having invalid left operands: ' . $_query, 400);
			}

			// validate Right Operand
			// to Empty String
			if (logical_operators.contains(right_operand) || right_operand == ";") {
				right_operand = "";
			// for Double NV Operators
			} else if (central_operators.contains(right_operand)) {
				//throw new WarpException(WarpException::SYNTAX_ERROR .  'The parameter is having double comparing operators: ' . $_query, 400);
			}

			// map to Queries
			// for Head Parameters, Assign Parameters and Join Parameters
			if (central_operator == ":=" || central_operator == "=" || join_operators.contains(central_operator)) {
				// validate Logical Part
				if (logical_operator != "&") {
					logical_operator = "&";
				}
			// for Search Parameters
			} else if (compare_operators.contains(central_operator)) {
				// validate Logical Part
				if (! logical_operators.contains(logical_operator)) {
					//throw new WarpException(WarpException::SYNTAX_ERROR . 'The Search Parameters are having invalid logical operators: ' . $_query, 400);
				}
			// for Others
			} else {
				continue;
			}

			// to Queries
			ArrayList<String> list = new ArrayList<>();
			list.add(logical_operator);
			list.add(left_operand);
			list.add(central_operator);
			list.add(right_operand);
			queries.add(list);
		}

		// return
		return queries;

	}

	public String encode(ArrayList<ArrayList<String>> _object)
	{
		// check Empty Object
		if (_object.isEmpty()) {
			//return '';
		}

		// drop First Logical Operator
		//unset($_object[0][0]);

		// check Data Type Flag
		boolean data_type_flag = false;
		for (int i = 0; i < _object.size(); i++) {
			if (_object.get(i).get(1) != "data-type") {
				continue;
			}
			// for Data Type
			// Notice: Processing must be not breaked because there ware multiple the value of “data-type”.
			//if ((_object.get(i).get(3) instanceof Boolean) && _object.get(i).get(3) == true) {
			//	data_type_flag = true;
			//}
		}

		return "encode";
	}

}
