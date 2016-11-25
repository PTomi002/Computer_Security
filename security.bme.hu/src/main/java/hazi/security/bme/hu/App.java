package hazi.security.bme.hu;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Optional;

/**
 * Hello world!
 *
 */
public class App {
	public static void main(String[] args) {
		String ip = args[0];
		String port = args[1];
		String input = "";

		try (final TelnetConnection conn = new TelnetConnection(ip, Integer.parseInt(port))) {
			if (!conn.connect()) {
				return;
			}

			BufferedReader br = null;
			try {
				br = new BufferedReader(new InputStreamReader(System.in));
				// conn.read();
				conn.read();
				while ((input = br.readLine()) != "QUIT") {
					if (input.contains("QUIT")) {
						return;
					}
					conn.send(input); 
					conn.read();
				}
			} finally {
				Optional.ofNullable(br).ifPresent((b) -> {
					try {
						b.close();
					} catch (IOException e) {
						System.err.println("Exception happened in the MAIN!");
						e.printStackTrace();
					}
				});
			}
		} catch (Exception e) {
			System.err.println("Exception happened in the MAIN!");
			e.printStackTrace();
		}
	}
}
