package network;

import java.util.Arrays;

/** Response returned by a server request. */
public record NetworkResponse(
    boolean success,
    String code,
    String message,
    String[] data
) {
    public NetworkResponse {
        data = data == null ? new String[0] : Arrays.copyOf(data, data.length);
    }

    @Override
    public String[] data() {
        return Arrays.copyOf(data, data.length);
    }

    public static NetworkResponse error(String code, String message) {
        return new NetworkResponse(false, code, message, new String[0]);
    }
}
