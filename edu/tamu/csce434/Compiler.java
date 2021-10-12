package edu.tamu.csce434;


import java.io.IOException;

public class Compiler
{
	private edu.tamu.csce434.Scanner scanner;
	private int token;
	private int nextRegister = 1;
	private int startCondRegister = 1;
	private int endCondRegister = 1;
	private java.util.Vector<int[]> storedInstructions = new java.util.Vector<>();
	int buf[] = new int[3000];
	int bufPointer = 0;
	public java.util.Map<String, Integer> registerMap = new java.util.HashMap<>();
	
	
	// Constructor of your Compiler
	public Compiler(String args)
	{
		
		scanner = new Scanner(args);
		
	}
	
	
	
	// Implement this function to start compiling your input file
	public int[] getProgram()  
	{
		computation();

		return java.util.Arrays.copyOf(buf, bufPointer);
	}

	private void printError(int i)
	{
		System.out.println("error");
		System.exit(0);
	}

	// Use this function to accept a Token and and to get the next Token from the Scanner
	private boolean accept(String s)
	{
		scanner.Next();
		token = scanner.sym;
		return scanner.String2Id(s) == token;
	}

	// Use this function whenever your program needs to expect a specific token
	private void expect(String s)
	{
		if (accept(s))
			return;

		printError(scanner.sym);

	}

	// Implement this function to start parsing your input file
	public void computation()
	{
		token = scanner.sym;

		computationCheck();
	}

	public void relOpCheck() {
		if (scanner.sym < 20 || scanner.sym > 25) {
			printError(scanner.sym);
		}
	}

	public boolean relOperation(int leftRegister, int relationType, int rightRegister) {
		int relOpRegister = bufPointer;
		buf[bufPointer] = DLX.assemble(1, relOpRegister, leftRegister, rightRegister);
		bufPointer++;
		nextRegister++;

		endCondRegister = bufPointer;
		switch (relationType) {
			case 20:
				buf[bufPointer] = DLX.assemble(40, relOpRegister, 0);
				bufPointer++;
			case 21:
				buf[bufPointer] = DLX.assemble(41, relOpRegister, 0);
				bufPointer++;
			case 22:
				buf[bufPointer] = DLX.assemble(42, relOpRegister, 0);
				bufPointer++;
			case 23:
				buf[bufPointer] = DLX.assemble(43, relOpRegister, 0);
				bufPointer++;
			case 24:
				buf[bufPointer] = DLX.assemble(44, relOpRegister, 0);
				bufPointer++;
			case 25:
				buf[bufPointer] = DLX.assemble(45, relOpRegister, 0);
				bufPointer++;
		}
		return false;
	}

	public int factorCheck(boolean activated) {
		int factorRegister = nextRegister;
		if (scanner.sym == 50) {
			scanner.Next();
			factorRegister = expressionCheck(activated);
			if (scanner.sym != 35) {
				printError(scanner.sym);
			}
		} else if (scanner.sym == 60) {
			buf[bufPointer] = DLX.assemble(16, factorRegister, 0,scanner.val);
			bufPointer++;
			nextRegister++;
		} else if (scanner.sym == 61) {
			factorRegister = registerMap.get(scanner.Id2String(scanner.id));
		} else if (scanner.sym == 100) {
			factorRegister = funcCallCheck(activated);
		} else {
			printError(scanner.sym);
		}
		return factorRegister;
	}

	public int termCheck(boolean activated) {
		int termRegister = nextRegister;
		int leftFactorRegister = factorCheck(activated);
		boolean multiplying;
		scanner.Next();
		if (scanner.sym != 1 && scanner.sym != 2) {
			return leftFactorRegister;
		}
		while (scanner.sym == 1 || scanner.sym == 2) {
			multiplying = scanner.sym == 1;
			scanner.Next();
			int rightFactorRegister = factorCheck(activated);
			if (multiplying) {
				buf[bufPointer] = DLX.assemble(2, termRegister, leftFactorRegister, rightFactorRegister);
			} else {
				buf[bufPointer] = DLX.assemble(3, termRegister, leftFactorRegister, rightFactorRegister);
			}
			bufPointer++;
			scanner.Next();
		}
		nextRegister++;
		return termRegister;
	}

	public int expressionCheck(boolean activated) {
		int expressionRegister = nextRegister;
		int leftExpressionRegister = termCheck(activated);
		boolean adding;
		if (scanner.sym != 11 && scanner.sym != 12) {
			return leftExpressionRegister;
		}
		while (scanner.sym == 11 || scanner.sym == 12) {
			adding = scanner.sym == 11;
			scanner.Next();
			int rightExpressionRegister = termCheck(activated);
			if (adding) {
				buf[bufPointer] = DLX.assemble(0, expressionRegister, leftExpressionRegister, rightExpressionRegister);
			} else {
				buf[bufPointer] = DLX.assemble(1, expressionRegister, leftExpressionRegister, rightExpressionRegister);
			}
			bufPointer++;
		}
		nextRegister++;
		return expressionRegister;
	}

