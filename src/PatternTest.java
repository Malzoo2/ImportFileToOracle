import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PatternTest {
	private static final String COMMA_PATTERN = "(\\,|\\r?\\n|\\r|^)(?:\"([^\"]*(?:\"\"[^\"]*)*)\"|([^\"\\,\\r\\n]*))";
	public static void main(String[] args) {
		String line = ",BM,LBBCMP00115PPM,Capital,Northern Planning,LBBCMP00115,CMP,Rule Based,25-Dec-15,,,Released,6,25-Dec-15,NORTHERN,,";
		
		

			
			Matcher matcher = Pattern.compile(COMMA_PATTERN).matcher(line);
		
			while (matcher.find()) {
					System.out.println(matcher.group(0));
			}
			
	}
}
