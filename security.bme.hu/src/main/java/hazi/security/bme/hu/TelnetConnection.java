package hazi.security.bme.hu;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.time.Duration;
import java.util.Optional;

public class TelnetConnection implements AutoCloseable {
	private Socket socket;
	private BufferedReader reader;
	private ByteArrayOutputStream writer;
	private InetSocketAddress host;

	public TelnetConnection(String ip, int port) {
		try {
			this.host = new InetSocketAddress(ip, port);
		} catch (Exception e) {
			System.err.println("Invalid host: " + ip + ":" + port + "!");
			e.printStackTrace();
		}
	}

	public boolean connect() {
		try {
			socket = new Socket(host.getAddress(), host.getPort());
			socket.setSoTimeout((int) Duration.ofMillis(300).toMillis());
			return true;
		} catch (Exception e) {
			System.err.println("Could not connect to: " + getAddress() + ":" + getPort() + "!");
			e.printStackTrace();
		}
		return false;
	}

	public String read() {
		String result = "";
		int size = Integer.MAX_VALUE;
		try {
			char[] array = new char[1024];
			reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			while ((size = reader.read(array)) != -1) {
				result += String.valueOf(array, 0, size);
			}
		} catch (SocketTimeoutException e) {
			System.out.println("There is no more data from socket!");
		} catch (Exception e) {
			System.err.println("Could not read from socket!");
			e.printStackTrace();
		}

		System.out.println("READ FROM SOCKET: " + System.lineSeparator() + result);
		return result;
	}

	public void send(String stringMessage) {
		System.out.println("SENDING MESSAGE: " + stringMessage);

		try {
			final int messageLength = stringMessage.getBytes().length;
			writer = new ByteArrayOutputStream();
			// 4 unsigned int
			byte[] prefixArray = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(messageLength).array();
			printBitRepresentation("PREFIX", prefixArray);
			byte[] messageArray = stringMessage.getBytes();
			printBitRepresentation("MESSAGE", messageArray);
			
			writer.write(prefixArray);
			writer.write(messageArray);
			writer.writeTo(socket.getOutputStream());
			writer.flush();
		} catch (Exception e) {
			System.err.println("Could not send message: " + stringMessage + "!");
			e.printStackTrace();
		}
	}

	private void printBitRepresentation(String message, byte[] byteArray) {
		final StringBuilder buffer = new StringBuilder(message + System.lineSeparator());
		for (int i = 0; i < byteArray.length; i++) {
//			convert byte array to binary string representation than get the last 8 character
			buffer.append(("0000000" + Integer.toBinaryString(byteArray[i])).replaceAll(".*(.{8})$", "$1") + " ");
		}
		System.out.println(buffer.toString());
	}

	public int getPort() {
		return Optional.ofNullable(host).map((h) -> h.getPort()).orElseGet(() -> {
			System.err.println("Host is null, returning -1!");
			return -1;
		});
	}

	public InetAddress getAddress() {
		return Optional.ofNullable(host).map((h) -> h.getAddress()).orElseGet(() -> {
			System.err.println("Host is null, returning null!");
			return null;
		});
	}

	@Override
	public void close() throws Exception {
		Optional.ofNullable(socket).ifPresent((sock) -> {
			try {
				System.out.println("Closing socket");
				socket.close();
				System.out.println("Closing socket OK");
			} catch (IOException e) {
				System.err.println("Could not close socket!");
				e.printStackTrace();
			}
		});
	}
}
