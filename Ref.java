package Vouchy;

import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.List;

final class Ref {
	protected final static String TOKEN = "INSERT TOKEN HERE";
	protected final static String ADDRESS = "jdbc:mysql://localhost:3306/vouchytestdb";
	protected final static String USERNAME = "root";
	protected final static String PASSWORD = "PASSWORD";
	final static boolean DEVMODE = true;
	final static String PREFIX = "$";
	
	/*
	 * Returns a list of strings that are found within a single string broken apart using the
	 * delimiter.
	 */
	protected static List<String> parse(String message, String delimiter) {
		int index = 0;
		int delimIndex = -1;
		ArrayList<String> list = new ArrayList<String>();
		do {
			delimIndex = message.indexOf(delimiter, index);
			if(delimIndex!=-1)
				list.add(message.substring(index, delimIndex));
			else
				list.add(message.substring(index, message.length()));
			index = delimIndex+delimiter.length();
		}while(delimIndex>=0);
		return list;
	}
	
	/*
	 * Connects every string in the provided list with a space starting at the beginIndex
	 */
	protected static String connect(List<String> strings,int beginIndex) {
		String str = "";
		for(int i = beginIndex;i<strings.size();i++)
			str+=strings.get(i)+" ";
		if(str.equals(""))
			return "NULL";
		return str;
	}
}
