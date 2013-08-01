package com.totsp.sync;

import java.io.IOException;
import java.util.logging.Level;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;

import com.google.api.client.extensions.android2.AndroidHttp;
import com.google.api.client.googleapis.GoogleHeaders;
import com.google.api.client.googleapis.extensions.android2.auth.GoogleAccountManager;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpResponseException;
import com.google.api.client.http.HttpTransport;

public class DocsTestActivity extends Activity {

	private static Level LOGGING_LEVEL = Level.CONFIG;

	private static final String AUTH_TOKEN_TYPE = "writely";

	private static final String TAG = "Docs";

	private static final int MENU_ADD = 0;

	private static final int MENU_ACCOUNTS = 1;

	private static final int CONTEXT_EDIT = 0;

	private static final int CONTEXT_DELETE = 1;

	private static final int REQUEST_AUTHENTICATE = 0;

	private static final String PREF = "2";

	private static final int DIALOG_ACCOUNTS = 0;

	private final HttpTransport transport = AndroidHttp
			.newCompatibleTransport();

	private String authToken;

	private String postLink;

	GoogleAccountManager accountManager;

	private GoogleHeaders headers = new GoogleHeaders();
	
	@Override
	  public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    accountManager = new GoogleAccountManager(this);
//	    Logger.getLogger().setLevel(LOGGING_LEVEL);
//	    client = new PicasaClient(transport.createRequestFactory(clientLogin));
//	    client.setApplicationName("Google-PicasaSample/1.0");
//	    getListView().setTextFilterEnabled(true);
//	    registerForContextMenu(getListView());
//	    Intent intent = getIntent();
//	    if (Intent.ACTION_SEND.equals(intent.getAction())) {
//	      sendData = new SendData(intent, getContentResolver());
//	    } else if (Intent.ACTION_MAIN.equals(getIntent().getAction())) {
//	      sendData = null;
//	    }
	    gotAccount(false);
	  }

	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case DIALOG_ACCOUNTS:
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle("Select a Google account");
			final AccountManager manager = AccountManager.get(this);
			final Account[] accounts = manager.getAccountsByType("com.google");
			final int size = accounts.length;
			String[] names = new String[size];
			for (int i = 0; i < size; i++) {
				names[i] = accounts[i].name;
			}
			builder.setItems(names, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					gotAccount(manager, accounts[which]);
				}
			});
			return builder.create();
		}
		return null;
	}

	private void gotAccount(boolean tokenExpired) {
		SharedPreferences settings = getSharedPreferences(PREF, 0);
		String accountName = settings.getString("accountName", null);
		if (accountName != null) {
			AccountManager manager = AccountManager.get(this);
			Account[] accounts = manager.getAccountsByType("com.google");
			int size = accounts.length;
			for (int i = 0; i < size; i++) {
				Account account = accounts[i];
				if (accountName.equals(account.name)) {
					if (tokenExpired) {
						manager.invalidateAuthToken("com.google",
								this.authToken);
					}
					gotAccount(manager, account);
					return;
				}
			}
		}
		showDialog(DIALOG_ACCOUNTS);
	}

	private void gotAccount(final AccountManager manager, final Account account) {
		SharedPreferences settings = getSharedPreferences(PREF, 0);
		SharedPreferences.Editor editor = settings.edit();
		editor.putString("accountName", account.name);
		editor.commit();
		new Thread() {

			@Override
			public void run() {
				try {
					final Bundle bundle = manager.getAuthToken(account,
							AUTH_TOKEN_TYPE, true, null, null).getResult();
					runOnUiThread(new Runnable() {

						public void run() {
							try {
								if (bundle
										.containsKey(AccountManager.KEY_INTENT)) {
									Intent intent = bundle
											.getParcelable(AccountManager.KEY_INTENT);
									int flags = intent.getFlags();
									flags &= ~Intent.FLAG_ACTIVITY_NEW_TASK;
									intent.setFlags(flags);
									startActivityForResult(intent,
											REQUEST_AUTHENTICATE);
								} else if (bundle
										.containsKey(AccountManager.KEY_AUTHTOKEN)) {
									authenticatedClientLogin(bundle
											.getString(AccountManager.KEY_AUTHTOKEN));
								}
							} catch (Exception e) {
								handleException(e);
							}
						}
					});
				} catch (Exception e) {
					handleException(e);
				}
			}
		}.start();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		switch (requestCode) {
		case REQUEST_AUTHENTICATE:
			if (resultCode == RESULT_OK) {
				gotAccount(false);
			} else {
				showDialog(DIALOG_ACCOUNTS);
			}
			break;
		}
	}

	private void authenticatedClientLogin(String authToken) {
		this.authToken = authToken;
		headers.setGoogleLogin(authToken);
		authenticated();
	}

	private void authenticated() {
		System.out.println("Done.");
		// if (sendData != null) {
		// try {
		// if (sendData.fileName != null) {
		// boolean success = false;
		// try {
		// InputStreamContent content = new InputStreamContent(
		// sendData.contentType,
		// getContentResolver().openInputStream(sendData.uri));
		// content.setLength(sendData.contentLength);
		// PicasaUrl url =
		// PicasaUrl.relativeToRoot("feed/api/user/default/albumid/default");
		// client.executeInsertPhotoEntry(url, content, sendData.fileName);
		// success = true;
		// } catch (IOException e) {
		// handleException(e);
		// }
		// setListAdapter(new ArrayAdapter<String>(
		// this, android.R.layout.simple_list_item_1, new String[] {success ?
		// "OK" : "ERROR"}));
		// }
		// } finally {
		// sendData = null;
		// }
		// } else {
		// executeRefreshAlbums();
		// }
	}

	void handleException(Exception e) {
		e.printStackTrace();
		if (e instanceof HttpResponseException) {
			HttpResponse response = ((HttpResponseException) e).getResponse();
			int statusCode = response.getStatusCode();
			try {
				response.ignore();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			if (statusCode == 401 || statusCode == 403) {
				gotAccount(true);
				return;
			}
			try {
				Log.e(TAG, response.parseAsString());
			} catch (IOException parseException) {
				parseException.printStackTrace();
			}
		}
		Log.e(TAG, e.getMessage(), e);
	}

}
