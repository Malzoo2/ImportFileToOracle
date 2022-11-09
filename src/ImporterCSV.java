
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.nio.charset.StandardCharsets;
import java.util.Map.Entry;
import java.util.regex.*;
import java.util.stream.*;

public class ImporterCSV {

	private static List<CharSequence> rows = new ArrayList<CharSequence>();
	private static Map<String, String> filesWithTables = new HashMap<String, String>(10);
	private static Path currentPath = Paths.get("CSV");
	private static Path insertFilePath = Paths.get("insert.sql");
	private static String headers, values = null;
	private static String dateString = "TO_DATE('%s', '%s')";

	private static final String DATE_REGEX_DAY_MON = "^([1-9]|([012][0-9])|(3[01]))\\/([0]{0,1}[1-9]|1[012])\\/\\d\\d\\d\\d [012]{0,1}[0-3]:[0-5][0-9]:[0-5][0-9]$";
	private static final String DATE_REGEX_MON_DAY = "^([0]{0,1}[1-9]|1[012])\\/([1-9]|([012][0-9])|(3[01]))\\/\\d\\d\\d\\d [012]{0,1}[0-3]:[0-5][0-9]:[0-5][0-9]$";
	private static final String COMMA_PATTERN = "(\\,|\\r?\\n|\\r|^)(?:\"([^\"]*(?:\"\"[^\"]*)*)\"|([^\"\\,\\r\\n]*))";

