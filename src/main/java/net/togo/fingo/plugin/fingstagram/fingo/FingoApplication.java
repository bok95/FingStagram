package net.togo.fingo.plugin.fingstagram.fingo;

import android.app.Application;
import android.widget.LinearLayout;

import java.util.ArrayList;

import fingo.plugin.IExternalFingoAction.State;

public class FingoApplication extends Application {
	private ArrayList<String> events = new ArrayList<String>();
	private LinearLayout container;
	private State currentState;
	private long waitTime = 0;

	private static class SingletonHolder {
		static final FingoApplication INSTANCE = new FingoApplication();
	}

	public static FingoApplication getInstance() {
		return SingletonHolder.INSTANCE;
	}

	public State getCurrentState() {
		return currentState;
	}

	public void setCurrentState(State currentState) {
		this.currentState = currentState;
	}

	// Add your functions

	public LinearLayout getContainer() {
		return container;
	}

	public void setContainer(LinearLayout container) {
		this.container = container;
	}

}
