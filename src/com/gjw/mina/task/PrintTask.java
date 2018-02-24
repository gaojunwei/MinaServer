package com.gjw.mina.task;


import com.gjw.mina.handler.HandlerEvent;

public class PrintTask implements Runnable{
	@Override
	public void run() {
		while(true)
		{
			HandlerEvent.getInstance().printMap();
			try {
				Thread.sleep(5*1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
}
