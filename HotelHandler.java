package listeners.messages;

import com.slack.api.app_backend.events.payload.EventsApiPayload;
import com.slack.api.bolt.App;
import com.slack.api.bolt.context.builtin.EventContext;
import com.slack.api.bolt.handler.BoltEventHandler;
import com.slack.api.bolt.response.Response;
import com.slack.api.model.event.MessageEvent;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import services.HotelService;

public class HotelHandler implements BoltEventHandler<MessageEvent> {

    private static final Logger logger = LoggerFactory.getLogger(HotelHandler.class);

    private static final Pattern HOTEL_PATTERN =
            Pattern.compile("\\b(stay|hotel|motel|hotels|motels)\\b", Pattern.CASE_INSENSITIVE);

    private final App app;

    // HashMap to track conversation state
    private final HashMap<String, Boolean> awaitingCityName = new HashMap<>();

    public HotelHandler(App app) {
        this.app = app;
    }

    @Override
    public Response apply(EventsApiPayload<MessageEvent> payload, EventContext ctx) {
        this.app.executorService().submit(() -> {
            try {
                var event = payload.getEvent();
                var message = event.getText();
                String userId = event.getUser();
                String botUserId = ctx.getBotUserId();
                System.out.println("**************************");

                if (message.contains("<@" + botUserId + ">")) {
                    logger.info("Message directed to bot: " + message);

                    message = message.replace("<@" + botUserId + ">", "").trim();

                    if (awaitingCityName.getOrDefault(userId, false)) {
                        String cityName = message.trim();
                        String hotelData = HotelService.getHotelsForCity(cityName);
                        ctx.say("Here are some hotels in " + cityName + ":\n" + hotelData);
                        awaitingCityName.put(userId, false);
                        return;
                    } else {
                        Matcher matcher = HOTEL_PATTERN.matcher(message);
                        boolean isMatch = matcher.find();
                        logger.info("*******************" + isMatch);
                        if (isMatch) {
                            logger.info("Hotel pattern matched for user " + userId);
                            String promptMessage = "What city are you looking for hotels in?";
                            ctx.say(promptMessage);
                            awaitingCityName.put(userId, true);
                        } else {
                            logger.info("Hotel pattern did not match for user " + userId);
                        }
                    }
                }
            } catch (Exception e) {
                logger.error("Error in HotelHandler", e);
            }
        });

        return Response.ok();
    }
}
