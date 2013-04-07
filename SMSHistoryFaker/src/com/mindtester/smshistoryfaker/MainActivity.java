package com.mindtester.smshistoryfaker;

import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentProviderClient;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.RemoteException;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds;
import android.provider.ContactsContract.Data;
import android.text.format.Time;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TimePicker;
import android.widget.Toast;

public class MainActivity extends Activity
{

	EditText editText_ToFrom;
	EditText editText_Message;
	Button Button_AddToHistory;
	DatePicker datePicker1;
	TimePicker timePicker1;
	CheckBox checkbox_MarkRead;
	Spinner spinner_Box;
	ImageButton searchButton;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		editText_ToFrom = (EditText) this.findViewById(R.id.editText_ToFrom);
		editText_Message = (EditText) this.findViewById(R.id.editText_Message);
		Button_AddToHistory = (Button) this
				.findViewById(R.id.button_AddToHistory);
		datePicker1 = (DatePicker) this.findViewById(R.id.datePicker1);
		timePicker1 = (TimePicker) this.findViewById(R.id.timePicker1);
		checkbox_MarkRead = (CheckBox) this
				.findViewById(R.id.checkBox_MarkRead);
		spinner_Box = (Spinner) this.findViewById(R.id.spinner_Box);
		searchButton = (ImageButton) this
				.findViewById(R.id.imageButton_SearchContacts);
		searchButton.setOnClickListener(ContactsButtonListener);

		String[] Options =
		{ this.getString(R.string.inbox), this.getString(R.string.sent) };

