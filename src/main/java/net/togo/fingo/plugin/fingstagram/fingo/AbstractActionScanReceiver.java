package net.togo.fingo.plugin.fingstagram.fingo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import java.util.HashMap;

import fingo.plugin.IExternalFingoAction;

public abstract class AbstractActionScanReceiver extends BroadcastReceiver
		implements IExternalFingoAction {

	protected Context context;

	@Override
	public void onReceive(Context context, Intent intent) {
		this.context = context;
		sendBroadcast(this);
	}

	private void sendBroadcast(IExternalFingoAction action) {
		Intent i = new Intent(Intent.ACTION_MAIN);

		i.setAction(IExternalFingoAction.INSTALLED_FINGO_ACTION);
		i.putExtra(Key.RESOURCE.name(), action.getIcon());
		i.putExtra(Key.PKG_NAME.name(),
				action.getPackageName());
		i.putExtra(Key.CLASS_NAME.name(),
				action.getClassName());
		i.putExtra(Key.TYPE.name(), action.getType()
				.ordinal());
		i.putExtra(Key.SUBJECT.name(), action.getSubject());
		i.putExtra(Key.DESCRIPTION.name(),
				action.getDescription());
		i.putExtra(Key.FOR_DEV.name(),
				action.getDescription());

		HashMap<State, String> icons = getIcons();
		for (State key : icons.keySet()) {
			i.putExtra(key.name(), icons.get(key));
		}
		context.sendBroadcast(i);
	}

}
