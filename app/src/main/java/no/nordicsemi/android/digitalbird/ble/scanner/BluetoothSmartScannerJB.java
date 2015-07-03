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

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.os.Handler;

import java.util.UUID;

/**
 * @see BluetoothSmartScanner
 */
public class BluetoothSmartScannerJB implements BluetoothSmartScanner, BluetoothAdapter.LeScanCallback {
	private final Handler mHandler;
	private Callback mCallback;

	public BluetoothSmartScannerJB() {
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

				bluetoothAdapter.stopLeScan(BluetoothSmartScannerJB.this);
				bluetoothAdapter.startLeScan(BluetoothSmartScannerJB.this);
				mHandler.postDelayed(this, SCANNING_INTERVAL);
			}
		}
	};

	@Override
	public void scan(final Callback callback) {
		mCallback = callback;

		// Start the task that will restart scanning.
		mHandler.postDelayed(restartScanningTask, SCANNING_INTERVAL);

		// Start scan. In this class we use the deprecated API from Android 4.3
		BluetoothAdapter.getDefaultAdapter().startLeScan(this);
	}

	@Override
	public void stop() {
		if (mCallback != null) {
			BluetoothAdapter.getDefaultAdapter().stopLeScan(this);
			mHandler.removeCallbacks(restartScanningTask);
			mCallback = null;
		}
	}

	@Override
	public void onLeScan(final BluetoothDevice device, final int rssi, final byte[] scanRecord) {
		// Simple implementation of 'Touch to Pair'. We are looking only for devices that are very close, based on RSSI.
		if (rssi < REQUIRED_RSSI || scanRecord == null)
			return;

		if (isMicroBitRemote(scanRecord)) {
			mCallback.onDeviceFound(device);
			stop();
		} // else do nothing
	}

	private boolean isMicroBitRemote(final byte[] scanRecord) {
		for (int i = 0; i < scanRecord.length; ++i) {
			final int length = scanRecord[i];
			final int type = scanRecord[i + 1] & 0xFF;

			if (type == GAP_COMPLETE_LIST_OR_128_BIT_SERVICE_CLASS_UUID) {
				// Decode the service UUID
				final int part0 = decodeUuid32(scanRecord, i + 14);
				final int part1 = decodeUuid32(scanRecord, i + 10);
				final int part2 = decodeUuid32(scanRecord, i + 6);
				final int part3 = decodeUuid32(scanRecord, i + 2);
				final UUID uuid = new UUID((((long) part0) << 32) + (part1 & 0xFFFFFFFFL), (((long) part2) << 32) + (part3 & 0xFFFFFFFFL));

				if (ADVERTISING_SERVICE_UUID.equals(uuid))
					return true;
			}
			i += length;
		}
		return false;
	}

	private static int decodeUuid32(final byte[] data, final int start) {
		final int b1 = data[start] & 0xff;
		final int b2 = data[start + 1] & 0xff;
		final int b3 = data[start + 2] & 0xff;
		final int b4 = data[start + 3] & 0xff;

		return b4 << 24 | b3 << 16 | b2 << 8 | b1;
	}
}
