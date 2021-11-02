package edu.tamu.csce434;


import java.io.Console;

public class Compiler
{
	private edu.tamu.csce434.Scanner scanner;
	int token;
	int nextRegister = 1;
	int currentGlobal = 0;
	int numParams = 0;
	java.util.Vector<Integer> ifCondLocation = new java.util.Vector<>();
	java.util.Vector<Integer> elseCondLocation = new java.util.Vector<>();
	java.util.Vector<Integer> whileCondLocation = new java.util.Vector<>();
	java.util.Vector<Integer> paramReg = new java.util.Vector<>();
	java.util.Vector<String> paramName = new java.util.Vector<>();
	int funcLocation;
	int buf[] = new int[3000];
	int bufPointer = 0;
	public java.util.Map<String, Integer> registerMap = new java.util.HashMap<>();
	public java.util.Map<String, Integer> functionOffsetMap = new java.util.HashMap<>();
	public java.util.Map<String, Integer> startingPc = new java.util.HashMap<>();

	// Constructor of your Compiler
	public Compiler(String args) {
		scanner = new Scanner(args);
	}

	// Implement this function to start compiling your input file
	public int[] getProgram() {
		computation();

		return java.util.Arrays.copyOf(buf, bufPointer);
	}

	private void printError(int i) {
		System.out.println("error");
		System.exit(0);
	}

	// Use this function to accept a Token and and to get the next Token from the Scanner
	private boolean accept(String s) {
		scanner.Next();
		token = scanner.sym;
		return scanner.String2Id(s) == token;
	}

	// Use this function whenever your program needs to expect a specific token
	private void expect(String s) {
		if (accept(s))
			return;

		printError(scanner.sym);
	}

	// Implement this function to start parsing your input file
	public void computation() {
		token = scanner.sym;

		computationCheck();
	}

	public void relOpCheck() {
		if (scanner.sym < 20 || scanner.sym > 25) {
			printError(scanner.sym);
		}
	}

	public void relOperation(int statementType, int leftRegister[], int relationType, int rightRegister[]) {
		int relOpRegister = nextRegister;
		int operationLocation = bufPointer;
		if (leftRegister[0] == 1 & rightRegister[0] == 1) {
			int comparison = leftRegister[1] - rightRegister[1];
			buf[bufPointer] = DLX.assemble(16, relOpRegister, 0, comparison);
		} else if (rightRegister[0] == 1) {
			buf[bufPointer] = DLX.assemble(17, relOpRegister, leftRegister[1], rightRegister[1]);
		} else {
			buf[bufPointer] = DLX.assemble(1, relOpRegister, leftRegister[1], rightRegister[1]);
		}
		bufPointer++;
		nextRegister++;

		if (statementType == 0) {
			ifCondLocation.add(0, bufPointer);
		} else {
			whileCondLocation.add(0, bufPointer);
			whileCondLocation.add(0, operationLocation);
		}

		switch (relationType) {
			case 20:
				buf[bufPointer] = DLX.assemble(41, relOpRegister, 0);
				bufPointer++;
				break;
			case 21:
				buf[bufPointer] = DLX.assemble(40, relOpRegister, 0);
				bufPointer++;
				break;
			case 22:
				buf[bufPointer] = DLX.assemble(43, relOpRegister, 0);
				bufPointer++;
				break;
			case 23:
				buf[bufPointer] = DLX.assemble(42, relOpRegister, 0);
				bufPointer++;
				break;
			case 24:
				buf[bufPointer] = DLX.assemble(45, relOpRegister, 0);
				bufPointer++;
				break;
			case 25:
				buf[bufPointer] = DLX.assemble(44, relOpRegister, 0);
				bufPointer++;
				break;
		}
	}

