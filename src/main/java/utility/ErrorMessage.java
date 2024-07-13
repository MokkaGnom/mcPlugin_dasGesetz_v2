package utility;

import java.util.List;

public record ErrorMessage(String message)
{
    public static final List<String> COMMAND_NO_OPTION_AVAILABLE = List.of("");
    public static final ErrorMessage NO_ERROR = new ErrorMessage("Success!");
    public static final ErrorMessage EMPTY_LIST = new ErrorMessage("Empty List!");
    public static final ErrorMessage NOT_A_PLAYER = new ErrorMessage("You are not a player!");
    public static final ErrorMessage UNKNOWN_PLUGIN = new ErrorMessage("Unknown Plugin!");
    public static final ErrorMessage UNKNOWN_SYNTAX = new ErrorMessage("Unknown Syntax!");
    public static final ErrorMessage UNKNOWN_ARGUMENT = new ErrorMessage("Unknown Argument!");
    public static final ErrorMessage NO_PERMISSION = new ErrorMessage("No Permission!");
}
