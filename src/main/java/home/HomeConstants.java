package home;

public interface HomeConstants
{
    String MAX_HOMES_JSON_KEY = "MaxHomes";
    String MAX_CREATE_DISTANCE_JSON_KEY = "MaxCreateDistance";

    String ERROR_HOME_EXISTS = "Home \"%s\" already exists";
    String ERROR_HOME_NOT_FOUND = "Home \"%s\" not found";
    String ERROR_MAX_HOME_COUNT = "Reached maximum amount of homes";
    String ERROR_INVALID_TARGET_BLOCK = "Invalid target block";

    String HOME_ADDED = "Home \"%s\" added";
    String HOME_REMOVED = "Home \"%s\" removed";
    String HOME_TELEPORTED = "Teleported to home \"%s\"";

    String HOMES_SAVED = "Homes saved! (P:%d H:%d)";
    String HOMES_LOADED = "Homes loaded! (P:%d H:%d)";
}