	public int[] factorCheck(boolean activated, boolean isFunction) {
		int[] factorNumReg = new int[2];
		int factorRegister = nextRegister;
		if (scanner.sym == 50) {
			scanner.Next();
			factorNumReg = expressionCheck(activated, isFunction);
			if (factorNumReg[0] == 1) {
				buf[bufPointer] = DLX.assemble(16, factorRegister, 0, factorNumReg[1]);
				factorNumReg[0] = 0;
				factorNumReg[1] = factorRegister;
				bufPointer++;
				nextRegister++;
			}
			if (scanner.sym != 35) {
				printError(scanner.sym);
			}
			return factorNumReg;
		} else if (scanner.sym == 60) {
			factorNumReg[0] = 1;
			factorNumReg[1] = scanner.val;
			return factorNumReg;
		} else if (scanner.sym == 61) {
			if (isFunction) {
				int offset = functionOffsetMap.get(scanner.Id2String(scanner.id));
				buf[bufPointer] = DLX.assemble(32, factorRegister, 28, offset);
			} else {
				int offset = registerMap.get(scanner.Id2String(scanner.id));
				buf[bufPointer] = DLX.assemble(32, factorRegister, 30, offset);
			}
			factorNumReg[1] = factorRegister;
			bufPointer++;
			nextRegister++;
			return factorNumReg;
		} else if (scanner.sym == 100) {
			factorRegister = funcCallCheck(activated, isFunction);
		} else {
			printError(scanner.sym);
		}
		factorNumReg[1] = factorRegister;
		return factorNumReg;
	}

	public int[] termCheck(boolean activated, boolean isFunction) {
		int[] termNumReg;
		int termRegister;
		int[] leftFactorRegister = factorCheck(activated, isFunction);
		boolean multiplying;
		// Needed for function calls
		if (scanner.sym != 70) {
			scanner.Next();
		}
		if (scanner.sym != 1 && scanner.sym != 2) {
			return leftFactorRegister;
		}
		while (scanner.sym == 1 || scanner.sym == 2) {
			multiplying = scanner.sym == 1;
			scanner.Next();
			int[] rightFactorRegister = factorCheck(activated, isFunction);
			termRegister = nextRegister;
			if (leftFactorRegister[0] == 1 & rightFactorRegister[0] == 1) {
				if (multiplying) {
					leftFactorRegister[1] = leftFactorRegister[1] * rightFactorRegister[1];
				} else {
					leftFactorRegister[1] = leftFactorRegister[1] / rightFactorRegister[1];
				}
			} else {
				if (leftFactorRegister[0] == 1) {
					if (multiplying) {
						buf[bufPointer] = DLX.assemble(18, termRegister, rightFactorRegister[1], leftFactorRegister[1]);
					} else {
						buf[bufPointer] = DLX.assemble(16, termRegister, 0, leftFactorRegister[1]);
						nextRegister++;
						bufPointer++;
						buf[bufPointer] = DLX.assemble(3, nextRegister, termRegister, rightFactorRegister[1]);
						termRegister = nextRegister;
					}
				} else if (rightFactorRegister[0] == 1) {
					if (multiplying) {
						buf[bufPointer] = DLX.assemble(18, termRegister, leftFactorRegister[1], rightFactorRegister[1]);
					} else {
						buf[bufPointer] = DLX.assemble(19, termRegister, leftFactorRegister[1], rightFactorRegister[1]);
					}
				} else {
					if (multiplying) {
						buf[bufPointer] = DLX.assemble(2, termRegister, leftFactorRegister[1], rightFactorRegister[1]);
					} else {
						buf[bufPointer] = DLX.assemble(3, termRegister, leftFactorRegister[1], rightFactorRegister[1]);
					}
				}
				leftFactorRegister[1] = termRegister;
				leftFactorRegister[0] = 0;
				bufPointer++;
				nextRegister++;
			}
			scanner.Next();
		}
		termNumReg = leftFactorRegister;
		return termNumReg;
	}

