// This enum holds the common constants used by both server and client as part of our established
// protocol (synchronize communications between client and server, which are asynchronous)
public enum PacketTypes {
    // These do not exactly follow IETF's RFC 4217, but since this is a custom version of FTPS for
    // PA2, it is still fine and acceptable (some of them are still unused for future purposes)
    FILE_HEADER_PACKET (0),
    FILE_DATA_PACKET (1),
    FILE_DIGEST_PACKET (2),
    PUB_KEY_PACKET (100),
    SESSION_KEY_PACKET (101),
    TEST_MESSAGE_PACKET (102),
    AUTH_LOGIN_USERNAME_PACKET (103),
    AUTH_LOGIN_PASSWORD_PACKET (104),
    VERIFY_SERVER_PACKET (105),
    VERIFY_CLIENT_PACKET (106),
    UPLOAD_FILE_PACKET (200),
    DOWNLOAD_FILE_PACKET (201),
    DELETE_FILE_PACKET (202),
    LIST_DIRECTORY_PACKET (203),
    OK_PACKET (80),
    STOP_PACKET (404),
    ERROR_PACKET (500);

    private final int value;

    PacketTypes(int value) {
        this.value = value;
    }

    // Enums are already static
    public int getValue() {
        return this.value;
    }
}
