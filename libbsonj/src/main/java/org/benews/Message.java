package org.benews;

/**
 * Created by zad on 16/02/15.
 */
public class Message {
	private final int what;
	private final Object obj;

	public Message(int messagePostProgress, Object progressAsyncTaskResult) {
		this.what = messagePostProgress;
		this.obj = progressAsyncTaskResult;
	}
}