	public int[] expressionCheck(boolean activated, boolean isFunction) {
		int[] exprNumReg;
		int expressionRegister;
		int[] leftExpressionRegister = termCheck(activated, isFunction);
		boolean adding;
		if (scanner.sym != 11 && scanner.sym != 12) {
			return leftExpressionRegister;
		}
		while (scanner.sym == 11 || scanner.sym == 12) {
			adding = scanner.sym == 11;
			scanner.Next();
			int[] rightExpressionRegister = termCheck(activated, isFunction);
			expressionRegister = nextRegister;
			if (leftExpressionRegister[0] == 1 & rightExpressionRegister[0] == 1) {
				if (adding) {
					leftExpressionRegister[1] = leftExpressionRegister[1] + rightExpressionRegister[1];
				} else {
					leftExpressionRegister[1] = leftExpressionRegister[1] - rightExpressionRegister[1];
				}
			} else {
				if (leftExpressionRegister[0] == 1) {
					if (adding) {
						buf[bufPointer] = DLX.assemble(16, expressionRegister, rightExpressionRegister[1], leftExpressionRegister[1]);
					} else {
						buf[bufPointer] = DLX.assemble(1, expressionRegister, leftExpressionRegister[1], rightExpressionRegister[1]);
					}
				} else if (rightExpressionRegister[0] == 1) {
					if (adding) {
						buf[bufPointer] = DLX.assemble(16, expressionRegister, leftExpressionRegister[1], rightExpressionRegister[1]);
					} else {
						buf[bufPointer] = DLX.assemble(17, expressionRegister, leftExpressionRegister[1], rightExpressionRegister[1]);
					}
				} else {
					if (adding) {
						buf[bufPointer] = DLX.assemble(0, expressionRegister, leftExpressionRegister[1], rightExpressionRegister[1]);
					} else {
						buf[bufPointer] = DLX.assemble(1, expressionRegister, leftExpressionRegister[1], rightExpressionRegister[1]);
					}
				}
				leftExpressionRegister[1] = expressionRegister;
				leftExpressionRegister[0] = 0;
				bufPointer++;
				nextRegister++;
			}
		}
		exprNumReg = leftExpressionRegister;
		return exprNumReg;
	}

	public void relationCheck(int statementType, boolean activated, boolean isFunction) {
		int[] leftRegister;
		int relationType;
		int[] rightRegister;

		leftRegister = expressionCheck(activated, isFunction);
		relationType = scanner.sym;
		relOpCheck();
		scanner.Next();
		rightRegister = expressionCheck(activated, isFunction);
		relOperation(statementType, leftRegister, relationType, rightRegister);
	}

	public void assignmentCheck(boolean activated, boolean isFunction) {
		scanner.Next();
		if (scanner.sym != 61) {
			printError(scanner.sym);
		}
		int offset;
		String currentIdent = scanner.Id2String(scanner.id);
		if (isFunction) {
			offset = functionOffsetMap.get(currentIdent);
		} else {
			offset = registerMap.get(currentIdent);
		}
		expect("<-");
		scanner.Next();
		int[] expressionRegister = expressionCheck(activated, isFunction);
		if (activated) {
			if (expressionRegister[0] == 1) {
				buf[bufPointer] = DLX.assemble(16, nextRegister, 0, expressionRegister[1]);
				bufPointer++;
				if (isFunction) {
					buf[bufPointer] = DLX.assemble(36, nextRegister, 28, offset);
				} else {
					buf[bufPointer] = DLX.assemble(36, nextRegister, 30, offset);
				}
				bufPointer++;
				nextRegister++;
				return;
			}
			if (isFunction) {
				buf[bufPointer] = DLX.assemble(36, expressionRegister[1], 28, offset);
			} else {
				buf[bufPointer] = DLX.assemble(36, expressionRegister[1], 30, offset);
			}
			bufPointer++;
		}
	}

