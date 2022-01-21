package eu.knowledge.engine.smartconnector.runtime.messaging;

import java.io.IOException;
import java.net.Socket;

public class NetUtils {
	public static boolean portAvailable(int port) {
    try (Socket ignored = new Socket("localhost", port)) {
        return false;
    } catch (IOException ignored) {
        return true;
    }
	}
}
