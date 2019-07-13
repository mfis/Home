package home.model;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import homecontroller.util.HomeAppConstants;

public class MessageQueue {

	private static MessageQueue instance;

	private LinkedBlockingQueue<Message> requests = new LinkedBlockingQueue<>(1000);

	private LinkedHashMap<String, Message> responses = new LinkedHashMap<String, Message>() {

		private static final long serialVersionUID = 1L;

		@Override
		protected boolean removeEldestEntry(Map.Entry<String, Message> eldest) {
			return size() > 50;
		}
	};

	private MessageQueue() {
		super();
	}

	public static synchronized MessageQueue getInstance() {
		if (instance == null) {
			instance = new MessageQueue();
		}
		return instance;
	}

	public boolean request(Message message, boolean waitForResponse) {

		System.out.println(
				"MESSAGE QUEUE SIZE = " + requests.size() + "  RESPONSE QUEUE SIZE = " + responses.size());
		requests.add(message);

		if (!waitForResponse) {
			return false;
		}

		long start = System.currentTimeMillis();
		long now;

		do {
			try {
				TimeUnit.MILLISECONDS.sleep(50);
			} catch (InterruptedException e) { // NOSONAR
				start = 0L;
			}
			now = System.currentTimeMillis();
		} while (!responses.containsKey(message.getUid()) && (now
				- start < HomeAppConstants.CONTROLLER_CLIENT_LONGPOLLING_RESPONSE_TIMEOUT_SECONDS * 1000L));

		Message removed = responses.remove(message.getUid());
		return removed != null && removed.isSuccessfullExecuted();
	}

	public Message pollMessage() {
		try {
			return requests.poll(HomeAppConstants.CONTROLLER_CLIENT_LONGPOLLING_REQUEST_TIMEOUT_SECONDS,
					TimeUnit.SECONDS);
		} catch (InterruptedException e) { // NOSONAR
			return null;
		}
	}

	public void addResponse(Message message) {
		responses.put(message.getUid(), message);
	}

}
