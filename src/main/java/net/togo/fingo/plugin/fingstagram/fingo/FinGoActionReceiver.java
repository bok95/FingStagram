package net.togo.fingo.plugin.fingstagram.fingo;

import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import net.togo.fingo.plugin.fingstagram.FingstagramService;
import net.togo.fingo.plugin.fingstagram.FloatingWindowService;


public class FinGoActionReceiver extends AbstractActionReceiver {

	@Override
	public void action1() {
        Toast.makeText(context, "action 1", Toast.LENGTH_SHORT).show();
        context.startService(new Intent(context, FloatingWindowService.class));
	}

	@Override
	public void action2() {
        Toast.makeText(context, "action 2", Toast.LENGTH_SHORT).show();
        context.stopService(new Intent(context, FloatingWindowService.class));
    }

	@Override
	public void action3() {
        Toast.makeText(context, "action 3", Toast.LENGTH_SHORT).show();
	}

	@Override
	protected String getClassName() {
		return StarterActivity.class.getName();
	}
}
