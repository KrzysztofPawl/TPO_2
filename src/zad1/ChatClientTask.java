/**
 * @author Pawłowski Krzysztof S22381
 */

package zad1;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

public class ChatClientTask extends FutureTask<ChatClient> {

    private final ChatClient client;

    public ChatClientTask(Callable<ChatClient> callable, ChatClient client) {
        super(callable);
        this.client = client;
    }

    public static ChatClientTask create(ChatClient c, List<String> msgs, int wait) {
        Callable<ChatClient> taskLogic = () -> {
            c.login();
            threadSleep(wait);

            for (String message : msgs) {
                c.send(message);
                threadSleep(wait);
            }

            c.logout();
            threadSleep(wait);
            return c;
        };

        return new ChatClientTask(taskLogic, c);
    }

    private static void threadSleep(int wait) throws InterruptedException {
        if (wait > 0) {
            Thread.sleep(wait);
        }
    }

    public ChatClient getClient() {
        return this.client;
    }
}