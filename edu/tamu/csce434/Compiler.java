package edu.tamu.csce434;


import java.io.Console;
import java.util.Vector;

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
	java.util.Vector<Integer> returnLocation = new java.util.Vector<>();
	java.util.Vector<java.util.Vector<Integer>> registersUsed = new java.util.Vector<>();
	java.util.Vector<Integer> currentRegisters = new java.util.Vector<>();
	int funcLocation;
	int buf[] = new int[3000];
	int bufPointer = 0;
	public java.util.Map<String, Integer> registerMap = new java.util.HashMap<>();
	public java.util.Map<String, Integer> functionOffsetMap = new java.util.HashMap<>();
	public java.util.Map<String, Integer> startingPc = new java.util.HashMap<>();
	public java.util.Map<String, Integer> arrayRowSize = new java.util.HashMap<>();
	public java.util.Map<String, Integer> arrayColumnSize = new java.util.HashMap<>();

	// Constructor of your Compiler
	public Compiler(String args) {
		scanner = new Scanner(args);
	}

	// Implement this function to start compiling your input file
	public int[] getProgram() {
		computation();

		return java.util.Arrays.copyOf(buf, bufPointer);
	}

	private void printError(int p) {
//		for (int i = 0; i < bufPointer; i++) {
//			DLX.disassem(buf[i]);
//			System.out.println("CD = " + i + " op = " + DLX.op + " a = " + DLX.a + " b = " + DLX.b + " c = " + DLX.c);
//		}
		System.out.println("error: " + p);
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

		printError(1);
	}

	// Implement this function to start parsing your input file
	public void computation() {
		token = scanner.sym;

		computationCheck();
	}

	public void relOpCheck() {
		if (scanner.sym < 20 || scanner.sym > 25) {
			printError(2);
		}
	}

	public void relOperation(int statementType, int leftRegister[], int relationType, int rightRegister[]) {
		int relOpRegister = nextRegister;
		if (leftRegister[0] == 1 & rightRegister[0] == 1) {
			int comparison = leftRegister[1] - rightRegister[1];
			buf[bufPointer] = DLX.assemble(16, relOpRegister, 0, comparison);
		} else if (rightRegister[0] == 1) {
			buf[bufPointer] = DLX.assemble(17, relOpRegister, leftRegister[1], rightRegister[1]);
		} else {
			buf[bufPointer] = DLX.assemble(1, relOpRegister, leftRegister[1], rightRegister[1]);
		}
		if (!currentRegisters.contains(nextRegister)) currentRegisters.add(nextRegister);
		bufPointer++;
		nextRegister++;

		if (statementType == 0) {
			ifCondLocation.add(0, bufPointer);
		} else {
			whileCondLocation.add(0, bufPointer);
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

	public int[] designatorCheck(boolean isFunction) {
		int[] designatorNumReg = new int[2];
		int designatorRegister = nextRegister;
		int offset;
		String currentIdent = scanner.Id2String(scanner.id);

		if (isFunction) {
			offset = functionOffsetMap.get(currentIdent);
		} else {
			offset = registerMap.get(currentIdent);
		}
		designatorNumReg[1] = designatorRegister;

		scanner.Next();
		if (scanner.sym == 32) {
			int arrayIndex;
			scanner.Next();
			int[] firstIndex = expressionCheck(isFunction);
			int firstRegister = firstIndex[1];
			if (firstIndex[0] == 1) {
				buf[bufPointer] = DLX.assemble(16, nextRegister, 0, firstIndex[1]);
				firstRegister = nextRegister;
				bufPointer++;
				nextRegister++;
			}
			if (scanner.sym != 34) {
				printError(40);
			}
			scanner.Next();
			if (scanner.sym == 32) {
				scanner.Next();
				int[] secondIndex = expressionCheck(isFunction);
				int secondRegister = secondIndex[1];
				if (secondIndex[0] == 1) {
					buf[bufPointer] = DLX.assemble(16, nextRegister, 0, secondIndex[1]);
					secondRegister = nextRegister;
					bufPointer++;
					nextRegister++;
				}

				buf[bufPointer] = DLX.assemble(14, secondRegister, arrayRowSize.get(currentIdent));
				bufPointer++;
				buf[bufPointer] = DLX.assemble(14, firstRegister, arrayColumnSize.get(currentIdent));
				bufPointer++;

				arrayIndex = 

				if (scanner.sym != 34) {
					printError(40);
				}
				scanner.Next();
			} else {
				buf[bufPointer] = DLX.assemble(14, firstRegister, arrayRowSize.get(currentIdent));
				bufPointer++;
			}
		} else {
			if (isFunction) {
				buf[bufPointer] = DLX.assemble(32, designatorRegister, 28, offset);
			} else {
				buf[bufPointer] = DLX.assemble(32, designatorRegister, 30, offset);
			}
			bufPointer++;
			if (!currentRegisters.contains(nextRegister)) currentRegisters.add(nextRegister);
			nextRegister++;
		}
		return designatorNumReg;
	}

	public int[] factorCheck(boolean isFunction) {
		int[] factorNumReg = new int[2];
		int factorRegister = nextRegister;
		if (!currentRegisters.contains(nextRegister)) currentRegisters.add(nextRegister);
		if (scanner.sym == 50) {
			scanner.Next();
			factorNumReg = expressionCheck(isFunction);
			if (factorNumReg[0] == 1) {
				buf[bufPointer] = DLX.assemble(16, factorRegister, 0, factorNumReg[1]);
				factorNumReg[0] = 0;
				factorNumReg[1] = factorRegister;
				bufPointer++;
				if (!currentRegisters.contains(nextRegister)) currentRegisters.add(nextRegister);
				nextRegister++;
			}
			if (scanner.sym != 35) {
				printError(3);
			}
			scanner.Next();
			return factorNumReg;
		} else if (scanner.sym == 60) {
			factorNumReg[0] = 1;
			factorNumReg[1] = scanner.val;
			scanner.Next();
			return factorNumReg;
		} else if (scanner.sym == 61) {
			factorNumReg = designatorCheck(isFunction);
			return factorNumReg;
		} else if (scanner.sym == 100) {
			factorRegister = funcCallCheck(isFunction);
			factorNumReg[1] = factorRegister;
			return factorNumReg;
		} else {
			printError(4);
		}
		factorNumReg[1] = factorRegister;
		scanner.Next();
		return factorNumReg;
	}

	public int[] termCheck(boolean isFunction) {
		int[] termNumReg;
		int termRegister;
		if (!currentRegisters.contains(nextRegister)) currentRegisters.add(nextRegister);
		int[] leftFactorRegister = factorCheck(isFunction);
		boolean multiplying;
		// Needed for function calls

		if (scanner.sym != 1 && scanner.sym != 2) {
			return leftFactorRegister;
		}
		while (scanner.sym == 1 || scanner.sym == 2) {
			multiplying = scanner.sym == 1;
			scanner.Next();
			int[] rightFactorRegister = factorCheck(isFunction);
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
						if (!currentRegisters.contains(nextRegister)) currentRegisters.add(nextRegister);
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
			if (scanner.sym != 70) {
				scanner.Next();
			}
		}
		termNumReg = leftFactorRegister;
		return termNumReg;
	}

	public int[] expressionCheck(boolean isFunction) {
		int[] exprNumReg;
		int expressionRegister;
		int[] leftExpressionRegister = termCheck(isFunction);
		boolean adding;
		if (scanner.sym != 11 && scanner.sym != 12) {
			return leftExpressionRegister;
		}
		while (scanner.sym == 11 || scanner.sym == 12) {
			adding = scanner.sym == 11;
			scanner.Next();
			int[] rightExpressionRegister = termCheck(isFunction);
			expressionRegister = nextRegister;
			if (!currentRegisters.contains(nextRegister)) currentRegisters.add(nextRegister);
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

	public void relationCheck(int statementType, boolean isFunction) {
		int[] leftRegister;
		int relationType;
		int[] rightRegister;

		leftRegister = expressionCheck(isFunction);
		relationType = scanner.sym;
		relOpCheck();
		scanner.Next();
		rightRegister = expressionCheck(isFunction);
		relOperation(statementType, leftRegister, relationType, rightRegister);
	}

	public void assignmentCheck(boolean isFunction) {
		scanner.Next();
		if (scanner.sym != 61) {
			printError(5);
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
		int[] expressionRegister = expressionCheck(isFunction);

		if (expressionRegister[0] == 1) {
			buf[bufPointer] = DLX.assemble(16, nextRegister, 0, expressionRegister[1]);
			bufPointer++;
			if (isFunction) {
				buf[bufPointer] = DLX.assemble(36, nextRegister, 28, offset);
			} else {
				buf[bufPointer] = DLX.assemble(36, nextRegister, 30, offset);
			}
			if (!currentRegisters.contains(nextRegister)) currentRegisters.add(nextRegister);
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

	public int predefinedFunc(boolean isFunction) {
		String funcType = scanner.Id2String(scanner.id);
		int[] funcValueRegister = new int[2];

		if (funcType.equals("inputnum")) {
			scanner.Next();
			if (scanner.sym == 50) {
				expect(")");
			}
			scanner.Next();
			int inputRegister = nextRegister;
			buf[bufPointer] = DLX.assemble(50, inputRegister);
			if (!currentRegisters.contains(nextRegister)) currentRegisters.add(nextRegister);
			bufPointer++;
			nextRegister++;
			return inputRegister;
		}

		expect("(");
		scanner.Next();
		if (scanner.sym != 35) {

			funcValueRegister = expressionCheck(isFunction);
			if (scanner.sym != 35) {
				printError(6);
			}
		}

		if (funcType.equals("outputnum")) {
			if (funcValueRegister[0] == 1) {
				buf[bufPointer] = DLX.assemble(16, nextRegister, 0, funcValueRegister[1]);
				funcValueRegister[1] = nextRegister;
				if (!currentRegisters.contains(nextRegister)) currentRegisters.add(nextRegister);
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

	public int funcCallCheck(boolean isFunction) {

		scanner.Next();

		if (scanner.sym != 61) {
			printError(7);
		}
		String funcType = scanner.Id2String(scanner.id);
		int[] funcValueRegister;

		if (funcType.equals("inputnum") || funcType.equals("outputnum") || funcType.equals("outputnewline")) {
			return predefinedFunc(isFunction);
		}

		nextRegister = 1;
		// Store all values in current registers
		for (int i = 0; i < currentRegisters.size(); i++) {
			buf[bufPointer] = DLX.assemble(38, currentRegisters.elementAt(i), 29, -4);
			bufPointer++;
		}

		registersUsed.add(currentRegisters);
		currentRegisters = new Vector<>();

		expect("(");
		scanner.Next();
		if (scanner.sym != 35) {
			funcValueRegister = expressionCheck(isFunction);
			if (funcValueRegister[0] == 1) {
				buf[bufPointer] = DLX.assemble(16, nextRegister, 0, funcValueRegister[1]);
				funcValueRegister[1] = nextRegister;
				bufPointer++;

			}
			buf[bufPointer] = DLX.assemble(38, funcValueRegister[1], 29, -4);
			if (!currentRegisters.contains(nextRegister)) currentRegisters.add(nextRegister);
			bufPointer++;
			nextRegister++;
			while (scanner.sym == 31) {
				scanner.Next();
				funcValueRegister = expressionCheck(isFunction);
				if (funcValueRegister[0] == 1) {
					buf[bufPointer] = DLX.assemble(16, nextRegister, 0, funcValueRegister[1]);
					funcValueRegister[1] = nextRegister;
					bufPointer++;
				}
				buf[bufPointer] = DLX.assemble(38, funcValueRegister[1], 29, -4);
				if (!currentRegisters.contains(nextRegister)) currentRegisters.add(nextRegister);
				bufPointer++;
				nextRegister++;
			}
			if (scanner.sym != 35) {
				printError(8);
			}
		}

		int funcLocation = startingPc.get(funcType) * 4;
		buf[bufPointer] = DLX.assemble(48, funcLocation);
		bufPointer++;

		scanner.Next();
		currentRegisters = registersUsed.remove(0);
		for (int i = currentRegisters.size() - 1; i >= 0; i--) {
			buf[bufPointer] = DLX.assemble(34, currentRegisters.elementAt(i), 29, 4);
			bufPointer++;
		}

		nextRegister = currentRegisters.size() > 0 ? currentRegisters.lastElement() + 1 : 1;

		buf[bufPointer] = DLX.assemble(32, nextRegister, 30, -4);
		if (!currentRegisters.contains(nextRegister)) currentRegisters.add(nextRegister);
		bufPointer++;
		nextRegister++;
		return currentRegisters.lastElement();
	}

	public void ifStatementCheck(boolean isFunction) {
		scanner.Next();
		relationCheck(0, isFunction);
		if (scanner.sym != 41) {
			printError(9);
		}
		scanner.Next();
		statSequenceCheck(isFunction);

		buf[bufPointer] = DLX.assemble(40, 0, 1);
		elseCondLocation.add(0, bufPointer);
		bufPointer++;

		int ifLocation = ifCondLocation.remove(0);

		DLX.disassem(buf[ifLocation]);

		buf[ifLocation] = DLX.assemble(DLX.op, DLX.a, bufPointer - ifLocation);

		if (scanner.sym == 90) {
			scanner.Next();
			statSequenceCheck(isFunction);
		}

		int elseLocation = elseCondLocation.remove(0);

		buf[elseLocation] = DLX.assemble(40, 0, bufPointer - elseLocation);

		if (scanner.sym != 82) {
			printError(10);
		}
		scanner.Next();
	}

	public void whileStatementCheck(boolean isFunction) {
		scanner.Next();
		int conditionLocation = bufPointer;
		relationCheck(1, isFunction);
		if (scanner.sym != 42) {
			printError(11);
		}
		scanner.Next();
		statSequenceCheck(isFunction);

		buf[bufPointer] = DLX.assemble(40, 0, conditionLocation - bufPointer);
		bufPointer++;

		int whileLocation = whileCondLocation.remove(0);

		DLX.disassem(buf[whileLocation]);

		buf[whileLocation] = DLX.assemble(DLX.op, DLX.a, bufPointer - whileLocation);

		if (scanner.sym != 81) {
			printError(12);
		}
		scanner.Next();
	}

	public void returnStatementCheck(boolean isFunction) {
		scanner.Next();
		if (scanner.sym == 70) {
			scanner.Next();
		}
		else {
			int[] expressionRegister = expressionCheck(isFunction);
			if (expressionRegister[0] == 1) {
				buf[bufPointer] = DLX.assemble(16, nextRegister, 0, expressionRegister[1]);
				expressionRegister[1] = nextRegister;
				bufPointer++;
			}
			buf[bufPointer] = DLX.assemble(36, expressionRegister[1], 30, -4);
			if (!currentRegisters.contains(nextRegister)) currentRegisters.add(nextRegister);
			bufPointer++;
			nextRegister++;
		}
		// Branches to the end of the function in case of returns before end of function
		buf[bufPointer] = DLX.assemble(40, 0, 0);
		returnLocation.add(bufPointer);
		bufPointer++;
	}

	public void statementCheck(boolean isFunction) {
		if (scanner.sym == 77) {
			assignmentCheck(isFunction);
		} else if (scanner.sym == 100) {
			funcCallCheck(isFunction);
		} else if (scanner.sym == 101) {
			ifStatementCheck(isFunction);
		} else if (scanner.sym == 102) {
			whileStatementCheck(isFunction);
		} else if (scanner.sym == 103 && isFunction) {
			returnStatementCheck(true);
		}
		else {
			printError(13);
		}
	}

	public void statSequenceCheck(boolean isFunction) {
		statementCheck(isFunction);
		while (scanner.sym == 70) {

			scanner.Next();
			if (isStatCheck()) {
				statementCheck(isFunction);

			} else {
				return;
			}
		}
	}

	public boolean isStatCheck() {
		return (scanner.sym == 77 || scanner.sym == 100 || scanner.sym == 101 || scanner.sym == 102 || scanner.sym == 103);
	}

	public int[] typeDeclCheck(boolean isFunction) {
		int[] typeSize = new int[3];
		if (scanner.sym == 110) {
			varDeclCheck(isFunction);
		} else if (scanner.sym == 111){
			int firstSize;
			scanner.Next();
			if (scanner.sym != 32) {
				printError(30);
			}
			scanner.Next();
			if (scanner.sym != 60) {
				printError(31);
			}
			firstSize = scanner.val;
			typeSize[1] = firstSize;
			scanner.Next();
			if (scanner.sym != 34) {
				printError(32);
			}
			scanner.Next();
			if (scanner.sym == 32) {
				int secondSize;
				scanner.Next();
				if (scanner.sym != 60) {
					printError(33);
				}
				secondSize = scanner.val;
				typeSize[2] = secondSize;
				scanner.Next();
				if (scanner.sym != 34) {
					printError(34);
				}
				scanner.Next();
				typeSize[0] = 2;
			} else {
				typeSize[0] = 1;
			}

		}
		return typeSize;
	}

	public void varDeclCheck(boolean isFunction) {
		currentGlobal = 0;
		int[] varTypeSize = typeDeclCheck(isFunction);
		if (scanner.sym != 61) {
			printError(14);
		}
		int arraySize;

		String currentIdent = scanner.Id2String(scanner.id);
		if (varTypeSize[0] == 0) {
			if (isFunction) {
				functionOffsetMap.put(currentIdent, (numParams + 1) * -4);
				buf[bufPointer] = DLX.assemble(36, 0, 28, (numParams + 1) * -4);
			} else {
				registerMap.put(currentIdent, currentGlobal);
				buf[bufPointer] = DLX.assemble(36, 0, 30, currentGlobal);
			}
			currentGlobal -= 4;
			numParams++;
		} else if (varTypeSize[0] == 1) {
			buf[bufPointer] = DLX.assemble(16, nextRegister, 0, varTypeSize[1]);
			arrayRowSize.put(currentIdent, nextRegister);
			bufPointer++;
			nextRegister++;

			arraySize = varTypeSize[1];
			if (isFunction) {
				functionOffsetMap.put(currentIdent, (numParams + 1) * -4);
				buf[bufPointer] = DLX.assemble(36, 0, 28, (numParams + 1) * -4);
			} else {
				registerMap.put(currentIdent, currentGlobal);
				buf[bufPointer] = DLX.assemble(36, 0, 30, currentGlobal);
			}
			numParams += arraySize;
			currentGlobal -= arraySize * 4;
		} else {
			buf[bufPointer] = DLX.assemble(16, nextRegister, 0, varTypeSize[2]);
			arrayRowSize.put(currentIdent, nextRegister);
			bufPointer++;
			nextRegister++;

			buf[bufPointer] = DLX.assemble(16, nextRegister, 0, varTypeSize[1]);
			arrayColumnSize.put(currentIdent, nextRegister);
			bufPointer++;
			nextRegister++;

			arraySize = varTypeSize[2] * varTypeSize[1];
			if (isFunction) {
				functionOffsetMap.put(currentIdent, (numParams + 1) * -4);
				buf[bufPointer] = DLX.assemble(36, 0, 28, (numParams + 1) * -4);
			} else {
				registerMap.put(currentIdent, currentGlobal);
				buf[bufPointer] = DLX.assemble(36, 0, 30, currentGlobal);
			}
			numParams += arraySize;
			currentGlobal -= arraySize * 4;
		}


		bufPointer++;

		scanner.Next();
		while (scanner.sym == 31) {
			scanner.Next();
			if (scanner.sym != 61) {
				printError(15);
			}
			currentIdent = scanner.Id2String(scanner.id);
			if (isFunction) {
				functionOffsetMap.put(currentIdent, (numParams + 1) * -4);
				buf[bufPointer] = DLX.assemble(36, 0, 28, (numParams + 1) * -4);
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
			printError(16);
		}
	}

	public void funcDeclCheck() {
		scanner.Next();
		if (scanner.sym != 61) {
			printError(17);
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

		for (int i = paramReg.size() - 1; i >= 0; i--) {
			buf[bufPointer] = DLX.assemble(36, paramReg.elementAt(paramReg.size() - 1 - i), 28, (i + 1) * -4);
			bufPointer++;
			functionOffsetMap.put(paramName.elementAt(i), (i + 1) * -4);
		}
		paramReg.clear();
		paramName.clear();

		scanner.Next();
		funcBodyCheck();
		scanner.Next();
		if (scanner.sym != 70) {
			printError(18);
		}

		buf[bufPointer] = DLX.assemble(34, 28, 29, 4);
		bufPointer++;
		buf[bufPointer] = DLX.assemble(34, 31, 29, 4);
		bufPointer++;
		buf[bufPointer] = DLX.assemble(49, 31);
		bufPointer++;

		nextRegister = 1;
	}

	public void formalParamCheck() {
		numParams = 0;
		if (scanner.sym != 50) {
			printError(19);
		}
		scanner.Next();
		while (scanner.sym != 35) {

			if (scanner.sym != 61) {
				printError(20);
			}
			buf[bufPointer] = DLX.assemble(34, nextRegister, 29, 4);
			if (!currentRegisters.contains(nextRegister)) currentRegisters.add(nextRegister);
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

		if (scanner.sym == 110 || scanner.sym == 111) {

			varDeclCheck(true);
			scanner.Next();
		}
		int localVarSize = (numParams + 1) * 4;
		buf[bufPointer] = DLX.assemble(17, 29, 29, localVarSize);
		bufPointer++;

		if (scanner.sym != 150) {
			printError(21);
		}
		scanner.Next();
		if (isStatCheck()) {
			statSequenceCheck(true);
		}
		if (scanner.sym != 80) {
			printError(22);
		}

		int returnInstruction;
		// Going back and setting branch location for returns here
		for (int i = 0; i < returnLocation.size(); i++) {
			returnInstruction = returnLocation.elementAt(i);
			DLX.disassem(buf[returnInstruction]);
			buf[returnInstruction] = DLX.assemble(DLX.op, DLX.a, bufPointer - returnInstruction);
		}
		returnLocation.clear();

		// Begins restoring pointers post function call
		buf[bufPointer] = DLX.assemble(16, 29, 29, localVarSize);
		bufPointer++;
	}

	public void computationCheck() {
		if (scanner.sym != 200) {
			printError(23);
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
			printError(24);
		}
		currentRegisters.clear();
		scanner.Next();
		statSequenceCheck(false);
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
//		System.out.println("Num Instructions: " + bufPointer);


	}
}