		ArrayAdapter<String> OptionsAdapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_spinner_dropdown_item, Options);

		spinner_Box.setAdapter(OptionsAdapter);
		Button_AddToHistory.setOnClickListener(AddToHistoryListener);

		this.processIntent(this.getIntent());
	}

	@Override
	protected void onNewIntent(Intent intent)
	{
		this.processIntent(intent);
	}

	OnClickListener ContactsButtonListener = new OnClickListener()
	{

		@Override
		public void onClick(View v)
		{
			Intent i = new Intent(Intent.ACTION_PICK,
					ContactsContract.Contacts.CONTENT_URI);

			Intent in = new Intent(Intent.ACTION_GET_CONTENT);
			in.setType(ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE);

			startActivityForResult(i, 1001);
		}

	};

	@Override
	public void onActivityResult(int reqCode, int resultCode, Intent data)
	{
		super.onActivityResult(reqCode, resultCode, data);
		if (resultCode == Activity.RESULT_OK)
		{
			switch (reqCode)
			{
			case 1001:
				SelectContact(data);
				break;
			}
			Log.i("ActivityResult", "ReqCode: " + reqCode + " Intent: "
					+ data.getData().toString());
		}

	}

	void SelectContact(Intent ContactData)
	{
		try
		{

			Uri ContactUri = ContactData.getData();
			Cursor C = this.getContentResolver().query(
					Data.CONTENT_URI,
					new String[]
					{ ContactsContract.CommonDataKinds.Phone.NUMBER, },
					Data.CONTACT_ID + "=? " + "AND " + Data.MIMETYPE + "='"
							+ CommonDataKinds.Phone.CONTENT_ITEM_TYPE + "' ",
					new String[]
					{ ContactUri.getLastPathSegment() }

					, null);
			if (C.getCount() > 0)
			{
				if (C.moveToFirst())
				{

					if (C.getCount() == 1)
					{
						editText_ToFrom.setText(C.getString(0));
					} else
					{

						final List<String> Numbers = new ArrayList<String>();

						do
						{
							boolean Found = false;
							for (String n : Numbers)
							{
								String CompString = C.getString(0)
										.toUpperCase().replace("-", "")
										.replace("(", "").replace(")", "")
										.replace("+", "");

								String Comp2 = n.toUpperCase().replace("-", "")
										.replace("(", "").replace(")", "")
										.replace("+", "");

								if (CompString.startsWith("1"))
									CompString = CompString.substring(1);

								if (Comp2.startsWith("1"))
								{
									Comp2 = Comp2.substring(1);
								}

								if (CompString.toUpperCase().trim()
										.compareTo(Comp2.toUpperCase().trim()) == 0)
									Found = true;
								Log.i("ComparingAdd", CompString + " == "
										+ Comp2 + " " + Found);
							}
							if (!Found)
								Numbers.add(C.getString(0));

						} while (C.moveToNext());

						AlertDialog.Builder NumSelect = new AlertDialog.Builder(
								this);
						NumSelect.setTitle("Pick a Mobile Number");

						for (Object o : Numbers.toArray())
						{
							Log.i("MultiNumbers", o.toString());
						}

						try

						{
							if (Numbers.size() == 1)
							{
								editText_ToFrom.setText(Numbers.get(0));
							} else
							{

								final ArrayAdapter<String> ma = new ArrayAdapter<String>(
										this,
										android.R.layout.select_dialog_item,
										Numbers);

								NumSelect.setAdapter(ma,
										new DialogInterface.OnClickListener()
										{
											@Override
											public void onClick(
													DialogInterface dialog,
													int which)
											{
												editText_ToFrom.setText(ma
														.getItem(which));
											}
										});

								NumSelect.create().show();
							}
						} catch (Exception ex)
						{
							Log.e("NumberSelect", ex.getMessage());
							ex.printStackTrace();
						}
					}
				}
			} else
			{
				Toast.makeText(this.getBaseContext(),
						"No Mobile Numbers Found", Toast.LENGTH_LONG).show();
				Log.w("FindMobile", "No mobile numbers found");
			}

		} catch (Exception ex)
		{
			Log.e("SelectContact", ex.getMessage());
			ex.printStackTrace();
		}
	}

	View.OnClickListener AddToHistoryListener = new View.OnClickListener()
	{

		@Override
		public void onClick(View v)
		{
			try
			{

				String Address = editText_ToFrom.getText().toString();
				String Message = editText_Message.getText().toString();
				boolean Read = checkbox_MarkRead.isChecked();

				Box WhichBox = Box.Inbox;
				if (spinner_Box.getSelectedItemPosition() == 1)
					WhichBox = Box.Sent;

				Time T = new Time();
				T.set(30, timePicker1.getCurrentMinute(),
						timePicker1.getCurrentHour(),
						datePicker1.getDayOfMonth(), datePicker1.getMonth(),
						datePicker1.getYear());

				MainActivity.this.AddMessage(Address, Message, T, WhichBox,
						Read);

				Toast.makeText(MainActivity.this,
						MainActivity.this.getString(R.string.MessageAdded),
						Toast.LENGTH_LONG).show();

			} catch (Exception ex)
			{
				ex.printStackTrace();
			}
		}
	};

	enum Box
	{
		Inbox, Sent
	}

	void AddMessage(String Address, String Message, Time Sent, Box b,
			boolean read)
	{

		Uri u = null;

		switch (b)
		{
		case Inbox:
			u = Uri.parse("content://sms/inbox");
			break;
		case Sent:
			u = Uri.parse("content://sms/sent");
			break;
		}

		if (u != null)
		{
			Cursor mCursor = this.getContentResolver().query(u, null, null,
					null, null);

			ContentProviderClient P = this.getContentResolver()
					.acquireContentProviderClient(u);

			ContentValues v = new ContentValues();
			v.put("body", Message);
			v.put("read", read);
			v.put("seen", true);
			v.put("address", Address);
			v.put("date", Sent.toMillis(false));
			v.put("date_sent", Sent.toMillis(false));

			try
			{
				int Thread = -1;
				Uri InsertedMessage = P.insert(u, v);
				Log.d("InsertedURI", InsertedMessage.toString());
				Cursor InsertedMessageCursor = this.getContentResolver().query(
						InsertedMessage, null, null, null, null);
				if (InsertedMessageCursor.moveToFirst())
				{
					Thread = InsertedMessageCursor.getInt(InsertedMessageCursor
							.getColumnIndex("thread_id"));
					Uri MessageThread = Uri.parse("content://mms-sms/threadID/"
							+ Thread);
					Log.d("MessageThread", MessageThread.toString());
					Intent V = new Intent(Intent.ACTION_VIEW);
					V.setData(MessageThread);
					this.startActivity(V);
				}

			} catch (RemoteException e)
			{

				e.printStackTrace();
			}

		}

	}

	void processIntent(Intent intent)
	{
		if (intent == null)
			return;

		if (Intent.ACTION_SENDTO.equals(intent.getAction()))
		{
			String destionationNumber = intent.getDataString();
			destionationNumber = URLDecoder.decode(destionationNumber);
			// clear the string
			destionationNumber = destionationNumber// .replace("-", "")
					.replace("smsto:", "").replace("sms:", "");
			editText_ToFrom.setText(destionationNumber);
		}

	}

}
