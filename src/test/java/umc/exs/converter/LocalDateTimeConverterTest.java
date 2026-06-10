package umc.exs.converter;

import java.time.LocalDateTime;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

public class LocalDateTimeConverterTest {
	@Test
	public void convertToEntityAttribute() {
		LocalDateTimeConverter l = new LocalDateTimeConverter();
		String dbData = "abc";
		LocalDateTime expected = null;
		LocalDateTime actual = l.convertToEntityAttribute(dbData);

		assertEquals(expected, actual);
	}
}
