package utility;

import manager.language.LocalizedString;

import java.util.*;

public enum ErrorMessage
{
    UNKNOWN_ERROR("Unknown error!"),
    UNKNOWN_SYNTAX("Unknown Syntax!"),
    UNKNOWN_ARGUMENT("Unknown Argument!"),
    UNKNOWN_PLUGIN("Unknown Plugin!"),
    NO_PERMISSION("No Permission!"),
    NOT_A_PLAYER("You are not a player!"),
    PLAYER_NOT_FOUND("Player not found!"),
    EMPTY_LIST("Empty List!");

    ErrorMessage(String message) {
        this.message = message;
        this.localizedMessage = new LocalizedString(Map.of(Locale.ENGLISH, message));
    }

    public static final List<String> COMMAND_NO_OPTION_AVAILABLE = List.of("");

    private final String message;
    private LocalizedString localizedMessage;

    public String getMessage() {
        return message;
    }

    public void setLocalizedString(LocalizedString localizedMessage) {
        this.localizedMessage = localizedMessage;
    }

    public LocalizedString getLocalizedMessage() {
        return this.localizedMessage;
    }
}