	public int predefinedFunc(boolean activated, boolean isFunction) {
		String funcType = scanner.Id2String(scanner.id);
		int[] funcValueRegister = new int[2];

		if (funcType.equals("inputnum")) {
			scanner.Next();
			if (scanner.sym == 50) {
				expect(")");
			}
			if (activated) {
				int inputRegister = nextRegister;
				buf[bufPointer] = DLX.assemble(50, inputRegister);
				bufPointer++;
				nextRegister++;
				return inputRegister;
			}
			return 0;
		}

		expect("(");
		scanner.Next();
		if (scanner.sym != 35) {

			funcValueRegister = expressionCheck(activated, isFunction);
			if (scanner.sym != 35) {
				printError(scanner.sym);
			}
		}

		if (funcType.equals("outputnum")) {
			if (funcValueRegister[0] == 1) {
				buf[bufPointer] = DLX.assemble(16, nextRegister, 0, funcValueRegister[1]);
				funcValueRegister[1] = nextRegister;
				bufPointer++;
				nextRegister++;
			}
			buf[bufPointer] = DLX.assemble(51, funcValueRegister[1]);
		} else {
			buf[bufPointer] = DLX.assemble(53);
		}
		bufPointer++;
		scanner.Next();
		return 0;
	}

	public int funcCallCheck(boolean activated, boolean isFunction) {

		scanner.Next();
		if (scanner.sym != 61) {
			printError(scanner.sym);
		}
		String funcType = scanner.Id2String(scanner.id);
		int[] funcValueRegister;

		if (funcType.equals("inputnum") || funcType.equals("outputnum") || funcType.equals("outputnewline")) {
			return predefinedFunc(activated, isFunction);
		}
		int currentRegs = nextRegister;
		nextRegister = 1;
		// Store all values in current registers
		for (int i = 1; i < currentRegs; i++) {
			buf[bufPointer] = DLX.assemble(38, i, 29, -4);
		}

		expect("(");
		scanner.Next();
		if (scanner.sym != 35) {
			funcValueRegister = expressionCheck(activated, isFunction);
			if (funcValueRegister[0] == 1) {
				buf[bufPointer] = DLX.assemble(16, nextRegister, 0, funcValueRegister[1]);
				funcValueRegister[1] = nextRegister;
				bufPointer++;

			}
			buf[bufPointer] = DLX.assemble(38, funcValueRegister[1], 29, -4);
			bufPointer++;
			nextRegister++;
			while (scanner.sym == 31) {
				scanner.Next();
				funcValueRegister = expressionCheck(activated, isFunction);
				if (funcValueRegister[0] == 1) {
					buf[bufPointer] = DLX.assemble(16, nextRegister, 0, funcValueRegister[1]);
					funcValueRegister[1] = nextRegister;
					bufPointer++;
				}
				buf[bufPointer] = DLX.assemble(38, funcValueRegister[1], 29, -4);
				bufPointer++;
				nextRegister++;
			}
			if (scanner.sym != 35) {
				printError(scanner.sym);
			}
		}
		int funcLocation = startingPc.get(funcType) * 4;
		buf[bufPointer] = DLX.assemble(48, funcLocation);
		bufPointer++;

		scanner.Next();
		for (int i = currentRegs - 1; i > 0; i--) {
			buf[bufPointer] = DLX.assemble(38, i, 29, 4);
		}
		nextRegister = currentRegs;
		buf[bufPointer] = DLX.assemble(32, nextRegister, 30, -4);
		bufPointer++;
		nextRegister++;
		return currentRegs;
	}

	public void ifStatementCheck(boolean activated, boolean isFunction) {
		scanner.Next();
		relationCheck(0, activated, isFunction);
		if (scanner.sym != 41) {
			printError(scanner.sym);
		}
		scanner.Next();
		statSequenceCheck(true, isFunction);

		buf[bufPointer] = DLX.assemble(40, 0, 1);
		elseCondLocation.add(0, bufPointer);
		bufPointer++;

		int ifLocation = ifCondLocation.remove(0);

		DLX.disassem(buf[ifLocation]);

		buf[ifLocation] = DLX.assemble(DLX.op, DLX.a, bufPointer - ifLocation);

		if (scanner.sym == 90) {
			scanner.Next();
			statSequenceCheck(true, isFunction);
		}

		int elseLocation = elseCondLocation.remove(0);

		buf[elseLocation] = DLX.assemble(40, 0, bufPointer - elseLocation);

		if (scanner.sym != 82) {
			printError(scanner.sym);
		}
		scanner.Next();
	}

