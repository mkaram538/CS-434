package edu.tamu.csce434;


import java.io.IOException;

public class Compiler
{
	private edu.tamu.csce434.Scanner scanner;
	int token;
	int nextRegister = 1;
	java.util.Vector<Integer> ifCondLocation = new java.util.Vector<>();
	java.util.Vector<Integer> elseCondLocation = new java.util.Vector<>();
	java.util.Vector<Integer> whileCondLocation = new java.util.Vector<>();
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
		for (i = 0; i < bufPointer; i++) {
			System.out.print(DLX.disassemble(buf[i]));
		}
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

	public void relOperation(int statementType, int leftRegister, int relationType, int rightRegister) {
		int relOpRegister = nextRegister;
		int operationLocation = bufPointer;
		buf[bufPointer] = DLX.assemble(1, relOpRegister, leftRegister, rightRegister);
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
		int termRegister = 0;
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
			termRegister = nextRegister;
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
		int expressionRegister = 0;
		int leftExpressionRegister = termCheck(activated);
		boolean adding;
		if (scanner.sym != 11 && scanner.sym != 12) {
			return leftExpressionRegister;
		}
		while (scanner.sym == 11 || scanner.sym == 12) {
			adding = scanner.sym == 11;
			scanner.Next();
			int rightExpressionRegister = termCheck(activated);
			expressionRegister = nextRegister;
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

	public void relationCheck(int statementType, boolean activated) {
		int leftRegister;
		int relationType;
		int rightRegister;

		leftRegister = expressionCheck(activated);
		relationType = scanner.sym;
		relOpCheck();
		scanner.Next();
		rightRegister = expressionCheck(activated);
		relOperation(statementType, leftRegister, relationType, rightRegister);
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
				return inputRegister;
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
		relationCheck(0, activated);
		if (scanner.sym != 41) {
			printError(scanner.sym);
		}
		scanner.Next();
		statSequenceCheck(true);

		buf[bufPointer] = DLX.assemble(40, 0, 1);
		elseCondLocation.add(0, bufPointer);
		bufPointer++;

		int ifLocation = ifCondLocation.remove(0);

		DLX.disassem(buf[ifLocation]);

		buf[ifLocation] = DLX.assemble(DLX.op, DLX.a, bufPointer - ifLocation);

		if (scanner.sym == 90) {
			scanner.Next();
			statSequenceCheck(true);
		}

		int elseLocation = elseCondLocation.remove(0);

		buf[elseLocation] = DLX.assemble(40, 0, bufPointer - elseLocation);

		if (scanner.sym != 82) {
			printError(scanner.sym);
		}
		scanner.Next();
	}

	public void whileStatementCheck(boolean activated) {
		scanner.Next();
		relationCheck(1, activated);
		if (scanner.sym != 42) {
			printError(scanner.sym);
		}
		scanner.Next();
		statSequenceCheck(true);

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
			if (scanner.sym == 77 || scanner.sym == 100 || scanner.sym == 101 || scanner.sym == 102) {
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

		for (int i = 0; i < bufPointer; i++) {
			System.out.print(DLX.disassemble(buf[i]));
		}
	}

}