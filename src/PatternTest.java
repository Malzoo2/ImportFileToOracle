import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PatternTest {
	private static final String COMMA_PATTERN = "(\\,|\\r?\\n|\\r|^)(?:\"([^\"]*(?:\"\"[^\"]*)*)\"|([^\"\\,\\r\\n]*))";
	public static void main(String[] args) {
		String line = ",KHRCLCT101-PMSCH,NPMLCT-M,,KHRCLCT101,1,30.41,Y,,,";
		
		

			
			Matcher matcher = Pattern.compile(COMMA_PATTERN).matcher(line);
		
			while (matcher.find()) {
					System.out.println(matcher.group(0));
			}
			
	}
}