	public void whileStatementCheck(boolean activated, boolean isFunction) {
		scanner.Next();
		relationCheck(1, activated, isFunction);
		if (scanner.sym != 42) {
			printError(scanner.sym);
		}
		scanner.Next();
		statSequenceCheck(true, isFunction);

		int conditionLocation = whileCondLocation.remove(0);

		buf[bufPointer] = DLX.assemble(40, 0, conditionLocation - bufPointer);
		bufPointer++;

		int whileLocation = whileCondLocation.remove(0);

		DLX.disassem(buf[whileLocation]);

		buf[whileLocation] = DLX.assemble(DLX.op, DLX.a, bufPointer - whileLocation);

		if (scanner.sym != 81) {
			printError(scanner.sym);
		}
		scanner.Next();
	}

	public void returnStatementCheck(boolean activated, boolean isFunction) {
		scanner.Next();
		if (scanner.sym == 70) {
			scanner.Next();
		}
		else {
			int[] expressionRegister = expressionCheck(activated, isFunction);
			if (expressionRegister[0] == 1) {
				buf[bufPointer] = DLX.assemble(16, nextRegister, 0, expressionRegister[1]);
				expressionRegister[1] = nextRegister;
				bufPointer++;
			}
			buf[bufPointer] = DLX.assemble(36, expressionRegister[1], 30, -4);
			bufPointer++;
			nextRegister++;
		}
	}

	public void statementCheck(boolean activated, boolean isFunction) {
		if (scanner.sym == 77) {
			assignmentCheck(activated, isFunction);
		} else if (scanner.sym == 100) {
			funcCallCheck(activated, isFunction);
		} else if (scanner.sym == 101) {
			ifStatementCheck(activated, isFunction);
		} else if (scanner.sym == 102) {
			whileStatementCheck(activated, isFunction);
		} else if (scanner.sym == 103 && isFunction) {
			returnStatementCheck(activated, true);
		}
		else {
			printError(scanner.sym);
		}
	}

	public void statSequenceCheck(boolean activated, boolean isFunction) {
		statementCheck(activated, isFunction);
		while (scanner.sym == 70) {

			scanner.Next();
			if (isStatCheck()) {
				statementCheck(activated, isFunction);

			} else {
				return;
			}
		}
	}

	public boolean isStatCheck() {
		return (scanner.sym == 77 || scanner.sym == 100 || scanner.sym == 101 || scanner.sym == 102 || scanner.sym == 103);
	}

	public void varDeclCheck(boolean isFunction) {
		currentGlobal = 0;
		scanner.Next();
		if (scanner.sym != 61) {
			printError(scanner.sym);
		}

		String currentIdent = scanner.Id2String(scanner.id);
		if (isFunction) {
			functionOffsetMap.put(currentIdent, numParams * -4);
			buf[bufPointer] = DLX.assemble(36, 0, 28, numParams * -4);
		} else {
			registerMap.put(currentIdent, currentGlobal);
			buf[bufPointer] = DLX.assemble(36, 0, 30, currentGlobal);
		}
		numParams++;
		bufPointer++;
		currentGlobal -= 4;
		scanner.Next();
		while (scanner.sym == 31) {
			scanner.Next();
			if (scanner.sym != 61) {
				printError(scanner.sym);
			}
			currentIdent = scanner.Id2String(scanner.id);
			if (isFunction) {
				functionOffsetMap.put(currentIdent, numParams * -4);
				buf[bufPointer] = DLX.assemble(36, 0, 28, numParams * -4);
			} else {
				registerMap.put(currentIdent, currentGlobal);
				buf[bufPointer] = DLX.assemble(36, 0, 30, currentGlobal);
			}
			numParams++;
			bufPointer++;
			currentGlobal -= 4;
			scanner.Next();
		}

		if (scanner.sym != 70) {
			printError(scanner.sym);
		}
	}

