package edu.tamu.csce434;

import java.io.IOException;
import java.io.LineNumberReader;
import java.io.FileReader;
import java.util.List;
import java.util.Vector;


public class Scanner {
    public int sym; // current token on the input
    public int currentChar;
    public int val; // value of last number encountered
    public int id;  // index of last identifier encountered
    public FileReader codeFile;
    public java.util.Map<String, Integer> tokenMap;
    public Vector<String> identMap;
    
	public void closefile() {
        try {
            codeFile.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
	}

	/** 
	 * Advance to the next token 
	 */
    public void Next() {
        int charType = charTypeCheck(currentChar);

        switch (charType) {
            case 0: Advance();
                    break;
            case 1: intHandler();
                    break;
            case 2: charHandler();
                    break;
            case 3: sym = 255;
                    break;
            case 4: symbolHandler();
                    break;
            case 5: commentCheck();
                    break;
        }
	}
  
    /**
     * Move to next char in the input
     */
	public void Advance() {
        try {
            currentChar = codeFile.read();
        } catch (IOException e) {
            e.printStackTrace();
        }
	    Next();
	}

	public int charTypeCheck(int charValue) {
	    if (charValue >= 0 && charValue <= 32) {
	        return 0;
        } else if (charValue >= 48 && charValue <= 57) {
	        return 1;
        } else if (charValue >= 97 && charValue <= 122) {
	        return 2;
        } else if (charValue < 0) {
            return 3;
        } else if (charValue == 47) {
	        return 5;
        }
	    return 4;
    }

    public void intHandler() {
	    String currentIntToken = "";
	    currentIntToken += (char)currentChar;

        try {
            currentChar = codeFile.read();
        } catch (IOException e) {
            e.printStackTrace();
        }
	    while (charTypeCheck(currentChar) == 1) {
            currentIntToken += (char)currentChar;
            try {
                currentChar = codeFile.read();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

	    sym = 60;
	    val = Integer.parseInt(currentIntToken);
    }

    public void charHandler() {
        String currentToken = "";
        currentToken += (char)currentChar;
        int charType;
        boolean tokenFound = false;

        while (!tokenFound) {
            try {
                currentChar = codeFile.read();
            } catch (IOException e) {
                e.printStackTrace();
            }
            charType = charTypeCheck(currentChar);

            if (tokenMap.containsKey(currentToken)) {
                sym = String2Id(currentToken);
                tokenFound = true;
            } else if (charType == 1 || charType == 2) {
                currentToken += (char)currentChar;
            }  else {
                identMap.add(currentToken);
                id = identMap.size() - 1;
                sym = 61;
                tokenFound = true;
            }
        };
    }

    public void symbolHandler() {
        String currentToken = "";
        currentToken += (char)currentChar;
        int charType;
        boolean foundError = false;

        while (!tokenMap.containsKey(currentToken)) {
            try {
                currentChar = codeFile.read();
            } catch (IOException e) {
                e.printStackTrace();
            }
            charType = charTypeCheck(currentChar);
            if (charType == 4) {
                currentToken += (char)currentChar;
            } else {
                foundError = true;
                sym = 0;
                break;
            }
        }

        if (!foundError) {
            String confirmedToken = currentToken;

            while (tokenMap.containsKey(currentToken)) {
                confirmedToken = currentToken;
                try {
                    currentChar = codeFile.read();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                currentToken += (char)currentChar;
            }

            sym = String2Id(confirmedToken);
        }
    }

    public void commentCheck() {
        try {
            currentChar = codeFile.read();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (currentChar == 47) {
            while (currentChar != 10 && currentChar != 13 && currentChar != -1) {
                try {
                    currentChar = codeFile.read();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (currentChar == -1) {
                sym = 255;
            } else {
                Advance();
            }
        } else {
            sym = 2;
        }
    }

    public Scanner(String fileName) {
	    mapSetup();
	    try {
            codeFile = new FileReader(fileName);
        } catch (IOException e) {
	        e.printStackTrace();
        }
        Advance();
    }

    /**
     * Converts given id to name; returns null in case of error
     */
    public String Id2String(int id) {
        return identMap.get(id);
    }

    /**
     * Signal an error message
     * 
     */
    public void Error(String errorMsg) {
        System.out.println(errorMsg);
    }

    /**
     * Converts given name to id; returns -1 in case of error
     */
    public int String2Id(String name) {
        if (!tokenMap.containsKey(name)) {
            return -1;
        }
        return tokenMap.get(name);
    }

    public void mapSetup() {
        identMap = new java.util.Vector<>();

        tokenMap = new java.util.HashMap<>();
        tokenMap.put("error", 0);
        tokenMap.put("*", 1);
        tokenMap.put("/", 2);
        tokenMap.put("+", 11);
        tokenMap.put("-", 12);
        tokenMap.put("==", 20);
        tokenMap.put("!=", 21);
        tokenMap.put("<", 22);
        tokenMap.put(">=", 23);
        tokenMap.put("<=", 24);
        tokenMap.put(">", 25);
        tokenMap.put(".", 30);
        tokenMap.put(",", 31);
        tokenMap.put("[", 32);
        tokenMap.put("]", 34);
        tokenMap.put(")", 35);
        tokenMap.put("<-", 40);
        tokenMap.put("then", 41);
        // tokenMap.put("do", 42);
        tokenMap.put("(", 50);
        tokenMap.put(";", 70);
        tokenMap.put("let", 77);
        tokenMap.put("}", 80);
        tokenMap.put("od", 81);
        tokenMap.put("fi", 82);
        tokenMap.put("else", 90);
        tokenMap.put("call", 100);
        tokenMap.put("if", 101);
        tokenMap.put("while", 102);
        // tokenMap.put("return", 103);
        tokenMap.put("var", 110);
        // tokenMap.put("array", 111);
        // tokenMap.put("function", 112);
        // tokenMap.put("procedure", 113);
        tokenMap.put("{", 150);
        tokenMap.put("main", 200);
    }
}

