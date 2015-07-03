/*************************************************************************************************************************************************
 * Copyright (c) 2015, Nordic Semiconductor
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors may be used to endorse or promote products derived from this
 * software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 ************************************************************************************************************************************************/

package no.nordicsemi.android.digitalbird;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import no.nordicsemi.android.digitalbird.ble.DigitalBirdBleManager;
import no.nordicsemi.android.digitalbird.ble.DigitalBirdBleManagerCallbacks;
import no.nordicsemi.android.digitalbird.ble.scanner.BluetoothSmartScannerFactory;
import no.nordicsemi.android.digitalbird.ble.scanner.BluetoothSmartScanner;
import no.nordicsemi.android.digitalbird.game.GameEngine;

public class MainActivity extends AppCompatActivity implements GameEngine.GameListener, BluetoothSmartScanner.Callback, DigitalBirdBleManagerCallbacks {
	private final static String TAG = "MainActivity";

	private final static String PREFS_BEST_SCORE = "best_score";

	private final static int STATE_SCANNING = 0;
	private final static int STATE_CONNECTING = 1;
	private final static int STATE_VALIDATING = 2;
	private final static int STATE_STARTING = 3;
	private final static int STATE_STARTED = 4;

	private GameEngine mGameEngine;
	private DigitalBirdBleManager mBleManager;
	private BluetoothSmartScanner mScanner;

	private SurfaceView mSurfaceView;
	private View mInfoView;
	private View mInfoTouchToPairView;
	private View mInfoProgressScanningView;
	private View mInfoProgressScanningOKView;
	private View mInfoProgressConnectingView;
	private View mInfoProgressConnectingOKView;
	private View mInfoProgressValidatingView;
	private View mInfoProgressValidatingOKView;
	private View mInfoProgressStartingView;
	private View mInfoProgressStartingOKView;
	private View mBluetoothDisabledView;
	private View mGameOverView;
	private TextView mTotalScoreView;
	private TextView mBestScoreView;
	private TextView mScoreView;
	private TextView mScoreShadowView;

	/**
	 * This Broadcast Receiver will listen for Bluetooth state changes. It will also toggle the Bluetooth Disabled view.
	 */
	private final BroadcastReceiver mBluetoothStateBroadcastReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(final Context context, final Intent intent) {
			final int newState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_OFF);