	public void funcDeclCheck() {
		scanner.Next();
		if (scanner.sym != 61) {
			printError(scanner.sym);
		}
		String funcType = scanner.Id2String(scanner.id);
		startingPc.put(funcType, bufPointer);

		scanner.Next();
		formalParamCheck();

		buf[bufPointer] = DLX.assemble(38, 31, 29, -4);
		bufPointer++;
		buf[bufPointer] = DLX.assemble(38, 28, 29, -4);
		bufPointer++;
		buf[bufPointer] = DLX.assemble(16, 28, 29, 0);
		bufPointer++;

		for (int i = 0; i < paramReg.size(); i++) {
			buf[bufPointer] = DLX.assemble(36, paramReg.remove(0), 28, i * -4);
			bufPointer++;
			functionOffsetMap.put(paramName.remove(0), i * -4);
		}

		scanner.Next();
		funcBodyCheck();

		scanner.Next();
		if (scanner.sym != 70) {
			printError(scanner.sym);
		}

		buf[bufPointer] = DLX.assemble(34, 28, 29, 4);
		bufPointer++;
		buf[bufPointer] = DLX.assemble(34, 31, 29, 4);
		bufPointer++;
		buf[bufPointer] = DLX.assemble(49, 31);
		bufPointer++;
	}

	public void formalParamCheck() {
		numParams = 0;
		if (scanner.sym != 50) {
			printError(scanner.sym);
		}
		scanner.Next();
		while (scanner.sym != 35) {

			if (scanner.sym != 61) {
				printError(scanner.sym);
			}
			buf[bufPointer] = DLX.assemble(34, nextRegister, 29, 4);
			bufPointer++;
			paramReg.add(0, nextRegister);
			paramName.add(0, scanner.Id2String(scanner.id));
			nextRegister++;

			numParams++;
			scanner.Next();
			if (scanner.sym == 31) {
				scanner.Next();
			}
		}
	}

	public void funcBodyCheck() {
		if (scanner.sym == 110) {
			varDeclCheck(true);
			scanner.Next();
		}
		int localVarSize = (numParams + 1) * 4;
		buf[bufPointer] = DLX.assemble(17, 29, 29, localVarSize);
		bufPointer++;

		if (scanner.sym != 150) {
			printError(scanner.sym);
		}
		scanner.Next();
		if (isStatCheck()) {
			statSequenceCheck(true, true);
		}
		if (scanner.sym != 80) {
			printError(scanner.sym);
		}

		buf[bufPointer] = DLX.assemble(16, 29, 29, localVarSize);
		bufPointer++;
	}

	public void computationCheck() {
		if (scanner.sym != 200) {
			printError(scanner.sym);
		}
		scanner.Next();

		// Check if next character is var, if so stores them in R30
		if (scanner.sym == 110) {
			varDeclCheck(false);
			scanner.Next();
		}

		buf[bufPointer] = DLX.assemble(17, 28, 30, (-1 * currentGlobal));
		bufPointer++;

		buf[bufPointer] = DLX.assemble(17, 29, 28, 0);
		bufPointer++;

		// Checks for "function" or "procedure"
		while (scanner.sym == 112 || scanner.sym == 113) {
			buf[bufPointer] = DLX.assemble(40, 0, 0);
			funcLocation = bufPointer;
			bufPointer++;
			funcDeclCheck();
			scanner.Next();
			DLX.disassem(buf[funcLocation]);
			buf[funcLocation] = DLX.assemble(DLX.op, DLX.a, bufPointer - funcLocation);
		}
		if (scanner.sym != 150) {
			printError(scanner.sym);
		}
		scanner.Next();
		statSequenceCheck(true, false);
		if (scanner.sym != 80) {
			printError(scanner.sym);
		}
		expect(".");
		buf[bufPointer] = DLX.assemble(49, 0);
		bufPointer++;

//		for (int i = 0; i < bufPointer; i++) {
//			DLX.disassem(buf[i]);
//			System.out.println("CD = " + i + " op = " + DLX.op + " a = " + DLX.a + " b = " + DLX.b + " c = " + DLX.c);
//		}


	}
}
