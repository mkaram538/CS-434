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
	java.util.Vector<Integer> assignmentRegisters = new java.util.Vector<>();
	int funcLocation;
	int buf[] = new int[3000];
	int bufPointer = 0;
	public java.util.Map<String, Integer> registerMap = new java.util.HashMap<>();
	public java.util.Map<String, Integer> functionOffsetMap = new java.util.HashMap<>();
	public java.util.Map<String, Integer> startingPc = new java.util.HashMap<>();
	public java.util.Map<String, Integer> arrayRowSize = new java.util.HashMap<>();
	public java.util.Map<String, Integer> arrayColumnSize = new java.util.HashMap<>();
	public java.util.Map<String, Integer> arrayLevelSize = new java.util.HashMap<>();

	// Constructor of your Compiler
	public Compiler(String args) {
		scanner = new Scanner(args);
	}

	// Implement this function to start compiling your input file
	public int[] getProgram() {
		computation();

		return java.util.Arrays.copyOf(buf, bufPointer);
	}

	int getNextRegister() {
		int currentReg = nextRegister;

		while (currentRegisters.contains(currentReg) || currentReg > 27) {
			if (currentReg < 1 || currentReg > 27) {
				currentReg = 1;
			} else {
				currentReg++;
			}
		}
		nextRegister = currentReg;
		return currentReg;
	}

	private void printError(int p) {
		for (int i = 0; i < bufPointer; i++) {
			DLX.disassem(buf[i]);
			System.out.println("CD = " + i + " op = " + DLX.op + " a = " + DLX.a + " b = " + DLX.b + " c = " + DLX.c);
		}
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
		int relOpRegister = getNextRegister();
		if (leftRegister[0] == 1 & rightRegister[0] == 1) {
			int comparison = leftRegister[1] - rightRegister[1];
			buf[bufPointer] = DLX.assemble(16, relOpRegister, 0, comparison);
		} else if (rightRegister[0] == 1) {
			buf[bufPointer] = DLX.assemble(17, relOpRegister, leftRegister[1], rightRegister[1]);
		} else {
			buf[bufPointer] = DLX.assemble(1, relOpRegister, leftRegister[1], rightRegister[1]);
		}
		if (!currentRegisters.contains(nextRegister)) currentRegisters.add(nextRegister);
		assignmentRegisters.add(nextRegister);
		bufPointer++;

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
		int designatorRegister;
		int offset;
		String currentIdent = scanner.Id2String(scanner.id);

		// Get starting index of variable in stack
		if (isFunction && functionOffsetMap.containsKey(currentIdent)) {
			offset = functionOffsetMap.get(currentIdent);
		} else if (!isFunction) {
			offset = registerMap.get(currentIdent);
		} else {
			offset = -1 * currentGlobal + registerMap.get(currentIdent);
			functionOffsetMap.put(currentIdent, offset);
		}

		scanner.Next();
		// If next character is [, then ident is an array
		if (scanner.sym == 32) {

			// 2 shows that this is an array
			designatorNumReg[0] = 2;
			int arrayIndexReg;
			int arrayIndex;
			scanner.Next();

			// Gets the register/value of the first index
			int[] firstIndex = expressionCheck(isFunction);
			if (firstIndex[0] == 1) {
				buf[bufPointer] = DLX.assemble(16, getNextRegister(), 0, firstIndex[1]);
				firstIndex[1] = nextRegister;
				bufPointer++;
				if (!currentRegisters.contains(nextRegister)) currentRegisters.add(nextRegister);
				assignmentRegisters.add(nextRegister);
				firstIndex[0] = 0;
			}
			int firstRegister = firstIndex[1];

			if (scanner.sym != 34) {
				printError(40);
			}
			scanner.Next();

			// If next character is [, then ident is a 2D array
			if (scanner.sym == 32) {
				scanner.Next();

				// Gets the register/value of the second index
				int[] secondIndex = expressionCheck(isFunction);
				if (secondIndex[0] == 1) {
					buf[bufPointer] = DLX.assemble(16, getNextRegister(), 0, secondIndex[1]);
					secondIndex[1] = nextRegister;
					secondIndex[0] = 0;
					if (!currentRegisters.contains(nextRegister)) currentRegisters.add(nextRegister);
					assignmentRegisters.add(nextRegister);
					bufPointer++;
				}
				int secondRegister = secondIndex[1];

				if (scanner.sym != 34) {
					printError(41);
				}
				scanner.Next();

				// If next character is [, then ident is a 3D array
				if (scanner.sym == 32) {
					scanner.Next();

					// Gets the register/value of the third index
					int[] thirdIndex = expressionCheck(isFunction);
					if (thirdIndex[0] == 1) {
						buf[bufPointer] = DLX.assemble(16, getNextRegister(), 0, thirdIndex[1]);
						thirdIndex[1] = nextRegister;
						bufPointer++;
						if (!currentRegisters.contains(nextRegister)) currentRegisters.add(nextRegister);
						assignmentRegisters.add(nextRegister);
						thirdIndex[0] = 0;
					}
					int thirdRegister = thirdIndex[1];

					// All indices are integers
					if (firstIndex[0] == 1 && secondIndex[0] == 1 && thirdIndex[0] == 1) {
						// Checks the indexes against the size of the array

						buf[bufPointer] = DLX.assemble(30, thirdRegister, arrayRowSize.get(currentIdent));
						bufPointer++;
						buf[bufPointer] = DLX.assemble(30, secondRegister, arrayColumnSize.get(currentIdent));
						bufPointer++;
						buf[bufPointer] = DLX.assemble(30, firstRegister, arrayLevelSize.get(currentIdent));
						bufPointer++;

						// Calculates final offset given that all inputs are integers
						arrayIndex = arrayRowSize.get(currentIdent) * arrayColumnSize.get(currentIdent) * firstIndex[1];
						arrayIndex += arrayRowSize.get(currentIdent) * secondIndex[1];
						arrayIndex += thirdIndex[1];
						offset = arrayIndex * -4 + offset;

						// Store offset in register
						arrayIndexReg = getNextRegister();
						buf[bufPointer] = DLX.assemble(16, arrayIndexReg, 0, offset);
						bufPointer++;
					} else {
						// Checks the indexes against the size of the array
						buf[bufPointer] = DLX.assemble(30, thirdRegister, arrayRowSize.get(currentIdent));
						bufPointer++;
						buf[bufPointer] = DLX.assemble(30, secondRegister, arrayColumnSize.get(currentIdent));
						bufPointer++;
						buf[bufPointer] = DLX.assemble(30, firstRegister, arrayLevelSize.get(currentIdent));
						bufPointer++;

						// Begin calculating the index of the specified index on stack
						arrayIndexReg = getNextRegister();
						designatorNumReg[1] = arrayIndexReg;

						int levelIndex = arrayRowSize.get(currentIdent) * arrayColumnSize.get(currentIdent);

						// Multiply the current row by the row size
						buf[bufPointer] = DLX.assemble(18, nextRegister, firstRegister, levelIndex);
						bufPointer++;

						int colIndexReg = getNextRegister();

						// Add the row offset with the column offset
						buf[bufPointer] = DLX.assemble(18, colIndexReg, secondRegister, arrayRowSize.get(currentIdent));
						bufPointer++;

						buf[bufPointer] = DLX.assemble(0, arrayIndexReg, arrayIndexReg, colIndexReg);
						bufPointer++;

						buf[bufPointer] = DLX.assemble(0, arrayIndexReg, arrayIndexReg, thirdRegister);
						bufPointer++;

						// Multiply result by 4 to get byte offset
						buf[bufPointer] = DLX.assemble(18, arrayIndexReg, arrayIndexReg, -4);
						bufPointer++;

						// Add array offset to base offset to get the stack index
						buf[bufPointer] = DLX.assemble(16, arrayIndexReg, arrayIndexReg, offset);
						bufPointer++;
					}

					if (scanner.sym != 34) {
						printError(40);
					}
					scanner.Next();
				}

				// Both indexes are integers
				if (firstIndex[0] == 1 && secondIndex[0] == 1) {
					// Checks the indexes against the size of the array
					buf[bufPointer] = DLX.assemble(30, secondRegister, arrayRowSize.get(currentIdent));
					bufPointer++;
					buf[bufPointer] = DLX.assemble(30, firstRegister, arrayColumnSize.get(currentIdent));
					bufPointer++;

					// Calculates final offset given that all inputs are integers
					arrayIndex = (arrayRowSize.get(currentIdent) * firstIndex[1] + secondIndex[1]) * -4;
					offset = arrayIndex + offset;

					// Store offset in register
					arrayIndexReg = getNextRegister();
					buf[bufPointer] = DLX.assemble(16, arrayIndexReg, 0, offset);
					bufPointer++;

				// Only first index is an integer
				} else if (firstIndex[0] == 1) {
					// Checks the indexes against the size of the array
					buf[bufPointer] = DLX.assemble(30, secondRegister, arrayRowSize.get(currentIdent));
					bufPointer++;
					buf[bufPointer] = DLX.assemble(30, firstRegister, arrayColumnSize.get(currentIdent));
					bufPointer++;

					arrayIndex = arrayRowSize.get(currentIdent) * firstIndex[1];
					// Add the row offset with the column offset
					buf[bufPointer] = DLX.assemble(16, getNextRegister(), secondRegister, arrayIndex);
					bufPointer++;

					// Multiply result by 4 to get byte offset
					buf[bufPointer] = DLX.assemble(18, nextRegister, nextRegister, -4);
					bufPointer++;

					arrayIndexReg = nextRegister;

					// Add array offset to base offset to get the stack index
					buf[bufPointer] = DLX.assemble(16, arrayIndexReg, arrayIndexReg, offset);
					bufPointer++;

				// Only second index is an integer
				} else if (secondIndex[0] == 1) {
					buf[bufPointer] = DLX.assemble(30, secondRegister, arrayRowSize.get(currentIdent));
					bufPointer++;
					buf[bufPointer] = DLX.assemble(30, firstRegister, arrayColumnSize.get(currentIdent));
					bufPointer++;

					arrayIndexReg = getNextRegister();

					// Multiply the current row by the row size
					buf[bufPointer] = DLX.assemble(18, nextRegister, firstRegister, arrayRowSize.get(currentIdent));
					bufPointer++;

					// Add the row offset with the column offset
					buf[bufPointer] = DLX.assemble(16, arrayIndexReg, arrayIndexReg, secondIndex[1]);
					bufPointer++;

					// Multiply result by 4 to get byte offset
					buf[bufPointer] = DLX.assemble(18, arrayIndexReg, arrayIndexReg, -4);
					bufPointer++;

					// Add array offset to base offset to get the stack index
					buf[bufPointer] = DLX.assemble(16, arrayIndexReg, arrayIndexReg, offset);
					bufPointer++;

				// Both indices are stored in registers
				} else {
					// Checks the indexes against the size of the array
					buf[bufPointer] = DLX.assemble(30, secondRegister, arrayRowSize.get(currentIdent));
					bufPointer++;
					buf[bufPointer] = DLX.assemble(30, firstRegister, arrayColumnSize.get(currentIdent));
					bufPointer++;

					// Begin calculating the index of the specified index on stack
					arrayIndexReg = getNextRegister();
					designatorNumReg[1] = arrayIndexReg;

					// Multiply the current row by the row size
					buf[bufPointer] = DLX.assemble(18, nextRegister, firstRegister, arrayRowSize.get(currentIdent));
					bufPointer++;

					// Add the row offset with the column offset
					buf[bufPointer] = DLX.assemble(0, arrayIndexReg, arrayIndexReg, secondRegister);
					bufPointer++;

					// Multiply result by 4 to get byte offset
					buf[bufPointer] = DLX.assemble(18, arrayIndexReg, arrayIndexReg, -4);
					bufPointer++;

					// Add array offset to base offset to get the stack index
					buf[bufPointer] = DLX.assemble(16, arrayIndexReg, arrayIndexReg, offset);
					bufPointer++;
				}

			// Branch is reached if ident is a 1D array
			} else {
				// Only index is an integer
				buf[bufPointer] = DLX.assemble(30, firstRegister, arrayRowSize.get(currentIdent));
				bufPointer++;
				if (firstIndex[0] == 1) {
					// Checks the indexes against the size of the array

					// Calculates final offset given that all inputs are integers
					arrayIndex = firstIndex[1] * -4;
					offset = arrayIndex + offset;

					// Store offset in register
					arrayIndexReg = getNextRegister();
					buf[bufPointer] = DLX.assemble(16, arrayIndexReg, 0, offset);

					// Only index is stored in register
				} else {
					// Checks the index against the row size

					// Multiplies the row index by 4 to get stack index
					arrayIndexReg = getNextRegister();
					buf[bufPointer] = DLX.assemble(18, nextRegister, firstRegister, -4);
					bufPointer++;

					// Add array offset to base offset to get the stack index
					buf[bufPointer] = DLX.assemble(16, arrayIndexReg, arrayIndexReg, offset);
				}
				bufPointer++;
			}
			// Store offset register in return array
			designatorNumReg[1] = arrayIndexReg;
			designatorRegister = arrayIndexReg;
		// Gets to this branch if ident is not an array
		} else {
			// Stores variable offset in designator register which is in return array
			buf[bufPointer] = DLX.assemble(16, getNextRegister(), 0, offset);
			designatorNumReg[1] = nextRegister;
			designatorRegister = nextRegister;
		}
		bufPointer++;
		if (!currentRegisters.contains(designatorRegister)) currentRegisters.add(designatorRegister);
		assignmentRegisters.add(nextRegister);
		return designatorNumReg;
	}

	public int[] factorCheck(boolean isFunction) {
		int[] factorNumReg = new int[2];
		int factorRegister = getNextRegister();

		// Factor is "(expression)"
		if (scanner.sym == 50) {
			scanner.Next();
			factorNumReg = expressionCheck(isFunction);
			if (factorNumReg[0] == 1) {
				buf[bufPointer] = DLX.assemble(16, factorRegister, 0, factorNumReg[1]);
				factorNumReg[0] = 0;
				factorNumReg[1] = factorRegister;
				bufPointer++;
			}
			if (scanner.sym != 35) {
				printError(3);
			}
			scanner.Next();
			if (!currentRegisters.contains(factorRegister)) currentRegisters.add(factorRegister);
			assignmentRegisters.add(nextRegister);
			return factorNumReg;
			// Factor is "number"
		} else if (scanner.sym == 60) {
			factorNumReg[0] = 1;
			factorNumReg[1] = scanner.val;
			scanner.Next();
			return factorNumReg;
			// Factor is "designator"
		} else if (scanner.sym == 61) {
			if (!currentRegisters.contains(factorRegister)) currentRegisters.add(factorRegister);
			assignmentRegisters.add(nextRegister);
			int offsetRegister = designatorCheck(isFunction)[1];
			// Stores the value at the passed offset in a register and returns it
			if (isFunction) {
				buf[bufPointer] = DLX.assemble(33, factorRegister, 28, offsetRegister);
			} else {
				buf[bufPointer] = DLX.assemble(33, factorRegister, 30, offsetRegister);
			}
			factorNumReg[1] = factorRegister;

			bufPointer++;
			return factorNumReg;
			// Factor is "funcCall"
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

		int[] leftFactorRegister = factorCheck(isFunction);
		boolean multiplying;
		// Needed for function calls

		if (scanner.sym != 1 && scanner.sym != 2) {
			return leftFactorRegister;
		}
		while (scanner.sym == 1 || scanner.sym == 2) {
			multiplying = scanner.sym == 1;
			scanner.Next();
			termRegister = getNextRegister();
			if (!currentRegisters.contains(nextRegister)) currentRegisters.add(nextRegister);
			assignmentRegisters.add(nextRegister);
			int[] rightFactorRegister = factorCheck(isFunction);
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
						buf[bufPointer] = DLX.assemble(16, getNextRegister(), 0, leftFactorRegister[1]);
						bufPointer++;
						buf[bufPointer] = DLX.assemble(3, termRegister, nextRegister, rightFactorRegister[1]);
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
			}
//			if (scanner.sym != 70) {
//				scanner.Next();
//			}
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
			expressionRegister = getNextRegister();
			if (!currentRegisters.contains(nextRegister)) currentRegisters.add(nextRegister);
			assignmentRegisters.add(nextRegister);
			int[] rightExpressionRegister = termCheck(isFunction);
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
		int[] designatorReg = designatorCheck(isFunction);
		int offsetRegister = designatorReg[1];

		if (scanner.sym != 40) {
			printError(50);
		}

		scanner.Next();
		assignmentRegisters.clear();
		int[] expressionRegister = expressionCheck(isFunction);

		// If expression is an integer
		if (expressionRegister[0] == 1) {
			// Store integer in new register
			buf[bufPointer] = DLX.assemble(16, getNextRegister(), 0, expressionRegister[1]);
			bufPointer++;

			// Store value of expression at offset register's value
			if (isFunction) {
				buf[bufPointer] = DLX.assemble(37, nextRegister, 28, offsetRegister);
			} else {
				buf[bufPointer] = DLX.assemble(37, nextRegister, 30, offsetRegister);
			}
			bufPointer++;
			return;
		}
		// Store value of expression at offset register's value
		if (isFunction) {
			buf[bufPointer] = DLX.assemble(37, expressionRegister[1], 28, offsetRegister);
		} else {
			buf[bufPointer] = DLX.assemble(37, expressionRegister[1], 30, offsetRegister);
		}
		bufPointer++;
		for (int i = 0; i < assignmentRegisters.size(); i++){
			currentRegisters.remove(assignmentRegisters.elementAt(i));
		}
		currentRegisters.clear();
	}

	public int predefinedFunc(boolean isFunction) {
		String funcType = scanner.Id2String(scanner.id);
		int[] funcValueRegister = new int[2];

		if (funcType.equals("outputnewline")) {
			buf[bufPointer] = DLX.assemble(53);
			scanner.Next();
			if (scanner.sym == 50) {
				expect(")");
				scanner.Next();
			}
			return 0;
		} else if (funcType.equals("inputnum")) {
			scanner.Next();
			if (scanner.sym == 50) {
				expect(")");
			}
			scanner.Next();
			int inputRegister = getNextRegister();
			buf[bufPointer] = DLX.assemble(50, inputRegister);
			if (!currentRegisters.contains(nextRegister)) currentRegisters.add(nextRegister);
			bufPointer++;
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
				buf[bufPointer] = DLX.assemble(16, getNextRegister(), 0, funcValueRegister[1]);
				funcValueRegister[1] = nextRegister;
				if (!currentRegisters.contains(nextRegister)) currentRegisters.add(nextRegister);
				bufPointer++;
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
				buf[bufPointer] = DLX.assemble(16, getNextRegister(), 0, funcValueRegister[1]);
				funcValueRegister[1] = nextRegister;
				bufPointer++;

			}
			buf[bufPointer] = DLX.assemble(38, funcValueRegister[1], 29, -4);
			if (!currentRegisters.contains(nextRegister)) currentRegisters.add(nextRegister);
			bufPointer++;

			while (scanner.sym == 31) {
				scanner.Next();
				funcValueRegister = expressionCheck(isFunction);
				if (funcValueRegister[0] == 1) {
					buf[bufPointer] = DLX.assemble(16, getNextRegister(), 0, funcValueRegister[1]);
					funcValueRegister[1] = nextRegister;
					bufPointer++;
				}
				buf[bufPointer] = DLX.assemble(38, funcValueRegister[1], 29, -4);
				if (!currentRegisters.contains(nextRegister)) currentRegisters.add(nextRegister);
				bufPointer++;
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

		buf[bufPointer] = DLX.assemble(32, getNextRegister(), 30, -4);
		if (!currentRegisters.contains(nextRegister)) currentRegisters.add(nextRegister);
		bufPointer++;
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
		} else if (scanner.sym != 82){
			int[] expressionRegister = expressionCheck(isFunction);
			if (expressionRegister[0] == 1) {
				buf[bufPointer] = DLX.assemble(16, getNextRegister(), 0, expressionRegister[1]);
				expressionRegister[1] = nextRegister;
				bufPointer++;
			}
			buf[bufPointer] = DLX.assemble(36, expressionRegister[1], 30, -4);
			if (!currentRegisters.contains(nextRegister)) currentRegisters.add(nextRegister);
			bufPointer++;
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
		// typeSize[var type][row size][col size][row size reg][col size reg]
		// typeSize[0] = var, typeSize[1] = 1D array, typeSize[2] = 2D array
		int[] typeSize = new int[7];
		// Type is var, pass back to varDecl
		if (scanner.sym == 110) {
			scanner.Next();
			return typeSize;
		// Type is array
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

			// Get and store first size value for array as register and value
			firstSize = scanner.val;
			typeSize[1] = firstSize;

			scanner.Next();
			if (scanner.sym != 34) {
				printError(32);
			}
			scanner.Next();

			// Type is 2D or 3D array
			if (scanner.sym == 32) {
				int secondSize;
				scanner.Next();
				if (scanner.sym != 60) {
					printError(33);
				}

				// Get and store second size value for array as register and value
				secondSize = scanner.val;
				typeSize[2] = secondSize;

				scanner.Next();
				if (scanner.sym != 34) {
					printError(34);
				}
				scanner.Next();

				// Type is 3D array
				if (scanner.sym == 32) {
					int thirdSize;
					scanner.Next();
					if (scanner.sym != 60) {
						printError(33);
					}

					// Get and store third size value for array as register and value
					thirdSize = scanner.val;
					typeSize[3] = thirdSize;

					scanner.Next();
					if (scanner.sym != 34) {
						printError(34);
					}
					scanner.Next();
					typeSize[0] = 3;
				}
				//Type is 2D array
				else {
					typeSize[0] = 2;
				}
			// Type is 1D array
			} else {
				typeSize[0] = 1;
			}
		}
		return typeSize;
	}

	public void varDeclCheck(boolean isFunction) {
		if (isFunction) {
			currentGlobal = 0;
		}
		// Passed back at "ident"
		int[] varTypeSize = typeDeclCheck(isFunction);
		if (scanner.sym != 61) {
			printError(14);
		}
		int arraySize;
		String currentIdent;

		do {
			// Necessary on multiple values
			if (scanner.sym == 31) {
				scanner.Next();
			}
			currentIdent = scanner.Id2String(scanner.id);
			// Variable is int
			if (varTypeSize[0] == 0) {
				if (isFunction) {
					functionOffsetMap.put(currentIdent, (numParams + 1) * -4);
					buf[bufPointer] = DLX.assemble(36, 0, 28, (numParams + 1) * -4);
					numParams++;
				} else {
					registerMap.put(currentIdent, currentGlobal);
					buf[bufPointer] = DLX.assemble(36, 0, 30, currentGlobal);
					currentGlobal -= 4;
				}


			// Variable is 1D array
			} else if (varTypeSize[0] == 1) {
				// Store first index, that is the size of the array
				arrayRowSize.put(currentIdent, varTypeSize[1]);
				arraySize = varTypeSize[1];

				// Index 1D array into stacks
				if (isFunction) {
					functionOffsetMap.put(currentIdent, (numParams + 1) * -4);
					buf[bufPointer] = DLX.assemble(36, 0, 28, (numParams + 1) * -4);
					// Increment numParams by the size of the array. That is the number of words needed for array
					numParams += arraySize;
				} else {
					registerMap.put(currentIdent, currentGlobal);
					buf[bufPointer] = DLX.assemble(36, 0, 30, currentGlobal);
					// Decrement currentGlobal by the byte size of the stack
					currentGlobal -= arraySize * 4;
				}



			// Variable is 2D array
			} else if (varTypeSize[0] == 2){
				// Store both indices, multiply both to get array size
				arrayRowSize.put(currentIdent, varTypeSize[2]);
				arrayColumnSize.put(currentIdent, varTypeSize[1]);
				arraySize = varTypeSize[2] * varTypeSize[1];

				// Index 2D array into stacks
				if (isFunction) {
					functionOffsetMap.put(currentIdent, (numParams + 1) * -4);
					buf[bufPointer] = DLX.assemble(36, 0, 28, (numParams + 1) * -4);
					// Increment numParams by the size of the array. That is the number of words needed for array
					numParams += arraySize;
				} else {
					registerMap.put(currentIdent, currentGlobal);
					buf[bufPointer] = DLX.assemble(36, 0, 30, currentGlobal);
					// Decrement currentGlobal by the byte size of the stack
					currentGlobal -= arraySize * 4;
				}


			// Variable is 3D array
			} else if (varTypeSize[0] == 3){
				// Store both indices, multiply both to get array size
				arrayRowSize.put(currentIdent, varTypeSize[3]);
				arrayColumnSize.put(currentIdent, varTypeSize[2]);
				arrayLevelSize.put(currentIdent, varTypeSize[1]);
				arraySize = varTypeSize[3] * varTypeSize[2] * varTypeSize[1];

				// Index 3D array into stacks
				if (isFunction) {
					functionOffsetMap.put(currentIdent, (numParams + 1) * -4);
					buf[bufPointer] = DLX.assemble(36, 0, 28, (numParams + 1) * -4);
					// Increment numParams by the size of the array. That is the number of words needed for array
					numParams += arraySize;
				} else {
					registerMap.put(currentIdent, currentGlobal);
					buf[bufPointer] = DLX.assemble(36, 0, 30, currentGlobal);
					// Decrement currentGlobal by the byte size of the stack
					currentGlobal -= arraySize * 4;
				}


			}
			bufPointer++;
			scanner.Next();
		} while (scanner.sym == 31);

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
			buf[bufPointer] = DLX.assemble(34, getNextRegister(), 29, 4);
			if (!currentRegisters.contains(nextRegister)) currentRegisters.add(nextRegister);
			bufPointer++;
			paramReg.add(0, nextRegister);
			paramName.add(0, scanner.Id2String(scanner.id));

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
		currentRegisters.clear();
		if (scanner.sym != 200) {
			printError(23);
		}
		scanner.Next();

		// Check if next character is "var" or "array", if so stores them in R30
		while (scanner.sym == 110 || scanner.sym == 111) {
			varDeclCheck(false);
			scanner.Next();
		}

		// Moves frame pointer to end of global variables
		buf[bufPointer] = DLX.assemble(17, 28, 30, (-1 * currentGlobal));
		bufPointer++;

		// Catches stack pointer up to frame pointer
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
