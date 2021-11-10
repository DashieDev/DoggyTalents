package doggytalents;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ChopinLogger {
    private static final Logger LOGGER = LogManager.getLogger("chopin");
    public static void l(String s) {
        ChopinLogger.LOGGER.info(s); 
    }
}
