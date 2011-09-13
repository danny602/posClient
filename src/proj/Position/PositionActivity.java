package proj.Position;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.MalformedURLException;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;

import ntu.com.google.zxing.client.android.R;
import android.R.string;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.text.style.SuperscriptSpan;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.TextView;

public class PositionActivity extends Activity {

	private MyWebView mapView;
	private Button scan;

	private TextView pointTitle;
	private String jsonURL;
	private String mapID;
	private String mapVer;
	private String pointID;
	private String title;

	private Thread thread;
	private ProgressDialog waitDownDialog;

	// public enum mapKey {
	// mapID, mapVer, title, map
	// }

	// Use for download handler message code
	private interface messageCode {
		public static final int DOWNLOAD_OK = 0;
		public static final int DOWNLOAD_FAILED = 1;
	}

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		findViews();
		setListeners();
		makeRootDir();
		startScan();
		// downloadMapJson(mapID);
		// Download.downImg();
		// showImage();// For test showing map without scan (just press return)

	}

	private void findViews() {
		pointTitle = (TextView) findViewById(R.id.plainText);

		mapView = (MyWebView) findViewById(R.id.myWebView1);
		// Enable to zoom in/out
		mapView.getSettings().setBuiltInZoomControls(true);
		// Enable to zoom in/out by double tap
		mapView.getSettings().setUseWideViewPort(true);

		scan = (Button) findViewById(R.id.scan);
	}

	private void setListeners() {
		scan.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				startScan();
			}
		});
	}

	private void startScan() {
		Intent intent = new Intent("ntu.com.google.zxing.client.android.SCAN");
		intent.setPackage("ntu.com.google.zxing.client.android");
		intent.putExtra("SCAN_MODE", "QR_CODE_MODE");
		startActivityForResult(intent, 0);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent intent) {

		if (requestCode == 0) {
			if (resultCode == RESULT_OK) {
				String contents = intent.getStringExtra("SCAN_RESULT");
				String format = intent.getStringExtra("SCAN_RESULT_FORMAT");
				Boolean isNeedUpdate;

				try {
					String[] contentArray = contents.split("\\?");
					String[] contentPart = contentArray[1].split("&");

					// Assign value
					jsonURL = contents;

					String[] tmp = contentPart[0].split("=");
					mapID = tmp[1];
					tmp = contentPart[1].split("=");
					mapVer = tmp[1];
					tmp = contentPart[2].split("=");
					pointID = tmp[1];
					tmp = contentPart[3].split("=");
					title = tmp[1];

					// Show title
					pointTitle.setText(title);

					// Check whether it need to download Json file
					isNeedUpdate = !checkMapVerUpdate(mapID, mapVer);

					// Need to Update
					if (isNeedUpdate) {
						waitDownDialog = new ProgressDialog(
								PositionActivity.this);
						waitDownDialog.setTitle("Downloading...");
						waitDownDialog.setMessage("Please wait!");
						waitDownDialog.show();
						// Start download Json file if need
						thread = new Thread(new Runnable() {
							// @Override
							public void run() {
								// super.run();
								Log.e("test", "yo");

								try {
									// Test URL (real = jsonURL)
									downloadMapJson(mapID,
											"http://dl.dropbox.com/u/22034772/example.json");

									// Test only download map img
									File SD = Environment
											.getExternalStorageDirectory();
									File test = new File(SD
											+ "/Position/last.json");

									FileReader in = new FileReader(test);
									BufferedReader stdin = new BufferedReader(
											in);

									String jsonString = "";
									String jsonString1 = null;
									while (((jsonString1 = stdin.readLine()) != null)) {
										jsonString = jsonString + jsonString1;
									}
									in.close();

									JSONObject jsonObj = new JSONObject(
											jsonString);
									String map = jsonObj.getString("map");
									DownloadHelper downloader = new DownloadHelper();
									downloader.downFile(map, Global.MapDirRoot
											+ "/" + mapID, "map.jpg");

									handler.sendEmptyMessage(messageCode.DOWNLOAD_OK);
								} catch (MalformedURLException e) {
									e.printStackTrace();
								} catch (IOException e) {
									e.printStackTrace();
									handler.sendEmptyMessage(messageCode.DOWNLOAD_FAILED);
								} catch (JSONException e) {
									e.printStackTrace();
								}
							}
						});
						thread.start();
					} else {
						showMapData(mapID);
					}

				} catch (Exception e) {
					// Handle when scan QR code which is not our form
					pointTitle.setText("QR code error!");
				}

				// Handle successful scan
			} else if (resultCode == RESULT_CANCELED) {
				// Handle cancel

				// Show AlertDialog for temporarily handle (Need to show last
				// map)
				Builder scanCancelDialog = new AlertDialog.Builder(
						PositionActivity.this);
				scanCancelDialog.setMessage("Scan canceled");
				scanCancelDialog.setTitle("Scan canceled").setPositiveButton(
						"OK", new DialogInterface.OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								// TODO Auto-generated method stub

							}
						});
				scanCancelDialog.show();

				// Show last map data
				showLastMapData();

			}
		}
	}

	private void makeRootDir() {
		// Make root dir if not in SD card yet

		Boolean isExistent;
		LookHelper looker = new LookHelper();
		isExistent = looker.look(Global.SDPathRoot, Global.MapDirName);

		if (!isExistent) {
			MakeDirHelper maker = new MakeDirHelper();
			maker.make(Global.SDPathRoot, Global.MapDirName);
		}

	}

	private Boolean searchMapDir(String mapId) {
		// Check whether the Map dir exists or not

		LookHelper looker = new LookHelper();

		return looker.look(Global.MapDirRoot, mapId);
	}

	private void downloadMapJson(String mapId, String downHttpUrl)
			throws IOException {
		// Download the JSON file

		DownloadHelper downloader = new DownloadHelper();

		File SDCardRoot = Environment.getExternalStorageDirectory();
		MakeDirHelper def = new MakeDirHelper();
		def.make(Global.MapDirRoot, mapId);

		downloader.downFile(downHttpUrl, Global.MapDirRoot + "/" + mapId, mapId
				+ ".json");

		// Copy last downloaded JSON file in to map dir root
		File abc = new File(SDCardRoot + "/" + Global.MapDirRoot + "/" + mapId
				+ "/" + mapId + ".json");
		File dstFile = new File(SDCardRoot + "/" + Global.MapDirRoot + "/"
				+ "last.json");

		BufferedInputStream in = new BufferedInputStream(new FileInputStream(
				abc));
		BufferedOutputStream out = new BufferedOutputStream(
				new FileOutputStream(dstFile));

		byte[] tmp = new byte[1024];

		while (in.read(tmp) != -1) {
			out.write(tmp);
		}
		in.close();
		out.close();

	}

	private Boolean checkMapVerUpdate(String mapId, String mapVer) {
		// Return False when it need to be updated
		return false;
	}

	private Boolean checkOldMapData(String MapId) {
		// Return true if there is old map data
		LookHelper look = new LookHelper();
		return look.look(Global.MapDirRoot, MapId);
	}

	private void showMapData(String mapId) {

		// Enable to zoom in/out

		mapView.getSettings().setBuiltInZoomControls(true);
		// Enable to zoom in/out by double tap
		mapView.getSettings().setUseWideViewPort(true);

		// Bind the image path
		// String data =
		// "<img src = \"file:///android_res/drawable/abc.png\" />";
		// String data =
		// "<img src = \"http:///indoorposition.appspot.com/image/map.jpg\" />";

		// String data = "<img src = \"file:///sdcard/somefile.jpg\" />";
		String data = "<img src = \"file:///sdcard/" + Global.MapDirName + "/"
				+ mapId + "/" + "map.jpg" + "\" />";

		final String mimeType = "text/html";
		final String encoding = "utf-8";

		// Set map id
		Global.MapId = mapId;
		// Show map IMG
		mapView.loadDataWithBaseURL("about:blank", data, mimeType, encoding, "");
		// Update the touch points data (touchevent and onDraw method)
		mapView.invalidate();

	}

	private void showLastMapData() {
		// Show last Map data if it exists

		File SDCardRoot = Environment.getExternalStorageDirectory();
		File lastJson = new File(SDCardRoot + "/" + "Position" + "/"
				+ "last.json");
		FileReader in;

		try {
			in = new FileReader(lastJson);
			BufferedReader stdin = new BufferedReader(in);

			String jsonString = "";
			String jsonString1 = null;
			while (((jsonString1 = stdin.readLine()) != null)) {
				jsonString = jsonString + jsonString1;
			}
			in.close();

			JSONObject jsonObj = new JSONObject(jsonString);
			String mapId = jsonObj.getString("mapID");

			showMapData(mapId);

		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			pointTitle.setText("No last map data!");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			pointTitle.setText("No last map data!");
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			pointTitle.setText("No last map data!");
		}

		pointTitle.setText("Last map data!");

	}

	private Handler handler = new Handler() {
		Boolean isOldMapData;

		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case messageCode.DOWNLOAD_OK:
				// Show map data when download finished
				showMapData(mapID);
				waitDownDialog.dismiss();

				break;
			case messageCode.DOWNLOAD_FAILED:
				// Show failed dialog when download failed
				waitDownDialog.dismiss();
				Builder gg = new AlertDialog.Builder(PositionActivity.this);
				gg.setMessage("Download failed!");
				gg.setTitle("Download failed!").setPositiveButton("OK",
						new DialogInterface.OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog,
									int which) {

							}
						});
				gg.show();
				break;
			}

			// Show old map data if it exist otherwise show last map
			isOldMapData = checkOldMapData(mapID);
			if (isOldMapData) {
				showMapData(mapID);
			} else {
				showLastMapData();
			}

			super.handleMessage(msg);
		}
	};

}