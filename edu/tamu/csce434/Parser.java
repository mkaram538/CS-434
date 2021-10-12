package edu.tamu.csce434;


import java.io.FileReader;
import java.io.IOException;

public class Parser
{
	private Scanner scanner;
	private int token;
	public FileReader inputFile;
	public java.util.Map<String, Integer> identMap = new java.util.HashMap<>();
	
	// Use this function to print errors, i is symbol/token value
	private void printError(int i)
	{
		System.out.println("error");
		System.exit(0);
	}
	
	
	// Constructor of your Parser
	public Parser(String args[])
	{
		if (args.length != 2)
		{
			System.out.println("Usage: java Parser testFileToScan dataFileToRead");
			System.exit(-1);
		}

		scanner = new Scanner(args[0]);

		try {
			inputFile = new FileReader(args[1]);
		} catch (IOException e) {
			e.printStackTrace();
		}
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

	public static void main(String[] args) 
	{
		Parser p = new Parser(args);
		p.computation();
	}

	public void relOpCheck() {
		if (scanner.sym < 20 || scanner.sym > 25) {
			printError(scanner.sym);
		}
	}

	public boolean relOperation(int leftValue, int relationType, int rightValue) {
		switch (relationType) {
			case 20:
				return leftValue == rightValue;
			case 21:
				return leftValue != rightValue;
			case 22:
				return leftValue < rightValue;
			case 23:
				return leftValue >= rightValue;
			case 24:
				return leftValue <= rightValue;
			case 25:
				return leftValue > rightValue;
		}
		return false;
	}

	public int factorCheck(boolean activated) {
		int factorValue = 0;
		if (scanner.sym == 50) {
			scanner.Next();
			factorValue = expressionCheck(activated);
			if (scanner.sym != 35) {
				printError(scanner.sym);
			}
		} else if (scanner.sym == 60) {
			factorValue = scanner.val;
		} else if (scanner.sym == 61) {
			factorValue = identMap.get(scanner.Id2String(scanner.id));
		} else if (scanner.sym == 100) {
			factorValue = funcCallCheck(activated);
		} else {
			printError(scanner.sym);
		}
		return factorValue;
	}

	public int termCheck(boolean activated) {
		int termValue = factorCheck(activated);
		boolean multiplying;
		scanner.Next();
		while (scanner.sym == 1 || scanner.sym == 2) {
			multiplying = scanner.sym == 1;
			scanner.Next();
			if (multiplying) {
				termValue = termValue * factorCheck(activated);
			} else {
				termValue = termValue / factorCheck(activated);
			}
			scanner.Next();
		}
		return termValue;
	}

	public int expressionCheck(boolean activated) {
		int expressionValue = termCheck(activated);
		boolean adding;
		while (scanner.sym == 11 || scanner.sym == 12) {
			adding = scanner.sym == 11;
			scanner.Next();
			if (adding) {
				expressionValue = expressionValue + termCheck(activated);
			} else {
				expressionValue = expressionValue - termCheck(activated);
			}
		}
		return expressionValue;
	}

	public boolean relationCheck(boolean activated) {
		int leftValue;
		int relationType;
		int rightValue;

		leftValue = expressionCheck(activated);
		relationType = scanner.sym;
		relOpCheck();
		scanner.Next();
		rightValue = expressionCheck(activated);
		return relOperation(leftValue, relationType, rightValue);
	}

	public void assignmentCheck(boolean activated) {
		scanner.Next();
		if (scanner.sym != 61) {
			printError(scanner.sym);
		}
		String currentIdent = scanner.Id2String(scanner.id);
		expect("<-");
		scanner.Next();
		int value = expressionCheck(activated);
		if (activated) {
			identMap.put(currentIdent, value);
		}
	}

	public int funcCallCheck(boolean activated) {
		scanner.Next();
		if (scanner.sym != 61) {
			printError(scanner.sym);
		}
		String funcType = scanner.Id2String(scanner.id);
		int funcValue = 0;

		if (funcType.equals("inputnum")) {
			scanner.Next();
			if (scanner.sym == 50) {
				expect(")");
			}
			if (activated) {
				return fileRead();
			}
			return 0;
		} else {
			expect("(");
			scanner.Next();
			if (scanner.sym != 35) {
				funcValue = expressionCheck(activated);
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
						System.out.print(funcValue);
						break;
					case "inputnum":
						return fileRead();
					case "outputnewline":
						System.out.print("\n");
						break;
				}
			}
			scanner.Next();
		}
		return 0;
	}

	public int fileRead() {
		String currentInt = "";
		int currentChar = 0;

		try {
			currentChar = inputFile.read();
		} catch (IOException e) {
			e.printStackTrace();
		}

		while (currentChar < 48 || currentChar > 57) {
			try {
				currentChar = inputFile.read();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		while (currentChar >= 48 && currentChar <= 57) {
			currentInt += (char)currentChar;
			try {
				currentChar = inputFile.read();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		return Integer.parseInt(currentInt);
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
		identMap.put(currentIdent, 0);
		scanner.Next();
		while (scanner.sym == 31) {
			scanner.Next();
			if (scanner.sym != 61) {
				printError(scanner.sym);
			}
			currentIdent = scanner.Id2String(scanner.id);
			identMap.put(currentIdent, 0);
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
	}
}