	public boolean relationCheck(boolean activated) {
		int leftRegister;
		int relationType;
		int rightRegister;

		leftRegister = expressionCheck(activated);
		relationType = scanner.sym;
		relOpCheck();
		scanner.Next();
		rightRegister = expressionCheck(activated);
		return relOperation(leftRegister, relationType, rightRegister);
	}

	public void assignmentCheck(boolean activated) {
		scanner.Next();
		if (scanner.sym != 61) {
			printError(scanner.sym);
		}
		String currentIdent = scanner.Id2String(scanner.id);
		expect("<-");
		scanner.Next();
		int expressionRegister = expressionCheck(activated);
		if (activated) {
			int register = registerMap.get(currentIdent);
			buf[bufPointer] = DLX.assemble(16, register, expressionRegister, 0);
			bufPointer++;
		}
	}

	public int funcCallCheck(boolean activated) {
		scanner.Next();
		if (scanner.sym != 61) {
			printError(scanner.sym);
		}
		String funcType = scanner.Id2String(scanner.id);
		int funcValueRegister = 0;

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
				return nextRegister;
			}
			return 0;
		} else {
			expect("(");
			scanner.Next();
			if (scanner.sym != 35) {
				funcValueRegister = expressionCheck(activated);
				while (scanner.sym == 31) {
					scanner.Next();
					expressionCheck(activated);
				}
				if (scanner.sym != 35) {
					printError(scanner.sym);
				}
			}
			if (activated) {
				switch (funcType) {
					case "outputnum":
						buf[bufPointer] = DLX.assemble(51, funcValueRegister);
						bufPointer++;
						break;
					case "outputnewline":
						buf[bufPointer] = DLX.assemble(53);
						bufPointer++;
						break;
				}
			}
			scanner.Next();
		}
		return 0;
	}

	public void ifStatementCheck(boolean activated) {
		scanner.Next();
		boolean ifValue = relationCheck(activated);
		if (scanner.sym != 41) {
			printError(scanner.sym);
		}
		scanner.Next();
		statSequenceCheck(ifValue && activated);

		if (scanner.sym == 90) {
			scanner.Next();
			statSequenceCheck(!ifValue && activated);
		}
		if (scanner.sym != 82) {
			printError(scanner.sym);
		}
		scanner.Next();
	}

	public void whileStatementCheck(boolean activated) {
		scanner.Next();

	}

	public void statementCheck(boolean activated) {
		if (scanner.sym == 77) {
			assignmentCheck(activated);
		} else if (scanner.sym == 100) {
			funcCallCheck(activated);
		} else if (scanner.sym == 101) {
			ifStatementCheck(activated);
		} else if (scanner.sym == 102) {
			whileStatementCheck(activated);
		}
		else {
			printError(scanner.sym);
		}
	}

	public void statSequenceCheck(boolean activated) {
		statementCheck(activated);
		while (scanner.sym == 70) {
			scanner.Next();
			if (scanner.sym == 77 || scanner.sym == 100 || scanner.sym == 101) {
				statementCheck(activated);
			} else {
				return;
			}
		}
	}

	public void varDeclCheck() {
		scanner.Next();
		if (scanner.sym != 61) {
			printError(scanner.sym);
		}
		String currentIdent = scanner.Id2String(scanner.id);
		registerMap.put(currentIdent, nextRegister);
		buf[bufPointer] = DLX.assemble(16, nextRegister, 0,0);
		bufPointer++;
		nextRegister++;
		scanner.Next();
		while (scanner.sym == 31) {
			scanner.Next();
			if (scanner.sym != 61) {
				printError(scanner.sym);
			}
			currentIdent = scanner.Id2String(scanner.id);
			registerMap.put(currentIdent, nextRegister);
			buf[bufPointer] = DLX.assemble(16, nextRegister, 0,0);
			bufPointer++;
			nextRegister++;
			scanner.Next();
		}

		if (scanner.sym != 70) {
			printError(scanner.sym);
		}
	}

	public void computationCheck() {
		if (scanner.sym != 200) {
			printError(scanner.sym);
		}
		scanner.Next();
		if (scanner.sym == 110) {
			varDeclCheck();
			scanner.Next();
		}
		if (scanner.sym != 150) {
			printError(scanner.sym);
		}
		scanner.Next();
		statSequenceCheck(true);
		if (scanner.sym != 80) {
			printError(scanner.sym);
		}
		expect(".");
		buf[bufPointer] = DLX.assemble(49, 0);
		bufPointer++;
	}

}