	public static void main(String[] args) {
		rows.add("SET DEFINE OFF;");
		setStaticFileVars();

		for (Entry<String, String> s : filesWithTables.entrySet()) {
			try {
				addTruncateTables(s);
				printFiles(s);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		rows.add("COMMIT;");
		
		try {
			Files.deleteIfExists(insertFilePath);
			Files.write(insertFilePath, rows, StandardCharsets.UTF_8, StandardOpenOption.CREATE);			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static String adjustLastCommaLine(String line) {
		if (line.lastIndexOf(',') + 1 == line.length() && line.length() != 0)
			return adjustLastCommaLine(line.substring(0, line.length() - 1));
		else
			return line;
	}

	private static void addTruncateTables(Entry<String, String> fileEntry) {
		rows.add("COMMIT;");
		rows.add("TRUNCATE TABLE XXPHCC." + fileEntry.getValue() + ";");
	}

	private static void printFiles(Entry<String, String> fileEntry) throws IOException {
		Path csvFilePath = currentPath.resolve(Paths.get(fileEntry.getKey()));
		List<String> fileLinesList = Files.readAllLines(csvFilePath, StandardCharsets.ISO_8859_1);

		String firstRow = fileLinesList.get(0).toUpperCase();
		firstRow = adjustLastCommaLine(firstRow);
		fileLinesList.remove(0);

		for (String line : fileLinesList) {
			line = adjustLastCommaLine(line);
			adjustHeadersAndValues(firstRow, line);
			getInsertByFile(fileEntry.getValue(), line);
		}
	}

	private static String getInsertByFile(String fileNo, String line) {
		if (line != null && !(line.replace(",", "").isEmpty())) {			
			StringBuilder outValue = new StringBuilder("INSERT INTO XXPHCC.");
			outValue.append(fileNo);
			outValue.append(" (");
			outValue.append(headers);
			outValue.append(") Values (");
			outValue.append(values);
			outValue.append(");");

			// filter special characters			
			filterLineOfSpecialCharachters(outValue, "'\"", "'");
			filterLineOfSpecialCharachters(outValue, "\"'", "'");
			

			rows.add(outValue.toString());

			return outValue.toString();
		}
		return null;
	}

	private static String convertDateStructure(String field) {
		if (field.matches(DATE_REGEX_DAY_MON))
			return String.format(dateString, field, "DD/MM/YYYY HH24:MI:SS");
		else if (field.matches(DATE_REGEX_MON_DAY))
			return String.format(dateString, field, "MM/DD/YYYY HH24:MI:SS");
		else
			return field;
	}

	private static void filterLineOfSpecialCharachters(StringBuilder outValue, String oldChar, String newChar) {
		while (outValue.indexOf(oldChar) != -1)
			outValue.replace(outValue.indexOf(oldChar), outValue.indexOf(oldChar) + oldChar.length(), newChar);
	}

	private static void adjustHeadersAndValues(String firstRow, String line) {

		String[] HeaderElements = firstRow.split(",");
		StringBuilder headerSB = new StringBuilder();
		StringBuilder ValuesSB = new StringBuilder();		
		boolean hasStartComma = line.startsWith(",");
		
		if (hasStartComma) ValuesSB.append("NULL,");
		
		Matcher matcher = Pattern.compile(COMMA_PATTERN).matcher(line);
		int ii = 0;
		String field = "";
		while (matcher.find()) {
			
			if (ii !=0) {
				headerSB.append(",");
				ValuesSB.append(",");
			}
			headerSB.append(HeaderElements[ii]);
			
			field = matcher.group(0);
			if (field.equals(","))
				ValuesSB.append("NULL");
			else {
			field = field.startsWith(",") ? field.substring(1) : field;
			field = convertDateStructure(field);
			ValuesSB.append("'");
			ValuesSB.append(field);
			ValuesSB.append("'");
			}
			ii++;
		}	
		
		if (hasStartComma) headerSB.append(","+HeaderElements[ii]);
		
		headers = headerSB.toString();
		headers = headers.replace("WIP ACCOUNTING CLASS", "WIP_ACCOUNTING_CLASS");
		headers = headers.replace("WARRANTY EXP DATE", "END_DATE");
		headers = headers.replace("ACTIVITY_NAME", "ACTIVTIY_NAME");
		headers = removeStrangeLetters(headers);
		values = ValuesSB.toString();
	}
	
	private static String removeStrangeLetters(String header) {
		if (Character.getNumericValue(header.charAt(0)) == -1)
			return removeStrangeLetters(header.substring(1, header.length() - 1));
		else
			return header;
	}

	private static Set<String> getCurrentFolderFiles() {
		Set<String> files = new HashSet<String>();
		try (Stream<Path> stream = Files.walk(currentPath, 1)) {
			files = stream.filter(file -> !Files.isDirectory(file)).map(Path::getFileName).map(Path::toString)
					.collect(Collectors.toSet());

		} catch (IOException e) {
			System.err.println("Please create csv folder and files.");
		}

		if (files.isEmpty())
			System.err.println("Please add files in csv folder.");

		return files;
	}

	private static void setStaticFileVars() {
		Set<String> files = getCurrentFolderFiles();
		for (String fileName : files) {
			String fileNameLower = fileName.toLowerCase();
			if (fileNameLower.endsWith(".csv")) {
				if (fileNameLower.indexOf("num") != -1)
					filesWithTables.put(fileName, "XX_EAM_ASSET_NUM_STG");
				else if (fileNameLower.indexOf("_area") != -1)
					filesWithTables.put(fileName, "XX_EAM_AREA_STG");
				else if (fileNameLower.indexOf("m_activ") != -1)
					filesWithTables.put(fileName, "XX_EAM_ACTIVITY_STG");
				else if (fileNameLower.indexOf("t_activ") != -1)
					filesWithTables.put(fileName, "XX_EAM_ASSET_ACTIVITY_STG");
				else if (fileNameLower.indexOf("attr") != -1)
					filesWithTables.put(fileName, "XX_EAM_ASSETATTR_VAL_STG");
				else if (fileNameLower.indexOf("_head") != -1)
					filesWithTables.put(fileName, "XX_EAM_PM_SCHEDULE_HEADER_STG");
				else if (fileNameLower.indexOf("_act_") != -1)
					filesWithTables.put(fileName, "XX_EAM_PM_SCHEDULE_ACT_STG");
				else if (fileNameLower.indexOf("rout") != -1)
					filesWithTables.put(fileName, "xx_EAM_MAINT_ROUTING_STG");
			}
		}
	}
}
