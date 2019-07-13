package home.model;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class MessageQueue {

	private static final int REQUEST_TIMEOUT_SECONDS = 60;

	private static final int RESPONSE_TIMEOUT_SECONDS = 20;

	private static MessageQueue instance;

	private LinkedBlockingQueue<Message> requests = new LinkedBlockingQueue<>(1000);

	private LinkedHashMap<String, Message> responses = new LinkedHashMap<String, Message>() {

		private static final long serialVersionUID = 1L;

		@Override
		protected boolean removeEldestEntry(Map.Entry<String, Message> eldest) {
			return size() > 1000;
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

	public boolean request(Message message) {

		System.out.println("MESSAGE QUEUE SIZE = " + requests.size());
		requests.add(message);
		long start = System.currentTimeMillis();
		long now;

		do {
			try {
				TimeUnit.MILLISECONDS.sleep(50);
			} catch (InterruptedException e) { // NOSONAR
				start = 0L;
			}
			now = System.currentTimeMillis();
		} while (!responses.containsKey(message.getUid())
				&& (now - start < RESPONSE_TIMEOUT_SECONDS * 1000L));

		return responses.remove(message.getUid()) != null;
	}

	public Message pollMessage() {
		try {
			return requests.poll(REQUEST_TIMEOUT_SECONDS, TimeUnit.SECONDS);
		} catch (InterruptedException e) { // NOSONAR
			return null;
		}
	}

	public void addResponse(Message message) {
		responses.put(message.getUid(), message);
	}

}
