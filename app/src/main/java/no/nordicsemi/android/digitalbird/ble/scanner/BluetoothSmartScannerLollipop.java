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

package no.nordicsemi.android.digitalbird.ble.scanner;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.os.Build;
import android.os.Handler;
import android.os.ParcelUuid;

/**
 * @see BluetoothSmartScanner
 */
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class BluetoothSmartScannerLollipop extends ScanCallback implements BluetoothSmartScanner {
	private final ParcelUuid REQUIRED_UUID = new ParcelUuid(ADVERTISING_SERVICE_UUID);
	private final Handler mHandler;
	private Callback mCallback;

	public BluetoothSmartScannerLollipop() {
		mHandler = new Handler();
	}

	/**
	 * On some devices, e.g Nexus 4 and Nexus 7 (2013), the device receives only one advertising packet from connectible devices per scan.
	 * In order to implement 'touch to pair' feature on those devices we need to restart scanning every few seconds.
	 * In consequences the 'touch to pair' feature will work a little bit slower on other devices, but will work somehow on those specified above.
	 */
	private final Runnable restartScanningTask = new Runnable() {
		@Override
		public void run() {
			if (mCallback != null) {
				final BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
				final BluetoothLeScanner scanner = bluetoothAdapter.getBluetoothLeScanner();
				final ScanSettings settings = new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build();

				if (scanner != null) {
					scanner.stopScan(BluetoothSmartScannerLollipop.this);
					scanner.startScan(null, settings, BluetoothSmartScannerLollipop.this);

					mHandler.postDelayed(this, SCANNING_INTERVAL);
				}
			}
		}
	};

	@Override
	public void scan(final Callback callback) {
		mCallback = callback;

		// Start scan. In this class we use the new scanner API from Android Lollipop.
		final BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		final BluetoothLeScanner scanner = bluetoothAdapter.getBluetoothLeScanner();
		final ScanSettings settings = new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build();

		// Start the task that will restart scanning.
		mHandler.postDelayed(restartScanningTask, SCANNING_INTERVAL);

		// Scanner may be null if Bluetooth adapter has been disabled.
		if (scanner != null)
			scanner.startScan(null, settings, this);
	}

	@Override
	public void stop() {
		if (mCallback != null) {
			final BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
			final BluetoothLeScanner scanner = bluetoothAdapter.getBluetoothLeScanner();

			// Scanner may be null if Bluetooth adapter has been disabled.
			if (scanner != null)
				scanner.stopScan(this);
			mHandler.removeCallbacks(restartScanningTask);
			mCallback = null;
		}
	}

	@Override
	public void onScanResult(final int callbackType, final ScanResult result) {
		// Simple implementation of 'Touch to Pair'. We are looking only for devices that are very close, based on RSSI.
		if (result == null || result.getRssi() < REQUIRED_RSSI || result.getScanRecord() == null || result.getScanRecord().getBytes() == null)
			return;

		if (mCallback == null) {
			// This should never happen but it does...
			final BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
			final BluetoothLeScanner scanner = bluetoothAdapter.getBluetoothLeScanner();

			// Scanner may be null if Bluetooth adapter has been disabled.
			if (scanner != null)
				scanner.stopScan(this);
			return;
		}
		if (isMicroBitRemote(result.getScanRecord())) {
			mCallback.onDeviceFound(result.getDevice());
			stop();
		} // else do nothing
	}


	private boolean isMicroBitRemote(final ScanRecord record) {
		return record.getServiceUuids().contains(REQUIRED_UUID);
	}
}