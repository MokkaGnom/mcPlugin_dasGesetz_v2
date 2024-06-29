package manager;

import java.util.List;

public interface OutputStrings
{
    List<String> COMMAND_NO_OPTION_AVAILABLE = List.of("");

    interface ERROR
    {
        String UNKNOWN_PLUGIN = "Ungültiges Plugin";
        String UNKNOWN_SYNTAX = "Ungültiger Syntax";
    }


}