			switch (newState) {
				case BluetoothAdapter.STATE_OFF:
					showBluetoothDisabledView();

					if (mGameEngine.isGameStarted())
						mGameEngine.pauseIfStarted();
					break;
				case BluetoothAdapter.STATE_ON:
					startScanningForDevice();
					break;
			}
		}
	};

	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// This flags allow to draw behind the status bar.
		getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
		setContentView(R.layout.activity_main);

		// Obtain view references.
		findViews();

		// Initialize Bluetooth Smart related objects.
		mBleManager = new DigitalBirdBleManager(this);
		mBleManager.setGattCallbacks(this);
		mScanner = BluetoothSmartScannerFactory.getScanner();
		registerReceiver(mBluetoothStateBroadcastReceiver, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));

		// Initialize the game engine.
		final SurfaceView surfaceView = mSurfaceView = (SurfaceView) findViewById(R.id.surface);
		mGameEngine = new GameEngine(surfaceView);
		mGameEngine.setGameListener(this);
	}

	@Override
	protected void onResume() {
		super.onResume();

		if (isBluetoothEnabled()) {
			// Bluetooth is enabled. We may start to scan for a device.
			startScanningForDevice();
		} else {
			// Bluetooth disabled. Just show the Bluetooth disabled view.
			showBluetoothDisabledView();
		}
	}

	@Override
	protected void onPause() {
		super.onPause();

		// Ensure the scanner is stopped.
		mScanner.stop();
	}

	@Override
	public void onBackPressed() {
		super.onBackPressed();

		// Disconnect from the device.
		mBleManager.disconnect();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();

		// Ensure the BLE manager is closed.
		mBleManager.close();

		unregisterReceiver(mBluetoothStateBroadcastReceiver);
	}

	private void findViews() {
		mInfoView = findViewById(R.id.info_intro);
		mInfoTouchToPairView = findViewById(R.id.info_touch_to_pair);
		mInfoProgressScanningView = findViewById(R.id.info_scanning);
		mInfoProgressScanningOKView = findViewById(R.id.info_scanning_ok);
		mInfoProgressConnectingView = findViewById(R.id.info_connecting);
		mInfoProgressConnectingOKView = findViewById(R.id.info_connecting_ok);
		mInfoProgressValidatingView = findViewById(R.id.info_validating);
		mInfoProgressValidatingOKView = findViewById(R.id.info_validating_ok);
		mInfoProgressStartingView = findViewById(R.id.info_starting);
		mInfoProgressStartingOKView = findViewById(R.id.info_starting_ok);
		mBluetoothDisabledView = findViewById(R.id.info_bluetooth_disabled);
		mGameOverView = findViewById(R.id.info_game_over);
		mTotalScoreView = (TextView) findViewById(R.id.total_score);
		mBestScoreView = (TextView) findViewById(R.id.best_score);
		mScoreView = (TextView) findViewById(R.id.score);
		mScoreShadowView = (TextView) findViewById(R.id.score_shadow);
	}

	private void startScanningForDevice() {
		showConnectionInfo(STATE_SCANNING);
		mScanner.scan(this);
	}

	private void showBluetoothDisabledView() {
		// This method is always called from the UI thread.
		mBluetoothDisabledView.setVisibility(View.VISIBLE);
		mScoreView.setVisibility(View.GONE);
		mScoreShadowView.setVisibility(View.GONE);
		mGameOverView.setVisibility(View.GONE);
		mInfoView.setVisibility(View.GONE);
	}

	private void showGameOverView(final int totalScore, final int bestScore) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				mScoreView.setVisibility(View.GONE);
				mScoreShadowView.setVisibility(View.GONE);
				mGameOverView.setVisibility(View.VISIBLE);
				mTotalScoreView.setText(String.valueOf(totalScore));
				mBestScoreView.setText(String.valueOf(bestScore));
			}
		});
	}

	private void showGame() {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				mBluetoothDisabledView.setVisibility(View.GONE);
				mGameOverView.setVisibility(View.GONE);
				mInfoView.setVisibility(View.GONE);

				// The game is ready and waits for player to press the button.
				mGameEngine.ready();
			}
		});
	}

	private void showConnectionInfo(final int status) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				mBluetoothDisabledView.setVisibility(View.GONE);
				mGameOverView.setVisibility(View.GONE);
				mScoreView.setVisibility(View.GONE);
				mScoreShadowView.setVisibility(View.GONE);
				mInfoView.setVisibility(View.VISIBLE);

				mSurfaceView.setKeepScreenOn(status == STATE_STARTED);

				mInfoTouchToPairView.setVisibility(status == STATE_SCANNING ? View.VISIBLE : View.GONE);
				mInfoProgressScanningView.setVisibility(status >= STATE_CONNECTING ? View.VISIBLE : View.GONE);
				mInfoProgressScanningOKView.setVisibility(status >= STATE_CONNECTING ? View.VISIBLE : View.GONE);
				mInfoProgressConnectingView.setVisibility(status >= STATE_CONNECTING ? View.VISIBLE : View.GONE);
				mInfoProgressConnectingOKView.setVisibility(status >= STATE_VALIDATING ? View.VISIBLE : View.GONE);
				mInfoProgressValidatingView.setVisibility(status >= STATE_VALIDATING ? View.VISIBLE : View.GONE);
				mInfoProgressValidatingOKView.setVisibility(status >= STATE_STARTING ? View.VISIBLE : View.GONE);
				mInfoProgressStartingView.setVisibility(status >= STATE_STARTING ? View.VISIBLE : View.GONE);
				mInfoProgressStartingOKView.setVisibility(status == STATE_STARTED ? View.VISIBLE : View.GONE);
			}
		});
	}

	private void showScore(final int points) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				mScoreView.setVisibility(View.VISIBLE);
				mScoreShadowView.setVisibility(View.VISIBLE);
				mScoreView.setText(String.valueOf(points));
				mScoreShadowView.setText(String.valueOf(points));
			}
		});
	}

	@Override
	public void onScore(final int points) {
		showScore(points);
	}

	@Override
	public boolean onGameOver(final int points) {
		final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
		int bestScore = preferences.getInt(PREFS_BEST_SCORE, 0);
		final boolean recordBeaten = points > bestScore;
		if (recordBeaten) {
			bestScore = points;
			preferences.edit().putInt(PREFS_BEST_SCORE, bestScore).apply();
		}

		showGameOverView(points, bestScore);
		return recordBeaten;
	}

	@Override
	public void onDeviceFound(final BluetoothDevice device) {
		// Scanner has been stopped automatically.
		showConnectionInfo(STATE_CONNECTING);

		// In order to work on Samsung S3 the connection must be made in the main thread.
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				mBleManager.connect(device);
			}
		});
	}

	@Override
	public void onButtonPressed() {
		switch (mGameEngine.getGameState()) {
			case STARTED:
				mGameEngine.onButtonClicked();
				break;
			case PAUSED:
				mGameEngine.resumeIfPaused();
				break;
			case OVER:
				showGame();
				break;
			case READY:
				showScore(0);
				mGameEngine.start();
				break;
		}
	}

	@Override
	public void onDeviceConnected() {
		showConnectionInfo(STATE_VALIDATING);
	}

	@Override
	public void onDeviceDisconnecting() {
		// not implemented
	}

	@Override
	public void onDeviceDisconnected() {
		if (!isFinishing()) {
			mGameEngine.reset();
			startScanningForDevice();
		}
	}

	@Override
	public void onLinklossOccur() {
		// not implemented. As we connect with autoConnect = false we will never get this callback (see BleManager), but rather onDeviceDisconnected.
	}

	@Override
	public void onServicesDiscovered(boolean optionalServicesFound) {
		showConnectionInfo(STATE_STARTING);
	}

	@Override
	public void onDeviceReady() {
		showConnectionInfo(STATE_STARTED);
		showGame();
	}

	@Override
	public void onBatteryValueReceived(final int value) {
		Log.i(TAG, "Battery lever read: " + value);
		// not implemented
	}

	@Override
	public void onBondingRequired() {
		// not implemented
	}

	@Override
	public void onBonded() {
		// not implemented
	}

	@Override
	public void onError(final String message, final int errorCode) {
		// This method is called from the Bluetooth thread. In order to show anything on the UI we need to use the UI thread.
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
			}
		});
	}

	@Override
	public void onDeviceNotSupported() {
		// This method is called from the Bluetooth thread. In order to show anything on the UI we need to use the UI thread.
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				Toast.makeText(MainActivity.this, R.string.error_device_not_supported, Toast.LENGTH_SHORT).show();
			}
		});

		startScanningForDevice();
	}

	private boolean isBluetoothEnabled() {
		return BluetoothAdapter.getDefaultAdapter().isEnabled();
	}
}
