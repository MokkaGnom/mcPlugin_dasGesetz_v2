package utility;

import java.util.List;

public class ErrorMessage
{
    public static final List<String> COMMAND_NO_OPTION_AVAILABLE = List.of("");
    public static final ErrorMessage NO_ERROR = new ErrorMessage("Success");
    public static final ErrorMessage NOT_A_PLAYER = new ErrorMessage("You are not a player!");
    public static final ErrorMessage UNKNOWN_PLUGIN = new ErrorMessage("Ungültiges Plugin");
    public static final ErrorMessage UNKNOWN_SYNTAX = new ErrorMessage("Ungültiger Syntax");

    private final String message;

    public ErrorMessage(String message)
    {
        this.message = message;
    }

    public String getMessage()
    {
        return message;
    }
}
