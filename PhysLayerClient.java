import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.HashMap;
import java.lang.StringBuilder;

public class PhysLayerClient {
	private static HashMap<String, Integer> table4B5B = new HashMap<String, Integer>();
	private static String[] signals = new String[320];
	private static byte[] decodedBytes = new byte[32];
	private static int[] sentBytes = new int[320];
	private static double baseline;

	public static void main(String[] args) {
		try (Socket socket = new Socket("18.221.102.182", 38002)) {
			InputStream is = socket.getInputStream();
			OutputStream os = socket.getOutputStream();
			System.out.println("Connected to server.");

			baseline = calculateBaseline(is);
			System.out.println("Baseline established from preamble: " + baseline);

			initialize4B5Btable();
			getGeneratedData(is);
			getSignal(320);

			String NRZI = decodeNRZI();
			reverseTranslate(NRZI);

			//print decoded message
			printDecodedBytes();
			sendDecodedBytes(os);

			//server response
			int response = is.read();
			if (response == 1) {
				System.out.println("Response good.");
			} else {
				System.out.println("Reponse bad.");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static void sendDecodedBytes(OutputStream os) throws IOException {
		for (int i = 0; i < decodedBytes.length; i++) {
			os.write(decodedBytes[i]);
		}
	}

	private static void printDecodedBytes() {
		System.out.print("Received 32 bytes: ");
		for (int i = 0; i < decodedBytes.length; i++) {
				System.out.printf("%02X", decodedBytes[i]);
			}
			System.out.println();
	}

	private static void initialize4B5Btable() {
		table4B5B.put("11110", 0);
		table4B5B.put("01001", 1);
		table4B5B.put("10100", 2);
		table4B5B.put("10101", 3);
		table4B5B.put("01010", 4);
		table4B5B.put("01011", 5);
		table4B5B.put("01110", 6);
		table4B5B.put("01111", 7);
		table4B5B.put("10010", 8);
		table4B5B.put("10011", 9);
		table4B5B.put("10110", 10);
		table4B5B.put("10111", 11);
		table4B5B.put("11010", 12);
		table4B5B.put("11011", 13);
		table4B5B.put("11100", 14);
		table4B5B.put("11101", 15);
	}

	private static double calculateBaseline(InputStream inputStream) throws IOException {
		double avg = 0;
		double val;
		for (int i = 0; i < 64; i++) {
			val = (double) inputStream.read();
			avg += val;
		}

		avg /= 64;
		return avg;
	}

	private static void getGeneratedData(InputStream inputStream) throws IOException {
		int val = 0;
		for (int i = 0; i < 320; i++) {
			val = (int) inputStream.read();
			sentBytes[i] = val;
		}
	}

	private static void getSignal(int signal) {
		int val;
		for (int i = 0; i < signal; i++) {
			val = sentBytes[i];

			if (val > baseline) {
				signals[i] = "H";
			} else {
				signals[i] = "L";
			}
		}
	}

	private static void reverseTranslate(String NRZI) {
		String upperByte = "";
		String lowerByte = "";
		int upperIndex = 0;
		int lowerIndex = 5;

		for (int i = 0; i < 32; i++) {
			while (upperIndex < (5 * ((i * 2) + 1))) {
				upperByte += NRZI.charAt(upperIndex);
				upperIndex++;
			}
			upperIndex += 5;

			while (lowerIndex < (10 * (i + 1))) {
				lowerByte += NRZI.charAt(lowerIndex);
				lowerIndex++;
			}
			lowerIndex += 5;

			int upperBits = table4B5B.get(upperByte);
			int lowerBits = table4B5B.get(lowerByte);

			decodedBytes[i] = (byte) (((upperBits << 4) | lowerBits) & 0xFF);

			upperByte = "";
			lowerByte = "";
		}
	}

	private static String decodeNRZI() {
		StringBuilder dec = new StringBuilder();
		if (signals[0].equals("L")) {
			dec.append("0");
		} else {
			dec.append("1");
		}

		for (int i = 1; i < 320; i++) {
			if (signals[i].equals("L")) {
				if (signals[i - 1].equals("L")) {
					dec.append("0");
				} else if (signals[i - 1].equals("H")) {
					dec.append("1");
				}
			} else if (signals[i].equals("H")) {
				if (signals[i - 1].equals("L")) {
					dec.append("1");
				} else if (signals[i - 1].equals("H")) {
					dec.append("0");
				}
			}
		}
		return dec.toString();
	}

}